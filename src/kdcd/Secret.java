package kdcd;

import java.io.InvalidObjectException;
import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

public class Secret implements JSONSerializable {
    public String user;
    public String secret;

    /**
     * Deserialize a Secret from a JSONType.
     * 
     * This method expects the JSONType to be a JSONObject with the following
     * fields:
     * 
     * <ul>
     * <li>user: A string representing the user associated with the secret.
     * <li>secret: A string representing the secret itself.
     * </ul>
     * 
     * @param json The JSONType containing the serialized Secret.
     * @throws InvalidObjectException If the JSONType is not a JSONObject, or
     *             if the JSONObject does not contain the expected fields.
     */
    @Override
    public void deserialize(JSONType json) throws InvalidObjectException {
        if (!(json instanceof JSONObject)) {
            throw new InvalidObjectException("Invalid JSON format for Secret");
        }
        JSONObject jsonObject = (JSONObject) json;
        this.user = jsonObject.getString("user");
        this.secret = jsonObject.getString("secret");
    }

    /**
     * Serialize this Secret to a JSONType.
     * 
     * The serialized JSONType is a JSONObject with the following fields:
     * 
     * <ul>
     * <li>user: A string representing the user associated with the secret.
     * <li>secret: A string representing the secret itself.
     * </ul>
     */
    @Override
    public JSONType toJSONType() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("user", this.user);
        jsonObject.put("secret", this.secret);
        return jsonObject;
    }

}