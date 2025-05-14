package ch.bbw.pr.tresorbackend.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;

@Service
public class PasswordEncryptionService {

   private final PasswordEncoder passwordEncoder;
   private final SecureRandom secureRandom = new SecureRandom();

   public String generatePepper() {
      byte[] bytes = new byte[16];
      secureRandom.nextBytes(bytes);
      return Base64.getEncoder().encodeToString(bytes);
   }


   public PasswordEncryptionService() {
      this.passwordEncoder = new BCryptPasswordEncoder();
   }

   public String hashPassword(String password, String pepper) {
      return passwordEncoder.encode(password + pepper);
   }

   public boolean verifyPassword(String rawPassword, String storedHash, String pepper) {
      return passwordEncoder.matches(rawPassword + pepper, storedHash);
   }

}