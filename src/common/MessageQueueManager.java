package common;

import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;
import merrimackutil.json.JSONSerializable;
import merrimackutil.json.JsonIO;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import javax.crypto.*;
import javax.crypto.spec.*;

public class MessageQueueManager implements JSONSerializable {
    private static final String QUEUE_FILE = "message_queue.json";
    private static final String UPPERCASE_QUEUE_FILE = "formatted_queue.json";
    private static final ConcurrentHashMap<String, BlockingQueue<JSONObject>> userQueues = new ConcurrentHashMap<>();
    private static final String SECRET_KEY_FILE = "queue_secret.key";
    private static final int AES_KEY_SIZE = 256;

    static {
        loadMessagesFromFile();
    }

    // Generate or load AES key
    private static SecretKey getSecretKey() throws Exception {
        File keyFile = new File(SECRET_KEY_FILE);
        if (!keyFile.exists()) {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(AES_KEY_SIZE);
            SecretKey secretKey = keyGen.generateKey();
            Files.write(keyFile.toPath(), secretKey.getEncoded());
            return secretKey;
        }
        byte[] keyBytes = Files.readAllBytes(keyFile.toPath());
        return new SecretKeySpec(keyBytes, "AES");
    }

    // Get the queue for a user, creating one if necessary
    private static BlockingQueue<JSONObject> getQueue(String user) {
        return userQueues.computeIfAbsent(user, k -> new LinkedBlockingQueue<>());
    }

    // Store a message for a specific user (encrypt and save to JSON)
    public static void putMessage(String user, JSONObject message) throws Exception {
        if (message == null || user == null || user.isEmpty()) {
            System.err.println("Invalid input: user or message is null.");
            return;
        }

        SecretKey key = getSecretKey();
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

        // Generate a random IV
        byte[] iv = new byte[12];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);

        // Encrypt message
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
        byte[] encryptedData = cipher.doFinal(message.toString().getBytes(StandardCharsets.UTF_8));

        // Store both the iv and the encrypted message in the queue
        JSONObject encryptedMessage = new JSONObject();
        encryptedMessage.put("iv", Base64.getEncoder().encodeToString(iv));
        encryptedMessage.put("message", Base64.getEncoder().encodeToString(encryptedData));

        // Add encrypted message to the queue and save state to file
        getQueue(user).put(encryptedMessage);
        saveMessagesToFile();

