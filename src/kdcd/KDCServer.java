package kdcd;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import com.google.gson.*;

class Config {
    String secretsFile;
    int port;
    long validityPeriod;
}

class CHAPServer {
    private final Map<String, String> userSecrets = new HashMap<>();
    private final SecureRandom random = new SecureRandom();
    
    public CHAPServer(String secretsFile) throws IOException {
        loadSecrets(secretsFile);
    }
    
    private void loadSecrets(String secretsFile) throws IOException {
        String json = new String(Files.readAllBytes(Paths.get(secretsFile)), StandardCharsets.UTF_8);
        Map<String, String> secrets = new Gson().fromJson(json, Map.class);
        userSecrets.putAll(secrets);
    }
    
    public String generateChallenge(String username) {
        if (!userSecrets.containsKey(username)) {
            return "{\"type\": \"RFC1994 Result\", \"result\": false}";
        }
        byte[] challengeBytes = new byte[32];
        random.nextBytes(challengeBytes);
        String challenge = Base64.getEncoder().encodeToString(challengeBytes);
        return "{\"type\": \"RFC1994 Challenge\", \"challenge\": \"" + challenge + "\"}";
    }
}

public class KDCServer {
    private static Config config;
    
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
            CHAPServer chapServer = new CHAPServer(config.secretsFile);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket, chapServer)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket socket, CHAPServer chapServer) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            String input = in.readLine();
            JsonObject request = JsonParser.parseString(input).getAsJsonObject();
            String response = "{}";
            if ("RFC1994 Initial".equals(request.get("type").getAsString())) {
                response = chapServer.generateChallenge(request.get("id").getAsString());
            }
            out.println(response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadConfig(String configFile) {
        try {
            String json = new String(Files.readAllBytes(Paths.get(configFile)), StandardCharsets.UTF_8);
            config = new Gson().fromJson(json, Config.class);
            System.out.println("Loaded configuration from: " + configFile);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}