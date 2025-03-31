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
        writer.println(jsonMessage.getFormattedJSON().toString());
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
        StringBuilder messageBuilder = new StringBuilder();
        String line;
    
        // Read lines until we get a complete message (i.e., valid JSON)
        while ((line = reader.readLine()) != null) {
            // Check if the connection is closed
            if (line.isEmpty()) {
                throw new IOException("Connection closed by peer");
            }
    
            // Accumulate lines to form the full message
            messageBuilder.append(line.trim());
    
            // Check if the accumulated message is a valid JSON object
            try {
                // Use JsonIO's isObject() to validate before parsing
                if (JsonIO.readObject(messageBuilder.toString()) != null && (JsonIO.readObject(messageBuilder.toString()).isObject()==true)) {
                    JSONObject jsonObject = JsonIO.readObject(messageBuilder.toString());
                    return jsonObject; // Successfully parsed the complete JSON object
                }
            } catch (Exception e) {
                // If the message is not a valid JSON object, continue reading
                System.err.println("json is parsing please wait " );
                continue;
            }
        }
    
        // If we exit the loop without returning, there was no valid JSON
        throw new IOException("No valid JSON received");
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

    /**
     * Retrieves the PrintWriter associated with this Channel.
     * 
     * @return The PrintWriter instance used for sending messages.
     */

    public PrintWriter getWriter() {
        return writer; // Return the PrintWriter instance to allow other methods to use it
    }

    /**
     * Retrieves the input stream of the socket associated with this Channel.
     * 
     * @return The input stream of the socket.
     * @throws UnsupportedOperationException If an IOException is thrown when
     *             attempting to get the input stream.
     */
    public InputStream getInputStream() {
        try {
            return socket.getInputStream(); // Return the input stream of the socket
        } catch (IOException e) {
            throw new UnsupportedOperationException("Error getting InputStream: " + e.getMessage());
        }
    }

    /**
     * Retrieves the output stream of the socket associated with this Channel.
     * 
     * @return The output stream of the socket.
     * @throws UnsupportedOperationException If an IOException is thrown when
     *             attempting to get the output stream.
     */

    public OutputStream getOutputStream() {
        try {
            return socket.getOutputStream(); // Return the output stream of the socket
        } catch (IOException e) {
            throw new UnsupportedOperationException("Error getting OutputStream: " + e.getMessage());
        }
    }

   
    
}
