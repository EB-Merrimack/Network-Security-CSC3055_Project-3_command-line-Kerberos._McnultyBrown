package client;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

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


    public static void connectToService(TicketResponse response, String password) {
    try {
        Tuple<String, Integer> serviceHost = getHostInfo(service);
        Socket socket = new Socket(serviceHost.getFirst(), serviceHost.getSecond());
        Channel serviceChannel = new Channel(socket);

        // Step 1: Derive session key from password
        String combined = combineIVandCipher(response.getTicket().getIv(), response.getSessionKey());
        String decryptedSessionKey = CryptoUtils.decryptAESGCM(combined, password);
        byte[] sessionKeyBytes = Base64.getDecoder().decode(decryptedSessionKey);
        SecretKeySpec ks = new SecretKeySpec(sessionKeyBytes, "AES");

        // Step 2: Generate fresh nonce Nc
        byte[] nonceClient = new byte[16];
        new SecureRandom().nextBytes(nonceClient);
        String base64Nc = Base64.getEncoder().encodeToString(nonceClient);

        // Step 3: Send ClientHello (Ticket + Nc)
        JSONObject ticketJson = (JSONObject) response.getTicket().toJSONType();
        ClientHello hello = new ClientHello(ticketJson, base64Nc);
        serviceChannel.sendMessage(hello);
        System.out.println("📤 Sent ClientHello");

        // Step 4: Receive HandshakeResponse
        JSONObject responseJson = serviceChannel.receiveMessage();
        HandshakeResponse handshake = new HandshakeResponse("", "", "", "");
        handshake.deserialize(responseJson);
        System.out.println("📥 Received HandshakeResponse");

        // Step 5: Decrypt Enc(Nc) and verify it matches original
        String encNcCombined = combineIVandCipher(handshake.getIv(), handshake.getEncryptedNonce());
        byte[] decrypted = CryptoUtils.decryptAESGCM(encNcCombined, ks).getBytes();
        if (!Base64.getEncoder().encodeToString(decrypted).equals(base64Nc)) {
            throw new SecurityException("❌ Server failed to prove knowledge of session key.");
        }

        System.out.println("✅ Verified encrypted Nc matches");

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
        System.out.println("📤 Sent ClientResponse");

        // From here, you can enter the encrypted communication phase.

    } catch (Exception e) {
        System.err.println("❌ Error during handshake with service: " + e.getMessage());
        e.printStackTrace();
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



                System.out.println("Ticket and session key received");
                System.out.println("Session key (base64): " + base64SessionKey);

                connectToService(resp, password);

                channel.close();
            } catch (Exception e) {
                System.err.println("Error requesting session key: " + e.getMessage());
            }
        }
    }
}