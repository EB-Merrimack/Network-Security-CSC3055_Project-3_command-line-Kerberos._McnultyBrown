/*package kdcd;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import merrimackutil.json.types.JSONArray;
import merrimackutil.json.types.JSONObject;

public class Secret {
    public String user;
    public String secret;

    // Static method to create a default empty secrets file if it doesn't exist
    public static void createDefaultSecretsFile(File file) {
        // Create the root JSON object and the "secrets" array
        JSONObject root = new JSONObject();
        JSONArray secArray = new JSONArray(); // Empty array

        // Add the empty "secrets" array to the root object
        root.put("secrets", secArray);

        // Write the JSON to the file
        try (PrintWriter writer = new PrintWriter(file)) {
            writer.println(root.getFormattedJSON());
            System.out.println("✅ secrets.json created successfully.");
        } catch (IOException e) {
            System.err.println("❌ Failed to create secrets.json: " + e.getMessage());
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        // Example usage: Create a default secrets file
        createDefaultSecretsFile(new File("secrets.json"));
    }
}*/
