package ch.bbw.pr.tresorbackend.util;

import org.jasypt.util.text.AES256TextEncryptor;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * EncryptUtil
 * Used to encrypt content.
 * @author Petr Cerny
 */
public class EncryptUtil {

   private final SecureRandom secureRandom = new SecureRandom();
   private final String pepper;

   public EncryptUtil(String pepper) {
      this.pepper = pepper;
   }

   public String encrypt(String plainText) {
      // 16 zufällige Bytes als salt
      byte[] saltBytes = new byte[16];
      secureRandom.nextBytes(saltBytes);

      // Salt zu String verwandeln
      String saltBase64 = Base64.getEncoder().encodeToString(saltBytes);

      // neuer AES-Verschlüsseler
      AES256TextEncryptor encryptor = new AES256TextEncryptor();
      encryptor.setPassword(pepper + saltBase64);

      // Text verschlüseln
      String encryptedText = encryptor.encrypt(plainText);

      return encryptedText + ":" + saltBase64;
   }

   public String decrypt(String encryptedWithSalt) {
      try {
         // Den Text trennen nach dem :
         String[] parts = encryptedWithSalt.split(":");
         if (parts.length != 2) {
            throw new IllegalArgumentException("[EncryptUtil] Invalid encrypted format: missing salt.");
         }

         String encryptedText = parts[0];
         String saltBase64 = parts[1];

         AES256TextEncryptor decryptor = new AES256TextEncryptor();
         decryptor.setPassword(pepper + saltBase64);

         // Text entschlüsseln
         return decryptor.decrypt(encryptedText);

      } catch (Exception e) {
         e.printStackTrace();
         throw new IllegalArgumentException("Decryption failed. Possible wrong password or corrupted data.", e);
      }
   }
}
