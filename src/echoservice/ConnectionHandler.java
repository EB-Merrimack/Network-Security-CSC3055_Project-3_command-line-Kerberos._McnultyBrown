package echoservice;

import merrimackutil.json.JsonIO;
import merrimackutil.util.NonceCache;
import merrimackutil.json.types.JSONObject;
import common.CryptoUtils;

import java.io.*;
import java.net.Socket;
import java.util.Base64;
import java.util.Scanner;

public class ConnectionHandler implements Runnable {
    private final Socket sock;
    private final String serviceSecret;
    private final NonceCache nonceCache;

    /**
     * Creates a new connection handler.
     * @param sock the socket associated with the connection.
     * @param serviceSecret the shared service secret for encryption.
     * @param nonceCache the shared nonce cache for replay prevention.
     */
    public ConnectionHandler(Socket sock, String serviceSecret, NonceCache nonceCache) {
        this.sock = sock;
        this.serviceSecret = serviceSecret;
        this.nonceCache = nonceCache;
    }

    /**
     * Handles the client connection.
     */
    public void run() {
        try (
            Scanner recv = new Scanner(sock.getInputStream());
            PrintWriter send = new PrintWriter(sock.getOutputStream(), true)
        ) {
            // Step 1: Receive JSON Message
            String jsonMessage = recv.nextLine();
            JSONObject receivedMessage = parseJsonMessage(jsonMessage);

            // Step 2: Verify nonce
            byte[] nonce = Base64.getDecoder().decode((String) receivedMessage.get("nonce"));
            if (nonceCache.containsNonce(nonce)) {
                System.out.println("Replay detected! Closing connection.");
                sendErrorMessage(send, "ERROR: Replay attack detected.");
                return;
            }
            nonceCache.addNonce(nonce);

            // Step 3: Decrypt message
            String encryptedMessage = (String) receivedMessage.get("message");
            String decryptedMessage = CryptoUtils.decryptAESGCM(encryptedMessage, serviceSecret);

            // Step 4: Transform message (to uppercase)
            String transformedMessage = decryptedMessage.toUpperCase();

            // Step 5: Encrypt the transformed message
            String encryptedResponse = CryptoUtils.encryptAESGCM(transformedMessage, serviceSecret);

            // Step 6: Send JSON Response
            JSONObject response = createResponseJson(encryptedResponse);
            JsonIO.writeSerializedObject(response, send);

        } catch (Exception e) {
            System.err.println("Connection error: " + e.getMessage());
        } finally {
            try {
                sock.close();
            } catch (IOException ignored) {}
        }
    }

    /**
     * Parses the incoming JSON message.
     */
    private JSONObject parseJsonMessage(String jsonMessage) {
        try {
            return (JSONObject) new org.json.simple.parser.JSONParser().parse(jsonMessage);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON message: " + e.getMessage());
        }
    }

    /**
     * Creates a response JSON with the encrypted message.
     */
    private JSONObject createResponseJson(String encryptedMessage) {
        JSONObject response = new JSONObject();
        response.put("type", "response");
        response.put("message", encryptedMessage);
        return response;
    }

    /**
     * Sends an error message as a JSON response.
     */
    private void sendErrorMessage(PrintWriter send, String errorMessage) {
        JSONObject errorResponse = new JSONObject();
        errorResponse.put("type", "error");
        errorResponse.put("message", errorMessage);
        JsonIO.writeSerializedObject(errorResponse, send);
    }
}
