package echoservice;

import java.io.InvalidObjectException;

import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

public class Config implements JSONSerializable {
    public int port;
    public boolean debug;
    public String serviceName;
    public String serviceSecret;

    @Override
    public void deserialize(JSONType json) throws InvalidObjectException {
        if (!(json instanceof JSONObject)) {
            throw new InvalidObjectException("Invalid JSON format for Config");
        }
        JSONObject jsonObject = (JSONObject) json;
        this.port = jsonObject.getInt("port");
        this.debug = jsonObject.getBoolean("debug");
        this.serviceName = jsonObject.getString("service-name");
        this.serviceSecret = jsonObject.getString("service-secret");

      
    }
    @Override
    public JSONType toJSONType() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("port", this.port);
        jsonObject.put("debug", this.debug);
        jsonObject.put("service-name", this.serviceName);
        jsonObject.put("service-secret", this.serviceSecret);
        return jsonObject;
    }
}
