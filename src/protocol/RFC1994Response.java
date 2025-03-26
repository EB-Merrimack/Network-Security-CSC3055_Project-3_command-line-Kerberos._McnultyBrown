package protocol;

import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONType;
import merrimackutil.json.types.JSONObject;
import java.io.InvalidObjectException;

public class RFC1994Response implements JSONSerializable {
    private String type = "RFC1994 Response";
    private String hash;

    public RFC1994Response(String hash) {
        this.hash = hash;
    }

    @Override
    public JSONType toJSONType() {
        JSONObject obj = new JSONObject();
        obj.put("type", type);
        obj.put("hash", hash);
        return obj;
    }

    @Override
    public void deserialize(JSONType json) throws InvalidObjectException {
        if (!(json instanceof JSONObject)) throw new InvalidObjectException("Not a JSON object");
        JSONObject obj = (JSONObject) json;
        this.type = obj.getString("type");
        this.hash = obj.getString("hash");
    }

    public String getHash() { return hash; }
}