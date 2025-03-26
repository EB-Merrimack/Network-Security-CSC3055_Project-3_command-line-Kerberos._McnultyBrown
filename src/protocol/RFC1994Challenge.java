package protocol;

import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONType;
import merrimackutil.json.types.JSONObject;
import java.io.InvalidObjectException;

public class RFC1994Challenge implements JSONSerializable {
    private String type = "RFC1994 Challenge";
    private String challenge; // Base64 encoded

    public RFC1994Challenge(String challenge) {
        this.challenge = challenge;
    }

    @Override
    public JSONType toJSONType() {
        JSONObject obj = new JSONObject();
        obj.put("type", type);
        obj.put("challenge", challenge);
        return obj;
    }

    @Override
    public void deserialize(JSONType json) throws InvalidObjectException {
        if (!(json instanceof JSONObject)) throw new InvalidObjectException("Not a JSON object");
        JSONObject obj = (JSONObject) json;
        this.type = obj.getString("type");
        this.challenge = obj.getString("challenge");
    }

    public String getChallenge() { return challenge; }
}