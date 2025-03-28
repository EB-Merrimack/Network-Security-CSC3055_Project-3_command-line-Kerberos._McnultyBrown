package signature;

import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;
import java.io.InvalidObjectException;

public class SignatureUsageMessage implements JSONSerializable {

    private JSONObject jsonObject;

    public SignatureUsageMessage() {
        // Initialize the JSONObject to store the message
        jsonObject = new JSONObject();
        jsonObject.put("usage", 
            "Signature Service\n" +
            "Usage:\n" +
            "  signature --config <configfile>\n" +
            "  signature --help\n" +
            "Options:\n" +
            "  -c, --config <configfile>   Set the config file (default is src/signatureservice/config.json).\n" +
            "  -h, --help                  Display the help message.\n");
    }

    /**
     * Deserialize a SignatureUsageMessage from a JSONType.
     * 
     * This method expects the JSONType to be a JSONObject with the following
     * field:
     * 
     * <ul>
     * <li>usage: A string representing the usage message itself.
     * </ul>
     * 
     * @param json The JSONType containing the serialized SignatureUsageMessage.
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
     * Serialize this SignatureUsageMessage to a JSONType.
     * 
     * The serialized JSONType is a JSONObject with a single field,
     * "usage", which is a string representing the usage message itself.
     * 
     * @return The JSONType containing the serialized SignatureUsageMessage.
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
