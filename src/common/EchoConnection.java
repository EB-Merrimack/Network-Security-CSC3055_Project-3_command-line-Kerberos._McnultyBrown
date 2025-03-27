package common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Base64;

import merrimackutil.json.JsonIO;
import merrimackutil.json.types.JSONObject;
import merrimackutil.util.NonceCache;

public class EchoConnection implements Runnable {

    private Socket sock;
    private NonceCache nonceCache;

    public EchoConnection(Socket sock, NonceCache nonceCache) {
        this.sock = sock;
        this.nonceCache = nonceCache;
    }

    @Override
    public void run() {
        System.out.println("EchoConnection.run() - Starting processing of connection from " + sock.getRemoteSocketAddress());

        // Read the incoming message (raw string from Socket)
        String incomingMessage = readMessageFromChannel();
        System.out.println("Received message from client: " + incomingMessage);

        // Parse the JSON message
        JSONObject message = JsonIO.readObject(incomingMessage);
        System.out.println("Parsed JSON message: " + message);

        // Decode the nonce (Base64)
        String encodedNonce = message.getString("nonce");
        System.out.println("Decoded nonce from message: " + encodedNonce);

        byte[] decodedNonce = decodeBase64(encodedNonce);
        System.out.println("Decoded nonce (Base64 to bytes): " + new String(decodedNonce));

        // Check if nonce has been seen before (replay attack prevention)
        if (nonceCache.containsNonce(decodedNonce)) {
            sendMessageToChannel("Replay attack detected.");
            System.out.println("Replay attack detected for nonce: " + encodedNonce);
            return;
        }

        // Add nonce to the cache to prevent future replays
        nonceCache.addNonce(decodedNonce);
        System.out.println("Nonce added to cache to prevent future replays: " + encodedNonce);

        // Process the 'data' field (user's message) and convert it to uppercase
        String data = message.getString("data");
        System.out.println("Original data received from client: " + data);

        String upperCaseData = data.toUpperCase();  // Convert the message to uppercase
        System.out.println("Converted data to uppercase: " + upperCaseData);

        // Send the transformed message back to the client
        sendMessageToChannel("Received data (uppercase): " + upperCaseData);
        System.out.println("Sent transformed message to client: " + upperCaseData);
    }

    private String readMessageFromChannel() {
        System.out.println("Reading message from channel...");
        // Read the incoming message from the socket's input stream using BufferedReader
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(sock.getInputStream()))) {
            StringBuilder message = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                message.append(line).append("\n");  // Read the whole message (assuming newline-delimited)
            }
            
            // If the connection is closed or no data is received, return an empty string.
            if (message.length() == 0) {
                throw new RuntimeException("Connection closed or no data received");
            }
    
            return message.toString();
        } catch (IOException e) {
            System.err.println("Error reading message from channel: " + e.getMessage());
            throw new RuntimeException("Error reading message", e);
        }
    }
    

    private void sendMessageToChannel(String message) {
        // Send the message back to the client via the socket's output stream
        try (PrintWriter writer = new PrintWriter(sock.getOutputStream(), true)) {
            writer.println(message);  // Write message and send to client
            System.out.println("Message sent to client: " + message);
        } catch (IOException e) {
            System.err.println("Error sending message to client: " + e.getMessage());
            throw new RuntimeException("Error sending message", e);
        }
    }

    private byte[] decodeBase64(String base64String) {
        System.out.println("Decoding Base64 string: " + base64String);
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(base64String);
            System.out.println("Decoded Base64 string to byte array: " + new String(decodedBytes));
            return decodedBytes;
        } catch (IllegalArgumentException e) {
            System.err.println("Error decoding Base64 string: " + e.getMessage());
            throw new RuntimeException("Error decoding Base64 string", e);
        }
    }
}
