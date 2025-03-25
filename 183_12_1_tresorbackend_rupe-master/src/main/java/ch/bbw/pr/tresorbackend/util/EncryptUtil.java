package ch.bbw.pr.tresorbackend.util;

import org.jasypt.util.text.AES256TextEncryptor;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;

/**
 * EncryptUtil
 * Used to encrypt content.
 * Not implemented yet.
 * @author Petr Cerny
 */
public class EncryptUtil {

   SecureRandom random = new SecureRandom();
   AES256TextEncryptor encryptor = new AES256TextEncryptor();
   byte[] salt = new byte[32];
   String secretKey;

   public EncryptUtil(String secretKey) {
      this.secretKey = secretKey;
   }

   public String encrypt(String data) {
      // random wert wird generiert und zum secret hinzugefügt
      random.nextBytes(salt);
      String decodedSalt = Base64.getEncoder().encodeToString(salt);
      secretKey = secretKey + decodedSalt;
      encryptor.setPassword(secretKey);

      data = encryptor.encrypt(data);

      return data;
   }

   public String decrypt(String data) {
      List<String> encryption = Arrays.asList(data.split(":"));
      String secret = encryption.get(0);
      String salt = encryption.get(1);

      byte[] decodedSalt = Base64.getDecoder().decode(salt);
      encryptor.setPassword(secret + decodedSalt);

      data = encryptor.decrypt(secret);
      return data;
   }
}
