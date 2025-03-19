package common;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.io.IOException;


public class ConnectionHandler implements Runnable
{
    private Socket sock;

    /**
     * Creates a new conneciton handler 
     * @param sock the socket associated with the connection.
     */
    public ConnectionHandler(Socket sock)
    {
        this.sock = sock;
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

            // Get the line from the client.
            String line = recv.nextLine();
            //echo uppercase line
            send.println(line.toUpperCase());
            

            // Close the connection.
            sock.close();
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }    
    }
}