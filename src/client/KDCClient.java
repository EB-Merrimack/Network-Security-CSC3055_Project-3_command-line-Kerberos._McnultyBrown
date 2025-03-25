package client;

import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.Scanner;

import common.CryptoUtils;
import common.Ticket;
import common.TicketRequest;
import common.TicketResponse;
import merrimackutil.cli.LongOption;
import merrimackutil.cli.OptionParser;
import merrimackutil.json.JsonIO;
import merrimackutil.json.types.JSONArray;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;
import merrimackutil.net.hostdb.HostsDatabase;
import merrimackutil.util.Tuple;


public class KDCClient {
    private static String user = null;
    private static String service = null;
    private static HostsDatabase hostsDb;
    private static String KDC_HOST;
    private static int KDC_PORT;

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

    public static Tuple<String, Ticket> requestSessionKey(String user, String service, String password) {
        try (
            Socket socket = new Socket(KDC_HOST, KDC_PORT);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            // 1. Send TicketRequest using Merrimack JSON utility
            TicketRequest request = new TicketRequest(service, user);
            JsonIO.writeSerializedObject(request, out); // writes as compact JSON string
    
            // 2. Read full JSON response line from the KDC
            String rawJson = in.readLine();  // full line, e.g. {"type":"Ticket Response", ...}
    
            // 3. Parse the raw string into a JSONObject
            JSONObject response = JsonIO.readObject(rawJson);
    
            // 4. Make sure it's the right message type
            if (!response.getString("type").equals("Ticket Response")) {
                System.err.println("❌ Unexpected message type: " + response.getString("type"));
                return null;
            }
    
            // 5. Deserialize the response into a TicketResponse object
            TicketResponse ticketResponse = new TicketResponse(null, null);
            ticketResponse.deserialize(response);
    
            // 6. Decrypt the session key (encrypted with root key derived from password)
            String decryptedSessionKey = CryptoUtils.decryptAESGCM(ticketResponse.getSessionKey(), password);
    
            // 7. Done — return the session key and the ticket
            System.out.println("✅ Session key retrieved and ticket deserialized.");
            return new Tuple<>(decryptedSessionKey, ticketResponse.getTicket());
    
        } catch (Exception e) {
            System.err.println("❌ Failed to complete session key request:");
            e.printStackTrace();
            return null;
        }
    }

    
    public static void main(String[] args) {
        processArgs(args);

        if (user == null) {
            user = promptForUsername("Enter username");
        }

        String password = promptForPassword("Enter password");

        // 🔐 Load KDC host info (forces hosts.json to be created if needed)
        Tuple<String, Integer> kdcHostInfo = getHostInfo("kdcd");
        KDC_HOST = kdcHostInfo.getFirst();
        KDC_PORT = kdcHostInfo.getSecond();

        if (authenticateWithKDC(user, password, KDC_HOST, KDC_PORT)) {
            System.out.println("✅ Proceeding to session key request...");
    
            // 🔑 Request session key + ticket
            Tuple<String, Ticket> result = requestSessionKey(user, service, password);
            if (result != null) {
                String sessionKey = result.getFirst();
                Ticket ticket = result.getSecond();
    
                System.out.println("🔓 Session Key: " + sessionKey);
                System.out.println("🎫 Ticket for service: " + ticket.getService());
    
                // TODO: Proceed to service handshake and echo communication
            } else {
                System.out.println("❌ Failed to retrieve session key or ticket.");
            }
        } else {
            System.out.println("❌ CHAP Authentication failed.");
        }
    }
}