package client;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.io.PrintWriter;
import java.util.Base64;
import java.security.SecureRandom;
import merrimackutil.json.types.JSONObject;

/**
 * Client to send a message with a nonce to the server.
 */
public class EchoClient {
  public static void main (String[] args)
  {
    Scanner scan = new Scanner(System.in);
    Socket sock = null;
    Scanner recv = null;
    PrintWriter send = null;

    try
    {
      // Set up a connection to the echo server running on the same machine.
      sock = new Socket("127.0.0.1", 5001);

      // Set up the streams for the socket.
      recv = new Scanner(sock.getInputStream());
      send = new PrintWriter(sock.getOutputStream(), true);
    }
    catch(UnknownHostException ex)
    {
      System.out.println("Host is unknown.");
      return;
    }
    catch(IOException ioe)
    {
      ioe.printStackTrace();
    }

    // Generate a nonce for this request
    byte[] nonce = generateNonce();

    // Prompt the user for a string to send
    System.out.print("Write a short message: ");
    String msg = scan.nextLine();

    // Create JSON object with the nonce and message
    JSONObject message = new JSONObject();
    message.put("nonce", encodeBase64(nonce));  // Encode the nonce as Base64
    message.put("data", msg);  // The message to send

    // Send the JSON object to the server
    send.println(message.toString());

    // Receive the response from the server
    String recvMsg = recv.nextLine();
    System.out.println("Server Said: " + recvMsg);
  }

  /**
   * Generates a random nonce.
   * @return a byte array representing the nonce.
   */
  private static byte[] generateNonce()
  {
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
  private static String encodeBase64(byte[] data)
  {
    return Base64.getEncoder().encodeToString(data);
  }
}
