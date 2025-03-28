package common;

import merrimackutil.json.types.JSONObject;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class MessageQueueServer {
    private static final int PORT = 5555;
    private static final ConcurrentHashMap<String, BlockingQueue<JSONObject>> userQueues = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Message Queue Server started on port " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to add a message to the user's queue
    public static void putMessage(String user, JSONObject message) throws InterruptedException {
        System.out.println("Stored message for user " + user + ": " + message.getFormattedJSON());
        getQueue(user).put(message); // Store the message in the queue for the user
    }

    // Method to take a message from the user's queue
    public static JSONObject takeMessage(String user) throws InterruptedException {
        return getQueue(user).take(); // Retrieve the next message for the user
    }

    // Helper method to get or create a queue for the user
    private static BlockingQueue<JSONObject> getQueue(String user) {
        return userQueues.computeIfAbsent(user, k -> new LinkedBlockingQueue<>());
    }

    // Client handler that processes requests
    private static class ClientHandler extends Thread {
        private final Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {

                String line;
                while ((line = reader.readLine()) != null) {
                    JSONObject request = merrimackutil.json.JsonIO.readObject(line);  // Read the object from string
                    String type = request.getString("type");
                    String user = request.getString("user");

                    if ("PUT".equals(type)) {
                        JSONObject message = request.getObject("message");
                        putMessage(user, message); // Store message in the queue for the user
                        writer.println("{\"status\":\"Message stored\"}");
                    } else if ("TAKE".equals(type)) {
                        JSONObject message = takeMessage(user); // Take message from the queue for the user
                        writer.println(message.getFormattedJSON()); // Send the message back
                    }
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
