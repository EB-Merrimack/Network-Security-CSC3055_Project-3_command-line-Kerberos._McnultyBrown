package common;

import java.io.Console;

import merrimackutil.cli.LongOption;
import merrimackutil.cli.OptionParser;
import merrimackutil.util.Tuple;

public class Driver {
    private static String user = null;
    private static String service = null;
    //HOST


    public static void usageClient() {
        System.out.println("usage: ");
        System.out.println("    client --hosts <hostfile> --user <user> --service <service>");
        System.out.println("    client --user <user> --service <service>");
        System.out.println("options: ");
        System.out.println("    -h, --hosts Set the hosts file.");
        System.out.println("    -u, --user The user name.");
        System.out.println("    -s, --service The name of the service.");
        System.exit(1);
    }

   /**
   * This method prompts for a username and returns the username to the
   * caller.
   *
   * @return A non-empty username as a string.
   */
    public static String promptForUsername(String msg) {
        String usrnm;
        Console cons = System.console();

        do{
            usrnm = new String(cons.readLine(msg + ": "));
        } while (usrnm.isEmpty());

        return usrnm;
    }

   /**
   * This method prompts for a password and returns the password to the
   * caller.
   *
   * @return A non-empty password as a string.
   */
    public static String promptForPassword(String msg) {
        String passwd;
        Console cons = System.console();

        do{
            passwd = new String(cons.readPassword(msg + ": "));
        } while (passwd.isEmpty());

        return passwd;
    }

   /**
   * Process the command line arguments.
   * 
   * @param args the array of command line arguments.
   */
    public static void processArgs(String[] args) {
        OptionParser parser;

        LongOption[] opts = new LongOption[3];
        opts[0] = new LongOption("hosts", false, 'h');
        opts[1] = new LongOption("user", true, 'u');
        opts[2] = new LongOption("service", true, 's');

        Tuple<Character, String> currOpt;

        parser = new OptionParser(args);
        parser.setLongOpts(opts);
        parser.setOptString("hu:s");

        while (parser.getOptIdx() != args.length) {
            currOpt = parser.getLongOpt(false);

            switch (currOpt.getFirst()) {
              case 'h':
                //doHost = true;
                break;
              case 'u':
                user = currOpt.getSecond();
                break;
              case 's':
                service = currOpt.getSecond();
                break;
            }
        }
    }
}