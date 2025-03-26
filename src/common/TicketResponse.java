package common;

import java.io.InvalidObjectException;

import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

public class TicketResponse implements JSONSerializable{
    private String type;        // "Ticket Response"
    private String sessionKey;  // Encrypted session key
    private Ticket ticket;      // Ticket information

    public TicketResponse(String sessionKey, Ticket ticket) {
        this.type = "Ticket Response";
        this.sessionKey = sessionKey;
        this.ticket = ticket;
    }

    // Getters and setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }

    @Override
    public void deserialize(JSONType arg0) throws InvalidObjectException {
        if (!(arg0 instanceof JSONObject)) {
            throw new InvalidObjectException("Expected a JSON object");
        }

        JSONObject json = (JSONObject) arg0;

        this.type = json.getString("type");
        this.sessionKey = json.getString("sessionKey");

        JSONObject ticketJson = json.getObject("ticket");

        Ticket t = new Ticket(
            ticketJson.getString("username"),
            ticketJson.getString("service"),
            ticketJson.getLong("validityTime"),
            ticketJson.getString("iv"),
            ticketJson.getString("encryptedSessionKey")
        );
        t.setCreationTime(ticketJson.getLong("creationTime"));

        this.ticket = t;
    }

    @Override
    public JSONType toJSONType() {
        JSONObject json = new JSONObject();
        json.put("type", this.type);
        json.put("sessionKey", this.sessionKey);

        // Build ticket as a JSONObject
        JSONObject ticketJson = new JSONObject();
        ticketJson.put("creationTime", ticket.getCreationTime());
        ticketJson.put("validityTime", ticket.getValidityTime());
        ticketJson.put("username", ticket.getUsername());
        ticketJson.put("service", ticket.getService());
        ticketJson.put("iv", ticket.getIv());
        ticketJson.put("encryptedSessionKey", ticket.getEncryptedSessionKey());

        json.put("ticket", ticketJson);
        return json;
    }
}
