package signature;

import java.io.InvalidObjectException;
import java.security.PrivateKey;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.util.Base64;

import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

public class Config implements JSONSerializable {
    public int port;
    public boolean debug;
    public String serviceName;
    public String serviceSecret;
    public String signingKey; // Base64 encoded signing key

    /**
     * Deserializes the configuration from the provided JSONType.
     * 
     * @param json The JSONType containing the serialized configuration.
     * @throws InvalidObjectException If the JSONType is not a JSONObject, or if
     *             the JSONObject does not contain the expected fields.
     */
    @Override
    public void deserialize(JSONType json) throws InvalidObjectException {
        if (!(json instanceof JSONObject)) {
            throw new InvalidObjectException("Invalid JSON format for Config");
        }
        JSONObject jsonObject = (JSONObject) json;
        
        // Deserialize the fields
        this.port = jsonObject.getInt("port");
        this.debug = jsonObject.getBoolean("debug");
        this.serviceName = jsonObject.getString("service-name");
        this.serviceSecret = jsonObject.getString("service-secret");
        
        // Deserialize the signing key
        if (jsonObject.containsKey("signing-key")) {
            this.signingKey = jsonObject.getString("signing-key");
        } else {
            throw new InvalidObjectException("Missing 'signing-key' in configuration");
        }
    }

    /**
     * Serializes the configuration to a JSONType.
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
        jsonObject.put("signing-key", this.signingKey);  // Add signing key to JSON output
        return jsonObject;
    }

    /**
     * Returns the signing key as a PrivateKey object.
     * The signing key is expected to be Base64 encoded in the configuration.
     * 
     * @return The PrivateKey object for signing.
     * @throws InvalidObjectException If the signing key is invalid.
     */
    public PrivateKey getSigningKey() throws InvalidObjectException {
        try {
            // Decode the Base64 encoded signing key
            byte[] decodedKey = Base64.getDecoder().decode(this.signingKey);

            // Use KeyFactory to generate the PrivateKey
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(new java.security.spec.PKCS8EncodedKeySpec(decodedKey));
        } catch (Exception e) {
            throw new InvalidObjectException("Failed to load signing key: " + e.getMessage());
        }
    }
}
