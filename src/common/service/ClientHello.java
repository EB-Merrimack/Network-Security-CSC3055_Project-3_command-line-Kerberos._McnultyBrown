package common.service;

import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

import java.io.InvalidObjectException;

public class ClientHello implements JSONSerializable {
    private String type = "Client Hello";
    private JSONObject ticket;
    private String nonce;    // base64 encoded

    public ClientHello(JSONObject ticket, String nonce) {
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
        this.ticket = obj.getObject("ticket"); // ✅ read as object
        this.nonce = obj.getString("nonce");
    }

    public JSONObject getTicket() {
        return this.ticket;
    }

    public String getNonce() {
        return nonce;
    }
}