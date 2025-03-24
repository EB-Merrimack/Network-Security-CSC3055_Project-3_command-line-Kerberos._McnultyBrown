package common;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.io.IOException;
import merrimackutil.util.NonceCache;

/**
 * Handles a single connection, including nonce management.
 * @author Zach Kissel
 */
public class ConnectionHandler implements Runnable
{
    private Socket sock;
    private NonceCache nonceCache;  // NonceCache to check for replay attacks

    /**
     * Creates a new connection handler with nonce cache.
     * @param sock the socket associated with the connection.
     * @param nonceCache the nonce cache to check for replay attacks.
     */
    public ConnectionHandler(Socket sock, NonceCache nonceCache)
    {
        this.sock = sock;
        this.nonceCache = nonceCache;  // Initialize with the nonce cache
    }

    /**
     * How to handle the connection
     */
    public void run() 
    {
        try
        {
            // Setup the streams for use.
            Scanner recv = new Scanner(sock.getInputStream());
            PrintWriter send = new PrintWriter(sock.getOutputStream(), true);

            // Receive the nonce from the client (as a byte array)
            byte[] receivedNonce = receiveNonceFromClient(recv);

            // Check if the nonce has been used before
            if (nonceCache.containsNonce(receivedNonce)) {
                send.println("Nonce replay detected! Request rejected.");
                sock.close();
                return;  // Reject the connection if the nonce is a replay
            }

            // Process the received message (convert to uppercase for echoing)
            String line = recv.nextLine();
            send.println(line.toUpperCase());  // Send the response back

            // Add the nonce to the cache to prevent replay attacks
            nonceCache.addNonce(receivedNonce);

            // Close the connection
            sock.close();
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }    
    }

    /**
     * Simulates receiving a nonce from the client.
     * (In practice, this would be part of the message or handshake)
     * @param recv the scanner to receive data.
     * @return the received nonce as a byte array.
     */
    private byte[] receiveNonceFromClient(Scanner recv)
    {
        // Example: Receive the nonce as a byte array (you can change the logic here)
        byte[] nonce = new byte[16];  // For example, 16-byte nonce
        recv.nextLine();  // Simulating nonce reception as part of the communication (this can be adjusted)

        // In a real case, you would decode the nonce properly from the client message.
        return nonce;  // Return the received nonce
    }
}
