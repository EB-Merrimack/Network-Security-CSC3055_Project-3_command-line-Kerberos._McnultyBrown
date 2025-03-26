package protocol;

import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONType;
import merrimackutil.json.types.JSONObject;
import java.io.InvalidObjectException;

public class RFC1994Claim implements JSONSerializable {
    private String type = "RFC1994 Initial";
    private String id;

    public RFC1994Claim(String id) {
        this.id = id;
    }

    @Override
    public JSONType toJSONType() {
        JSONObject json = new JSONObject();
        json.put("type", type);
        json.put("id", id);
        return json;
    }

    @Override
    public void deserialize(JSONType json) throws InvalidObjectException {
        if (!(json instanceof JSONObject)) throw new InvalidObjectException("Not a JSON object");
        JSONObject obj = (JSONObject) json;
        this.type = obj.getString("type");
        this.id = obj.getString("id");
    }

    public String getId() { return id; }
}
