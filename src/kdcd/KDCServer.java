package kdcd;

public class KDCServer {
    public static void usageClient() {
        System.out.println("usage:");
        System.out.println("kdcd");
        System.out.println("kdcd --config <configfile>");
        System.out.println("kdcd --help");
        System.out.println("options:");
        System.out.println("  -c, --config Set the config file.");
        System.out.println("  -h, --help Display the help.");
        System.exit(1);
    }
}
