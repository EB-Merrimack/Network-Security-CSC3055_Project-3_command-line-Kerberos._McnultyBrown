package common;

import java.io.IOException;
import merrimackutil.util.NonceCache;
import merrimackutil.json.JsonIO;
import merrimackutil.json.types.JSONObject;


public class ConnectionHandler implements Runnable
{
    private Channel channel;
    private NonceCache nonceCache;  // NonceCache to check for replay attacks

    /**
     * Creates a new connection handler with nonce cache.
     * @param channel the channel associated with the connection.
     * @param nonceCache the nonce cache to check for replay attacks.
     */
    public ConnectionHandler(Channel channel, NonceCache nonceCache)
    {
        this.channel = channel;
        this.nonceCache = nonceCache;  // Initialize with the nonce cache
    }

    /**
     * How to handle the connection
     */
    public void run() 
    {
        try
        {
            // Receive the nonce from the client (as part of the message)
            JSONObject message = channel.receiveMessage();
            byte[] receivedNonce = extractNonceFromMessage(message);

            // Check if the nonce has been used before
            if (nonceCache.containsNonce(receivedNonce)) {
                JSONObject response = new JSONObject();
                response.put("error", "Nonce replay detected! Request rejected.");
                channel.sendMessage(response);
                channel.close();
                return;  // Reject the connection if the nonce is a replay
            }

            // Process the received message (convert to uppercase for echoing)
            String receivedData = message.getString("data");  // Assuming "data" is the key
            String responseData = receivedData.toUpperCase();

            // Send the response back to the client
            JSONObject response = new JSONObject();
            response.put("data", responseData);
            channel.sendMessage(response);

            // Add the nonce to the cache to prevent replay attacks
            nonceCache.addNonce(receivedNonce);

            // Close the connection
            channel.close();
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }    
    }

    /**
     * Simulates extracting the nonce from the received message.
     * @param message the received JSON message.
     * @return the extracted nonce as a byte array.
     */
    private byte[] extractNonceFromMessage(JSONObject message)
    {
        // Example: Extract the nonce (in practice, it would be part of the message)
        byte[] nonce = new byte[16];  // For example, 16-byte nonce

        // In a real case, you would extract the nonce properly from the client message.
        return nonce;  // Return the extracted nonce
    }
}
