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
    public static Map<String, String> secrets = new HashMap<>();
    private static NonceCache nonceCache;

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
        String configPath = "src/kdcd/kdc-config.json";

        if (args.length == 2 && (args[0].equals("-c") || args[0].equals("--config"))) {
            configPath = args[1]; // use the path provided
        } else if (args.length == 1 && (args[0].equals("-h") || args[0].equals("--help"))) {
            usageClient(); // show usage
        } else if (args.length > 0) {
            usageClient(); // invalid usage
        }

        // Create default config/secrets if they don't exist
        createDefaultSecretsFileIfMissing("src/kdcd/secrets.json");
        createDefaultConfigFileIfMissing(configPath, "secrets.json");

        // Load config and start
        loadConfig(configPath);
        startServer();
    }

    private static void startServer() {
        System.out.println("Starting KDC server on port " + config.port);
        try (ServerSocket serverSocket = new ServerSocket(config.port)) {
            ExecutorService executorService = Executors.newFixedThreadPool(10);
            nonceCache = new NonceCache(16, 60); // 16-byte nonces, 60s lifespan

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
                throw new FileNotFoundException("Configuration file not found: " + configFile);
            }

            JSONObject configJson = JsonIO.readObject(file);
            if (configJson == null) {
                throw new IOException("Error reading configuration file");
            }

            config = new Config();
            File configFileObj = new File(configFile);
            String configDir = configFileObj.getParent();
            String secretsFileName = configJson.getString("secrets-file");
            config.secretsFile = new File(configDir, secretsFileName).getPath();            config.port = configJson.getInt("port");
            config.validityPeriod = configJson.getLong("validity-period");

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
            if (!file.exists()) {
                throw new FileNotFoundException("Secrets file not found: " + secretsFile);
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

    private static void createDefaultSecretsFileIfMissing(String path) {
        File file = new File(path);
        if (!file.exists()) {
            System.out.println("Creating default secrets file: " + path);
            JSONObject root = new JSONObject();
            root.put("secrets", new JSONArray());

            try (PrintWriter writer = new PrintWriter(file)) {
                writer.println(root.getFormattedJSON());
                System.out.println("✅ Empty secrets file created.");
            } catch (IOException e) {
                System.err.println("❌ Failed to create secrets file: " + e.getMessage());
                System.exit(1);
            }
        }
    }

    private static void createDefaultConfigFileIfMissing(String configPath, String secretsPath) {
        File file = new File(configPath);
        if (!file.exists()) {
            System.out.println("Creating default KDC config: " + configPath);

            JSONObject root = new JSONObject();
            root.put("secrets-file", secretsPath);
            root.put("port", 5000);
            root.put("validity-period", 60000); // 60 seconds

            try (PrintWriter writer = new PrintWriter(file)) {
                writer.println(root.getFormattedJSON());
                System.out.println("✅ Default config file created.");
            } catch (IOException e) {
                System.err.println("❌ Failed to create config file: " + e.getMessage());
                System.exit(1);
            }
        }
    }
}