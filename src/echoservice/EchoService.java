package echoservice;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * EchoService to handle incoming client connections.
 */
public class EchoService
{
    private final int port;
    private ServerSocket serverSocket;
    private final ExecutorService executorService;

    /**
     * Constructor to initialize the echo service.
     * @param port the port the service should listen on.
     */
    public EchoService(int port)
    {
        this.port = port;
        this.executorService = Executors.newFixedThreadPool(10); // Maximum 10 simultaneous connections.
    }
    public static void usageClient() {
        System.out.println("usage:");
      System.out.println("echoservice");
      System.out.println("echoservice --config <configfile>");
      System.out.println("echoservice --help");
      System.out.println("options:");
      System.out.println("  -c, --config Set the config file.");
      System.out.println("  -h, --help Display the help.");
        System.exit(1);
    }

    /**
     * Starts the echo service.
     */
    public void start() throws IOException
    {
        serverSocket = new ServerSocket(port);
        System.out.println("EchoService started on port " + port);

        // Loop to accept incoming client connections.
        while (true)
        {
            try
            {
                // Accept a new client connection.
                Socket clientSocket = serverSocket.accept();

                // Create a new ConnectionHandler for this client.
                ConnectionHandler handler = new ConnectionHandler(clientSocket);

                // Submit the handler to the executor for processing.
                executorService.submit(handler);
            }
            catch (IOException e)
            {
                System.err.println("Error while accepting connection: " + e.getMessage());
            }
        }
    }

    /**
     * Stops the echo service.
     */
    public void stop() throws IOException
    {
        if (serverSocket != null && !serverSocket.isClosed())
        {
            serverSocket.close();
            executorService.shutdown();
            System.out.println("EchoService stopped.");
        }
    }

    public static void main(String[] args)
    {
        try
        {
            // Set the port for the echo service (e.g., 12345).
            int port = 5000;
            EchoService service = new EchoService(port);

            // Start the service.
            service.start();
        }
        catch (IOException e)
        {
            System.err.println("Failed to start EchoService: " + e.getMessage());
        }
    }
}
