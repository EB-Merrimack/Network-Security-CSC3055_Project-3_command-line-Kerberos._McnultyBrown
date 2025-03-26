package kdcd;

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
            "kdcd\n" +
            "kdcd --config <configfile>\n" +
            "kdcd --help\n" +
            "options:\n" +
            "  -c, --config <configfile>   Set the config file.\n" +
            "  -h, --help                  Display the help.\n");
    }

/**
 * Deserializes a JSONType into the UsageMessage object.
 *
 * This method expects the JSONType to be a JSONObject containing a 
 * "usage" field. The value of this field is extracted and stored 
 * in the internal JSONObject of this object.
 *
 * @param arg0 The JSONType to be deserialized, expected to be a JSONObject.
 * @throws InvalidObjectException If the input JSONType is not a JSONObject.
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
 * Converts the UsageMessage object to a JSONType.
 *
 * This method provides a JSON representation of the UsageMessage
 * by returning the internal JSONObject that stores the usage message.
 *
 * @return A JSONType representing the UsageMessage.
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
