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
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.web.multipart.MultipartFile;
import edu.cit.myalkansya.dto.UserUpdateDTO;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;

// Add this import with your other imports
import edu.cit.myalkansya.dto.PasswordChangeRequest;

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
        String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
        Optional<UserEntity> userOpt = userService.findByEmail(email);
 
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }
 
        UserEntity user = userOpt.get();
        // Include the profilePicture in the response
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("userId", user.getUserId());
        responseMap.put("firstname", user.getFirstname());
        responseMap.put("lastname", user.getLastname());
        responseMap.put("email", user.getEmail());
        responseMap.put("currency", user.getCurrency());
        responseMap.put("totalSavings", user.getTotalSavings());
        responseMap.put("profilePicture", user.getProfilePicture());
        
        return ResponseEntity.ok(responseMap);
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
    
    @PutMapping("/update")
    public ResponseEntity<?> updateUser(@RequestBody UserUpdateDTO updateData, 
                                       @RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            UserEntity updatedUser = userService.updateUser(email, updateData);
            
            // Return updated user data
            return ResponseEntity.ok(Map.of(
                "userId", updatedUser.getUserId(),
                "firstname", updatedUser.getFirstname(),
                "lastname", updatedUser.getLastname(),
                "email", updatedUser.getEmail(),
                "currency", updatedUser.getCurrency(),
                "totalSavings", updatedUser.getTotalSavings(),
                "profilePicture", updatedUser.getProfilePicture()
            ));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            logger.severe("Error updating user: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to update user: " + e.getMessage());
        }
    }
    
    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody PasswordChangeRequest request,
                                      @RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            boolean success = userService.changePassword(email, request.getCurrentPassword(), request.getNewPassword());
            
            if (success) {
                return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Current password is incorrect"));
            }
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "User not found"));
        } catch (Exception e) {
            logger.severe("Error changing password: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to change password: " + e.getMessage()));
        }
    }
    
    @PostMapping("/uploadProfilePicture")
    public ResponseEntity<?> uploadProfilePicture(@RequestParam("profilePicture") MultipartFile file,
                                                 @RequestHeader("Authorization") String token) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please upload a file");
            }
            
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            String profilePictureUrl = userService.uploadProfilePicture(email, file);
            
            // Get the updated user to return all necessary info
            UserEntity user = userService.findByEmail(email).orElseThrow(() -> new NoSuchElementException("User not found"));
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("message", "Profile picture uploaded successfully");
            responseData.put("profilePicture", profilePictureUrl);
            responseData.put("userId", user.getUserId());
            
            return ResponseEntity.ok(responseData);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        } catch (IOException e) {
            logger.severe("Error uploading profile picture: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload profile picture: " + e.getMessage());
        } catch (Exception e) {
            logger.severe("Unexpected error uploading profile picture: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred: " + e.getMessage());
        }
    }
    
    @GetMapping("/profile-pictures/{filename:.+}")
    public ResponseEntity<?> serveProfilePicture(@PathVariable String filename) {
        try {
            Path filePath = Paths.get("user-profile-pictures").resolve(filename).normalize();
            logger.info("Attempting to serve profile picture from: " + filePath.toAbsolutePath());
            
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists()) {
                // Determine content type
                String contentType = determineContentType(filename);
                
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, contentType)
                        .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                        .header(HttpHeaders.PRAGMA, "no-cache")
                        .header(HttpHeaders.EXPIRES, "0")
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                logger.warning("Profile picture file not found: " + filePath.toAbsolutePath());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found");
            }
        } catch (Exception e) {
            logger.severe("Error serving profile picture: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to serve profile picture: " + e.getMessage());
        }
    }

    private String determineContentType(String filename) {
        if (filename.endsWith(".png")) return "image/png";
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) return "image/jpeg";
        if (filename.endsWith(".gif")) return "image/gif";
        if (filename.endsWith(".svg")) return "image/svg+xml";
        if (filename.endsWith(".webp")) return "image/webp";
        return "application/octet-stream"; // Default
    }
}
