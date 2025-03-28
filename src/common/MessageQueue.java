package common;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import merrimackutil.json.types.JSONObject;

public class MessageQueue {
    private static final BlockingQueue<JSONObject> queue = new ArrayBlockingQueue<>(1); // A simple queue to hold one message

    // Put a message into the queue
    public static void putMessage(JSONObject msg) throws InterruptedException {
        queue.put(msg); // This will block if the queue is full
    }

    // Take a message from the queue
    public static JSONObject takeMessage() throws InterruptedException {
        return queue.take(); // This will block if the queue is empty
    }
}
