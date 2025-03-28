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

import java.security.*;
import java.util.Base64;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class SigningServiceHandler implements Runnable {
    private Channel channel;
    private signature.Config config;
    private RSAPrivateKey privateKey;

    public SigningServiceHandler(Channel channel, NonceCache nonceCache, signature.Config config2, PrivateKey signingKey) {
        this.channel = channel;
        this.config = config2;

        try {
            // Load RSA private key from config (base64 encoded)
            String privateKeyBase64 = config2.signingKey;  // base64-encoded private key from config file
            byte[] decodedKey = Base64.getDecoder().decode(privateKeyBase64);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodedKey);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            this.privateKey = (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
        } catch (Exception e) {
            System.err.println("❌ Error loading private key: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            System.out.println("🔓 Receiving ClientHello...");
            JSONObject helloJson = channel.receiveMessage();
            ClientHello hello = new ClientHello(null, "");
            hello.deserialize(helloJson);

            // 🔐 Session setup (similar to EchoServiceHandler, without nonce handling)

            // 🧾 Step 3: Receive ClientResponse
            System.out.println("📥 Waiting for ClientResponse...");
            JSONObject clientRespJson = channel.receiveMessage();
            ClientResponse clientResp = new ClientResponse("", "", "", "");
            clientResp.deserialize(clientRespJson);

            // ✅ Session handshake verified (simplified here)

            while (true) {
                try {
                    // Step 1: Receive encrypted message from client
                    JSONObject incomingMsg = channel.receiveMessage();
                    String ivBase64 = incomingMsg.getString("iv");
                    String cipherBase64 = incomingMsg.getString("message");

                    byte[] msgIv = Base64.getDecoder().decode(ivBase64);
                    byte[] ciphertext = Base64.getDecoder().decode(cipherBase64);

                    // Step 2: Decrypt the encrypted message
                    // In this signing service, we just need the plain text message
                    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                    GCMParameterSpec spec = new GCMParameterSpec(128, msgIv);
                    cipher.init(Cipher.DECRYPT_MODE, null, spec);  // No session key, since it's a signing service
                    byte[] plainBytes = cipher.doFinal(ciphertext);
                    String decryptedStr = new String(plainBytes, StandardCharsets.UTF_8);

                    // Step 3: Parse decrypted JSON message
                    JSONObject payload = JsonIO.readObject(decryptedStr);
                    String message = payload.getString("message");

                    System.out.println("📥 [SERVICE] Received message: " + message);

                    // Step 4: Sign the SHA-256 hash of the received message
                    byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    byte[] hash = digest.digest(messageBytes);

                    // Sign the hash using the RSA private key
                    Signature signature = Signature.getInstance("SHA256withRSA");
                    signature.initSign(privateKey);
                    signature.update(hash);
                    byte[] signedHash = signature.sign();

                    // Step 5: Send the signature back to the client
                    String base64Signature = Base64.getEncoder().encodeToString(signedHash);

                    // Construct response JSON with the signature
                    JSONObject responseJson = new JSONObject();
                    responseJson.put("signature", base64Signature);
                    channel.sendMessage(responseJson);

                    System.out.println("📤 [SERVICE] Responded with signature: " + base64Signature);

                } catch (Exception e) {
                    System.err.println("❌ [SERVICE] Error in communication loop: " + e.getMessage());
                    e.printStackTrace();
                    channel.close();
                    break;
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Error in SigningServiceHandler: " + e.getMessage());
            e.printStackTrace();
            channel.close();
        }
    }
}
