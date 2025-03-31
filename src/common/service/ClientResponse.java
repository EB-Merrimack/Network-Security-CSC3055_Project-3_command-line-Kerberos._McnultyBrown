package common.service;

import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

import java.io.InvalidObjectException;

public class ClientResponse implements JSONSerializable {
    private String type = "Client Response";
    private String nonce;           // Nr (base64)
    private String clientId;        // Client username
    private String iv;              // Base64 IV
    private String encryptedNonce; // Enc(Ns) with ks

    public ClientResponse(String nonce, String clientId, String iv, String encryptedNonce) {
        this.nonce = nonce;
        this.clientId = clientId;
        this.iv = iv;
        this.encryptedNonce = encryptedNonce;
    }

/**
 * Converts this ClientResponse object into a JSONType.
 *
 * The resulting JSONType is a JSONObject with the following fields:
 * <ul>
 *   <li>type: A string representing the type of the response, which is "Client Response".
 *   <li>nonce: A base64-encoded string representing the client's nonce.
 *   <li>clientId: A string representing the client's username.
 *   <li>iv: A base64-encoded string representing the initialization vector.
 *   <li>encryptedNonce: A string representing the encrypted nonce.
 * </ul>
 *
 * @return The JSONType containing the serialized ClientResponse.
 */

    @Override
    public JSONType toJSONType() {
        JSONObject obj = new JSONObject();
        obj.put("type", type);
        obj.put("nonce", nonce);
        obj.put("clientId", clientId);
        obj.put("iv", iv);
        obj.put("encryptedNonce", encryptedNonce);
        return obj;
    }

    /**
     * Deserialize a ClientResponse from a JSONType.
     * 
     * This method expects the JSONType to be a JSONObject with the following
     * fields:
     * 
     * <ul>
     *   <li>type: A string representing the type of the response, which is "Client Response".
     *   <li>nonce: A string representing the client's nonce.
     *   <li>clientId: A string representing the client's username.
     *   <li>iv: A string representing the initialization vector.
     *   <li>encryptedNonce: A string representing the encrypted nonce.
     * </ul>
     * 
     * @param json The JSONType containing the serialized ClientResponse.
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
        this.clientId = obj.getString("clientId");
        this.iv = obj.getString("iv");
        this.encryptedNonce = obj.getString("encryptedNonce");
    }

    /**
     * Get the client nonce as a base64-encoded string.
     * 
     * @return the client nonce as a base64-encoded string.
     */
    public String getNonce() {
        return nonce;
    }

    /**
     * Get the client username.
     * 
     * @return the client username.
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Get the initialization vector (IV).
     * 
     * @return the initialization vector as a string.
     */

    public String getIv() {
        return iv;
    }

    /**
     * Get the encrypted client nonce as a base64-encoded string.
     * 
     * @return the encrypted client nonce as a base64-encoded string.
     */
    public String getEncryptedNonce() {
        return encryptedNonce;
    }
}
