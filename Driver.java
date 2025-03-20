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
import java.util.Scanner;

import common.TicketRequest;

/**
 * This is adapted to manage the service, daemon, and client with configuration and help options.
 * 
 */
public class Driver {
  
  // Common options for both service, daemon, and client
  private static boolean showHelp = false;
  private static String configFile = null;
  
  // Client-specific options
  private static String hostsFile = null;
  private static String user = null;
  private static String service = null;

  // Example list of available services, including the ticket request option
  private static String[] availableServices = {
    "kdcclient", "echoservice", "ticketrequest", "kdcconfig"
  };

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
   * Simulate receiving and processing a TicketRequest.
   */
  public static void processTicketRequest(String service, String user) {
    // Simulate creating a TicketRequest
    TicketRequest ticketRequest = new TicketRequest(service, user);

    // Print the ticket request as JSON
    System.out.println("Received TicketRequest: " + ticketRequest.toJSON());

    // Here, you could implement logic to handle the ticket request
    // such as verifying the user or performing authentication.
  }

  /**
   * Simulate displaying KDC Configuration options.
   */
  public static void viewKdcConfig() {
    // Simulate loading and displaying KDC configuration
    System.out.println("Displaying KDC Configuration Options:");

    // Here you would load the actual configuration (e.g., from a file)
    // For demonstration purposes, we'll just show some sample configurations
    System.out.println("KDC Configuration:");
    System.out.println(" - KDC Host: kdc.example.com");
    System.out.println(" - Default Realm: EXAMPLE.COM");
    System.out.println(" - Encryption Types: AES256-SHA256, RC4-HMAC");
    System.out.println(" - Maximum Ticket Lifetime: 24 hours");
  }

  /**
   * Present the user with available service options and handle ticket request.
   */
  public static void requestTicketFromService() {
    Scanner scanner = new Scanner(System.in);
    System.out.println("Available Services:");
    for (int i = 0; i < availableServices.length; i++) {
      System.out.println((i + 1) + ". " + availableServices[i]);
    }

    System.out.print("Select a service by entering the number (1-" + availableServices.length + "): ");
    int choice = scanner.nextInt();

    if (choice < 1 || choice > availableServices.length) {
      System.out.println("Invalid choice. Exiting.");
      System.exit(1);
    }

    String selectedService = availableServices[choice - 1];
    System.out.println("You selected: " + selectedService);

    if ("ticketrequest".equals(selectedService)) {
      // Handle ticket request specifically
      System.out.print("Enter your username for the ticket request: ");
      scanner.nextLine();  // consume the newline
      String username = scanner.nextLine();

      // Process ticket request for the selected service and username
      processTicketRequest(selectedService, username);
    } else if ("kdcconfig".equals(selectedService)) {
      // Handle KDC configuration display
      viewKdcConfig();
    } else {
      // Continue with other service logic (e.g., kdcclient, echoservice)
      System.out.println("Running selected service: " + selectedService);
    }
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
      // Here we initiate the service program logic
    } else {
      System.out.println("KDC Daemon running with config: " + configFile);
    }

    // Request a ticket for the selected service (if this is part of the service flow)
    requestTicketFromService();
  }
}
