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

    /**
     * Serialize this ClientHello to a JSONType.
     * 
     * The serialized JSONType is a JSONObject with the following fields:
     * 
     * <ul>
     * <li>type: A string representing the type of the message, which is
     *     "Client Hello".
     * <li>ticket: A JSONObject representing the ticket provided by the client.
     * <li>nonce: A string representing the client's nonce, which is base64
     *     encoded.
     * </ul>
     * 
     * @return The JSONType containing the serialized ClientHello.
     */
    @Override
    public JSONType toJSONType() {
        JSONObject obj = new JSONObject();
        obj.put("type", type);
        obj.put("ticket", ticket);
        obj.put("nonce", nonce);
        return obj;
    }

    /**
     * Deserialize a ClientHello from a JSONType.
     * 
     * This method expects the JSONType to be a JSONObject with the following
     * fields:
     * 
     * <ul>
     * <li>type: A string representing the type of the object, which should be
     * "Client Hello".
     * <li>ticket: A JSONObject representing the ticket associated with the
     * client hello.
     * <li>nonce: A string representing the nonce associated with the client
     * hello, base64 encoded.
     * </ul>
     * 
     * @param json The JSONType containing the serialized ClientHello.
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
        this.ticket = obj.getObject("ticket"); // ✅ read as object
        this.nonce = obj.getString("nonce");
    }

    /**
     * Get the ticket associated with the client hello.
     * @return the ticket as a JSONObject
     */

    public JSONObject getTicket() {
        return this.ticket;
    }

    /**
     * Get the nonce included with the client hello, as a base64-encoded string.
     * @return the nonce as a base64-encoded string
     */
    public String getNonce() {
        return nonce;
    }
}