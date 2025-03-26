package kdcd;

import java.io.File;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.PrintWriter;

import merrimackutil.json.JsonIO;
import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

public class Config implements JSONSerializable {
    public String secretsFile;
    public int port;
    public long validityPeriod;

    @Override
    public void deserialize(JSONType json) throws InvalidObjectException {
        if (!(json instanceof JSONObject)) {
            throw new InvalidObjectException("Invalid JSON format for Config");
        }
        JSONObject jsonObject = (JSONObject) json;
        this.secretsFile = jsonObject.getString("secrets-file");
        this.port = jsonObject.getInt("port");
        this.validityPeriod = jsonObject.getLong("validity-period");
    }

    @Override
    public JSONType toJSONType() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("secrets-file", this.secretsFile);
        jsonObject.put("port", this.port);
        jsonObject.put("validity-period", this.validityPeriod);
        return jsonObject;
    }

    public static String createDefaultConfig(String configFile) throws IOException {
        Config defaultConfig = new Config();
        defaultConfig.secretsFile = "secrets.json";
        defaultConfig.port = 8080;
        defaultConfig.validityPeriod = 60000;
        
        JSONObject jsonObject = (JSONObject) defaultConfig.toJSONType();
        File file = new File(configFile);
        try (PrintWriter writer = new PrintWriter(file)) {
            writer.write(jsonObject.toString());
        }
        return configFile;
    }
}
