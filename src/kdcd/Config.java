package kdcd;

import java.io.InvalidObjectException;
import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

public class Config implements JSONSerializable {
    public String secretsFile;
    public int port;
    public long validityPeriod;

    /**
     * Deserialize a Config from a JSONType.
     * 
     * This method expects the JSONType to be a JSONObject with the following
     * fields:
     * 
     * <ul>
     * <li>secrets-file: A string representing the path to the file containing
     * the secrets.
     * <li>port: An integer representing the port to listen on.
     * <li>validity-period: A long representing the maximum time in milliseconds
     * that a ticket is valid for.
     * </ul>
     * 
     * @param json The JSONType containing the serialized Config.
     * @throws InvalidObjectException If the JSONType is not a JSONObject, or
     *             if the JSONObject does not contain the expected fields.
     */
    @Override
    public void deserialize(JSONType json) throws InvalidObjectException {
        if (!(json instanceof JSONObject)) {
            throw new InvalidObjectException("Invalid JSON format for Config");
        }
        JSONObject jsonObject = (JSONObject) json;
        this.secretsFile = jsonObject.getString("secrets-file");
        this.port = jsonObject.getInt("port");
        this.validityPeriod = jsonObject.getLong("validity-period");
    }

    /**
     * Serialize this Config to a JSONType.
     * 
     * The serialized JSONType is a JSONObject with the following fields:
     * 
     * <ul>
     * <li>secrets-file: A string representing the path to the file containing
     * the secrets.
     * <li>port: An integer representing the port to listen on.
     * <li>validity-period: A long representing the maximum time in milliseconds
     * that a ticket is valid for.
     * </ul>
     */
    @Override
    public JSONType toJSONType() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("secrets-file", this.secretsFile);
        jsonObject.put("port", this.port);
        jsonObject.put("validity-period", this.validityPeriod);
        return jsonObject;
    }

}
