package kdcd;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import common.ConnectionHandler;
import merrimackutil.json.*;
import merrimackutil.json.types.JSONArray;
import merrimackutil.json.types.JSONObject;
import merrimackutil.util.NonceCache;

public class KDCServer {
    private static Config config;
    private static Map<String, String> secrets = new HashMap<>();
    private static NonceCache nonceCache;  // NonceCache to store nonces

    public static void usageClient() {
        System.out.println("usage:");
        System.out.println("kdcd");
        System.out.println("kdcd --config <configfile>");
        System.out.println("kdcd --help");
        System.out.println("options:");
        System.out.println("  -c, --config Set the config file.");
        System.out.println("  -h, --help Display the help.");
        System.exit(1);
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            // If no arguments are passed, check for a config file and load it.
            loadConfig();
            startServer();
        } else if (args.length == 2 && (args[0].equals("-c") || args[0].equals("--config"))) {
            loadConfig(args[1]);
            startServer();
        } else if (args.length == 1 && (args[0].equals("-h") || args[0].equals("--help"))) {
            usageClient();
        } else {
            System.err.println("Invalid arguments provided.");
            usageClient();
        }
    }

    private static void startServer() {
        System.out.println("Starting KDC server on port " + config.port);
        try (ServerSocket serverSocket = new ServerSocket(config.port)) {
            // Thread pool to handle multiple connections.
            ExecutorService executorService = Executors.newFixedThreadPool(10);

            // Initialize the NonceCache (with size and age limit from config or defaults)
            nonceCache = new NonceCache(16, 60); // Example: 16-byte nonces and 60 seconds validity period
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                // Pass the secrets map and client socket to each handler
                executorService.submit(new ConnectionHandler(clientSocket, nonceCache)); // Pass nonceCache to handler
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadConfig() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Config file not provided. Please enter the path to the configuration file: ");
        String configFile = scanner.nextLine();
        loadConfig(configFile);
    }

    private static void loadConfig(String configFile) {
        try {
            // Read the configuration file to get server settings.
            File file = new File(configFile);
            if (!file.exists()) {
                throw new FileNotFoundException("Configuration file not found: " + configFile);
            }

            // Parse the config as a JSONObject.
            JSONObject configJson = JsonIO.readObject(file);
            if (configJson == null) {
                throw new IOException("Error reading configuration file");
            }

            // Initialize the config object.
            config = new Config();
            config.secretsFile = configJson.getString("secrets-file");
            config.port = configJson.getInt("port");
            config.validityPeriod = configJson.getLong("validity-period");

            // Load the secrets file.
            loadSecrets(config.secretsFile);
            System.out.println("Loaded configuration from: " + configFile);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    // Load secrets from the secrets JSON file.
    private static void loadSecrets(String secretsFile) {
        try {
            // Read the secrets JSON file.
            File file = new File(secretsFile);
            if (!file.exists()) {
                throw new FileNotFoundException("Secrets file not found: " + secretsFile);
            }

            // Parse the secrets file as a JSONObject.
            JSONObject secretsJson = JsonIO.readObject(file);
            if (secretsJson == null) {
                throw new IOException("Error reading secrets file");
            }

            // Get the array of secrets.
            JSONArray secretsArray = secretsJson.getArray("secrets");
            for (int i = 0; i < secretsArray.size(); i++) {
                JSONObject secretObj = secretsArray.getObject(i);
                String user = secretObj.getString("user");
                String secret = secretObj.getString("secret");

                // Store the secret in the map (username -> secret).
                secrets.put(user, secret);
                System.out.println("Loaded secret for user: " + user);
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
