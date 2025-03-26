package client;

import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;
import java.io.InvalidObjectException;

public class ClientHello implements JSONSerializable {
    public String ticket;
    public String nonce;

    @Override
    public void deserialize(JSONType json) throws InvalidObjectException {
        if (!(json instanceof JSONObject)) {
            throw new InvalidObjectException("Invalid JSON format for ClientHello");
        }
        JSONObject jsonObject = (JSONObject) json;
        this.ticket = jsonObject.getString("ticket");
        this.nonce = jsonObject.getString("nonce");
    }

    @Override
    public JSONType toJSONType() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "Client Hello");
        jsonObject.put("ticket", this.ticket);
        jsonObject.put("nonce", this.nonce);
        return jsonObject;
    }
}

class HandshakeResponse implements JSONSerializable {
    public String service;
    public String encryptedResponse;
    public String nonce;
    public String iv;

    @Override
    public void deserialize(JSONType json) throws InvalidObjectException {
        if (!(json instanceof JSONObject)) {
            throw new InvalidObjectException("Invalid JSON format for HandshakeResponse");
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
}

class ServiceMessage implements JSONSerializable {
    public String service;
    public String id;
    public String encryptedPayload;
    public String nonce;
    public String iv;

    @Override
    public void deserialize(JSONType json) throws InvalidObjectException {
        if (!(json instanceof JSONObject)) {
            throw new InvalidObjectException("Invalid JSON format for ServiceMessage");
        }
        JSONObject jsonObject = (JSONObject) json;
        this.service = jsonObject.getString("service");
        this.id = jsonObject.getString("id");
        this.encryptedPayload = jsonObject.getString("encrypted-payload");
        this.nonce = jsonObject.getString("nonce");
        this.iv = jsonObject.getString("iv");
    }

    @Override
    public JSONType toJSONType() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "Service Message");
        jsonObject.put("service", this.service);
        jsonObject.put("id", this.id);
        jsonObject.put("encrypted-payload", this.encryptedPayload);
        jsonObject.put("nonce", this.nonce);
        jsonObject.put("iv", this.iv);
        return jsonObject;
    }
}
