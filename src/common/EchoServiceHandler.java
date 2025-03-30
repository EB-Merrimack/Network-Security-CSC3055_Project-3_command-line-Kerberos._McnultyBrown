package common;

import merrimackutil.json.JsonIO;
import merrimackutil.json.types.JSONObject;
import merrimackutil.util.NonceCache;
import common.service.ClientHello;
import common.service.ClientResponse;
import common.service.HandshakeResponse;
import echoservice.Config;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.util.Arrays;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
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
            System.out.println("🔑 [SERVICE] Decrypted session key (base64): " + sessionKeyDecoded);
            System.out.println("🔑 [SERVICE] Session key bytes: " + Base64.getEncoder().encodeToString(sessionKeyBytes));
            SecretKeySpec ks = new SecretKeySpec(sessionKeyBytes, "AES");

            // 🔐 Encrypt client's nonce (Nc)
            byte[] nonceClient = Base64.getDecoder().decode(hello.getNonce());
            System.out.println("📥 [SERVICE] Received Nc from client: " + hello.getNonce());
            byte[] nonceServer = new byte[16];
            new SecureRandom().nextBytes(nonceServer);
            String base64Ns = Base64.getEncoder().encodeToString(nonceServer);

            byte[] ivBytes = new byte[12];
            new SecureRandom().nextBytes(ivBytes);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(128, ivBytes);
            cipher.init(Cipher.ENCRYPT_MODE, ks, spec);
            byte[] encryptedNonce = cipher.doFinal(nonceClient);
            System.out.println("📤 [SERVICE] Encrypted Nc (enc(Nc)): " + Base64.getEncoder().encodeToString(encryptedNonce));
            System.out.println("📤 [SERVICE] IV used: " + Base64.getEncoder().encodeToString(ivBytes));

            String ivOut = Base64.getEncoder().encodeToString(ivBytes);
            String encNc = Base64.getEncoder().encodeToString(encryptedNonce);

            // 📤 Send HandshakeResponse
            HandshakeResponse response = new HandshakeResponse(base64Ns, ticket.getService(), ivOut, encNc);
            channel.sendMessage(response);

          // 🧾 Step 3: Receive ClientResponse
System.out.println("📥 Waiting for ClientResponse...");
JSONObject clientRespJson = channel.receiveMessage();
System.out.println("📥 Received ClientResponse JSON: " + clientRespJson.toString());

ClientResponse clientResp = new ClientResponse("", "", "", "");
clientResp.deserialize(clientRespJson);

System.out.println("📥 Parsed ClientResponse: ");
System.out.println("  - IV: " + clientResp.getIv());
System.out.println("  - Encrypted Nonce: " + clientResp.getEncryptedNonce());

// 🔓 Decrypt enc(Ns)
System.out.println("🔐 Decoding IV and Encrypted Nonce...");
byte[] ivBytesResp = Base64.getDecoder().decode(clientResp.getIv());
byte[] encNs = Base64.getDecoder().decode(clientResp.getEncryptedNonce());

System.out.println("🔍 IV Length: " + ivBytesResp.length + " bytes");
System.out.println("🔍 Encrypted Nonce Length: " + encNs.length + " bytes");

Cipher decryptCipher = Cipher.getInstance("AES/GCM/NoPadding");
GCMParameterSpec gcmSpec = new GCMParameterSpec(128, ivBytesResp);

try {
    System.out.println("🔐 Initializing decryption...");
    decryptCipher.init(Cipher.DECRYPT_MODE, ks, gcmSpec);
    
    byte[] decryptedNs = decryptCipher.doFinal(encNs);
    String base64DecryptedNs = Base64.getEncoder().encodeToString(decryptedNs);

    System.out.println("🔓 Decrypted Ns: " + base64DecryptedNs);
    System.out.println("🔓 Original Ns: " + base64Ns);

    // ✅ Verify it matches original Ns
    if (!base64DecryptedNs.equals(base64Ns)) {
        System.err.println("❌ Client failed to prove knowledge of session key.");
        throw new SecurityException("Client failed to prove knowledge of session key.");
    }

    System.out.println("✅ Client handshake verified!");
    System.out.println("🤝 Session established with user: " + clientResp.getClientId());

    // 🧾 Step 4: Receive ClientReques
   // Step 4: Receive ClientRequest and decrypt message
   JSONObject clientReqJson = channel.receiveMessage();
   System.out.println("📥 Received ClientRequest: " + clientReqJson);

   String encryptedMessage = clientReqJson.getString("message");
   byte[] encryptedMessageBytes = Base64.getDecoder().decode(encryptedMessage);
   String ivMessage = clientReqJson.getString("iv");
   byte[] ivMessageBytes = Base64.getDecoder().decode(ivMessage);

   // Step 5: Decrypt the message from the client
   System.out.println("🔐 Decrypting the client message...");
   Cipher decryptMessageCipher = Cipher.getInstance("AES/GCM/NoPadding");
   GCMParameterSpec messageSpec = new GCMParameterSpec(128, ivMessageBytes);

   decryptMessageCipher.init(Cipher.DECRYPT_MODE, ks, messageSpec);
   byte[] decryptedMessageBytes = decryptMessageCipher.doFinal(encryptedMessageBytes);
   String decryptedMessage = new String(decryptedMessageBytes, StandardCharsets.UTF_8);
   System.out.println("🔓 Decrypted Message: " + decryptedMessage);

   // Step 6: Convert the message to uppercase
   String upperCaseMessage = decryptedMessage.toUpperCase();
   System.out.println("🆙 Uppercased Message: " + upperCaseMessage);

   // Step 7: Encrypt the uppercase message before sending back
   byte[] encryptedUppercaseMessage = encryptMessage(upperCaseMessage, ivMessageBytes,ks);

   // Step 8: Prepare response JSON and send it back to the client
   JSONObject echoResponseJson = new JSONObject();
   echoResponseJson.put("iv", Base64.getEncoder().encodeToString(ivMessageBytes));
   echoResponseJson.put("message", Base64.getEncoder().encodeToString(encryptedUppercaseMessage));

   System.out.println("📤 Sending Echo Response: " + echoResponseJson.toString());
   channel.sendMessage(echoResponseJson);

} catch (Exception e) {
    System.err.println("❌ Error decrypting message: " + e.getMessage());
    throw new SecurityException("Error decrypting message: " + e.getMessage());
}

} catch (SecurityException e) {
System.err.println("❌ Security Exception: " + e.getMessage());
throw e; // Re-throw to ensure the exception is properly handled
} catch (Exception e) {
System.err.println("❌ General Exception: " + e.getMessage());
throw new RuntimeException("An error occurred during the handshake process.", e);
}
}


// Combine IV and cipher text for decryption
private String combineIVandCipher(String iv, String cipherText) {
byte[] ivBytes = Base64.getDecoder().decode(iv);
byte[] cipherBytes = Base64.getDecoder().decode(cipherText);
byte[] combined = new byte[ivBytes.length + cipherBytes.length];
System.arraycopy(ivBytes, 0, combined, 0, ivBytes.length);
System.arraycopy(cipherBytes, 0, combined, ivBytes.length, cipherBytes.length);
return Base64.getEncoder().encodeToString(combined);
}

// Encrypt the message using AES-GCM
private byte[] encryptMessage(String message, byte[] iv, SecretKeySpec ks) throws Exception {
Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
GCMParameterSpec spec = new GCMParameterSpec(128, iv);
cipher.init(Cipher.ENCRYPT_MODE, ks, spec);
return cipher.doFinal(message.getBytes(StandardCharsets.UTF_8));
}
}