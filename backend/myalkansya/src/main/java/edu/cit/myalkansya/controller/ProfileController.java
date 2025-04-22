package edu.cit.myalkansya.controller;

import edu.cit.myalkansya.dto.ProfileUpdateRequest;
import edu.cit.myalkansya.entity.UserEntity;
import edu.cit.myalkansya.repository.UserRepository;
import edu.cit.myalkansya.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/users")
public class ProfileController {

    private static final Logger logger = Logger.getLogger(ProfileController.class.getName());

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private final Path fileStorageLocation = Paths.get("uploads/profile-pictures").toAbsolutePath().normalize();

    public ProfileController() {
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException ex) {
            logger.severe("Could not create the directory where the uploaded files will be stored: " + ex.getMessage());
        }
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateProfile(
            @RequestHeader("Authorization") String token,
            @RequestBody ProfileUpdateRequest request) {
        
        try {
            if (token == null || !token.startsWith("Bearer ")) {
                logger.warning("Invalid authorization token format");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token format"));
            }

            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            if (email == null || email.isEmpty()) {
                logger.warning("Could not extract email from token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid token"));
            }
            
            logger.info("Attempting to update profile for user with email: " + email);
            
            Optional<UserEntity> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) {
                logger.warning("User not found with email: " + email);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not found"));
            }

            UserEntity user = userOpt.get();
            logger.info("Found user: " + user.getUserId() + ", " + user.getFirstname() + " " + user.getLastname());
            
            // Validate request data
            if (request == null) {
                logger.warning("Profile update request is null");
                return ResponseEntity.badRequest().body(Map.of("error", "Request body cannot be null"));
            }
            
            // Update user fields with null checks
            if (request.getFirstname() != null && !request.getFirstname().trim().isEmpty()) {
                user.setFirstname(request.getFirstname());
            } else {
                logger.warning("First name is null or empty in request");
            }
            
            if (request.getLastname() != null && !request.getLastname().trim().isEmpty()) {
                user.setLastname(request.getLastname());
            } else {
                logger.warning("Last name is null or empty in request");
            }
            
            // Only update email if it's provided and valid
            if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
                // Check if new email already exists for another user
                if (!request.getEmail().equals(email)) {
                    Optional<UserEntity> existingUserWithEmail = userRepository.findByEmail(request.getEmail());
                    if (existingUserWithEmail.isPresent() && existingUserWithEmail.get().getUserId() != user.getUserId()) {
                        logger.warning("Email already in use: " + request.getEmail());
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(Map.of("error", "Email already in use by another user"));
                    }
                }
                user.setEmail(request.getEmail());
            } else {
                logger.warning("Email is null or empty in request");
            }
            
            if (request.getCurrency() != null && !request.getCurrency().trim().isEmpty()) {
                user.setCurrency(request.getCurrency());
            }

            // Save updated user
            logger.info("Saving updated user profile");
            UserEntity updatedUser = userRepository.save(user);
            logger.info("User profile updated successfully");

            return ResponseEntity.ok(Map.of(
                "userId", updatedUser.getUserId(),
                "firstname", updatedUser.getFirstname(),
                "lastname", updatedUser.getLastname(),
                "email", updatedUser.getEmail(),
                "currency", updatedUser.getCurrency(),
                "profilePicture", updatedUser.getProfilePicture() != null ? updatedUser.getProfilePicture() : "",
                "totalSavings", updatedUser.getTotalSavings()
            ));
        } catch (Exception e) {
            logger.severe("Error updating profile: " + e.getMessage());
            if (e.getMessage() == null) {
                // Log stack trace when message is null
                StackTraceElement[] stackTrace = e.getStackTrace();
                StringBuilder stackTraceStr = new StringBuilder();
                for (int i = 0; i < Math.min(5, stackTrace.length); i++) {
                    stackTraceStr.append(stackTrace[i].toString()).append("\n");
                }
                logger.severe("Exception with null message. Stack trace: " + stackTraceStr);
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update profile: " + 
                        (e.getMessage() != null ? e.getMessage() : "Internal server error")));
        }
    }

    @PostMapping(value = "/uploadProfilePicture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadProfilePicture(
            @RequestHeader("Authorization") String token,
            @RequestParam("profilePicture") MultipartFile file) {
        
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Please select a file to upload"));
            }

            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            Optional<UserEntity> userOpt = userRepository.findByEmail(email);

            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not found"));
            }

            UserEntity user = userOpt.get();

            // Generate unique filename
            String fileExtension = getFileExtension(file.getOriginalFilename());
            String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
            Path targetLocation = this.fileStorageLocation.resolve(uniqueFileName);
            
            // Copy file to storage location
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            
            // Create URL (adjust this according to your actual server URL configuration)
            String fileUrl = "/uploads/profile-pictures/" + uniqueFileName;
            user.setProfilePicture(fileUrl);
            
            // Save updated user
            UserEntity updatedUser = userRepository.save(user);
            
            return ResponseEntity.ok(Map.of(
                "userId", updatedUser.getUserId(),
                "firstname", updatedUser.getFirstname(),
                "lastname", updatedUser.getLastname(),
                "email", updatedUser.getEmail(),
                "currency", updatedUser.getCurrency(),
                "profilePicture", updatedUser.getProfilePicture(),
                "totalSavings", updatedUser.getTotalSavings()
            ));
        } catch (Exception e) {
            logger.severe("Error uploading profile picture: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Could not upload file: " + e.getMessage()));
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") == -1) {
            return ".jpg"; // Default extension
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }
}
