package kdcd;

import java.io.IOException;
import java.io.PrintWriter;

import merrimackutil.json.types.JSONArray;
import merrimackutil.json.types.JSONObject;

public class Config {
    public String secretsFile;
    public int port;
    public long validityPeriod;
    public static String createDefaultConfig(String configFile) {
     JSONObject config = new JSONObject();

            config.put("secrets-file", "secrets.json");
            config.put("port", 5000);
            config.put("validity-period", "60000");
                JSONArray configArray = new JSONArray();
                configArray.add(config);

                JSONObject root = new JSONObject();

                try (PrintWriter writer = new PrintWriter(configFile)) {
                    writer.println(root.getFormattedJSON());
                    System.out.println("✅ secrets.json created manually before loading.");
                }

            catch (IOException e) {
                System.err.println("❌ Failed to create hosts.json: " + e.getMessage());
                System.exit(1);
            }
        throw new UnsupportedOperationException("Unimplemented method 'createDefaultSecretsFile'");
    }
}