        System.out.println("Encrypted message stored for user: " + user);
    }

    // Retrieve and remove a message for a specific user (blocking)
    public static JSONObject takeMessage(String user) throws Exception {
        if (user == null || user.isEmpty()) {
            System.err.println("Invalid user input.");
            return null;
        }

        System.out.println("Waiting for message for user: " + user);
        JSONObject encryptedMessage = getQueue(user).take(); // Blocks until a message is available

        // Decrypt message
        SecretKey key = getSecretKey();
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

        // Get the IV and the encrypted data from the message
        byte[] iv = Base64.getDecoder().decode(encryptedMessage.getString("iv"));
        byte[] encryptedData = Base64.getDecoder().decode(encryptedMessage.getString("message"));

        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
        byte[] decryptedData = cipher.doFinal(encryptedData);

        // Remove message from queue and save the updated state
        getQueue(user).remove(encryptedMessage);
        saveMessagesToFile();

        // Prepare the decrypted message and IV for return
        JSONObject message = new JSONObject();
        message.put("iv", Base64.getEncoder().encodeToString(iv));
        message.put("message", Base64.getEncoder().encodeToString(decryptedData));

        System.out.println("Decrypted message retrieved for user: " + user);
        return message;
    }

    // Load messages from the JSON file into memory
    private static void loadMessagesFromFile() {
        File file = new File(QUEUE_FILE);
        if (!file.exists()) return;

        try {
            JSONObject json = JsonIO.readObject(file);
            for (String user : json.keySet()) {
                Object value = json.get(user);
                if (value instanceof List<?>) {
                    List<?> messagesList = (List<?>) value;
                    List<JSONObject> messages = new ArrayList<>();
                    for (Object msg : messagesList) {
                        if (msg instanceof JSONObject) {
                            messages.add((JSONObject) msg);
                        }
                    }
                    BlockingQueue<JSONObject> queue = new LinkedBlockingQueue<>(messages);
                    userQueues.put(user, queue);
                }
            }
            System.out.println("Loaded message queue from file.");
        } catch (Exception e) {
            System.err.println("Error loading messages: " + e.getMessage());
        }
    }

    // Save the current messages to the JSON file.
    // Each user is stored as a root key with a JSON object having separate "message" and "iv" keys.
    private static void saveMessagesToFile() {
        JSONObject json = new JSONObject();
        for (String user : userQueues.keySet()) {
            // Use peek() so we don’t remove the message from the queue.
            JSONObject message = userQueues.get(user).peek();
            if (message != null) {
                JSONObject formattedMessage = new JSONObject();
                formattedMessage.put("message", message.getString("message"));
                formattedMessage.put("iv", message.getString("iv"));
                json.put(user, formattedMessage);
            }
        }
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(QUEUE_FILE))) {
            writer.write(json.toString());
            System.out.println("Message queue saved to file.");
        } catch (IOException e) {
            System.err.println("Error saving messages: " + e.getMessage());
        }
    }

    // Convert the MessageQueueManager object into a JSON type with the desired format.
    // Each user is a root key with an object containing "message" and "iv".
    @Override
    public JSONObject toJSONType() {
        JSONObject json = new JSONObject();
        for (String user : userQueues.keySet()) {
            JSONObject message = userQueues.get(user).peek();
            if (message != null) {
                JSONObject formattedMessage = new JSONObject();
                formattedMessage.put("message", message.getString("message"));
                formattedMessage.put("iv", message.getString("iv"));
                json.put(user, formattedMessage);
            }
        }
        return json;
    }

    // Create a JSON type where the message is converted to uppercase.
    public JSONObject toUpperCaseJSONType() {
        JSONObject json = new JSONObject();
        for (String user : userQueues.keySet()) {
            JSONObject encryptedMessage = userQueues.get(user).peek();
            if (encryptedMessage != null) {
                try {
                    SecretKey key = getSecretKey();
                    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                    byte[] iv = Base64.getDecoder().decode(encryptedMessage.getString("iv"));
                    byte[] encryptedData = Base64.getDecoder().decode(encryptedMessage.getString("message"));
                    cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
                    byte[] decryptedData = cipher.doFinal(encryptedData);
                    String plainMessage = new String(decryptedData, StandardCharsets.UTF_8);
                    
                    JSONObject formattedMessage = new JSONObject();
                    formattedMessage.put("message", plainMessage.toUpperCase());
                    formattedMessage.put("iv", encryptedMessage.getString("iv"));
                    json.put(user, formattedMessage);
                } catch (Exception e) {
                    System.err.println("Error decrypting message for user " + user + ": " + e.getMessage());
                }
            }
        }
        return json;
    }

    // Save the uppercase JSON to a separate file, e.g., formatted_queue.json.
    public static void saveUpperCaseMessagesToFile() {
        MessageQueueManager manager = new MessageQueueManager();
        JSONObject upperCaseJson = manager.toUpperCaseJSONType();
        File file = new File(UPPERCASE_QUEUE_FILE);
        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath())) {
            writer.write(upperCaseJson.toString());
            System.out.println("Formatted (uppercase) message queue saved to file.");
        } catch (IOException e) {
            System.err.println("Error saving formatted messages: " + e.getMessage());
        }
    }

    @Override
    public void deserialize(JSONType jsonType) throws InvalidObjectException {
        if (!(jsonType instanceof JSONObject)) {
            throw new InvalidObjectException("Expected a JSONObject for deserialization.");
        }
        JSONObject json = (JSONObject) jsonType;
        userQueues.clear();
        for (String user : json.keySet()) {
            Object value = json.get(user);
            // Here we expect a single JSON object per user.
            if (value instanceof JSONObject) {
                JSONObject msgObj = (JSONObject) value;
                List<JSONObject> messages = new ArrayList<>();
                messages.add(msgObj);
                userQueues.put(user, new LinkedBlockingQueue<>(messages));
            }
        }
        System.out.println("Message queues deserialized from JSON.");
    }
}