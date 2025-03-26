package common.service;

import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

import java.io.InvalidObjectException;

public class ClientHello implements JSONSerializable {
    private String type = "Client Hello";
    private String ticket;   // Serialized ticket as base64 or JSON string
    private String nonce;    // base64 encoded

    public ClientHello(String ticket, String nonce) {
        this.ticket = ticket;
        this.nonce = nonce;
    }

    @Override
    public JSONType toJSONType() {
        JSONObject obj = new JSONObject();
        obj.put("type", type);
        obj.put("ticket", ticket);
        obj.put("nonce", nonce);
        return obj;
    }

    @Override
    public void deserialize(JSONType json) throws InvalidObjectException {
        if (!(json instanceof JSONObject)) {
            throw new InvalidObjectException("Expected JSONObject");
        }
        JSONObject obj = (JSONObject) json;
        this.type = obj.getString("type");
        this.ticket = obj.getString("ticket");
        this.nonce = obj.getString("nonce");
    }

    public String getTicket() {
        return ticket;
    }

    public String getNonce() {
        return nonce;
    }
}