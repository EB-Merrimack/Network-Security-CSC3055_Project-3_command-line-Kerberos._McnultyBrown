import merrimackutil.cli.LongOption;
import merrimackutil.cli.OptionParser;
import merrimackutil.exception.BadFileFormatException;
import merrimackutil.util.Tuple;
import merrimackutil.json.JsonIO;
import merrimackutil.json.types.JSONObject;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.InvalidObjectException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * This class implements the view and controller for the secrets vault.
 * This is adapted to manage the service, daemon, and client with configuration and help options.
 * 
 * @author Zach Kissel
 */
public class Driver {
  
  // Common options for both service, daemon, and client
  private static boolean showHelp = false;
  private static String configFile = null;
  
  // Client-specific options
  private static String hostsFile = null;
  private static String user = null;
  private static String service = null;

  /**
   * Display the usage message and fatally exit.
   */
  public static void usage(String program) {
    if (program.equals("kdcd")) {
      System.out.println("usage:");
      System.out.println("kdcd");
      System.out.println("kdcd --config <configfile>");
      System.out.println("kdcd --help");
      System.out.println("options:");
      System.out.println("  -c, --config Set the config file.");
      System.out.println("  -h, --help Display the help.");
    } else if (program.equals("echoservice")) {
      System.out.println("usage:");
      System.out.println("echoservice");
      System.out.println("echoservice --config <configfile>");
      System.out.println("echoservice --help");
      System.out.println("options:");
      System.out.println("  -c, --config Set the config file.");
      System.out.println("  -h, --help Display the help.");
    } else if (program.equals("kdcclient")) {
      System.out.println("usage:");
      System.out.println("client --hosts <hostfile> --user <user> --service <service>");
      System.out.println("client --user <user> --service <service>");
      System.out.println("options:");
      System.out.println("  -h, --hosts Set the hosts file.");
      System.out.println("  -u, --user The user name.");
      System.out.println("  -s, --service The name of the service.");
    }
    System.exit(1);
  }

  /**
   * Process the command line arguments.
   * 
   * @param args the array of command line arguments.
   * @param program The name of the program (client, service, or daemon)
   */
  public static void processArgs(String[] args, String program) {
    OptionParser parser;

    LongOption[] opts;
    if (program.equals("kdcclient")) {
      opts = new LongOption[4];
      opts[0] = new LongOption("config", true, 'c');
      opts[1] = new LongOption("help", false, 'h');
      opts[2] = new LongOption("user", true, 'u');
      opts[3] = new LongOption("service", true, 's');
    } else if (program.equals("echoservice")) {
      opts = new LongOption[2];
      opts[0] = new LongOption("config", true, 'c');
      opts[1] = new LongOption("help", false, 'h');
    } else { // Default is KDC Daemon (kdcd)
      opts = new LongOption[2];
      opts[0] = new LongOption("config", true, 'c');
      opts[1] = new LongOption("help", false, 'h');
    }

    Tuple<Character, String> currOpt;

    parser = new OptionParser(args);
    parser.setLongOpts(opts);
    parser.setOptString("hc:u:s:");

    while (parser.getOptIdx() != args.length) {
      currOpt = parser.getLongOpt(false);

      switch (currOpt.getFirst()) {
        case 'h':
          showHelp = true;
          break;
        case 'c':
          configFile = currOpt.getSecond();
          break;
        case 'u':
          user = currOpt.getSecond();
          break;
        case 's':
          service = currOpt.getSecond();
          break;
        case '?':
          usage(program);
          break;
      }
    }

    if (showHelp) {
      usage(program);
    }

    if (program.equals("kdcclient")) {
      if (user == null || service == null) {
        System.out.println("User and service are required for client.");
        usage(program);
      }
    } else {
      if (configFile == null) {
        System.out.println("Config file is required.");
        usage(program);
      }
    }
  }

  /**
   * Loads the configuration file for both service, daemon, and client.
   */
  public static void loadConfig(String configFile) {
    // Implement configuration loading logic here
    System.out.println("Loading config from: " + configFile);
  }

  /**
   * Main entry point for the application (service, daemon, or client).
   * 
   * @param args the array of command line arguments.
   */
  public static void main(String[] args) {
    String program = "kdcd"; // Default program name (KDC Daemon)

    if (args.length == 0) {
      usage(program);
    }

    // Determine which program is being run (client, service, or daemon)
    if (args[0].equals("kdcclient")) {
      program = "kdcclient";
    } else if (args[0].equals("echoservice")) {
      program = "echoservice";
    }

    // Handle command line arguments for both client, service, and daemon
    processArgs(args, program);

    // Load the configuration
    loadConfig(configFile);

    // Continue with the execution logic specific to each case
    if (program.equals("kdcclient")) {
      System.out.println("Client running with user: " + user + ", service: " + service);
    } else if (program.equals("echoservice")) {
      System.out.println("Service running with config: " + configFile);
    } else {
      System.out.println("KDC Daemon running with config: " + configFile);
    }
  }
}
