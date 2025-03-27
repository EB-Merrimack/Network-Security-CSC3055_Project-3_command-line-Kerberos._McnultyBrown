package client;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.io.PrintWriter;
import java.util.Base64;
import merrimackutil.util.NonceCache;
import java.security.SecureRandom;
import merrimackutil.json.types.JSONObject;
import common.Channel;

/**
 * Client to send a message with a nonce to the server.
 */
public class EchoClient {

    public static void main(String[] args, Channel channel) {
        Scanner scan = new Scanner(System.in);
        Socket sock = null;
        Scanner recv = null;
        PrintWriter send = null;
        NonceCache nonceCache = new NonceCache(16, 10);  // Create a new NonceCache instance

        try {
            // Set up a connection to the echo server running on the same machine.
            sock = new Socket("127.0.0.1", 5001);

            // Set up the streams for the socket.
            recv = new Scanner(sock.getInputStream());
            send = new PrintWriter(sock.getOutputStream(), true);
            
            // Now, create a Channel instance using the socket
            channel = new Channel(sock);  // Assuming Channel class is designed to take a socket
        } catch (UnknownHostException ex) {
            System.out.println("Host is unknown.");
            return;
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        // Generate a nonce for this request
        byte[] nonce = generateNonce();

        // Ensure that the nonce is valid before adding it to the cache
        try {
            nonceCache.addNonce(nonce);  // Add the nonce to the cache
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid nonce length: " + e.getMessage());
            return;
        }

        // Prompt the user for a string to send
        System.out.print("Write a short message: ");
        String msg = scan.nextLine();

        try {
            // Create JSON object with the nonce and message
            JSONObject message = new JSONObject();

            if (nonce != null && msg != null) {
                message.put("nonce", encodeBase64(nonce));  // Encode the nonce as Base64
                message.put("data", msg);  // The message to send
            } else {
                System.out.println("Error: Nonce or message is null.");
                return;
            }

            // Send the JSON object to the server via Channel
            channel.getWriter().println(message.toString());
            System.out.println("Message sent to server: " + message.toString());
        } catch (Exception e) {
            System.out.println("Error occurred while preparing the message: " + e.getMessage());
        }

        // Receive the response from the server
        try {
            if (recv.hasNextLine()) {
                String recvMsg = recv.nextLine();
                System.out.println("Server Said: " + recvMsg);
            } else {
                System.out.println("No response from server.");
            }
        } catch (Exception e) {
            System.out.println("Error receiving response: " + e.getMessage());
        }
    }

    /**
     * Generates a random nonce.
     * @return a byte array representing the nonce.
     */
    private static byte[] generateNonce() {
        SecureRandom random = new SecureRandom();
        byte[] nonce = new byte[16];  // Nonce size (16 bytes)
        random.nextBytes(nonce);  // Fill with random bytes
        return nonce;
    }

    /**
     * Encodes a byte array to Base64.
     * @param data the byte array to encode.
     * @return the Base64 encoded string.
     */
    private static String encodeBase64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }
}
