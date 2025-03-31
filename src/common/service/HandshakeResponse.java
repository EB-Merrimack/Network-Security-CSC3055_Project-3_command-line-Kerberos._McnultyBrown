package common.service;

import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

import java.io.InvalidObjectException;

public class HandshakeResponse implements JSONSerializable {
    private String type = "Handshake Response";
    private String nonce;      // Ns (base64)
    private String service;    // Service name
    private String iv;         // Base64 IV
    private String encryptedNonce; // Enc(Nc) with ks

    public HandshakeResponse(String nonce, String service, String iv, String encryptedNonce) {
        this.nonce = nonce;
        this.service = service;
        this.iv = iv;
        this.encryptedNonce = encryptedNonce;
    }

    /**
     * Serialize this HandshakeResponse to a JSONType.
     * 
     * The serialized JSONType is a JSONObject with the following fields:
     * 
     * <ul>
     * <li>type: A string representing the type of the message, which is
     *     "Handshake Response".
     * <li>nonce: A string representing the Ns, the base64-encoded nonce.
     * <li>service: A string representing the name of the service.
     * <li>iv: A string representing the base64-encoded initialization vector
     *     (IV) used in the encryption of the encryptedNonce.
     * <li>encryptedNonce: A string representing the encrypted nonce, which is
     *     the plaintext of the encrypted nonce.
     * </ul>
     * 
     * @return The JSONType containing the serialized HandshakeResponse.
     */
    @Override
    public JSONType toJSONType() {
        JSONObject obj = new JSONObject();
        obj.put("type", type);
        obj.put("nonce", nonce);
        obj.put("service", service);
        obj.put("iv", iv);
        obj.put("encryptedNonce", encryptedNonce);
        return obj;
    }

    /**
     * Deserialize a HandshakeResponse from a JSONType.
     * 
     * This method expects the JSONType to be a JSONObject with the following
     * fields:
     * 
     * <ul>
     * <li>type: A string representing the type of the message, which is
     *     "Handshake Response".
     * <li>nonce: A string representing the Ns, the base64-encoded nonce.
     * <li>service: A string representing the name of the service.
     * <li>iv: A string representing the base64-encoded initialization vector
     *     (IV) used in the encryption of the encryptedNonce.
     * <li>encryptedNonce: A string representing the encrypted nonce, which is
     *     the plaintext of the encrypted nonce.
     * </ul>
     * 
     * @param json The JSONType containing the serialized HandshakeResponse.
     * @throws InvalidObjectException If the JSONType is not a JSONObject, or
     *             if the JSONObject does not contain the expected fields.
     */
    @Override
    public void deserialize(JSONType json) throws InvalidObjectException {
        if (!(json instanceof JSONObject)) {
            throw new InvalidObjectException("Expected JSONObject");
        }
        JSONObject obj = (JSONObject) json;
        this.type = obj.getString("type");
        this.nonce = obj.getString("nonce");
        this.service = obj.getString("service");
        this.iv = obj.getString("iv");
        this.encryptedNonce = obj.getString("encryptedNonce");
    }

    /**
     * Get the nonce included with the handshake response, as a base64-encoded string.
     * @return the nonce as a base64-encoded string
     */
    public String getNonce() {
        return nonce;
    }

    /**
     * Get the name of the service associated with the handshake response.
     * @return the name of the service
     */
    public String getService() {
        return service;
    }

    /**
     * Get the initialization vector (IV) used in the encryption of the
     * encryptedNonce, as a base64-encoded string.
     * @return the IV as a base64-encoded string
     */
    public String getIv() {
        return iv;
    }

    /**
     * Get the encrypted nonce from the handshake response, as a base64-encoded
     * string. This is the ciphertext of the encrypted nonce.
     * @return the encrypted nonce as a base64-encoded string
     */
    public String getEncryptedNonce() {
        return encryptedNonce;
    }
}
