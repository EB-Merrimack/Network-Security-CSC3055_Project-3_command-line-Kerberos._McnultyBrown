package kdcd;

import java.io.*;
import java.net.*;
import java.util.*;

import common.ConnectionHandler;
import merrimackutil.json.*;
import merrimackutil.json.types.JSONObject;

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
            while (true) {
                Socket clientSocket = serverSocket.accept();
                // Use the ConnectionHandler to handle the client connection.
                new Thread(new ConnectionHandler(clientSocket)).start();
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
            // Use JsonIO to read the configuration object from the file
            File file = new File(configFile);
            if (!file.exists()) {
                throw new FileNotFoundException("Configuration file not found: " + configFile);
            }

            // Read the config as a JSONObject
            JSONObject configJson = JsonIO.readObject(file);
            if (configJson == null) {
                throw new IOException("Error reading configuration file");
            }

            // Initialize the config object
            config = new Config();
            config.secretsFile = configJson.getString("secrets-file");
            config.port = configJson.getInt("port");
            config.validityPeriod = configJson.getLong("validity-period");
            System.out.println("Loaded configuration from: " + configFile);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
