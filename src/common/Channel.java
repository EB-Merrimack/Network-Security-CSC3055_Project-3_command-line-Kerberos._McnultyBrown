package common;

import java.io.*;
import java.net.Socket;
import merrimackutil.json.JsonIO;
import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;
import java.io.InvalidObjectException;

public class Channel implements JSONSerializable {
    private final Socket socket;
    private final BufferedReader reader;
    private final PrintWriter writer;

    public Channel(Socket socket) throws IOException {
        this.socket = socket;
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
    }

    public void sendMessage(JSONObject jsonMessage2) {
            // Create a JSONObject and wrap the string message inside it
            JSONObject jsonMessage = new JSONObject();
            jsonMessage.put("message", jsonMessage2);

        // Send the JSONObject using the existing sendMessage method
        sendMessage(jsonMessage);
    }

    public void sendMessage(JSONSerializable message) {
        // Use JsonIO.writeSerializedObject to serialize and send the JSON object
        JsonIO.writeSerializedObject(message, writer);
        System.out.println("Sent: " + message.toString());
    }

    public JSONObject receiveMessage() throws IOException {
        String line = reader.readLine();
        if (line == null) {
            throw new IOException("Connection closed by peer");
        }
        System.out.println("Received: " + line);
        return JsonIO.readObject(line); // Deserialize received string into JSONObject
    }

    public void close() {
        try {
            reader.close();
            writer.close();
            socket.close();
        } catch (IOException e) {
            System.err.println("Error closing channel: " + e.getMessage());
        }
    }

    @Override
    public void deserialize(JSONType json) throws InvalidObjectException {
        if (!(json instanceof JSONObject)) {
            throw new InvalidObjectException("Invalid JSON format for Channel");
        }
    }

    @Override
    public JSONType toJSONType() {
        return new JSONObject();
    }

    public PrintWriter getWriter() {
        return writer; // Return the PrintWriter instance to allow other methods to use it
    }
}
