package kdcd;

import java.io.File;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import merrimackutil.json.JsonIO;
import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONArray;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

public class Secret implements JSONSerializable {
    public String user;
    public String secret;

    @Override
    public void deserialize(JSONType json) throws InvalidObjectException {
        if (!(json instanceof JSONObject)) {
            throw new InvalidObjectException("Invalid JSON format for Secret");
        }
        JSONObject jsonObject = (JSONObject) json;
        this.user = jsonObject.getString("user");
        this.secret = jsonObject.getString("secret");
    }

    @Override
    public JSONType toJSONType() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("user", this.user);
        jsonObject.put("secret", this.secret);
        return jsonObject;
    }

    public static void createDefaultSecretsFile(File file) throws IOException {
        List<Secret> defaultSecrets = new ArrayList<>();
        Secret defaultSecret = new Secret();
        defaultSecret.user = "default_user";
        defaultSecret.secret = "default_secret";
        defaultSecrets.add(defaultSecret);

        JSONArray secretsArray = new JSONArray();
        for (Secret secret : defaultSecrets) {
            secretsArray.add(secret.toJSONType());
        }

        JSONObject secretsJson = new JSONObject();
        secretsJson.put("secrets", secretsArray);
        
        try (PrintWriter writer = new PrintWriter(file)) {
            writer.write(secretsJson.toString());
        }
    }
}