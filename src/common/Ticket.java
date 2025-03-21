package common;

public class Ticket {
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

    // Utility to serialize Ticket to JSON string
    public String toJson() {
        return "{ \"creationTime\": " + creationTime + ", " +
               "\"validityTime\": " + validityTime + ", " +
               "\"username\": \"" + username + "\", " +
               "\"service\": \"" + service + "\", " +
               "\"iv\": \"" + iv + "\", " +
               "\"encryptedSessionKey\": \"" + encryptedSessionKey + "\" }";
    }
}
