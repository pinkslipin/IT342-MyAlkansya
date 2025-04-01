package edu.cit.myalkansya.controller;

import edu.cit.myalkansya.dto.AuthResponse;
import edu.cit.myalkansya.dto.GoogleUserDTO;
import edu.cit.myalkansya.dto.LoginRequest;
import edu.cit.myalkansya.dto.RegisterRequest;
import edu.cit.myalkansya.dto.FacebookAuthRequest;
import edu.cit.myalkansya.dto.FacebookUserDTO;
import edu.cit.myalkansya.entity.UserEntity;
import edu.cit.myalkansya.repository.UserRepository;
import edu.cit.myalkansya.service.UserService;
import edu.cit.myalkansya.security.JwtUtil;
import edu.cit.myalkansya.security.GoogleTokenVerifier;
import edu.cit.myalkansya.security.FacebookTokenVerifier;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger logger = Logger.getLogger(UserController.class.getName());
    
    private final GoogleTokenVerifier googleTokenVerifier;
    private final FacebookTokenVerifier facebookTokenVerifier;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
     
    @Autowired  
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
        userService.registerLocalUser(
            request.getFirstname(),
            request.getLastname(),
            request.getEmail(),
            request.getPassword(),
            request.getCurrency()
        );
        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Optional<UserEntity> userOpt = userService.findByEmail(request.getEmail());

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password");
        }

        UserEntity user = userOpt.get();

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password");
        }

        String token = jwtUtil.generateToken(user.getEmail());

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("user", Map.of(
            "userId", user.getUserId(),
            "firstname", user.getFirstname(),
            "lastname", user.getLastname(),
            "email", user.getEmail(),
            "currency", user.getCurrency(),
            "totalSavings", user.getTotalSavings()
        ));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String token) {
        String email = jwtUtil.extractEmail(token.replace("Bearer ", "")); // Fixed method name
        Optional<UserEntity> userOpt = userService.findByEmail(email);
 
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }
 
        UserEntity user = userOpt.get();
        return ResponseEntity.ok(Map.of(
            "userId", user.getUserId(),
            "firstname", user.getFirstname(),
            "lastname", user.getLastname(),
            "email", user.getEmail(),
            "currency", user.getCurrency(),
            "totalSavings", user.getTotalSavings()
        ));
    }

    public UserController(
            UserService userService,
            JwtUtil jwtUtil,
            GoogleTokenVerifier googleTokenVerifier,
            FacebookTokenVerifier facebookTokenVerifier,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.googleTokenVerifier = googleTokenVerifier;
        this.facebookTokenVerifier = facebookTokenVerifier;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Google login endpoint - only for existing users
    @PostMapping("/google/login")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> request) {
        String idToken = request.get("idToken");
        if (idToken == null || idToken.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("ID Token is required for authentication");
        }
    
        GoogleUserDTO googleUser = googleTokenVerifier.verifyGoogleToken(idToken);
        if (googleUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Google ID Token");
        }
    
        String email = googleUser.getEmail();
    
        if (email == null || email.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email is required for authentication");
        }
    
        // Check if user exists
        Optional<UserEntity> userOptional = userService.findByEmail(email);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("No user found with this Google account. Please sign up first.");
        }
    
        UserEntity user = userOptional.get();
        
        // Generate JWT token
        String token = jwtUtil.generateToken(user.getEmail());
    
        AuthResponse authResponse = new AuthResponse(googleUser, token);
        return ResponseEntity.ok(authResponse);
    }
    
    // Google registration endpoint - for new users only
    @PostMapping("/google/register")
    public ResponseEntity<?> registerWithGoogle(@RequestBody Map<String, String> request) {
        String idToken = request.get("idToken");
        if (idToken == null || idToken.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("ID Token is required for registration");
        }
    
        GoogleUserDTO googleUser = googleTokenVerifier.verifyGoogleToken(idToken);
        if (googleUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Google ID Token");
        }
    
        String email = googleUser.getEmail();
    
        if (email == null || email.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email is required for registration");
        }
    
        // Check if user already exists
        Optional<UserEntity> existingUser = userService.findByEmail(email);
        if (existingUser.isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("User with this Google account already exists. Please sign in instead.");
        }
    
        // Create new user
        UserEntity user = new UserEntity();
        user.setFirstname(googleUser.getFirstname());
        user.setLastname(googleUser.getLastname());
        user.setEmail(email);
        user.setProfilePicture(googleUser.getProfilePicture());
        user.setProviderId(googleUser.getProviderId());
        user.setAuthProvider("GOOGLE");
        user = userService.save(user);
    
        // Generate JWT token
        String token = jwtUtil.generateToken(user.getEmail());
    
        AuthResponse authResponse = new AuthResponse(googleUser, token);
        return ResponseEntity.ok(authResponse);
    }

    //old endpoint for backward compatibility 
    @Deprecated
    @PostMapping("/google")
    public ResponseEntity<?> authenticateWithGoogle(@RequestBody Map<String, String> request) {
        // Delegate to the new registration endpoint
        return registerWithGoogle(request);
    }

    @PostMapping("/facebook/login")
    public ResponseEntity<?> facebookLogin(@RequestBody FacebookAuthRequest request) {
        String accessToken = request.getAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Facebook access token is required");
        }
    
        // Verify the Facebook token and get user info
        FacebookUserDTO facebookUser = facebookTokenVerifier.verifyFacebookToken(accessToken);
        if (facebookUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Facebook access token");
        }
        
        String email = facebookUser.getEmail();
        
        if (email == null || email.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Email is required for authentication. Please ensure your Facebook account has a valid email.");
        }
    
        // Check if user exists
        Optional<UserEntity> userOptional = userService.findByEmail(email);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("No user found with this Facebook account. Please sign up first.");
        }
    
        UserEntity user = userOptional.get();
        
        // Generate JWT token
        String token = jwtUtil.generateToken(user.getEmail());
    
        // Create AuthResponse with user info and token
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("userId", user.getUserId());
        userMap.put("firstname", user.getFirstname());
        userMap.put("lastname", user.getLastname());
        userMap.put("email", user.getEmail());
        userMap.put("providerId", facebookUser.getProviderId());
        
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("user", userMap);
    
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/facebook/register")
    public ResponseEntity<?> registerWithFacebook(@RequestBody FacebookAuthRequest request) {
        String accessToken = request.getAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Facebook access token is required");
        }
    
        try {
            // Verify the Facebook token and get user info
            FacebookUserDTO facebookUser = facebookTokenVerifier.verifyFacebookToken(accessToken);
            if (facebookUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Facebook access token");
            }
            
            String email = facebookUser.getEmail();
            String providerId = facebookUser.getProviderId();
            
            if (email == null || email.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Email is required for registration. Please ensure your Facebook account has a valid email.");
            }
            
            // Check if user already exists
            Optional<UserEntity> existingUser = userService.findByEmail(email);
            if (existingUser.isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("User with this Facebook account already exists. Please sign in instead.");
            }
        
            // Create new user
            UserEntity user = new UserEntity();
            user.setFirstname(facebookUser.getFirstname());
            user.setLastname(facebookUser.getLastname());
            user.setEmail(email);
            user.setProfilePicture(facebookUser.getProfilePicture());
            user.setProviderId(providerId);
            user.setAuthProvider("FACEBOOK");
            
            try {
                user = userService.save(user);
                logger.info("Registered new Facebook user: " + email + ", ProviderId: " + providerId);
            } catch (Exception e) {
                logger.severe("Error saving Facebook user: " + e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to create user account: " + e.getMessage());
            }
        
            // Generate JWT token
            String token = jwtUtil.generateToken(user.getEmail());
        
            // Create AuthResponse with user info and token
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("userId", user.getUserId());
            userMap.put("firstname", user.getFirstname());
            userMap.put("lastname", user.getLastname());
            userMap.put("email", user.getEmail());
            userMap.put("providerId", providerId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("user", userMap);
        
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.severe("Unexpected error in Facebook registration: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An unexpected error occurred: " + e.getMessage());
        }
    }
    
}
