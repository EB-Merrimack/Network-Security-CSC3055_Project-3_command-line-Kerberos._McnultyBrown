package signature;

import java.io.File;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import merrimackutil.json.JSONSerializable;
import merrimackutil.json.JsonIO;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

public class Config implements JSONSerializable {
    public int port;
    public boolean debug;
    public String serviceName;
    public String serviceSecret;
    public String signingKey; // Base64 encoded signing key

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
        
        if (jsonObject.containsKey("signing-key")) {
            this.signingKey = jsonObject.getString("signing-key");
        } else {
            throw new InvalidObjectException("Missing 'signing-key' in configuration");
        }
    }

    @Override
    public JSONType toJSONType() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("port", this.port);
        jsonObject.put("debug", this.debug);
        jsonObject.put("service-name", this.serviceName);
        jsonObject.put("service-secret", this.serviceSecret);
        jsonObject.put("signing-key", this.signingKey);
        return jsonObject;
    }

    public PrivateKey getSigningKey() throws InvalidObjectException {
        try {
            byte[] decodedKey = Base64.getDecoder().decode(this.signingKey);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(decodedKey));
        } catch (Exception e) {
            throw new InvalidObjectException("Failed to load signing key: " + e.getMessage());
        }
    }

    /**
     * Generates a new RSA private key, stores it in the config, and returns it.
     * 
     * @return A newly generated RSAPrivateKey.
     * @throws InvalidObjectException If key generation fails.
     */
    public RSAPrivateKey createNewPrivateKey() throws InvalidObjectException {
        try {
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
            keyPairGen.initialize(2048);
            KeyPair keyPair = keyPairGen.generateKeyPair();
    
            RSAPrivateKey newPrivateKey = (RSAPrivateKey) keyPair.getPrivate();
            this.signingKey = Base64.getEncoder().encodeToString(newPrivateKey.getEncoded());
    
            // Save updated config to file
            saveConfig();
    
            return newPrivateKey;
        } catch (Exception e) {
            throw new InvalidObjectException("Failed to generate new signing key: " + e.getMessage());
        }
    }
    public void saveConfig() throws IOException {
        File file = new File("src/signature/config.json");
        JsonIO.writeFormattedObject(this, file);  // Use the Config object directly
    }
    

}
