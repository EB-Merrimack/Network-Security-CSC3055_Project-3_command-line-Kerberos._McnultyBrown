package client;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Scanner;

import common.Channel;
import common.CryptoUtils;
import common.TicketRequest;
import common.TicketResponse;
import merrimackutil.cli.LongOption;
import merrimackutil.cli.OptionParser;
import merrimackutil.json.JsonIO;
import merrimackutil.json.types.JSONArray;
import merrimackutil.json.types.JSONObject;
import merrimackutil.net.hostdb.HostsDatabase;
import merrimackutil.util.Tuple;
import protocol.RFC1994Challenge;
import protocol.RFC1994Claim;
import protocol.RFC1994Response;
import protocol.RFC1994Result;

public class KDCClient {
    private static String user = null;
    private static String service = null;
    private static String kdcHost;
    private static int kdcPort;

    public static void usageClient(Channel channel) {
        // Create an instance of the UsageMessage class
        UsageMessage usageMessage = new UsageMessage();
    
        // Send the serialized JSON object over the channel using JsonIO.writeSerializedObject
        JsonIO.writeSerializedObject(usageMessage, channel.getWriter());  // Ensure you're passing PrintWriter
    
        // Exit the program
        System.exit(1);
    }

    public static void main(String[] args) {
        String hostsFile = "host.json";
        String user = null;
        String service = null;

        UsageMessage usageMessage = new UsageMessage();

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-h") || args[i].equals("--hosts")) {
                if (i + 1 < args.length) {
                    hostsFile = args[i + 1];
                    i++; // Skip the next argument as it's the value for --hosts
                } else {
                    System.err.println("Error: Missing value for --hosts.");
                    System.out.println(usageMessage.getUsageMessage());
                    return;
                }
            } else if (args[i].equals("-u") || args[i].equals("--user")) {
                if (i + 1 < args.length) {
                    user = args[i + 1];
                    i++; // Skip the next argument as it's the value for --user
                } else {
                    System.err.println("Error: Missing value for --user.");
                    System.out.println(usageMessage.getUsageMessage());
                    return;
                }
            } else if (args[i].equals("-s") || args[i].equals("--service")) {
                if (i + 1 < args.length) {
                    service = args[i + 1];
                    i++; // Skip the next argument as it's the value for --service
                } else {
                    System.err.println("Error: Missing value for --service.");
                    System.out.println(usageMessage.getUsageMessage());
                    return;
                }
            } else if (args[i].equals("-h") || args[i].equals("--help")) {
                System.out.println(usageMessage.getUsageMessage());
                return; // Exit after showing help
            } else {
                System.err.println("Invalid argument: " + args[i]);
                System.out.println(usageMessage.getUsageMessage());
                return; // Exit on invalid argument
            }
        }

        // Ensure that both --user and --service are provided
        if (user == null || service == null) {
            System.err.println("Error: Both --user and --service must be specified.");
            System.out.println(usageMessage.getUsageMessage());
            return;
        }

        // Proceed with normal execution
        userauth(args, user, service, hostsFile);
    }

    public static String promptForUsername(String msg) {
        String usrnm;
        Console cons = System.console();

        do {
            usrnm = new String(cons.readLine(msg + ": "));
        } while (usrnm.isEmpty());

        return usrnm;
    }

    public static String promptForPassword(String msg) {
        Console cons = System.console();
        char[] passwdArray = null;
    
        do {
            System.out.print(msg + ": ");
            passwdArray = cons.readPassword(); // This hides the password input with dots
        } while (passwdArray == null || passwdArray.length == 0); // Ensure non-empty password
    
        return new String(passwdArray); // Convert char array to String
    }
    

    public static Tuple<String, Integer> getHostInfo(String hostName) {
        File file = new File("hosts.json");

        // If file doesn't exist, create a default one
        if (!file.exists()) {
            try {
                System.out.println("Creating default hosts.json...");

                JSONObject kdcObj = new JSONObject();
                kdcObj.put("host-name", "kdcd");
                kdcObj.put("address", "127.0.0.1");
                kdcObj.put("port", 5000);

                JSONArray hostArray = new JSONArray();
                hostArray.add(kdcObj);

                JSONObject root = new JSONObject();
                root.put("hosts", hostArray);

                try (PrintWriter writer = new PrintWriter(file)) {
                    writer.println(root.getFormattedJSON());
                    System.out.println("hosts.json created manually before loading.");
                }

            } catch (IOException e) {
                System.err.println("Failed to create hosts.json: " + e.getMessage());
                System.exit(1);
            }
        }

        // Load host info
        try {
            HostsDatabase db = new HostsDatabase(file);

            if (!db.hostKnown(hostName)) {
                System.err.println("Unknown host: " + hostName);
                System.exit(1);
            }

            String address = db.getAddress(hostName);
            int port = db.getPort(hostName);
            return new Tuple<>(address, port);

        } catch (Exception e) {
            System.err.println("Error loading host file: " + e.getMessage());
            System.exit(1);
            return null;
        }
    }

    public static Channel authenticateWithKDC(String username, String password, String host, int port) {
        try {
            // Open connection manually
            Socket socket = new Socket(host, port);
            Channel channel = new Channel(socket);

            // Message 1: Send identity claim
            RFC1994Claim claim = new RFC1994Claim(username);
            channel.sendMessage(claim);

            // Message 2: Receive challenge
            JSONObject challengeJson = channel.receiveMessage();
            if (!challengeJson.getString("type").equals("RFC1994 Challenge")) {
                System.out.println("Authentication failed: Unknown user or bad response");
                channel.close(); // clean up
                return null;
            }

            RFC1994Challenge challenge = new RFC1994Challenge("");
            challenge.deserialize(challengeJson);
            byte[] challengeBytes = Base64.getDecoder().decode(challenge.getChallenge());

            // Compute hash of password and challenge using SHA-256
            byte[] secretBytes = (password + new String(challengeBytes)).getBytes();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(secretBytes);
            String hashBase64 = Base64.getEncoder().encodeToString(hashBytes);

            // Message 3: Send response
            RFC1994Response response = new RFC1994Response(hashBase64);
            channel.sendMessage(response);

            // Message 4: Receive result
            JSONObject resultJson = channel.receiveMessage();
            RFC1994Result result = new RFC1994Result(false);
            result.deserialize(resultJson);

            if (result.getResult()) {
                System.out.println("Authentication successful");
                startEchoClient();
                return channel; // return open channel
            } else {
                System.out.println("Authentication failed: Invalid password");
                channel.close();
                return null;
            }

        } catch (Exception e) {
            System.err.println("Error during authentication: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static String combineIVandCipher(String iv, String cipherText) {
        byte[] ivBytes = Base64.getDecoder().decode(iv);
        byte[] cipherBytes = Base64.getDecoder().decode(cipherText);
        byte[] combined = new byte[ivBytes.length + cipherBytes.length];
        System.arraycopy(ivBytes, 0, combined, 0, ivBytes.length);
        System.arraycopy(cipherBytes, 0, combined, ivBytes.length, cipherBytes.length);
        return Base64.getEncoder().encodeToString(combined);
    }

    private static void userauth(String[] args, String user, String service, String hostsFile) {
        if (user == null) {
            user = promptForUsername("Enter username");
        } else if (service == null) {
            service = promptForUsername("Enter service");
        }

        if (hostsFile == null) {
            hostsFile = promptForUsername("Enter hosts file");
        }

        // 🔑 Prompt for password
        System.out.println("Prompting for password...");
        String password = promptForPassword("Enter password");

        // 🔐 Load KDC host info (forces hosts.json to be created if needed)
        Tuple<String, Integer> kdcHostInfo = getHostInfo("kdcd");
        kdcHost = kdcHostInfo.getFirst();
        kdcPort = kdcHostInfo.getSecond();

        Channel channel = authenticateWithKDC(user, password, kdcHost, kdcPort);
        if (channel != null) {
            try {
                // 🔑 Request a ticket from KDC
                new TicketRequest(service, user);
                JSONObject respJson = channel.receiveMessage();
                TicketResponse resp = new TicketResponse(null, null);
                resp.deserialize(respJson);

                String combined = combineIVandCipher(resp.getTicket().getIv(), resp.getSessionKey());
                String decryptedBase64Key = CryptoUtils.decryptAESGCM(combined, password);

                System.out.println("Ticket and session key received");
                System.out.println("Session key (base64): " + decryptedBase64Key);

                // If service is echoService, start EchoClient
                if (service.equalsIgnoreCase("echoService")) {
                    startEchoClient();
                } else {
                    System.out.println("Service is not known.");
                }

                channel.close();
            } catch (Exception e) {
                System.err.println("Error requesting session key: " + e.getMessage());
            }
        }
    }

    private static void startEchoClient() {
        EchoClient.main(new String[]{}); // Start the EchoClient
    }
}
