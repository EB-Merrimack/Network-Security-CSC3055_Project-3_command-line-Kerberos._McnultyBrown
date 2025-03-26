package client;

import java.io.InvalidObjectException;
import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

public class Host implements JSONSerializable {
    public String address;
    public int port;
    public String hostName;

    /**
     * Deserialize a Host from a JSONType.
     * 
     * This method expects the JSONType to be a JSONObject with the following
     * fields:
     * 
     * <ul>
     * <li>address: A string representing the host address.</li>
     * <li>port: An integer representing the port.</li>
     * <li>host-name: A string representing the host name.</li>
     * </ul>
     * 
     * @param json The JSONType containing the serialized Host.
     * @throws InvalidObjectException If the JSONType is not a JSONObject, or
     *             if the JSONObject does not contain the expected fields.
     */
    @Override
    public void deserialize(JSONType json) throws InvalidObjectException {
        if (!(json instanceof JSONObject)) {
            throw new InvalidObjectException("Invalid JSON format for Host");
        }
        JSONObject jsonObject = (JSONObject) json;
        this.address = jsonObject.getString("address");
        this.port = jsonObject.getInt("port");
        this.hostName = jsonObject.getString("host-name");
    }

    /**
     * Serialize this Host to a JSONType.
     * 
     * The serialized JSONType is a JSONObject with the following fields:
     * 
     * <ul>
     * <li>address: A string representing the host address.</li>
     * <li>port: An integer representing the port.</li>
     * <li>host-name: A string representing the host name.</li>
     * </ul>
     */
    @Override
    public JSONType toJSONType() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("address", this.address);
        jsonObject.put("port", this.port);
        jsonObject.put("host-name", this.hostName);
        return jsonObject;
    }
}
