package ch.bbw.pr.tresorbackend.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordEncryptionService {

   private final PasswordEncoder passwordEncoder;

   private static final String PEPPER = "thisIsAVerySecretPepper:)"; // Pepper was verschlüsselt und zum password hinzugefügt wird

   public PasswordEncryptionService() {
      this.passwordEncoder = new BCryptPasswordEncoder();
   }

   public String hashPassword(String password) {
      String passwordWithPepper = password + PEPPER;
      // encryption mit passwordEncoder
      return passwordEncoder.encode(passwordWithPepper);
   }

   public boolean verifyPassword(String rawPassword, String storedHash) {
      String rawWithPepper = rawPassword + PEPPER;
      return passwordEncoder.matches(rawWithPepper, storedHash);
   }

}