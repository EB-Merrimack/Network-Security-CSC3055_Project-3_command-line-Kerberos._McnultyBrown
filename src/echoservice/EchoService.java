package echoservice;

/*
 *   Copyright (C) 2022 -- 2023  Zachary A. Kissel
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import java.net.Socket;
import java.net.ServerSocket;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EchoService {

    private static String configFile = null; // Config file must be explicitly set

    public static void usageClient() {
        System.out.println("usage:");
        System.out.println("  echoservice");
        System.out.println("  echoservice --config <configfile>");
        System.out.println("  echoservice --help");
        System.out.println("options:");
        System.out.println("  -c, --config <configfile>   Set the config file.");
        System.out.println("  -h, --help                  Display the help.");
        System.exit(0);
    }

    public static void main(String[] args) {
        // If no arguments are provided, or if "echoservice" is provided, start the default EchoService
    if (args.length == 0 || args[0].equalsIgnoreCase("echoservice")) {
      startServer("config.json");
      return;
  }

      String configFile = null;
      
      // Handle command-line arguments
      for (int i = 0; i < args.length; i++) {
          switch (args[i]) {
              case "-h":
              case "--help":
                  usageClient();
                  return;
              case "-c":
              case "--config":
                  if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                      configFile = args[i + 1];
                      i++; // Skip the next argument (config file name)
                  } else {
                      System.err.println("Error: Missing <configfile> after -c/--config.");
                      usageClient();
                  }
                  break;
              default:
                  // We just ignore "echoservice" or any other unknown argument in this block.
                  if (args[i].equalsIgnoreCase("echoservice")) {
                      // Skip processing "echoservice" as it's just a placeholder
                      continue;
                  }
                  System.err.println("Error: Unrecognized option: " + args[i]);
                  usageClient();
                  return;
          }
      }
  
      // If no args or only "echoservice" is given, start the EchoService
      startServer(configFile);
  }
  
  /**
   * Starts the EchoService server, optionally using a config file.
   */
  private static void startServer(String configFile) {
    if (configFile == null) {
        System.out.println("No configuration file provided. Running EchoService with default settings...");
    } else {
        System.out.println("Using configuration file: " + configFile);
    }

    // Create a new thread to run the EchoService so it doesn't block the command line
    Thread serverThread = new Thread(() -> {
        ExecutorService pool = Executors.newFixedThreadPool(10);

        try (ServerSocket server = new ServerSocket(5000)) {
            System.out.println("EchoService started on port 5000.");
            while (true) {
                Socket sock = server.accept();
                System.out.println("Connection received.");
                pool.execute(new ConnectionHandler(sock));
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    });

    // Start the server in a background thread
    serverThread.setDaemon(true);  // Set the thread as daemon so it doesn't block program termination
    serverThread.start();
    
    System.out.println("EchoService is now running in the background. You can return to the command line.");
}
  
}
