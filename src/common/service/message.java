package common.service;

import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;
import merrimackutil.json.JSONSerializable;
//Message class utilized within echoservice
public class Message implements JSONSerializable {
    private String iv;
    private String message;

    // Constructor with user
    public Message(String message, String iv ) {
       
        this.message = message;
        this.iv = iv;
    }

    // Getters and setters (optional but useful)
    public String getIv() {
        return iv;
    }

    /**
     * Set the initialization vector for the message
     * @param iv the initialization vector
     */
    public void setIv(String iv) {
        this.iv = iv;
    }

    /**
     * Get the message being sent
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Set the message being sent
     * @param message the message
     */
    public void setMessage(String message) {
        this.message = message;
    }



    // Implementing serialize() method from JSONSerializable interface
    @Override
    public String serialize() {
        JSONObject json = new JSONObject();
        json.put("iv", iv);
        json.put("message", message);
        return json.toString();  // Serialize as a JSON string
    }

    // Implementing deserialize() method from JSONSerializable interface
    @Override
    public void deserialize(JSONType obj) {
        if (obj instanceof JSONObject) {
            JSONObject json = (JSONObject) obj;
            this.iv = json.getString("iv");
            this.message = json.getString("message");
        }
    }

    // Implementing toJSONType() method from JSONSerializable interface
    @Override
    public JSONType toJSONType() {
        JSONObject json = new JSONObject();
        json.put("iv", iv);
        json.put("message", message);
        return json;  // Convert the object into a JSON type (JSONObject)
    }
}
