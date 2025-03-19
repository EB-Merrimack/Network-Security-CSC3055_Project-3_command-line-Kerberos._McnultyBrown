
import java.net.Socket;
import java.net.ServerSocket;
import java.util.Scanner;
import java.io.PrintWriter;
import java.io.IOException;

public class EchoServer {
  public static void main(String[] args)
  {
    try
    {
      ServerSocket server = new ServerSocket(5000);

      // Loop forever handing connections.
      while (true)
      {
        // Wait for a connection.
        Socket sock = server.accept();

        System.out.println("Connection received.");

        // Setup the streams for use.
        Scanner recv = new Scanner(sock.getInputStream());
        PrintWriter send = new PrintWriter(sock.getOutputStream(), true);

        // Get the line from the client.
        String line = recv.nextLine();
        System.out.println("Client said: " + line);

          // Convert to uppercase and send back
          send.println(line.toUpperCase());

        // Close the connection.
        sock.close();
      }
    }
    catch(IOException ioe)
    {
      ioe.printStackTrace();
    }
  }

}
