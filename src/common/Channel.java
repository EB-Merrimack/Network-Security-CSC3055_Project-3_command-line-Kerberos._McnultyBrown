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

    // Getter for PrintWriter (to send data through the channel)
    public PrintWriter getWriter() {
        return this.writer;
    }

    public void sendMessage(JSONObject message) {
        writer.println(message.toString());
        System.out.println("Sent: " + message.toString());
    }

    public JSONObject receiveMessage() throws IOException {
        String line = reader.readLine();
        if (line == null) {
            throw new IOException("Connection closed by peer");
        }
        System.out.println("Received: " + line);
        return JsonIO.readObject(line);
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
        // In case you need to handle JSON deserialization for the Channel
        if (!(json instanceof JSONObject)) {
            throw new InvalidObjectException("Invalid JSON format for Channel");
        }
        // For example, you might populate the Channel object from the JSON
    }

    @Override
    public JSONType toJSONType() {
        // Convert Channel's relevant data to JSON if needed
        JSONObject json = new JSONObject();
        json.put("socket", socket.toString());
        return json;
    }
}
