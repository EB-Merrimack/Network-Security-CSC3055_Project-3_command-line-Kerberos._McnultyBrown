package echoservice;

import java.io.InvalidObjectException;

import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

public class Config implements JSONSerializable {
    public int port;
    public boolean debug;
    public String serviceName;
    public String serviceSecret;

/**
 * Deserializes the configuration from the provided JSONType.
 * 
 * This method expects the JSONType to be a JSONObject with the following
 * fields:
 * 
 * <ul>
 * <li>port: An integer representing the port number.
 * <li>debug: A boolean indicating if debug mode is enabled.
 * <li>service-name: A string representing the name of the service.
 * <li>service-secret: A string containing the service's secret.
 * </ul>
 * 
 * @param json The JSONType containing the serialized configuration.
 * @throws InvalidObjectException If the JSONType is not a JSONObject, or
 *             if the JSONObject does not contain the expected fields.
 */

    @Override
    public void deserialize(JSONType json) throws InvalidObjectException {
        if (!(json instanceof JSONObject)) {
            throw new InvalidObjectException("Invalid JSON format for Config");
        }
        JSONObject jsonObject = (JSONObject) json;
        this.port = jsonObject.getInt("port");
        this.debug = jsonObject.getBoolean("debug");
        this.serviceName = jsonObject.getString("service-name");
        this.serviceSecret = jsonObject.getString("service-secret");

      
    }
    /**
     * Serializes the configuration to a JSONType.
     * 
     * The serialized JSONType is a JSONObject with the following fields:
     * 
     * <ul>
     * <li>port: An integer representing the port number.
     * <li>debug: A boolean indicating if debug mode is enabled.
     * <li>service-name: A string representing the name of the service.
     * <li>service-secret: A string containing the service's secret.
     * </ul>
     * 
     * @return The JSONType containing the serialized configuration.
     */
    @Override
    public JSONType toJSONType() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("port", this.port);
        jsonObject.put("debug", this.debug);
        jsonObject.put("service-name", this.serviceName);
        jsonObject.put("service-secret", this.serviceSecret);
        return jsonObject;
    }
}
