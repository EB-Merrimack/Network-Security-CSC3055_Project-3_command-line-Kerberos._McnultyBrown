package common;

import java.util.concurrent.*;

import merrimackutil.json.types.JSONObject;

public class MessageQueueManager {
    private static final ConcurrentHashMap<String, BlockingQueue<JSONObject>> userQueues = new ConcurrentHashMap<>();

    // Get the queue for a user, creating one if necessary
    private static BlockingQueue<JSONObject> getQueue(String user) {
        return userQueues.computeIfAbsent(user, k -> new LinkedBlockingQueue<>());
    }

    // Store a message for a specific user
    public static void putMessage(String user, JSONObject message) throws InterruptedException {
        getQueue(user).put(message);
        System.out.println("Message stored for user: " + user + " -> " + message.getFormattedJSON());
    }

    // Retrieve a message for a specific user (blocking)
    public static JSONObject takeMessage(String user) throws InterruptedException {
        System.out.println("Waiting for message for user: " + user);
        return getQueue(user).take(); // Blocks until a message is available
    }
}
