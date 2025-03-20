import java.io.FileReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import merrimackutil.util.NonceCache;

public class EchoService {
    private static final int MAX_THREADS = 10;
    private final String serviceName;
    private final String serviceSecret;
    private final int port;
    private final NonceCache nonceCache;
    private final ExecutorService executor;

    public EchoService(String configFile) throws Exception {
        // Load JSON config
        JSONObject config = (JSONObject) new JSONParser().parse(new FileReader(configFile));
        this.serviceName = (String) config.get("service-name");
        this.serviceSecret = (String) config.get("service-secret");
        this.port = ((Long) config.get("port")).intValue();

        // Initialize NonceCache (16-byte nonces, 60s validity)
        this.nonceCache = new NonceCache(16, 60);
        this.executor = Executors.newFixedThreadPool(MAX_THREADS);
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println(serviceName + " running on port " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                executor.execute(new ConnectionHandler(clientSocket, serviceSecret, nonceCache));
            }
        } catch (Exception e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
            executor.shutdown();
        }
    }

    public static void main(String[] args) {
        try {
            new EchoService("config.json").start();
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
        }
    }
}
