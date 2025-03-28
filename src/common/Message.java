package common;

import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;
import merrimackutil.json.JSONSerializable;

public class Message implements JSONSerializable {
    private String iv;
    private String message;
    private String user;  // New field to store the associated user

    // Constructor with user
    public Message(String iv, String message, String user) {
        this.iv = iv;
        this.message = message;
        this.user = user;
    }

    // Getters and setters (optional but useful)
    public String getIv() {
        return iv;
    }

    public void setIv(String iv) {
        this.iv = iv;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    // Implementing serialize() method from JSONSerializable interface
    @Override
    public String serialize() {
        JSONObject json = new JSONObject();
        json.put("iv", iv);
        json.put("message", message);
        json.put("user", user);  // Serialize the user field
        return json.toString();  // Serialize as a JSON string
    }

    // Implementing deserialize() method from JSONSerializable interface
    @Override
    public void deserialize(JSONType obj) {
        if (obj instanceof JSONObject) {
            JSONObject json = (JSONObject) obj;
            this.iv = json.getString("iv");
            this.message = json.getString("message");
            this.user = json.getString("user");  // Deserialize the user field
        }
    }

    // Implementing toJSONType() method from JSONSerializable interface
    @Override
    public JSONType toJSONType() {
        JSONObject json = new JSONObject();
        json.put("iv", iv);
        json.put("message", message);
        json.put("user", user);  // Include the user field in the JSON object
        return json;  // Convert the object into a JSON type (JSONObject)
    }
}
