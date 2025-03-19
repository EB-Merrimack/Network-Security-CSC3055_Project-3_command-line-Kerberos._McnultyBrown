package client;

import java.io.Console;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.Scanner;

import merrimackutil.cli.LongOption;
import merrimackutil.cli.OptionParser;
import merrimackutil.util.Tuple;
public class KDCClient {
        private static String user = null;
        private static String service = null;
        private static final String KDC_HOST = "127.0.0.1";
        private static final int KDC_PORT = 6000;
    
    
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
    
        public static boolean authenticateWithKDC(String username, String password) {
            try (Socket socket = new Socket(KDC_HOST, KDC_PORT);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 Scanner in = new Scanner(socket.getInputStream())) {
    
                // Send authentication claim
                out.println("{\"type\": \"claim\", \"username\": \"" + username + "\"}");
                
                // Receive challenge from KDC
                String response = in.nextLine();
                if (response.contains("failure")) {
                    System.out.println("Authentication failed: User not found");
                    return false;
                }
                String challenge = response.split(":")[1].replace("}", "");
                
                // Compute response hash (SHA-256(password + challenge))
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest((password + challenge).getBytes());
                
                // Send response hash to KDC
                out.println("{\"type\": \"response\", \"hash\": \"" + bytesToHex(hash) + "\"}");
                
                // Receive authentication result
                String result = in.nextLine();
                if (result.contains("success")) {
                    System.out.println("Authentication successful");
                    return true;
                } else {
                    System.out.println("Authentication failed");
                    return false;
                }
            } catch (IOException | java.security.NoSuchAlgorithmException e) {
                e.printStackTrace();
                return false;
            }
        }
    
        public static String bytesToHex(byte[] bytes) {
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        }
    
        public static void main(String[] args) {
            processArgs(args);
            if (user == null) {
                user = promptForUsername("Enter username");
            }
            String password = promptForPassword("Enter password");
            
            if (authenticateWithKDC(user, password)) {
                System.out.println("Proceeding to session key request...");
                // Next step: Request session key
            }
        }
    }
