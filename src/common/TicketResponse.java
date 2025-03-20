package common;

import java.util.Base64;

public class TicketResponse {
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

    // Utility to serialize TicketResponse to JSON string (or use a JSON library like Gson or Jackson)
    public String toJson() {
        return "{ \"type\": \"" + type + "\", " +
               "\"sessionKey\": \"" + sessionKey + "\", " +
               "\"ticket\": " + ticket.toJson() + " }";
    }
}
