package common;

import common.*;
import merrimackutil.json.JsonIO;
import merrimackutil.json.types.JSONObject;
import merrimackutil.util.NonceCache;
import common.service.ClientHello;
import common.service.HandshakeResponse;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.security.SecureRandom;

public class EchoServiceHandler implements Runnable {
    private Channel channel;
    private NonceCache nonceCache;

    public EchoServiceHandler(Channel channel, NonceCache nonceCache) {
        this.channel = channel;
        this.nonceCache = nonceCache;
    }

    @Override
    public void run() {
        try {
            System.out.println("🔓 Receiving ClientHello...");
            JSONObject helloJson = channel.receiveMessage();
            ClientHello hello = new ClientHello("", "");
            hello.deserialize(helloJson);

            // Decode and deserialize the ticket
            System.out.println("🎟️ Parsing Ticket...");
            JSONObject ticketJson = JsonIO.readObject(hello.getTicket());
            Ticket ticket = new Ticket(
                ticketJson.getString("username"),
                ticketJson.getString("service"),
                ticketJson.getLong("validityTime"),
                ticketJson.getString("iv"),
                ticketJson.getString("encryptedSessionKey")
            );
            ticket.setCreationTime(ticketJson.getLong("creationTime"));

            // 🔐 Derive session key from encrypted data in ticket
            String base64Key = ticket.getEncryptedSessionKey();
            String iv = ticket.getIv();
            String password = "servicepass"; // Your known shared password with KDC
            String combined = combineIVandCipher(iv, base64Key);

            String sessionKeyDecoded = CryptoUtils.decryptAESGCM(combined, password);
            byte[] sessionKeyBytes = Base64.getDecoder().decode(sessionKeyDecoded);
            SecretKeySpec ks = new SecretKeySpec(sessionKeyBytes, "AES");

            // 🔐 Encrypt client's nonce (Nc)
            byte[] nonceClient = Base64.getDecoder().decode(hello.getNonce());
            byte[] nonceServer = new byte[16];
            new SecureRandom().nextBytes(nonceServer);
            String base64Ns = Base64.getEncoder().encodeToString(nonceServer);

            byte[] ivBytes = new byte[12];
            new SecureRandom().nextBytes(ivBytes);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(128, ivBytes);
            cipher.init(Cipher.ENCRYPT_MODE, ks, spec);
            byte[] encryptedNonce = cipher.doFinal(nonceClient);

            String ivOut = Base64.getEncoder().encodeToString(ivBytes);
            String encNc = Base64.getEncoder().encodeToString(encryptedNonce);

            // 📤 Send HandshakeResponse
            HandshakeResponse response = new HandshakeResponse(base64Ns, ticket.getService(), ivOut, encNc);
            channel.sendMessage(response);

            // ✅ Next step would be waiting for ClientResponse
            // ... (we’ll implement that next)

        } catch (Exception e) {
            System.err.println("❌ Error in EchoServiceHandler: " + e.getMessage());
            e.printStackTrace();
            channel.close();
        }
    }

    private String combineIVandCipher(String iv, String cipherText) {
        byte[] ivBytes = Base64.getDecoder().decode(iv);
        byte[] cipherBytes = Base64.getDecoder().decode(cipherText);
        byte[] combined = new byte[ivBytes.length + cipherBytes.length];
        System.arraycopy(ivBytes, 0, combined, 0, ivBytes.length);
        System.arraycopy(cipherBytes, 0, combined, ivBytes.length, cipherBytes.length);
        return Base64.getEncoder().encodeToString(combined);
    }
}