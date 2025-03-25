package common;

import java.io.PrintWriter;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.Scanner;
import java.util.UUID;

import kdcd.KDCServer;

import java.io.IOException;
import merrimackutil.util.NonceCache;

/**
 * Handles a single connection, including nonce management.
 * @author Zach Kissel
 */
public class ConnectionHandler implements Runnable
{
    private Socket sock;
    private NonceCache nonceCache;  // NonceCache to check for replay attacks

    /**
     * Creates a new connection handler with nonce cache.
     * @param sock the socket associated with the connection.
     * @param nonceCache the nonce cache to check for replay attacks.
     */
    public ConnectionHandler(Socket sock, NonceCache nonceCache)
    {
        this.sock = sock;
        this.nonceCache = nonceCache;  // Initialize with the nonce cache
    }

    /**
     * How to handle the connection
     */
    public void run() {
    try {
        Scanner recv = new Scanner(sock.getInputStream());
        PrintWriter send = new PrintWriter(sock.getOutputStream(), true);

        // Message 1: Receive claim
        String rawClaim = recv.nextLine();
        System.out.println("📨 Received CHAP claim: " + rawClaim);

        if (!rawClaim.contains("username")) {
            send.println("{\"type\": \"status\", \"status\": \"failure\"}");
            sock.close();
            return;
        }

        String username = rawClaim.split(":")[1].replace("\"", "").replace("}", "").trim();

        // Lookup secret from secrets map
        String secret = KDCServer.secrets.get(username);
        if (secret == null) {
            System.out.println("❌ Unknown user: " + username);
            send.println("{\"type\": \"status\", \"status\": \"failure\"}");
            sock.close();
            return;
        }

        // Message 2: Generate and send challenge (nonce)
        String challenge = UUID.randomUUID().toString(); // simple random nonce
        send.println("{\"type\": \"challenge\", \"nonce\": \"" + challenge + "\"}");
        System.out.println("🧩 Sent challenge: " + challenge);

        // Message 3: Receive hash from client
        String response = recv.nextLine();
        String receivedHash = response.split(":")[1].replace("\"", "").replace("}", "").trim();

        // Recompute hash on server side
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String concat = secret + challenge;
        byte[] serverHash = digest.digest(concat.getBytes());

        String expectedHash = bytesToHex(serverHash);

        // Message 4: Compare hashes and send result
        if (receivedHash.equals(expectedHash)) {
            System.out.println("✅ CHAP authentication successful for: " + username);
            send.println("{\"type\": \"status\", \"status\": \"success\"}");
        } else {
            System.out.println("❌ Invalid CHAP hash for: " + username);
            send.println("{\"type\": \"status\", \"status\": \"failure\"}");
        }

        sock.close();
    } catch (Exception e) {
        e.printStackTrace();
    }
}

private static String bytesToHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
        sb.append(String.format("%02x", b));
    }
    return sb.toString();
}

    /**
     * Simulates receiving a nonce from the client.
     * (In practice, this would be part of the message or handshake)
     * @param recv the scanner to receive data.
     * @return the received nonce as a byte array.
     */
    private byte[] receiveNonceFromClient(Scanner recv)
    {
        // Example: Receive the nonce as a byte array (you can change the logic here)
        byte[] nonce = new byte[16];  // For example, 16-byte nonce
        recv.nextLine();  // Simulating nonce reception as part of the communication (this can be adjusted)

        // In a real case, you would decode the nonce properly from the client message.
        return nonce;  // Return the received nonce
    }
}
