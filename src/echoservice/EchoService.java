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
import common.EchoServiceHandler;
import merrimackutil.util.NonceCache;

public class EchoService {

    private static final String DEFAULT_CONFIG_FILE = "src/echoservice/config.json";
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
        String configFile = DEFAULT_CONFIG_FILE;

        if (args.length == 0) {
            // No args? Use default config
            System.out.println("🛠 Using default config file: " + configFile);
        } else if (args.length == 2 && (args[0].equals("-c") || args[0].equals("--config"))) {
            configFile = args[1];
        } else if (args.length == 1 && (args[0].equals("-h") || args[0].equals("--help"))) {
            usageClient(channel);
            return;
        } else {
            System.err.println("Error: Unrecognized arguments.");
            usageClient(channel);
            return;
        }

        // Start server
        startServer(configFile);
    }

    /**
     * Starts the EchoService server, optionally using a config file.
     */
    private static void startServer(String configFile) {
        if (configFile == null) {
            configFile = DEFAULT_CONFIG_FILE;
        }
    
        System.out.println("Using configuration file: " + configFile);
    
        try {
            loadConfig(configFile);
        } catch (IOException e) {
            System.err.println("Error loading configuration: " + e.getMessage());
            return;
        }
    
        nonceCache = new NonceCache(16, 60000);
        // ✅ Start server immediately (NO background thread for now)
        try (ServerSocket server = new ServerSocket(config.port)) {
            System.out.println("EchoService started on port " + config.port);
            ExecutorService pool = Executors.newFixedThreadPool(10);
    
            while (true) {
                Socket sock = server.accept();
                System.out.println("Connection received.");
                Channel connChannel = new Channel(sock);
                pool.execute(new EchoServiceHandler(connChannel, nonceCache, config));
            }
    
        } catch (IOException e) {
            e.printStackTrace();
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
     * Load secrets from the secrets file (stub method to implement loading secrets).
     * 
     * @param secretsFile Path to the secrets file.
     */
    private static void loadSecrets(String secretsFile) {
        // Implement your secret loading logic here, possibly deserializing secrets from a file
        System.out.println("Loading secrets from: " + secretsFile);
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
}
