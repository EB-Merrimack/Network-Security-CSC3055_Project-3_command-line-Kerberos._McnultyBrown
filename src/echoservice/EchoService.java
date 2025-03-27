package echoservice;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import merrimackutil.json.JsonIO;
import merrimackutil.json.types.JSONObject;
import common.Channel;
import common.ConnectionHandler;
import merrimackutil.util.NonceCache;

import java.util.Base64;

public class EchoService {

    private static String configFile = null; // Config file must be explicitly set
    private static NonceCache nonceCache; // NonceCache instance to prevent replay attacks
    private static Channel channel; // Channel instance for sending messages
    private static Config config; // Config object to store configuration details

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
            startServer("src/echoservice/config.json");
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

        // Load the configuration from the JSON file
        try {
            loadConfig(configFile);
        } catch (IOException e) {
            System.err.println("Error loading configuration: " + e.getMessage());
            return;
        }

        // Create a new thread to run the EchoService so it doesn't block the command line
        ExecutorService pool = Executors.newFixedThreadPool(10);  // Use a thread pool with 10 threads

        try (ServerSocket server = new ServerSocket(config.port)) {  // Use port from config
            System.out.println("EchoService started on port " + config.port);

            while (true) {
                Socket sock = server.accept();
                System.out.println("Connection received.");

                // Pass the nonce cache and channel to the connection handler
                // The ExecutorService will handle client connections in a thread pool
                pool.execute(new common.ConnectionHandler(channel, nonceCache));
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            // Shutdown the thread pool when done
            pool.shutdown();
        }
    }

    /**
     * Loads the configuration from the specified file.
     * 
     * @param configFile The path to the config file.
     * @throws IOException If there is an error reading the file.
     */
    private static void loadConfig(String configFile) throws IOException {
        try {
            File file = new File(configFile);
            if (!file.exists()) {
                sendMessageToChannel(channel, "Config file not found and must be created: " + configFile);
                System.exit(1);
            }

            JSONObject configJson = JsonIO.readObject(new File(configFile));
            if (configJson == null) {
                sendMessageToChannel(channel, "Error reading configuration file");
                System.exit(1);
            }

            config = new Config();
            config.port = configJson.getInt("port");
            config.debug = configJson.getBoolean("debug");
            config.serviceName = configJson.getString("service-name");
            config.serviceSecret = configJson.getString("service-secret");
            sendMessageToChannel(channel, "Loaded configuration from: " + configFile);
        } catch (IOException e) {
            e.printStackTrace();
            throw new IOException("Error loading configuration: " + e.getMessage(), e);
        }
    }

    /**
     * Utility method to send messages to the channel (for example, the console).
     * 
     * @param channel The channel to send the message to.
     * @param message The message to send.
     */
    private static void sendMessageToChannel(Channel channel, String message) {
        // Send message to the channel (this is a placeholder for actual implementation)
        System.out.println(message);  // Just print to the console for now
    }

    // Add method to decode Base64
    private static byte[] decodeBase64(String base64String) {
        return Base64.getDecoder().decode(base64String);
    }
}
