package signature;

import java.io.IOException;
import java.io.InputStreamReader;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;

import common.Channel;
import merrimackutil.util.NonceCache;
import merrimackutil.json.JsonIO;
import merrimackutil.json.types.JSONObject;

public class SignatureServiceHandler implements Runnable {

    private static final String DEFAULT_CONFIG_FILE = "src/signatureservice/config.json";
    private Channel channel;
    private NonceCache nonceCache;
    private Config config;
    private PrivateKey signingKey;

    public SignatureServiceHandler(Channel channel, NonceCache nonceCache, Config config, PrivateKey signingKey) {
        this.channel = channel;
        this.nonceCache = nonceCache;
        this.config = config;
        this.signingKey = signingKey;
    }

    public static void usageClient(Channel channel) {
        // Create an instance of the UsageMessage class
        SignatureUsageMessage usageMessage = new SignatureUsageMessage();
    
        // Send the serialized JSON object over the channel using JsonIO.writeSerializedObject
        JsonIO.writeSerializedObject(usageMessage, channel.getWriter());  // Ensure you're passing PrintWriter
    
        // Exit the program
        System.exit(1);
    }

    public static void main(String[] args) throws IOException {
        String configFile = DEFAULT_CONFIG_FILE;

        // Channel and config initialization should happen before usageClient
        Channel channel = new Channel(null); // Create a new Channel instance
        Config config = new Config();  // Assuming Config is a class that loads the config

        if (args.length == 0) {
            // No args? Use default config
            System.out.println("🛠 Using default config file: " + configFile);
        } else if (args.length == 2 && (args[0].equals("-c") || args[0].equals("--config"))) {
            configFile = args[1];
        } else if (args.length == 1 && (args[0].equals("-h") || args[0].equals("--help"))) {
            usageClient(channel);
            return;
        } else {
            System.err.println("Error: Unrecognized arguments.");
            usageClient(channel);
            return;
        }

        PrivateKey signingKey = config.getSigningKey();  // Assuming your config has the signing key

        // Start server
        startServer(configFile, channel, config, signingKey);
    }

    @Override
    public void run() {
        try {
            // Handle incoming requests and respond accordingly
            JSONObject receivedMessage = JsonIO.readObject(new InputStreamReader(channel.getInputStream()));

            // Ensure valid JSON was received
            if (receivedMessage == null) {
                sendErrorResponse("Invalid or empty message");
                return;
            }

            // Check if the request is a valid signing request
            if ("Signature Request".equals(receivedMessage.getString("type"))) {
                // Validate nonce to prevent replay attacks
                String nonce = receivedMessage.getString("nonce");
                byte[] nonceBytes = nonce.getBytes();  // Convert nonce from String to byte[]

                // Check if the nonce already exists in the cache
                if (nonceCache.containsNonce(nonceBytes)) {
                    sendErrorResponse("Nonce replay detected! Request rejected.");
                    return;
                }

                // Add the nonce to the cache
                nonceCache.addNonce(nonceBytes);

                // Extract the message from the request
                String message = receivedMessage.getString("message");

                // Generate the signature
                String signature = generateSignature(message);

                if (signature == null) {
                    sendErrorResponse("Failed to generate signature");
                    return;
                }

                // Prepare response with the signature
                JSONObject response = new JSONObject();
                response.put("type", "Signature Response");
                response.put("signature", signature);

                // Send the signature back to the client using channel
                channel.sendMessage(response); // Use sendMessage from Channel
            } else {
                // Handle invalid request (could log or send an error message)
                sendErrorResponse("Invalid request type: " + receivedMessage.getString("type"));
            }
        } catch (IOException | IllegalArgumentException e) {
            // Handle other exceptions
            e.printStackTrace();
            sendErrorResponse("Error processing request: " + e.getMessage());
        }
    }

    private void sendErrorResponse(String errorMessage) {
        JSONObject response = new JSONObject();
        response.put("type", "Error");
        response.put("message", errorMessage);

        // Send error response using channel
        channel.sendMessage(response);
    }

    private String generateSignature(String message) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(signingKey);
            signature.update(message.getBytes());

            // Generate the signed message (signature)
            byte[] signedBytes = signature.sign();
            return Base64.getEncoder().encodeToString(signedBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;  // Return null if signature generation fails
        }
    }

    private static void startServer(String configFile, Channel channel, Config config, PrivateKey signingKey) {
        // Initialize the SignatureServiceHandler and start handling requests
        SignatureServiceHandler handler = new SignatureServiceHandler(channel, new NonceCache(), config, signingKey);
        new Thread(handler).start();
    }
}
