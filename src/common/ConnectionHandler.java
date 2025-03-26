package common;

import java.io.IOException;
import merrimackutil.util.NonceCache;
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

            if (nonceCache.containsNonce(receivedNonce)) {
                // Create a JSONObject to represent the error message
                JSONObject response = new JSONObject();
                response.put("error", "Nonce replay detected! Request rejected.");
                
                // Convert the response JSONObject to a JSON string
                String jsonResponse = response.toString();  // Convert JSONObject to string
                
                // Send the JSON string over the channel
                channel.getWriter().println(jsonResponse);  // Send the string over the channel
                
                // Close the channel to terminate the connection
                channel.close();
                
                // Return to reject further processing of this request
                return;
            }
            
            // Continue processing the request if the nonce was not a replay...

            // Process the received message (convert to uppercase for echoing)
            String receivedData = message.getString("data");  // Assuming "data" is the key
            String responseData = receivedData.toUpperCase();

            // Create a new JSONObject and add data
            JSONObject response = new JSONObject();
            response.put("data", responseData);

            // Convert the response JSONObject to a JSON string
            String jsonResponse = response.toString();  // Convert JSONObject to string
            
            // Send the JSON string over the channel
            channel.getWriter().println(jsonResponse);  // Send the string over the channel

            // Close the channel to terminate the connection (if needed)
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
