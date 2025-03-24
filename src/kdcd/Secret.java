package kdcd;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import merrimackutil.json.types.JSONArray;
import merrimackutil.json.types.JSONObject;

public class Secret {
    public String user;
    public String secret;
    public static void createDefaultSecretsFile(File file) {
       JSONObject secrets = new JSONObject();

                JSONArray secArray = new JSONArray();
                secArray.add(secrets);

                JSONObject root = new JSONObject();
                root.put("secrets", secArray);

                try (PrintWriter writer = new PrintWriter(file)) {
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
