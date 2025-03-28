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

            while (true) {
                try {
                    // Step 1: Receive encrypted message from client
                    JSONObject incomingMsg = channel.receiveMessage();
                    System.out.println("📥 [SERVICE] Raw incoming message: " + incomingMsg.toString());
            
                    String ivBase64 = incomingMsg.getString("iv");
                    String cipherBase64 = incomingMsg.getString("message");
            
                    byte[] msgIv = Base64.getDecoder().decode(ivBase64);
                    byte[] ciphertext = Base64.getDecoder().decode(cipherBase64);
            
                    System.out.println("🔍 [DEBUG] Decoded IV: " + Arrays.toString(msgIv));
                    System.out.println("🔍 [DEBUG] Decoded Ciphertext Length: " + ciphertext.length);
            
                    // Step 2: Decrypt with session key
                    try {
                        Cipher decryptMsgCipher = Cipher.getInstance("AES/GCM/NoPadding");
                        GCMParameterSpec decryptSpec = new GCMParameterSpec(128, msgIv);
                        decryptCipher.init(Cipher.DECRYPT_MODE, ks, decryptSpec);
                        byte[] plainBytes = decryptMsgCipher.doFinal(ciphertext);
                        String decryptedStr = new String(plainBytes, StandardCharsets.UTF_8);
            
                        System.out.println("🔓 [DEBUG] Decrypted JSON: " + decryptedStr);
            
                        // Step 3: Parse decrypted JSON message
                        JSONObject payload = JsonIO.readObject(decryptedStr);
                        String receivedNonce = payload.getString("nonce");
                        String sender = payload.getString("user");
                        String targetService = payload.getString("service");
                        String message = payload.getString("message");
            
                        System.out.println("🔍 [DEBUG] Parsed JSON - Nonce: " + receivedNonce);
                        System.out.println("🔍 [DEBUG] Parsed JSON - Sender: " + sender);
                        System.out.println("🔍 [DEBUG] Parsed JSON - Target Service: " + targetService);
                        System.out.println("🔍 [DEBUG] Parsed JSON - Message: " + message);
            
                        if (!targetService.equals(config.serviceName)) {
                            System.err.println("❌ [SERVICE] Message intended for '" + targetService + "', but this is '" + config.serviceName + "'");
                            channel.close();
                            break;
                        }
            
                        // Step 4: Validate nonce
                        byte[] nonceBytes = Base64.getDecoder().decode(receivedNonce);
            
                        if (nonceCache.containsNonce(nonceBytes)) {
                            System.err.println("⚠️ [SERVICE] Replay detected! Nonce has already been used.");
                            channel.close();
                            break;
                        }
            
                        // ✅ Add nonce to cache
                        nonceCache.addNonce(nonceBytes);
                        System.out.println("✅ [SERVICE] Nonce accepted and stored.");
            
                        System.out.println("📥 [SERVICE] Received from " + sender + ": " + message);
            
                        // Step 5: Process message
                        String responseText = message.toUpperCase();
            
                        // Step 6: Encrypt response
                        byte[] responseIv = new byte[12];
                        new SecureRandom().nextBytes(responseIv);
                        Cipher encryptCipher = Cipher.getInstance("AES/GCM/NoPadding");
                        encryptCipher.init(Cipher.ENCRYPT_MODE, ks, new GCMParameterSpec(128, responseIv));
                        byte[] encryptedResponse = encryptCipher.doFinal(responseText.getBytes(StandardCharsets.UTF_8));
            
                        System.out.println("🔐 [DEBUG] Response IV: " + Arrays.toString(responseIv));
                        System.out.println("🔐 [DEBUG] Encrypted Response Length: " + encryptedResponse.length);
            
                        // Step 7: Send encrypted response
                        JSONObject responseJson = new JSONObject();
                        responseJson.put("iv", Base64.getEncoder().encodeToString(responseIv));
                        responseJson.put("message", Base64.getEncoder().encodeToString(encryptedResponse));
                        channel.sendMessage(responseJson);
            
                        System.out.println("📤 [SERVICE] Responded with: " + responseText);
            
                    } catch (Exception decryptException) {
                        System.err.println("❌ [SERVICE] Decryption error: " + decryptException.getMessage());
                        decryptException.printStackTrace();
                        channel.close();
                        break;  // Exit loop on decryption failure
                    }
            
                } catch (Exception e) {
                    System.err.println("❌ [SERVICE] Error in communication loop: " + e.getMessage());
                    e.printStackTrace();
                    channel.close();
                    break;  // Exit loop on communication failure
                }
            }
            
            System.out.println("👋 [SERVICE] Goodbye!");            
        }
        
        catch (Exception e) {
            System.err.println("❌ [SERVICE] Error: " + e.getMessage());
            e.printStackTrace();
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