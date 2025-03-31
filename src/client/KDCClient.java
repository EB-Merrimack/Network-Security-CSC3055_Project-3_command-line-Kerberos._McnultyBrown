package client;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import common.Channel;
import common.CryptoUtils;
import common.TicketRequest;
import common.TicketResponse;
import common.service.ClientHello;
import common.service.ClientResponse;
import common.service.HandshakeResponse;
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

    /**
     * Send a UsageMessage over the given channel, then exit the program.
     * This method is intended to be used when the client needs to send
     * a usage message to the service, such as when the user provides invalid
     * command-line arguments.
     * @param channel The Channel over which to send the usage message.
     */
   public static void usageClient(Channel channel) {
        // Create an instance of the UsageMessage class
        UsageMessage usageMessage = new UsageMessage();
    
        // Send the serialized JSON object over the channel using JsonIO.writeSerializedObject
        JsonIO.writeSerializedObject(usageMessage, channel.getWriter());  // Ensure you're passing PrintWriter
    
        // Exit the program
        System.exit(1);
    }


    
    /**
     * Prompt the user for a username. This method will continue to
     * prompt the user until a non-empty string is entered.
     * @param msg The prompt message to display to the user.
     * @return The username entered by the user.
     */
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
    /**
     * Main method for the KDC client.
     * @param args
     *        Command line arguments. The client accepts the following arguments:
     *        <ul>
     *            <li> -u, --user        The username to use for authentication.
     *            <li> -s, --service The service name to use for authentication.
     *        </ul>
     *        If the username is not provided, the client will prompt the user to
     *        enter a username.
     */
    public static void main(String[] args) {
        Map<String, String> argsMap = processArgs(args);
           // If user is not provided in arguments, prompt for username
           if (argsMap == null) {
            return; // Exit if argument parsing fails
        }
        user = argsMap.get("user");
        service = argsMap.get("service");
        if (user == null) {
            user = promptForUsername("Enter username");
        }

        String password = promptForPassword("Enter password");

        // 🔐 Load KDC host info (forces hosts.json to be created if needed)
        Tuple<String, Integer> kdcHostInfo = getHostInfo("kdcd");
        kdcHost = kdcHostInfo.getFirst();
        kdcPort = kdcHostInfo.getSecond();

        Channel channel = authenticateWithKDC(user, password, kdcHost, kdcPort);
        if (channel != null) {
            try {
                TicketRequest req = new TicketRequest(service, user);
                channel.sendMessage(req);

                JSONObject respJson = channel.receiveMessage();
                TicketResponse resp = new TicketResponse(null, null);
                resp.deserialize(respJson);

                String encryptedSessionKey = resp.getSessionKey();
                String base64SessionKey = CryptoUtils.decryptAESGCM(encryptedSessionKey, password);


                connectToService(resp, base64SessionKey);

                channel.close();
            } catch (Exception e) {
                System.err.println("Error requesting session key: " + e.getMessage());
            }
        }
    }
    /**
     * Process the command line arguments to the client.
     *
     * This method interprets the command line arguments passed to the client and
     * sets the global variables user and service accordingly.  If the arguments
     * do not include values for both user and service, the method prints an
     * error message and returns.
     *
     * @param args The command line arguments to the client.
     */
       public static Map<String, String> processArgs(String[] args) {
        OptionParser parser;
    
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
                    return null; // Return null to indicate error
                }
            } else if (args[i].equals("-u") || args[i].equals("--user")) {
                if (i + 1 < args.length) {
                    user = args[i + 1];
                    i++; // Skip the next argument as it's the value for --user
                } else {
                    System.err.println("Error: Missing value for --user.");
                    System.out.println(usageMessage.getUsageMessage());
                    return null; // Return null to indicate error
                }
            } else if (args[i].equals("-s") || args[i].equals("--service")) {
                if (i + 1 < args.length) {
                    service = args[i + 1];
                    i++; // Skip the next argument as it's the value for --service
                } else {
                    System.err.println("Error: Missing value for --service.");
                    System.out.println(usageMessage.getUsageMessage());
                    return null; // Return null to indicate error
                }
            } else if (args[i].equals("-h") || args[i].equals("--help")) {
                System.out.println(usageMessage.getUsageMessage());
                return null; // Exit after showing help
            } else {
                System.err.println("Invalid argument: " + args[i]);
                System.out.println(usageMessage.getUsageMessage());
                return null; // Exit on invalid argument
            }
        }

        // Return the user and service in a map
        Map<String, String> result = new HashMap<>();
        result.put("user", user);
        result.put("service", service);
        return result;
    }

    

