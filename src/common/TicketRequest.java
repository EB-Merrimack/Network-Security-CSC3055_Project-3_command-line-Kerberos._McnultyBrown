package common;

import merrimackutil.json.types.JSONType;
import merrimackutil.json.types.JSONObject;

import java.io.InvalidObjectException;

import merrimackutil.json.JSONSerializable;

public class TicketRequest implements JSONSerializable, JSONType {
    private String type;
    private String service;
    private String id;

    public TicketRequest(String service, String id) {
        this.type = "Ticket Request";
        this.service = service;
        this.id = id;
    }

    @Override
    public JSONType toJSONType() {
        JSONObject json = new JSONObject();
        json.put("type", this.type);
        json.put("service", this.service);
        json.put("id", this.id);
        return json;
    }

    // Use the utility to get a formatted JSON string
    @Override
    public String getFormattedJSON() {
        // Delegate the call to JSONObject's getFormattedJSON() method
        return toJSONType().getFormattedJSON(); // JSONObject already handles the formatting
    }

    @Override
    public String toJSON() {
        return toJSONType().toString();
    }

    @Override
    public boolean isArray() {
        return false; // It's not an array
    }

    @Override
    public boolean isObject() {
        return true; // It's an object
    }

   

    // Main method for testing
    public static void main(String[] args) {
        TicketRequest ticketRequest = new TicketRequest("ExampleService", "user123");
        System.out.println("Formatted JSON: " + ticketRequest.getFormattedJSON());
    }

    @Override
    public void deserialize(JSONType arg0) throws InvalidObjectException {
        if (arg0 == null || !arg0.isObject()) {
            throw new InvalidObjectException("Invalid JSON type for deserialization.");
        }
        
        JSONObject jsonObject = (JSONObject) arg0; // Cast to JSONObject to access the fields
        
        // Deserialize fields
        this.type = jsonObject.getString("type");
        this.service = jsonObject.getString("service");
        this.id = jsonObject.getString("id");

    }
}
