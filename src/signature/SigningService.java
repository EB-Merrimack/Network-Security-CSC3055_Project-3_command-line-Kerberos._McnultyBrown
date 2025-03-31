package signature;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import merrimackutil.json.JsonIO;
import merrimackutil.json.types.JSONObject;
import common.Channel;
import common.SigningServiceHandler;
import merrimackutil.util.NonceCache;

public class SigningService {

    private static final String DEFAULT_CONFIG_FILE = "src/signature/config.json";
    private static NonceCache nonceCache;
    private static Config config;
    private static PrivateKey signingKey;
    private static Channel channel; // Channel instance for sending messages

    public static void usageClient() {
        System.out.println("Usage: java SigningService [-c <config.json>] | [-h]");
        System.out.println("  -c, --config <file>    Specify config file");
        System.out.println("  -h, --help             Show this help message");
    }

    public static void main(String[] args) throws InvalidKeySpecException {
        String configFile = DEFAULT_CONFIG_FILE;

        if (args.length == 0) {
            System.out.println("🛠 Using default config file: " + configFile);
        } else if (args.length == 2 && (args[0].equals("-c") || args[0].equals("--config"))) {
            configFile = args[1];
        } else if (args.length == 1 && (args[0].equals("-h") || args[0].equals("--help"))) {
            usageClient();
            return;
        } else {
            System.err.println("Error: Unrecognized arguments.");
            usageClient();
            return;
        }

        startServer(configFile);
    }

    private static void startServer(String configFile) throws InvalidKeySpecException {
        try {
            loadConfig(configFile);
        } catch (IOException | NoSuchAlgorithmException e) {
            System.err.println("Error loading configuration: " + e.getMessage());
            return;
        }

        nonceCache = new NonceCache(16, 60000);
        ExecutorService pool = Executors.newFixedThreadPool(10);

        try (ServerSocket server = new ServerSocket(config.port)) {
            System.out.println("SigningService started on port " + config.port);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down SigningService...");
                pool.shutdown();
            }));

            while (true) {
                Socket clientSocket = server.accept();
                System.out.println("Connection received.");

                Channel connChannel = new Channel(clientSocket);
                pool.execute(new SigningServiceHandler(connChannel, nonceCache, config, signingKey));
            }

        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

  private static void loadConfig(String configFile) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
    File file = new File(configFile);
    if (!file.exists()) {
        throw new IOException("Config file not found: " + configFile);
    }

    JSONObject configJson = JsonIO.readObject(file);
    if (configJson == null) {
        throw new IOException("Invalid or empty config file");
    }

    config = new Config();
    config.port = configJson.getInt("port");
    config.debug = configJson.getBoolean("debug");
    config.serviceName = configJson.getString("service-name");
    config.serviceSecret = configJson.getString("service-secret");

    try {
        if (configJson.containsValue("signing-key") && !configJson.getString("signing-key").isEmpty()) {
            // Load existing signing key
            byte[] signingKeyBytes = Base64.getDecoder().decode(configJson.getString("signing-key"));
            signingKey = KeyFactory.getInstance("RSA")
                    .generatePrivate(new java.security.spec.PKCS8EncodedKeySpec(signingKeyBytes));
        } else {
            // Let Config handle key generation
            System.out.println("🔑 No signing key found in config. Generating a new one...");
            signingKey = config.createNewPrivateKey();
            System.out.println("✅ New signing key generated.");
        }
    } catch (InvalidKeySpecException e) {
        throw new InvalidKeySpecException("Error loading or generating signing key: " + e.getMessage());
    }
}

    }
    
    
    
