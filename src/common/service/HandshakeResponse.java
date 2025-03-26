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

    public String getNonce() {
        return nonce;
    }

    public String getService() {
        return service;
    }

    public String getIv() {
        return iv;
    }

    public String getEncryptedNonce() {
        return encryptedNonce;
    }
}
