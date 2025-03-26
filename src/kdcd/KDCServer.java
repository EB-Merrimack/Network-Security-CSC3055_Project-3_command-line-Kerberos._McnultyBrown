package kdcd;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import common.Channel;
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

    public static void usageClient(Channel channel) {
        String usageMessage = "usage:\n" +
            "kdcd\n" +
            "kdcd --config <configfile>\n" +
            "kdcd --help\n" +
            "options:\n" +
            "  -c, --config Set the config file.\n" +
            "  -h, --help Display the help.\n";
        
        sendMessageToChannel(channel, usageMessage);
        System.exit(1);
    }

    public static void main(String[] args) {
        String configFile = DEFAULT_CONFIG_FILE;
        
        if (args.length == 2 && (args[0].equals("-c") || args[0].equals("--config"))) {
            configFile = args[1];
        } else if (args.length == 1 && (args[0].equals("-h") || args[0].equals("--help"))) {
            usageClient(null); // Null passed since no active channel in `main` method context
        } else if (args.length > 0) {
            System.err.println("Invalid arguments provided.");
            usageClient(null);
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
                Channel channel = new Channel(clientSocket);
                executorService.submit(new ConnectionHandler(channel, nonceCache));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadConfig(String configFile) {
        try {
            File file = new File(configFile);
            if (!file.exists()) {
                sendMessageToChannel(null, "Config file not found and must be created: " + configFile); // No channel, sent to console
                System.exit(1);
            }

            JSONObject configJson = JsonIO.readObject(new File(configFile));
            if (configJson == null) {
                sendMessageToChannel(null, "Error reading configuration file");
                System.exit(1);
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
                    sendMessageToChannel(null, "Invalid 'validity-period' format in config. Using default: 60000 ms.");
                    config.validityPeriod = 60000L; // Default fallback
                }
            }

            loadSecrets(config.secretsFile);
            sendMessageToChannel(null, "Loaded configuration from: " + configFile);
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
                sendMessageToChannel(null, "Secrets file not found, this must be created");
                System.exit(1);
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
                    sendMessageToChannel(null, "Warning: Missing 'user' or 'secret' in entry " + i);
                    continue;
                }

                secrets.put(user, secret);
                sendMessageToChannel(null, "Loaded secret for user: " + user);
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void sendMessageToChannel(Channel channel, String message) {
        if (channel != null) {
            JSONObject jsonMessage = new JSONObject();
            jsonMessage.put("message", message);
            channel.sendMessage(jsonMessage);
        } else {
            // Print to console if no active channel
            System.out.println(message);
        }
    }
}
