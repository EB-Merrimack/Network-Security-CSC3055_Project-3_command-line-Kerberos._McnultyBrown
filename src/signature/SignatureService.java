package signature;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import merrimackutil.json.JSONSerializable;
import merrimackutil.json.JsonIO;
import merrimackutil.json.types.JSONObject;
import common.Channel;
import merrimackutil.util.NonceCache;

public class SignatureService {

    private static final String DEFAULT_CONFIG_FILE = "src/signatureservice/config.json";
    private static final String SIGNING_KEY_PLACEHOLDER = "<PASTE_BASE64_RSA_PRIVATE_KEY_HERE>";
    
    private static NonceCache nonceCache;
    private static Config config;
    private static PrivateKey signingKey;

    public static void usageClient() {
        System.out.println("Usage: java SignatureService [-c <config file>] | [-h]");
        System.exit(1);
    }

    public static void main(String[] args) {
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
        
        try (ServerSocket server = new ServerSocket(config.port)) {
            System.out.println("✅ SignatureService started on port " + config.port);
            ExecutorService pool = Executors.newFixedThreadPool(10);

            while (true) {
                Socket sock = server.accept();
                System.out.println("🔗 Connection received.");
                Channel connChannel = new Channel(sock);
                pool.execute(new SignatureServiceHandler(connChannel, nonceCache, config, signingKey));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadConfig(String configFile) throws IOException {
        File file = new File(configFile);
        if (!file.exists()) {
            System.err.println("❌ Config file not found: " + configFile);
            System.exit(1);
        }

        JSONObject configJson = JsonIO.readObject(file);
        if (configJson == null) {
            System.err.println("❌ Error reading configuration file.");
            System.exit(1);
        }

        try {
            config = new Config();
            config.port = configJson.getInt("port");
            config.debug = configJson.getBoolean("debug");
            config.serviceName = configJson.getString("service-name");

            if (!configJson.containsKey("signing-key")) {  // Corrected to containsKey
                System.err.println("❌ Error: Missing 'signing-key' in configuration.");
                System.exit(1);
            }

            String signingKeyBase64 = configJson.getString("signing-key");
            if (signingKeyBase64.equals(SIGNING_KEY_PLACEHOLDER)) {
                System.out.println("⚠️ No signing key found. Generating RSA private key...");
                signingKeyBase64 = generateAndSaveRSAKey();
                updateConfigWithNewSigningKey(configFile, signingKeyBase64);
            }

            signingKey = loadPrivateKey(signingKeyBase64);
            System.out.println("✅ Configuration loaded successfully.");
        } catch (Exception e) {
            System.err.println("❌ Error parsing configuration: " + e.getMessage());
            System.exit(1);
        }
    }

    private static String generateAndSaveRSAKey() throws Exception {
        // Generate RSA key pair
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);  // Key size
        KeyPair keyPair = keyGen.generateKeyPair();

        // Get private key
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        // Convert private key to Base64
        byte[] privateKeyBytes = privateKey.getEncoded();
        String privateKeyBase64 = Base64.getEncoder().encodeToString(privateKeyBytes);

        // Save private key to file
        try (FileOutputStream fos = new FileOutputStream("private_key.pem")) {
            fos.write(privateKeyBytes);
        }

        return privateKeyBase64;
    }

    private static void updateConfigWithNewSigningKey(String configFile, String signingKeyBase64) throws IOException {
        File file = new File(configFile);
        JSONObject configJson = JsonIO.readObject(file);
    
        if (configJson != null) {
            configJson.put("signing-key", signingKeyBase64);
    
            // Manually write the JSONObject to the file
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(configJson.toString().getBytes());  // Assuming JSONObject has a toString() method
                fos.flush();
                System.out.println("✅ Updated 'signing-key' in configuration file.");
            } catch (IOException e) {
                System.err.println("❌ Error writing configuration file: " + e.getMessage());
                System.exit(1);
            }
        } else {
            System.err.println("❌ Error reading configuration file.");
            System.exit(1);
        }
    }
    
    

    private static PrivateKey loadPrivateKey(String base64Key) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }
}
