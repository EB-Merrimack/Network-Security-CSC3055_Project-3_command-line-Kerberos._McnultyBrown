package protocol;

import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONType;
import merrimackutil.json.types.JSONObject;
import java.io.InvalidObjectException;

public class RFC1994Result implements JSONSerializable {
    private String type = "RFC1994 Result";
    private boolean result;

    public RFC1994Result(boolean result) {
        this.result = result;
    }

    @Override
    public JSONType toJSONType() {
        JSONObject obj = new JSONObject();
        obj.put("type", type);
        obj.put("result", result);
        return obj;
    }

    @Override
    public void deserialize(JSONType json) throws InvalidObjectException {
        if (!(json instanceof JSONObject)) throw new InvalidObjectException("Not a JSON object");
        JSONObject obj = (JSONObject) json;
        this.type = obj.getString("type");
        this.result = obj.getBoolean("result");
    }

    public boolean getResult() { return result; }
}
