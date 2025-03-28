package common;

import java.io.*;
import java.net.Socket;
import merrimackutil.json.JsonIO;
import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

public class Channel implements JSONSerializable {
    private final Socket socket;
    private final BufferedReader reader;
    private final PrintWriter writer;
    private JSONObject echoedMessage = null; // Holds the echoed message
    private boolean messageAvailable = false; // Flag to indicate when a message is available


    public Channel(Socket socket) throws IOException {
        this.socket = socket;
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
    }

    /**
     * Takes a JSON object and makes it serializable to be able to be written 
     * through the send message method.
     * 
     * @param message The message to send, as a JSONObject.
     */
    public void sendMessage(JSONObject jsonMessage) {
        writer.println(jsonMessage.getFormattedJSON());
        System.out.println("Sent: " + jsonMessage.getFormattedJSON());
    }

    /**
     * Send a message over the channel to the socket. 
     * Using write serialized object.
     * 
     * @param message The message to send, as a JSONSerializable object.
     */
    public void sendMessage(JSONSerializable message) {
        // Use JsonIO.writeSerializedObject to serialize and send the JSON object
        JsonIO.writeSerializedObject(message, writer);
        System.out.println("Sent: " + message.toString());
    }

    /**
     * Receives a message from the channel.
     * 
     * This method reads a line from the input stream of the socket, 
     * deserializes it into a JSONObject, and returns it. If the connection 
     * is closed by the peer, an IOException is thrown.
     * 
     * @return The received message as a JSONObject.
     * @throws IOException If the connection is closed by the peer.
     */
    public JSONObject receiveMessage() throws IOException {
        String line = reader.readLine();
        if (line == null) {
            throw new IOException("Connection closed by peer");
        }
        System.out.println("Received: " + line);
        return JsonIO.readObject(line); // Deserialize received string into JSONObject
    }

    /**
     * Close the channel and associated socket.
     * 
     * This method calls the close methods of the reader, writer, and socket in
     * order to release any system resources associated with the channel.
     * 
     * If an IOException is thrown in the process of closing the channel, an
     * error message is printed to System.err.
     */
    public void close() {
        try {
            reader.close();
            writer.close();
            socket.close();
        } catch (IOException e) {
            System.err.println("Error closing channel: " + e.getMessage());
        }
    }

    /**
     * Deserialize a Channel from a JSONType.
     * 
     * This method expects the JSONType to be a JSONObject. If the JSONType is
     * not a JSONObject, an InvalidObjectException is thrown.
     * 
     * @param json The JSONType containing the serialized Channel.
     * @throws InvalidObjectException If the JSONType is not a JSONObject.
     */
    @Override
    public void deserialize(JSONType json) throws InvalidObjectException {
        if (!(json instanceof JSONObject)) {
            throw new InvalidObjectException("Invalid JSON format for Channel");
        }
    }

    /**
     * Serialize the Channel to a JSONType.
     * 
     * The serialized JSONType is an empty JSONObject, as the Channel 
     * currently does not contain serializable fields.
     * 
     * @return The JSONType containing the serialized Channel.
     */
    @Override
    public JSONType toJSONType() {
        return new JSONObject();
    }

    public PrintWriter getWriter() {
        return writer; // Return the PrintWriter instance to allow other methods to use it
    }

    public InputStream getInputStream() {
        try {
            return socket.getInputStream(); // Return the input stream of the socket
        } catch (IOException e) {
            throw new UnsupportedOperationException("Error getting InputStream: " + e.getMessage());
        }
    }

    public OutputStream getOutputStream() {
        try {
            return socket.getOutputStream(); // Return the output stream of the socket
        } catch (IOException e) {
            throw new UnsupportedOperationException("Error getting OutputStream: " + e.getMessage());
        }
    }

    /**
     * This method sends an echo message, containing both the IV and message.
     * The method assumes that the message and IV are properly formatted in JSON.
     * 
     * @param iv The Initialization Vector (IV) for encryption.
     * @param message The message to send, typically a JSON object.
     */
   /**
     * Sends an echo message containing the IV and the encrypted message.
     * 
     * @param msgObj The JSON object that contains both the IV and encrypted message.
     */
    public void sendechoMessage(JSONObject msgObj) {
        sendMessage(msgObj);
        System.out.println("Echo Message Sent: " + msgObj.getFormattedJSON());

        try {
            // Put the message in the shared queue
            MessageQueue.putMessage(msgObj);
            System.out.println("Message stored in the shared queue: " + msgObj.getFormattedJSON());
        } catch (InterruptedException e) {
            System.err.println("Error putting message in the queue: " + e.getMessage());
        }
    }

    /**
     * This method waits for a response to an echo message and returns it once received.
     * It holds the message in the queue until the server calls for it.
     * 
     * @return The echoed message as a JSONObject.
     * @throws IOException If the connection is closed by the peer.
     */
    public JSONObject receiveEchoMessage() throws IOException {
        try {
            System.out.println("Waiting for message from shared queue...");
            // Take the message from the shared queue (this will block until a message is available)
            JSONObject message = MessageQueue.takeMessage();
            System.out.println("Message Retrieved from shared queue: " + message.getFormattedJSON());
            return message;
        } catch (InterruptedException e) {
            throw new IOException("Error receiving message from queue", e);
        }
    }
}
