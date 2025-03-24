package client;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.Scanner;

import merrimackutil.cli.LongOption;
import merrimackutil.cli.OptionParser;
import merrimackutil.json.types.JSONArray;
import merrimackutil.json.types.JSONObject;
import merrimackutil.net.hostdb.HostsDatabase;
import merrimackutil.util.Tuple;

public class KDCClient {
    private static String user = null;
    private static String service = null;
    private static HostsDatabase hostsDb;
    private static String kdcHost;
    private static int kdcPort;

    public static void usageClient() {
        System.out.println("usage: ");
        System.out.println("    client --hosts <hostfile> --user <user> --service <service>");
        System.out.println("    client --user <user> --service <service>");
        System.out.println("options: ");
        System.out.println("    -h, --hosts Set the hosts file.");
        System.out.println("    -u, --user The user name.");
        System.out.println("    -s, --service The name of the service.");
        System.exit(1);
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
        String passwd;
        Console cons = System.console();

        do {
            passwd = new String(cons.readPassword(msg + ": "));
        } while (passwd.isEmpty());

        return passwd;
    }

    public static void processArgs(String[] args) {
        OptionParser parser;

        LongOption[] opts = new LongOption[3];
        opts[0] = new LongOption("hosts", false, 'h');
        opts[1] = new LongOption("user", true, 'u');
        opts[2] = new LongOption("service", true, 's');

        Tuple<Character, String> currOpt;

        parser = new OptionParser(args);
        parser.setLongOpts(opts);
        parser.setOptString("hu:s");

        while (parser.getOptIdx() != args.length) {
            currOpt = parser.getLongOpt(false);

            switch (currOpt.getFirst()) {
                case 'h':
                    // Optionally support setting custom hosts file here
                    break;
                case 'u':
                    user = currOpt.getSecond();
                    break;
                case 's':
                    service = currOpt.getSecond();
                    break;
            }
        }
    }

    public static Tuple<String, Integer> getHostInfo(String hostName) {
        File file = new File("hosts.json");

        // Step 1: If file doesn't exist, create a default one
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
                    System.out.println("✅ hosts.json created manually before loading.");
                }

            } catch (IOException e) {
                System.err.println("❌ Failed to create hosts.json: " + e.getMessage());
                System.exit(1);
            }
        }

        // Step 2: Load host info
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

    public static boolean authenticateWithKDC(String username, String password, String host, int port) {
        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             Scanner in = new Scanner(socket.getInputStream())) {

            // Message 1: Send authentication claim
            out.println("{\"type\": \"claim\", \"username\": \"" + username + "\"}");

            // Message 2: Receive challenge from KDC
            String response = in.nextLine();
            if (response.contains("failure")) {
                System.out.println("Authentication failed: User not found");
                return false;
            }
            String challenge = response.split(":")[1].replace("}", "");

            // Message 3: Compute response hash (SHA-256(password + challenge))
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((password + challenge).getBytes());

            // Send response hash to KDC
            out.println("{\"type\": \"response\", \"hash\": \"" + bytesToHex(hash) + "\"}");

            // Message 4: Receive authentication result
            String result = in.nextLine();
            if (result.contains("success")) {
                System.out.println("Authentication successful");
                return true;
            } else {
                System.out.println("Authentication failed");
                return false;
            }
        } catch (IOException | java.security.NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        processArgs(args);

        if (user == null) {
            user = promptForUsername("Enter username");
        }

        String password = promptForPassword("Enter password");

        // 🔐 Load KDC host info (forces hosts.json to be created if needed)
        Tuple<String, Integer> kdcHostInfo = getHostInfo("kdcd");
        kdcHost = kdcHostInfo.getFirst();
        kdcPort = kdcHostInfo.getSecond();

        // 🔒 Run CHAP protocol
        if (authenticateWithKDC(user, password, kdcHost, kdcPort)) {
            System.out.println("Proceeding to session key request...");
            // 🚧 TODO: Implement session key request next
        }
    }
}