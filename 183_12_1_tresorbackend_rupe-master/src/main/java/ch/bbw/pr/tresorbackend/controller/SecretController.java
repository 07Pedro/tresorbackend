package ch.bbw.pr.tresorbackend.controller;

import ch.bbw.pr.tresorbackend.model.Secret;
import ch.bbw.pr.tresorbackend.model.NewSecret;
import ch.bbw.pr.tresorbackend.model.EncryptCredentials;
import ch.bbw.pr.tresorbackend.model.User;
import ch.bbw.pr.tresorbackend.service.SecretService;
import ch.bbw.pr.tresorbackend.service.UserService;
import ch.bbw.pr.tresorbackend.util.EncryptUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.validation.Valid;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * SecretController
 * @author Peter Rutschmann
 */
@RestController
@RequestMapping("/api/secrets")
public class SecretController {

   private SecretService secretService;
   private UserService userService;

   public SecretController(SecretService secretService, UserService userService) {
      this.secretService = secretService;
      this.userService = userService;
   }

   @CrossOrigin(origins = "${CROSS_ORIGIN}")
   @PostMapping
   public ResponseEntity<String> createSecret2(@Valid @RequestBody NewSecret newSecret, BindingResult bindingResult) {

      // Überprüft ob es errors im input gibt. Wenn ja gibt eine Liste von errors zurück
      if (bindingResult.hasErrors()) {
         List<String> errors = bindingResult.getFieldErrors().stream()
               .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
               .collect(Collectors.toList());
         System.out.println("SecretController.createSecret " + errors);

         JsonArray arr = new JsonArray();
         errors.forEach(arr::add);
         JsonObject obj = new JsonObject();
         obj.add("message", arr);
         return ResponseEntity.badRequest().body(obj.toString());
      }

      User user = userService.findByEmail(newSecret.getEmail()); // Users mail

      JsonObject contentJson = JsonParser.parseString(newSecret.getContent().toString()).getAsJsonObject(); // Der Inhalt des Secrets wird als JsonObject deffiniert
      String encryptedContent = encryptJsonValues(contentJson, newSecret.getEncryptPassword()); // aufruf der encryption funktion mit secret und passowrd

      Secret secret = new Secret(null, user.getId(), encryptedContent); // zusammensetzen des Secrets (id: wird in db generiert, user_id: id des Users im User-table, encryptedContent: verschlüsselte values)
      secretService.createSecret(secret); // secret wird als secret gespeichert

      JsonObject obj = new JsonObject();
      obj.addProperty("answer", "Secret saved");
      return ResponseEntity.accepted().body(obj.toString());
   }

   // Build Get Secrets by userId REST API -> keine changes gemacht
   @CrossOrigin(origins = "${CROSS_ORIGIN}")
   @PostMapping("/byuserid")
   public ResponseEntity<List<Secret>> getSecretsByUserId(@RequestBody EncryptCredentials credentials) {
      System.out.println("SecretController.getSecretsByUserId " + credentials);

      List<Secret> secrets = secretService.getSecretsByUserId(credentials.getUserId());
      if (secrets.isEmpty()) {
         System.out.println("SecretController.getSecretsByUserId secret isEmpty");
         return ResponseEntity.notFound().build();
      }
      //Decrypt content
      for(Secret secret: secrets) {
         try {
            secret.setContent(new EncryptUtil(credentials.getEncryptPassword()).decrypt(secret.getContent()));
         } catch (EncryptionOperationNotPossibleException e) {
            System.out.println("SecretController.getSecretsByUserId " + e + " " + secret);
            secret.setContent("not encryptable. Wrong password?");
         }
      }

      System.out.println("SecretController.getSecretsByUserId " + secrets);
      return ResponseEntity.ok(secrets);
   }

   @CrossOrigin(origins = "${CROSS_ORIGIN}")
   @PostMapping("/byemail") // endpoint
   public ResponseEntity<List<Secret>> getSecretsByEmail(@RequestBody EncryptCredentials credentials) {
      System.out.println("SecretController.getSecretsByEmail called with credentials: " + credentials); // mitgegebene credentials

      User user = userService.findByEmail(credentials.getEmail()); // User durch email gefunden
      // return wenn kein user
      if (user == null) {
         System.out.println("User not found for email: " + credentials.getEmail());
         return ResponseEntity.status(HttpStatus.NOT_FOUND).body(List.of());
      }

      // Liste der Secrets, welche die gleiche id haben wie der User
      List<Secret> secrets = secretService.getSecretsByUserId(user.getId());
      // handling wenn keine secrets vorhanden sind
      if (secrets.isEmpty()) {
         System.out.println("No secrets found for user with email: " + credentials.getEmail());
         return ResponseEntity.status(HttpStatus.NOT_FOUND).body(List.of());
      }

      // foreach loop durch alle secrets
      for (Secret secret : secrets) {
         if (!secret.getUserId().equals(user.getId())) { // check ob user_id übereinstimmen
            System.out.println("Access denied to secret with id: " + secret.getId());
            continue;  // weitermachen bis alle secrets gefiltirt wurden
         }
         try {
            String decryptedContent = decryptJsonValues(secret.getContent(), credentials.getEncryptPassword()); // das secret und encrypted passwort werden entschlüsselt
            System.out.println("Decrypted secret content for secret ID: " + secret.getId() + " - " + decryptedContent);
            secret.setContent(decryptedContent); // das secret wird im unverschlüsseltem zustand gespeichert
         } catch (Exception e) { // error handling
            secret.setContent("{\"error\": \"Could not decrypt content.\"}");
            System.out.println("Decryption failed for secret with id: " + secret.getId());
         }
      }

      System.out.println("SecretController.getSecretsByEmail " + secrets);
      return ResponseEntity.ok(secrets);
   }

   private String encryptJsonValues(JsonObject json, String password) {
      EncryptUtil encryptUtil = new EncryptUtil(password); // Verschlüsselungslogik mit Passwort
      JsonObject encrypted = new JsonObject(); // neues leeres JSON
      for (String key : json.keySet()) { // loop um jeden key zu verschlüsseln
         encrypted.addProperty(
                 key,
                 encryptUtil.encrypt(json.get(key).getAsString())
         );
      }
      return encrypted.toString(); // return as string
   }

   private String decryptJsonValues(String content, String password) {
      EncryptUtil encryptUtil = new EncryptUtil(password); // Entschlüsselungslogik mit Passwort
      JsonObject encrypted = JsonParser.parseString(content).getAsJsonObject();// String "content" wird zu JSON
      JsonObject decrypted = new JsonObject(); // neues leeres JSON
      for (String key : encrypted.keySet()) { // loop um jeden key zu entschlüsseln
         try {
            decrypted.addProperty(
                    key,
                    encryptUtil.decrypt(encrypted.get(key).getAsString())
            );
         } catch (EncryptionOperationNotPossibleException e) {
            decrypted.addProperty(key, "Decryption failed"); // error handling
         }
      }
      return decrypted.toString();// return as string
   }
}
