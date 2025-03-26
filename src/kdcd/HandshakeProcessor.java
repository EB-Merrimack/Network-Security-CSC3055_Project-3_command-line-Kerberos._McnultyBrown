package kdcd;

import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;
import java.io.InvalidObjectException;
import java.util.Base64;

public class HandshakeProcessor implements JSONSerializable {
    private String service;
    private String encryptedResponse;
    private String nonce;
    private String iv;

    public HandshakeProcessor(String service, String encryptedResponse, String nonce, String iv) {
        this.service = service;
        this.encryptedResponse = encryptedResponse;
        this.nonce = nonce;
        this.iv = iv;
    }

    public HandshakeProcessor() {}

    @Override
    public void deserialize(JSONType json) throws InvalidObjectException {
        if (!(json instanceof JSONObject)) {
            throw new InvalidObjectException("Invalid JSON format for Handshake Response");
        }
        JSONObject jsonObject = (JSONObject) json;
        this.service = jsonObject.getString("service");
        this.encryptedResponse = jsonObject.getString("encrypted-response");
        this.nonce = jsonObject.getString("nonce");
        this.iv = jsonObject.getString("iv");
    }

    @Override
    public JSONType toJSONType() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "Handshake Challenge Response");
        jsonObject.put("service", this.service);
        jsonObject.put("encrypted-response", this.encryptedResponse);
        jsonObject.put("nonce", this.nonce);
        jsonObject.put("iv", this.iv);
        return jsonObject;
    }

    public static HandshakeProcessor processClientHello(JSONObject clientHello) {
        String ticket = clientHello.getString("ticket");
        String nonce = clientHello.getString("nonce");

        // Validate the ticket (Decryption and verification logic required)
        String serviceName = "example-service";  // Extract from decrypted ticket
        String encryptedResponse = Base64.getEncoder().encodeToString("server-response".getBytes());
        String iv = Base64.getEncoder().encodeToString("random-iv".getBytes());

        return new HandshakeProcessor(serviceName, encryptedResponse, nonce, iv);
    }
}
