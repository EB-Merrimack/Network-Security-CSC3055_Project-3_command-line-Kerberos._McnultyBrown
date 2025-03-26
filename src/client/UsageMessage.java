package client;

import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;
import java.io.InvalidObjectException;

public class UsageMessage implements JSONSerializable {

    private JSONObject jsonObject;

    public UsageMessage() {
        // Initialize the JSONObject to store the message
        jsonObject = new JSONObject();
        jsonObject.put("usage", 
            "usage: \n" +
            "    client --hosts <hostfile> --user <user> --service <service>\n" +
            "    client --user <user> --service <service>\n" +
            "options: \n" +
            "    -h, --hosts       Set the hosts file.\n" +
            "    -u, --user        The user name.\n" +
            "    -s, --service     The name of the service.\n");
    }

    /**
     * Deserialize a UsageMessage from a JSONType.
     * 
     * This method expects the JSONType to be a JSONObject with the following
     * field:
     * 
     * <ul>
     * <li>usage: A string representing the usage message itself.</li>
     * </ul>
     * 
     * @param json The JSONType containing the serialized UsageMessage.
     * @throws InvalidObjectException If the JSONType is not a JSONObject, or
     *             if the JSONObject does not contain the expected field.
     */
    @Override
    public void deserialize(JSONType arg0) throws InvalidObjectException {
        if (arg0 instanceof JSONObject) {
            JSONObject jsonObj = (JSONObject) arg0;
            // Extract the usage message and set it
            String usage = jsonObj.getString("usage");
            jsonObject.put("usage", usage);  // Store the usage message in this object's JSONObject
        } else {
            throw new InvalidObjectException("Expected a JSONObject for deserialization");
        }
    }

    /**
     * Serialize this UsageMessage to a JSONType.
     * 
     * The serialized JSONType is a JSONObject with a single field,
     * "usage", which is a string representing the usage message itself.
     * 
     * @return The JSONType containing the serialized UsageMessage.
     */
    @Override
    public JSONType toJSONType() {
        // Return the internal JSONObject as the JSON representation
        return jsonObject;
    }

    // Optionally, provide a method to access the usage message directly
    public String getUsageMessage() {
        return jsonObject.getString("usage");
    }

}
