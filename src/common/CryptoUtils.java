package common;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import org.bouncycastle.crypto.generators.SCrypt;

public class CryptoUtils {

    private static final String AES_ALGORITHM = "AES";
    private static final String AES_GCM_MODE = "AES/GCM/NoPadding";

    /**
     * Encrypts a message using AES/GCM/NoPadding.
     */
    public static String encryptAESGCM(String message, String password) throws Exception {
        // Derive key from password using SCRYPT
        byte[] key = deriveKey(password);
        Cipher cipher = Cipher.getInstance(AES_GCM_MODE);
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, AES_ALGORITHM), spec);

        byte[] encryptedMessage = cipher.doFinal(message.getBytes(StandardCharsets.UTF_8));
        byte[] combined = new byte[iv.length + encryptedMessage.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encryptedMessage, 0, combined, iv.length, encryptedMessage.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    /**
     * Decrypts a message using AES/GCM/NoPadding.
     */
    public static String decryptAESGCM(String encryptedMessage, String password) throws Exception {
        byte[] decodedMessage = Base64.getDecoder().decode(encryptedMessage);
        byte[] iv = new byte[12];
        System.arraycopy(decodedMessage, 0, iv, 0, iv.length);
        byte[] cipherText = new byte[decodedMessage.length - iv.length];
        System.arraycopy(decodedMessage, iv.length, cipherText, 0, cipherText.length);

        // Derive key from password using SCRYPT
        byte[] key = deriveKey(password);
        Cipher cipher = Cipher.getInstance(AES_GCM_MODE);
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, AES_ALGORITHM), spec);

        byte[] decryptedMessage = cipher.doFinal(cipherText);
        return new String(decryptedMessage, StandardCharsets.UTF_8);
    }

    public static String decryptAESGCM(String encryptedBase64, SecretKey key) throws Exception {
        byte[] decodedMessage = Base64.getDecoder().decode(encryptedBase64);
        byte[] iv = new byte[12];
        System.arraycopy(decodedMessage, 0, iv, 0, iv.length);
        byte[] cipherText = new byte[decodedMessage.length - iv.length];
        System.arraycopy(decodedMessage, iv.length, cipherText, 0, cipherText.length);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);

        byte[] decryptedMessage = cipher.doFinal(cipherText);
        return new String(decryptedMessage, StandardCharsets.UTF_8);
    }

    public static byte[] decryptAESGCMToBytes(String encryptedBase64, SecretKey key) throws Exception {
        byte[] decodedMessage = Base64.getDecoder().decode(encryptedBase64);
        byte[] iv = new byte[12];
        System.arraycopy(decodedMessage, 0, iv, 0, iv.length);
        byte[] cipherText = new byte[decodedMessage.length - iv.length];
        System.arraycopy(decodedMessage, iv.length, cipherText, 0, cipherText.length);
    
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);
    
        return cipher.doFinal(cipherText); // ✅ this is your original nonce
    }
    /**
     * Derives an AES key from a password using SCRYPT.
     */
    private static byte[] deriveKey(String password) throws Exception {
        byte[] salt = password.getBytes(StandardCharsets.UTF_8); // Use password as salt
        return SCrypt.generate(password.getBytes(StandardCharsets.UTF_8), salt, 16384, 8, 1, 32);
    }
}
