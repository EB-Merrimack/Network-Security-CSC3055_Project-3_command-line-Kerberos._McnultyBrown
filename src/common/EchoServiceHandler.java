package common;

import merrimackutil.json.JsonIO;
import merrimackutil.json.types.JSONObject;
import merrimackutil.util.NonceCache;
import common.service.ClientHello;
import common.service.ClientResponse;
import common.service.HandshakeResponse;
import echoservice.Config;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.security.SecureRandom;

public class EchoServiceHandler implements Runnable {
    private Channel channel;
    private NonceCache nonceCache;
    private Config config;

    public EchoServiceHandler(Channel channel, NonceCache nonceCache, Config config) {
        this.channel = channel;
        this.nonceCache = nonceCache;
        this.config = config;

    }

    @Override
    public void run() {
        try {
            System.out.println("🔓 Receiving ClientHello...");
            JSONObject helloJson = channel.receiveMessage();
            ClientHello hello = new ClientHello(null, "");
            hello.deserialize(helloJson);

            // ✅ Use the actual JSONObject from hello
            JSONObject ticketJson = hello.getTicket(); // no more JsonIO.readObject

            System.out.println("🎟️ Parsing Ticket...");
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
            String password = config.serviceSecret;            
            String combined = combineIVandCipher(iv, base64Key);

            System.out.println("🔐 Decrypting session key with password: " + password);
            System.out.println("🔐 Combined IV+Cipher: " + combined);
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

            // 🧾 Step 3: Receive ClientResponse
            System.out.println("📥 Waiting for ClientResponse...");
            JSONObject clientRespJson = channel.receiveMessage();
            ClientResponse clientResp = new ClientResponse("", "", "", "");
            clientResp.deserialize(clientRespJson);

            // 🔓 Decrypt enc(Ns)
            System.out.println("🔐 Decrypting client's proof (enc(Ns))...");
            byte[] ivBytesResp = Base64.getDecoder().decode(clientResp.getIv());
            byte[] encNs = Base64.getDecoder().decode(clientResp.getEncryptedNonce());

            Cipher decryptCipher = Cipher.getInstance("AES/GCM/NoPadding");
            decryptCipher.init(Cipher.DECRYPT_MODE, ks, new GCMParameterSpec(128, ivBytesResp));
            byte[] decryptedNs = decryptCipher.doFinal(encNs);
            String base64DecryptedNs = Base64.getEncoder().encodeToString(decryptedNs);

            // ✅ Verify it matches original Ns
            if (!base64DecryptedNs.equals(base64Ns)) {
                throw new SecurityException("❌ Client failed to prove knowledge of session key.");
            }

            System.out.println("✅ Client handshake verified!");
            System.out.println("🤝 Session established with user: " + clientResp.getClientId());

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