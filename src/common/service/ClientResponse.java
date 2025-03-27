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

    public String getNonce() {
        return nonce;
    }

    public String getClientId() {
        return clientId;
    }

    public String getIv() {
        return iv;
    }

    public String getEncryptedNonce() {
        return encryptedNonce;
    }
}
