package common;

import java.io.IOException;
import java.net.Socket;
import merrimackutil.util.NonceCache;
import merrimackutil.json.types.JSONObject;
import common.Channel;
import kdcd.KDCServer;

public class EchoConnection implements Runnable {
    private Socket sock;
    private NonceCache nonceCache; // NonceCache to check for replay attacks
    private Channel channel; // Channel instance for communication

    /**
     * Creates a new connection handler with nonce cache and channel.
     * @param sock the socket associated with the connection.
     * @param nonceCache the nonce cache to check for replay attacks.
     */
    public EchoConnection(Socket sock, NonceCache nonceCache) throws IOException {
        this.sock = sock;
        this.nonceCache = nonceCache;

        // Create a new Channel instance with the socket
        this.channel = new Channel(sock);
    }

    /**
     * How to handle the connection
     */
    public void run() {
        try {
            // Receive the message from the client
            JSONObject message = channel.receiveMessage(); // Assume Channel handles input stream

            // Extract the nonce from the received message
            byte[] receivedNonce = extractNonceFromMessage(message);

            // Check if the nonce has already been used
            if (nonceCache.containsNonce(receivedNonce)) {
                // Create a response indicating a replay attack
                JSONObject response = new JSONObject();
                response.put("error", "Nonce replay detected! Request rejected.");
                
                // Send the error response back to the client
                channel.sendMessage(response); // Assume Channel handles output stream

                // Close the connection
                sock.close();
                return;
            }

            // If the nonce is not a replay, add it to the nonce cache
            nonceCache.addNonce(receivedNonce);

            // Process the received data (e.g., echo it back)
            String receivedData = message.getString("data"); // Assuming the message contains data in "data" field
            String responseData = receivedData.toUpperCase(); // Process the message (e.g., convert to uppercase)

            // Create a new response message
            JSONObject response = new JSONObject();
            response.put("data", responseData);

            // Send the response back to the client
            channel.sendMessage(response); // Assume Channel handles output stream

            // Close the connection
            sock.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            try {
                sock.close(); // Ensure the socket is closed if an error occurs
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Simulates extracting the nonce from the received message.
     * @param message the received JSON message.
     * @return the extracted nonce as a byte array.
     */
    private byte[] extractNonceFromMessage(JSONObject message) {
        // Extract the nonce from the message JSON. Assuming the nonce is stored under the key "nonce".
        String nonceString = message.getString("nonce");  // Get the nonce as a String (it might be Base64 encoded)
        
        if (nonceString == null || nonceString.isEmpty()) {
            throw new IllegalArgumentException("Nonce missing or empty in the message.");
        }
        
        // Convert the nonce string (Base64 encoded) to a byte array
        byte[] nonce = decodeBase64(nonceString);  // Assuming the nonce is Base64 encoded
        
        // Return the byte array of the nonce
        return nonce;
    }
    
    /**
     * Decodes a Base64 encoded string into a byte array.
     * @param encoded The Base64 encoded string.
     * @return The decoded byte array.
     */
    private byte[] decodeBase64(String encoded) {
        return java.util.Base64.getDecoder().decode(encoded);  // Using Java's Base64 decoder
    }
}
