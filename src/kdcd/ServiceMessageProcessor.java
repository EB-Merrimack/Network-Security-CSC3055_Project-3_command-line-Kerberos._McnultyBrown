package kdcd;

import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;
import java.io.InvalidObjectException;

public class ServiceMessageProcessor implements JSONSerializable {
    private String service;
    private String userId;
    private String encryptedPayload;
    private String nonce;
    private String iv;

    public ServiceMessageProcessor(String service, String userId, String encryptedPayload, String nonce, String iv) {
        this.service = service;
        this.userId = userId;
        this.encryptedPayload = encryptedPayload;
        this.nonce = nonce;
        this.iv = iv;
    }

    public ServiceMessageProcessor() {}

    @Override
    public void deserialize(JSONType json) throws InvalidObjectException {
        if (!(json instanceof JSONObject)) {
            throw new InvalidObjectException("Invalid JSON format for Service Message");
        }
        JSONObject jsonObject = (JSONObject) json;
        this.service = jsonObject.getString("service");
        this.userId = jsonObject.getString("id");
        this.encryptedPayload = jsonObject.getString("encrypted-payload");
        this.nonce = jsonObject.getString("nonce");
        this.iv = jsonObject.getString("iv");
    }

    @Override
    public JSONType toJSONType() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "Service Message");
        jsonObject.put("service", this.service);
        jsonObject.put("id", this.userId);
        jsonObject.put("encrypted-payload", this.encryptedPayload);
        jsonObject.put("nonce", this.nonce);
        jsonObject.put("iv", this.iv);
        return jsonObject;
    }

    public static void processServiceMessage(JSONObject message) {
        String service = message.getString("service");
        String userId = message.getString("id");
        String encryptedPayload = message.getString("encrypted-payload");
        String nonce = message.getString("nonce");
        String iv = message.getString("iv");

        // Decrypt the payload and process it
        System.out.println("Received message for service: " + service + " from user: " + userId);
        
        // Response logic can be added here
    }
}
