
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.io.PrintWriter;
import java.io.IOException;

public class EchoClient {
  public static void main (String[] args)
  {
    Scanner scan = new Scanner(System.in);
    Socket sock;
    Scanner recv = null;
    PrintWriter send = null;

    try
    {
      // Set up a connection to the echo server running on the same machine.
      sock = new Socket("127.0.0.1", 5000);


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

    // Prompt the user for a string to send.
    System.out.print("Write a short message: ");
    String msg = scan.nextLine();

    // Send the message to the server.
    send.println(msg);

    // Echo the response to the screen.
    String recvMsg = recv.nextLine();
    System.out.println("Server Said: " + recvMsg);
  }
}
