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
    private static NonceCache nonceCache;
    private static final String DEFAULT_CONFIG_FILE = "src/kdcd/config.json";

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
        String configFile = DEFAULT_CONFIG_FILE;
        
        if (args.length == 2 && (args[0].equals("-c") || args[0].equals("--config"))) {
            configFile = args[1];
        } else if (args.length == 1 && (args[0].equals("-h") || args[0].equals("--help"))) {
            usageClient();
        } else if (args.length > 0) {
            System.err.println("Invalid arguments provided.");
            usageClient();
        }

        loadConfig(configFile);
        startServer();
    }

    private static void startServer() {
        System.out.println("Starting KDC server on port " + config.port);
        try (ServerSocket serverSocket = new ServerSocket(config.port)) {
            ExecutorService executorService = Executors.newFixedThreadPool(10);
            nonceCache = new NonceCache(16, 60);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down KDC server...");
                executorService.shutdown();
                try {
                    if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                        executorService.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    executorService.shutdownNow();
                }
            }));

            while (true) {
                Socket clientSocket = serverSocket.accept();
                executorService.submit(new ConnectionHandler(clientSocket, nonceCache));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadConfig(String configFile) {
        try {
            File file = new File(configFile);
            if (!file.exists()) {
                System.err.println("Config file not found: " + configFile);
                configFile = Config.createDefaultConfig(configFile);
            }

            JSONObject configJson = JsonIO.readObject(new File(configFile));
            if (configJson == null) {
                throw new IOException("Error reading configuration file");
            }

            config = new Config();
            config.secretsFile = configJson.getString("secrets-file");
            config.port = configJson.getInt("port");
            Object validityObj = configJson.get("validity-period");
            if (validityObj instanceof Number) {
                config.validityPeriod = ((Number) validityObj).longValue();
            } else if (validityObj instanceof String) {
                try {
                    config.validityPeriod = Long.parseLong((String) validityObj);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid 'validity-period' format in config. Using default: 60000 ms.");
                    config.validityPeriod = 60000L; // Default fallback
                }
            }

            loadSecrets(config.secretsFile);
            System.out.println("Loaded configuration from: " + configFile);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void loadSecrets(String secretsFile) {
        try {
            File file = new File(secretsFile);
            if (!file.isAbsolute()) {
                file = new File("src/kdcd", secretsFile);
            }

            if (!file.exists()) {
                System.out.println("Secrets file not found, this must be created");
            }

            JSONObject secretsJson = JsonIO.readObject(file);
            if (secretsJson == null) {
                throw new IOException("Error reading secrets file");
            }

            JSONArray secretsArray = secretsJson.getArray("secrets");
            if (secretsArray == null) {
                throw new IOException("No 'secrets' array found in secrets file.");
            }

            for (int i = 0; i < secretsArray.size(); i++) {
                JSONObject secretObj = secretsArray.getObject(i);
                String user = secretObj.getString("user");
                String secret = secretObj.getString("secret");

                if (user == null || secret == null) {
                    System.err.println("Warning: Missing 'user' or 'secret' in entry " + i);
                    continue;
                }

                secrets.put(user, secret);
                System.out.println("Loaded secret for user: " + user);
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}