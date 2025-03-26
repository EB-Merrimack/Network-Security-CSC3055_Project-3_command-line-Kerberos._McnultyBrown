package echoservice;

import java.net.Socket;
import java.net.ServerSocket;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import common.Channel;
import common.ConnectionHandler;
import merrimackutil.json.JsonIO;
import merrimackutil.util.NonceCache;  // Import NonceCache

public class EchoService {

    private static String configFile = null; // Config file must be explicitly set
    private static NonceCache nonceCache; // NonceCache instance to prevent replay attacks
    private static Channel channel; // Channel instance for sending messages
    public static void usageClient(Channel channel) {
        // Create an instance of the UsageMessage class
        UsageMessage usageMessage = new UsageMessage();
    
        // Send the serialized JSON object over the channel using JsonIO.writeSerializedObject
        JsonIO.writeSerializedObject(usageMessage, channel.getWriter());  // Ensure you're passing PrintWriter
    
        // Exit the program
        System.exit(1);
    }
    

    public static void main(String[] args) {
        // If no arguments are provided, or if "echoservice" is provided, start the default EchoService
        if (args.length == 0 || args[0].equalsIgnoreCase("echoservice")) {
            startServer("config.json");
            return;
        }

        // Handle command-line arguments
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-h":
                case "--help":
                    usageClient(channel);
                    return;
                case "-c":
                case "--config":
                    if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                        configFile = args[i + 1];
                        i++; // Skip the next argument (config file name)
                    } else {
                        throw new IllegalArgumentException("Error: Missing <configfile> after -c/--config.");
                    }
                    break;
                default:
                    if (args[i].equalsIgnoreCase("echoservice")) {
                        continue;
                    }
                    throw new IllegalArgumentException("Error: Unrecognized option: " + args[i]);
            }
        }

        // Start the EchoService server
        startServer(configFile);
    }

    /**
     * Starts the EchoService server, optionally using a config file.
     */
    private static void startServer(String configFile) {
        if (configFile == null) {
            throw new IllegalArgumentException("No configuration file provided. Running EchoService with default settings...");
        } else {
            System.out.println("Using configuration file: " + configFile);
        }

        // Initialize NonceCache (You can specify size and validity period here)
        nonceCache = new NonceCache(16, 300);  // 16-byte nonce, valid for 300 seconds

        // Create a new thread to run the EchoService so it doesn't block the command line
        Thread serverThread = new Thread(() -> {
            ExecutorService pool = Executors.newFixedThreadPool(10);

            try (ServerSocket server = new ServerSocket(5000)) {
                System.out.println("EchoService started on port 5000.");
                while (true) {
                    Socket sock = server.accept();
                    System.out.println("Connection received.");
                    // Pass the nonce cache to the connection handler
                    pool.execute(new ConnectionHandler(channel, nonceCache));
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        });

        // Start the server in a background thread
        serverThread.setDaemon(true);  // Set the thread as daemon so it doesn't block program termination
        serverThread.start();
        
        System.out.println("EchoService is now running in the background. You can return to the command line.");
    }
}
