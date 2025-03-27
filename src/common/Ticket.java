package common;

import java.io.InvalidObjectException;

import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

public class Ticket implements JSONSerializable{
    private long creationTime;
    private long validityTime;   // Validity time in milliseconds
    private String username;
    private String service;
    private String iv;           // IV for encryption
    private String encryptedSessionKey;

    public Ticket(String username, String service, long validityTime, String iv, String encryptedSessionKey) {
        this.creationTime = System.currentTimeMillis();
        this.validityTime = validityTime;
        this.username = username;
        this.service = service;
        this.iv = iv;
        this.encryptedSessionKey = encryptedSessionKey;
    }

    // Getters and setters
    public long getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    public long getValidityTime() {
        return validityTime;
    }

    public void setValidityTime(long validityTime) {
        this.validityTime = validityTime;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getIv() {
        return iv;
    }

    public void setIv(String iv) {
        this.iv = iv;
    }

    public String getEncryptedSessionKey() {
        return encryptedSessionKey;
    }

    public void setEncryptedSessionKey(String encryptedSessionKey) {
        this.encryptedSessionKey = encryptedSessionKey;
    }

    @Override
    public JSONType toJSONType() {
        JSONObject json = new JSONObject();
        json.put("creationTime", this.creationTime);
        json.put("validityTime", this.validityTime);
        json.put("username", this.username);
        json.put("service", this.service);
        json.put("iv", this.iv);
        json.put("encryptedSessionKey", this.encryptedSessionKey);
        return json;
    }

    @Override
    public void deserialize(JSONType arg0) throws InvalidObjectException {
        if (!(arg0 instanceof JSONObject)) {
            throw new InvalidObjectException("Expected a JSONObject.");
        }

        JSONObject json = (JSONObject) arg0;

        this.creationTime = json.getLong("creationTime");
        this.validityTime = json.getLong("validityTime");
        this.username = json.getString("username");
        this.service = json.getString("service");
        this.iv = json.getString("iv");
        this.encryptedSessionKey = json.getString("encryptedSessionKey");
    }
}
