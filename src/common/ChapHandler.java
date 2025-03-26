package common;

import client.TicketRequest;
import common.Channel;
import common.CryptoUtils;
import common.Ticket;
import merrimackutil.json.types.JSONObject;
import merrimackutil.util.NonceCache;
import protocol.RFC1994Challenge;
import protocol.RFC1994Claim;
import protocol.RFC1994Response;
import protocol.RFC1994Result;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

public class ChapHandler {

    private final Channel channel;
    private final NonceCache nonceCache;
    private final Map<String, String> secrets;

    public ChapHandler(Channel channel, NonceCache nonceCache, Map<String, String> secrets) {
        this.channel = channel;
        this.nonceCache = nonceCache;
        this.secrets = secrets;
    }

    public void run() throws IOException {
        try {
            // === CHAP STEP 1: Receive RFC1994Claim ===
            JSONObject claimJson = channel.receiveMessage();
            RFC1994Claim claim = new RFC1994Claim("");
            claim.deserialize(claimJson);
            String user = claim.getId();

            // === CHAP STEP 2: Validate user ===
            if (!secrets.containsKey(user)) {
                channel.sendMessage(new RFC1994Result(false));
                channel.close();
                return;
            }

            // === CHAP STEP 3: Generate 32-byte nonce ===
            byte[] nonceBytes = new byte[32];
            new SecureRandom().nextBytes(nonceBytes);
            nonceCache.addNonce(nonceBytes);
            String base64Nonce = Base64.getEncoder().encodeToString(nonceBytes);

            // === CHAP STEP 4: Send challenge ===
            channel.sendMessage(new RFC1994Challenge(base64Nonce));

            // === CHAP STEP 5: Receive response ===
            JSONObject responseJson = channel.receiveMessage();
            RFC1994Response response = new RFC1994Response("");
            response.deserialize(responseJson);

            // === CHAP STEP 6: Recalculate expected hash ===
            String sharedSecret = secrets.get(user);
            byte[] expected = MessageDigest.getInstance("SHA-256")
                    .digest((sharedSecret + new String(nonceBytes)).getBytes());
            String expectedBase64 = Base64.getEncoder().encodeToString(expected);

            // === CHAP STEP 7: Validate ===
            boolean valid = expectedBase64.equals(response.getHash());
            channel.sendMessage(new RFC1994Result(valid));

            if (!valid) {
                channel.close();
                return;
            }

            // === TICKET STEP 1: Receive TicketRequest ===
            JSONObject ticketReqJson = channel.receiveMessage();
            TicketRequest ticketReq = new TicketRequest("", "");
            ticketReq.deserialize(ticketReqJson);

            // === TICKET STEP 2: Generate session key (ks) ===
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            SecretKey sessionKey = keyGen.generateKey();
            byte[] sessionKeyBytes = sessionKey.getEncoded();
            String base64SessionKey = Base64.getEncoder().encodeToString(sessionKeyBytes);

            // === TICKET STEP 3: Encrypt session key with root key ===
            String clientPassword = secrets.get(ticketReq.getId());
            String encryptedSessionKey = CryptoUtils.encryptAESGCM(base64SessionKey, clientPassword);

            // Split into IV and ciphertext
            String[] parts = extractEncryptedParts(encryptedSessionKey);
            String base64IV = parts[0];
            String encryptedKeyOnly = parts[1];

            // === TICKET STEP 4: Build Ticket ===
            Ticket ticket = new Ticket(ticketReq.getId(), ticketReq.getService(), 60000L, base64IV, encryptedKeyOnly);

            // === TICKET STEP 5: Send TicketResponse ===
            TicketResponse responseMsg = new TicketResponse(encryptedKeyOnly, ticket);
            channel.sendMessage(responseMsg);

        } catch (Exception e) {
            System.err.println("❌ Error in ChapHandler: " + e.getMessage());
            channel.sendMessage(new RFC1994Result(false));
            channel.close();
        }
    }

    /**
     * Helper method to split encrypted message into IV and ciphertext (both Base64).
     */
    private String[] extractEncryptedParts(String encryptedPayload) {
        byte[] full = Base64.getDecoder().decode(encryptedPayload);
        byte[] iv = new byte[12];
        byte[] encrypted = new byte[full.length - 12];
        System.arraycopy(full, 0, iv, 0, 12);
        System.arraycopy(full, 12, encrypted, 0, encrypted.length);
        return new String[]{
                Base64.getEncoder().encodeToString(iv),
                Base64.getEncoder().encodeToString(encrypted)
        };
    }
}