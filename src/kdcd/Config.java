package kdcd;

public class Config {
    public String secretsFile;
    public int port;
    public long validityPeriod;
}











/*package kdcd;

import java.io.IOException;
import java.io.PrintWriter;

import merrimackutil.json.types.JSONObject;

public class Config {
    public String secretsFile;
    public int port;
    public long validityPeriod;

    // Create a default config file
    public static String createDefaultConfig(String configFile) {
        System.out.println("✅ Creating default config.json...");

        // Create the config JSON object
        JSONObject config = new JSONObject();
        config.put("secrets-file", "secrets.json");
        config.put("port", 5000);
        config.put("validity-period", 60000); // Keep as an integer

        try (PrintWriter writer = new PrintWriter(configFile)) {
            // Write the formatted JSON to the file
            writer.println(config.getFormattedJSON());
            System.out.println("✅ config.json created successfully at " + configFile);
        } catch (IOException e) {
            System.err.println("❌ Failed to create config.json: " + e.getMessage());
            System.exit(1); // Exit the program if creating the config fails
        }
                return configFile;
    }

    public static void main(String[] args) {
        // Example usage: Create the default config file at a given path
        createDefaultConfig("config.json");
    }
}*/
