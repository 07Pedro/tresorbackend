package ch.bbw.pr.tresorbackend.controller;

import ch.bbw.pr.tresorbackend.model.ConfigProperties;
import ch.bbw.pr.tresorbackend.model.EmailAdress;
import ch.bbw.pr.tresorbackend.model.RegisterUser;
import ch.bbw.pr.tresorbackend.model.User;
import ch.bbw.pr.tresorbackend.service.PasswordEncryptionService;
import ch.bbw.pr.tresorbackend.service.UserService;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * UserController
 * @author Peter Rutschmann
 */
@RestController
@AllArgsConstructor
@RequestMapping("api/users")
public class UserController {

   private UserService userService;
   private PasswordEncryptionService passwordService;
   private final ConfigProperties configProperties;
   private static final Logger logger = LoggerFactory.getLogger(UserController.class);

   @Autowired
   public UserController(ConfigProperties configProperties, UserService userService,
                         PasswordEncryptionService passwordService) {
      this.configProperties = configProperties;
      // Logging in the constructor
      logger.info("UserController initialized: " + configProperties.getOrigin());
      logger.debug("UserController.UserController: Cross Origin Config: {}", configProperties.getOrigin());
      this.userService = userService;
      this.passwordService = passwordService;
   }

   // build create User REST API
   @CrossOrigin(origins = "${CROSS_ORIGIN}")
   @PostMapping ("/user/register")
   public ResponseEntity<String> createUser(@Valid @RequestBody RegisterUser registerUser, BindingResult bindingResult) {

      if (!isCaptchaValid(registerUser.getRecaptchaToken())) {
         JsonObject obj = new JsonObject();
         obj.addProperty("error", "Captcha validation failed.");
         return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new Gson().toJson(obj));
      }

      //input validation
      if (bindingResult.hasErrors()) {
         List<String> errors = bindingResult.getFieldErrors().stream()
               .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
               .collect(Collectors.toList());

         JsonArray arr = new JsonArray();
         errors.forEach(arr::add);
         JsonObject obj = new JsonObject();
         obj.add("message", arr);
         String json = new Gson().toJson(obj);

         return ResponseEntity.badRequest().body(json);
      }

       // Neue Passwortstärke-Prüfung
       if (!isPasswordStrong(registerUser.getPassword())) {
           JsonObject obj = new JsonObject();
           obj.addProperty("error", "Password is too weak. It must be at least 8 characters long and contain uppercase, lowercase, number and special character.");
           return ResponseEntity.badRequest().body(new Gson().toJson(obj));
       }

       //Generiere individuelle Pepper für den User
       String pepper = passwordService.generatePepper();
       String hashedPassword = passwordService.hashPassword(registerUser.getPassword(), pepper);

      //transform registerUser to user
      User user = new User(
            null,
               registerUser.getFirstName(),
               registerUser.getLastName(),
               registerUser.getEmail(),
               hashedPassword,
               pepper);

      User savedUser = userService.createUser(user);
      JsonObject obj = new JsonObject();
      obj.addProperty("answer", "User Saved");
      String json = new Gson().toJson(obj);
      return ResponseEntity.accepted().body(json);
   }

    private boolean isPasswordStrong(String password) {
        if (password == null) return false;
        if (password.length() < 8) return false;
        if (!password.matches(".*[A-Z].*")) return false;    // Großbuchstabe
        if (!password.matches(".*[a-z].*")) return false;    // Kleinbuchstabe
        if (!password.matches(".*\\d.*")) return false;      // Zahl
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) return false; // Sonderzeichen
        return true;
    }



    // build get user by id REST API
   // http://localhost:8080/api/users/1
   @CrossOrigin(origins = "${CROSS_ORIGIN}")
   @GetMapping("{id}")
   public ResponseEntity<User> getUserById(@PathVariable("id") Long userId) {
      User user = userService.getUserById(userId);
      return new ResponseEntity<>(user, HttpStatus.OK);
   }

   // Build Get All Users REST API
   // http://localhost:8080/api/users
   @CrossOrigin(origins = "${CROSS_ORIGIN}")
   @GetMapping
   public ResponseEntity<List<User>> getAllUsers() {
      List<User> users = userService.getAllUsers();
      return new ResponseEntity<>(users, HttpStatus.OK);
   }

   // Build Update User REST API
   // http://localhost:8080/api/users/1
   @CrossOrigin(origins = "${CROSS_ORIGIN}")
   @PutMapping("{id}")
   public ResponseEntity<User> updateUser(@PathVariable("id") Long userId,
                                          @RequestBody User user) {
      user.setId(userId);
      User updatedUser = userService.updateUser(user);
      return new ResponseEntity<>(updatedUser, HttpStatus.OK);
   }

   // Build Delete User REST API
   @CrossOrigin(origins = "${CROSS_ORIGIN}")
   @DeleteMapping("{id}")
   public ResponseEntity<String> deleteUser(@PathVariable("id") Long userId) {
      userService.deleteUser(userId);
      return new ResponseEntity<>("User successfully deleted!", HttpStatus.OK);
   }


   // get user id by email
   @CrossOrigin(origins = "${CROSS_ORIGIN}")
   @PostMapping("/byemail")
   public ResponseEntity<String> getUserIdByEmail(@RequestBody EmailAdress email, BindingResult bindingResult) {
      //input validation
      if (bindingResult.hasErrors()) {
         List<String> errors = bindingResult.getFieldErrors().stream()
               .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
               .collect(Collectors.toList());

         JsonArray arr = new JsonArray();
         errors.forEach(arr::add);
         JsonObject obj = new JsonObject();
         obj.add("message", arr);
         String json = new Gson().toJson(obj);

         return ResponseEntity.badRequest().body(json);
      }


      User user = userService.findByEmail(email.getEmail());
      if (user == null) {
         JsonObject obj = new JsonObject();
         obj.addProperty("message", "No user found with this email");
         String json = new Gson().toJson(obj);

         return ResponseEntity.badRequest().body(json);
      }
      JsonObject obj = new JsonObject();
      obj.addProperty("answer", user.getId());
      String json = new Gson().toJson(obj);
      return ResponseEntity.accepted().body(json);
   }

   // funktion zum check, ob passwort übereinanderstimmt
   @PostMapping("/login")
   public ResponseEntity<String> loginUser(@RequestBody RegisterUser loginRequest) {
      // get user email
      User user = userService.findByEmail(loginRequest.getEmail());

      if (user == null || user.getPepper() == null) {
         return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password");
      }

      boolean isPasswordCorrect = passwordService.verifyPassword(
              loginRequest.getPassword(),
              user.getPassword(),
              user.getPepper()
      );

      if (!isPasswordCorrect) {
         return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password");
      }

      return ResponseEntity.ok("Login successful");
   }

   private boolean isCaptchaValid(String token) {
      String secretKey = "6Ldm8hUrAAAAABLbDNOCctHhzRHAOdvhZXoT5wj7";
      String url = "https://www.google.com/recaptcha/api/siteverify";

      RestTemplate restTemplate = new RestTemplate();
      MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
      params.add("secret", secretKey);
      params.add("response", token);

      try {
         String response = restTemplate.postForObject(url, params, String.class);
         JsonObject json = new Gson().fromJson(response, JsonObject.class);
         return json.get("success").getAsBoolean();
      } catch (Exception e) {
         return false;
      }
   }

}