/**
 * Retrieves the host information (address and port) for the specified host name.
 *
 * If the "hosts.json" file does not exist, a default file is created with 
 * pre-defined host entries. The method then loads the host information from 
 * this file using the `HostsDatabase` class.
 *
 * @param hostName The name of the host whose information is to be retrieved.
 * @return A Tuple containing the host address as a String and port as an Integer.
 * @throws RuntimeException If the host is unknown or if there is an error loading 
 *         the host file, the method prints an error message and exits the program.
 */

    public static Tuple<String, Integer> getHostInfo(String hostName) {
        File file = new File("hosts.json");

        //If file doesn't exist, create a default one
        if (!file.exists()) {
            try {
                System.out.println("Creating default hosts.json...");

                JSONObject kdcObj = new JSONObject();
                kdcObj.put("host-name", "kdcd");
                kdcObj.put("address", "127.0.0.1");
                kdcObj.put("port", 5000);

                JSONObject echoObj = new JSONObject();
                echoObj.put("host-name", "echoservice");
                echoObj.put("address", "127.0.0.1");
                echoObj.put("port", 5001); // 👈 Match what your EchoService is actually using

                JSONArray hostArray = new JSONArray();
                hostArray.add(kdcObj);
                hostArray.add(echoObj);

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

        //Load host info
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

    /**
     * Authenticate with the KDC using RFC 1994.
     *
     * Connects to the KDC at the given host and port, and sends an identity
     * claim.  The KDC responds with a challenge, which this function computes a
     * hash of using the given password.  The hash is then sent back to the
     * KDC, which responds with a boolean indicating whether the password was
     * valid.  If the password was valid, this function returns the open
     * connection to the KDC.  Otherwise, it closes the connection and returns
     * null.
     *
     * @param username The username to authenticate with.
     * @param password The password to use for authentication.
     * @param host The hostname of the KDC.
     * @param port The port of the KDC.
     * @return An open Channel to the KDC if authentication was successful, or
     *         null if authentication failed.
     */
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

    /**
     * Converts a byte array to a hexadecimal string.
     * @param bytes The byte array to convert.
     * @return A string containing the hexadecimal representation of the byte array.
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Combines an initialization vector (IV) and cipher text into a single base64-encoded string.
     * @param iv The base64-encoded IV.
     * @param cipherText The base64-encoded cipher text.
     * @return A new string containing the combined IV and cipher text, both still base64-encoded.
     */
    public static String combineIVandCipher(String iv, String cipherText) {
        byte[] ivBytes = Base64.getDecoder().decode(iv);
        byte[] cipherBytes = Base64.getDecoder().decode(cipherText);
        byte[] combined = new byte[ivBytes.length + cipherBytes.length];
        System.arraycopy(ivBytes, 0, combined, 0, ivBytes.length);
        System.arraycopy(cipherBytes, 0, combined, ivBytes.length, cipherBytes.length);
        return Base64.getEncoder().encodeToString(combined);
    }


    /**
     * Connect to the service and establish a secure communication channel.
     * 
     * This method will establish a secure channel with the service by performing the
     * necessary steps of the protocol. It will first generate a fresh nonce Nc and
     * send a ClientHello to the service with the ticket and Nc. Then, it will receive
     * a HandshakeResponse from the service and decrypt it to obtain the nonce Nc.
     * If the decrypted nonce does not match the original nonce Nc, the method will
     * throw a SecurityException.
     * 
     * After verifying the nonce, the method will generate a fresh nonce Nr and send
     * a ClientResponse to the service with the nonce Nr and the encrypted nonce Ns.
     * Finally, the method will start a communication phase where it will read
     * messages from the user, encrypt them using AES-GCM, and send them to the
     * service. The method will also receive encrypted responses from the service,
     * decrypt them, and print them to the console.
     * 
     * @param response the response from the KDC containing the ticket and session key
     * @param base64SessionKey the session key in base64 encoding
     * @throws IOException if an I/O error occurs during the communication with the service
     * @throws GeneralSecurityException if a security error occurs during the protocol
     */
    public static void connectToService(TicketResponse response, String base64SessionKey) {
    try {
        Tuple<String, Integer> serviceHost = getHostInfo(service);
        Socket socket = new Socket(serviceHost.getFirst(), serviceHost.getSecond());
        Channel serviceChannel = new Channel(socket);

        // Step 1: Derive session key from password
        byte[] sessionKeyBytes = Base64.getDecoder().decode(base64SessionKey);
        SecretKeySpec ks = new SecretKeySpec(sessionKeyBytes, "AES");

        // Step 2: Generate fresh nonce Nc
        byte[] nonceClient = new byte[16];
        new SecureRandom().nextBytes(nonceClient);
        String base64Nc = Base64.getEncoder().encodeToString(nonceClient);

        // Step 3: Send ClientHello (Ticket + Nc)
        JSONObject ticketJson = (JSONObject) response.getTicket().toJSONType();
        ClientHello hello = new ClientHello(ticketJson, base64Nc);
        serviceChannel.sendMessage(hello);

        // Step 4: Receive HandshakeResponse
        JSONObject responseJson = serviceChannel.receiveMessage();
        HandshakeResponse handshake = new HandshakeResponse("", "", "", "");
        handshake.deserialize(responseJson);

        // Step 5: Decrypt Enc(Nc) and verify it matches original
        String encNcCombined = combineIVandCipher(handshake.getIv(), handshake.getEncryptedNonce());
        byte[] decrypted = CryptoUtils.decryptAESGCMToBytes(encNcCombined, ks);
        if (!Base64.getEncoder().encodeToString(decrypted).equals(base64Nc)) {
            throw new SecurityException("❌ Server failed to prove knowledge of session key.");
        }


        // Step 6: Generate fresh Nr and send ClientResponse
        byte[] nonceR = new byte[16];
        new SecureRandom().nextBytes(nonceR);
        String base64Nr = Base64.getEncoder().encodeToString(nonceR);

        byte[] nonceS = Base64.getDecoder().decode(handshake.getNonce()); // Ns
        byte[] responseIv = new byte[12];
        new SecureRandom().nextBytes(responseIv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, ks, new GCMParameterSpec(128, responseIv));
        byte[] encNs = cipher.doFinal(nonceS);

        ClientResponse finalResp = new ClientResponse(
            base64Nr,
            user,
            Base64.getEncoder().encodeToString(responseIv),
            Base64.getEncoder().encodeToString(encNs)
        );
        serviceChannel.sendMessage(finalResp);

        System.out.println("🟢 Secure service session started. Type a message or /quit to exit.");
Scanner sc = new Scanner(System.in);

  // Start the communication phase
  while (true) {
    System.out.print("Message to send: ");
    String input = sc.nextLine();

    if (input.equalsIgnoreCase("/quit")) {
        System.out.println("👋 Exiting session.");
        serviceChannel.close();
        break;
    }

    // Construct message JSON with nonce, user, service, and message
    byte[] msgIv = new byte[12];
    new SecureRandom().nextBytes(msgIv);
    byte[] msgNonce = new byte[16];
    new SecureRandom().nextBytes(msgNonce);
    String base64Nonce = Base64.getEncoder().encodeToString(msgNonce);

    JSONObject payload = new JSONObject();
    payload.put("nonce", base64Nonce);
    payload.put("user", user);
    payload.put("service", service);
    payload.put("message", input);
    
    String payloadStr = payload.toString();  // Correct way to convert to a string
    Cipher encryptCipher = Cipher.getInstance("AES/GCM/NoPadding");
    encryptCipher.init(Cipher.ENCRYPT_MODE, ks, new GCMParameterSpec(128, msgIv));
    byte[] encryptedMessage = encryptCipher.doFinal(payloadStr.getBytes(StandardCharsets.UTF_8));

    JSONObject msgObj = new JSONObject();
    msgObj.put("message", Base64.getEncoder().encodeToString(encryptedMessage));
    msgObj.put("iv", Base64.getEncoder().encodeToString(msgIv));

    // Send the encrypted message to EchoService
    serviceChannel.sendMessage(msgObj);
  // Receive encrypted response
    JSONObject respJson = serviceChannel.receiveMessage();
    byte[] respIv = Base64.getDecoder().decode(respJson.getString("iv"));
    byte[] respCipher = Base64.getDecoder().decode(respJson.getString("message"));

    Cipher decryptCipher = Cipher.getInstance("AES/GCM/NoPadding");
    decryptCipher.init(Cipher.DECRYPT_MODE, ks, new GCMParameterSpec(128, respIv));
    byte[] decryptedResponse = decryptCipher.doFinal(respCipher);

    String responseText = new String(decryptedResponse, StandardCharsets.UTF_8);
    System.out.println("Response: " + responseText);
    System.out.println("thank you for using the service");
    System.exit(0);
}
} catch (Exception e) {
System.err.println("❌ Error during encrypted message exchange: " + e.getMessage());
e.printStackTrace();
}

   
    }
}