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
import java.util.Scanner;
import java.io.PrintWriter;
import java.io.IOException;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import echoservice.ConnectionHandler;

public class EchoService{
  
  public static void usageClient() {
    System.out.println("usage:");
  System.out.println("echoservice");
  System.out.println("echoservice --config <configfile>");
  System.out.println("echoservice --help");
  System.out.println("options:");
  System.out.println("  -c, --config Set the config file.");
  System.out.println("  -h, --help Display the help.");
    System.exit(1);
}
  public static void main(String[] args)
  {
    // A pool with 10 threads.
    ExecutorService pool = Executors.newFixedThreadPool(10);

    try
    {
      ServerSocket server = new ServerSocket(5000);

      // Loop forever handing connections.
      while (true)
      {
        // Wait for a connection.
        Socket sock = server.accept();

        System.out.println("Connection received.");

        pool.execute(new ConnectionHandler(sock));
      }
    }
    catch(IOException ioe)
    {
      ioe.printStackTrace();
    }
  }

}
