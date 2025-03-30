package client;

import merrimackutil.json.types.JSONObject;
import java.net.Socket;
import java.io.PrintWriter;
import java.util.Scanner;

import common.Channel;

public class EchoClient {
    public static JSONObject sendMessage(JSONObject msgObj) {
        System.out.println("EchoClient sending JSON message: " + msgObj.toString());
        try (Socket sock = new Socket("127.0.0.1", 5000);
             PrintWriter send = new PrintWriter(sock.getOutputStream(), true);
             Scanner recv = new Scanner(sock.getInputStream())) {

            // Send the JSON message
            send.println(msgObj.toString());

            // Receive the response
            if (recv.hasNextLine()) {
                String recvMsg = recv.nextLine();
                System.out.println("Server Said: " + recvMsg);

                // Convert received string to uppercase
                String upperCaseResponse = recvMsg.toUpperCase();

                // Create a new JSONObject with the uppercase response
                JSONObject responseObj = new JSONObject();
                responseObj.put("response", upperCaseResponse);

             

                return responseObj;
            } else {
                System.err.println("❌ No response received from server.");
                return null;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
