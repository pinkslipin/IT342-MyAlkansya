package edu.cit.myalkansya.service;

import edu.cit.myalkansya.dto.GoogleUserDTO;
import edu.cit.myalkansya.dto.UserUpdateDTO;
import edu.cit.myalkansya.entity.UserEntity;
import edu.cit.myalkansya.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private PasswordEncoder encoder;

    public UserEntity registerLocalUser(String firstname, String lastname, String email, String password, String currency) {
        UserEntity user = new UserEntity();
        user.setFirstname(firstname);
        user.setLastname(lastname);
        user.setEmail(email);
        user.setPassword(encoder.encode(password));
        user.setAuthProvider("LOCAL");
        user.setCurrency(currency);
        return userRepo.save(user);
    }

    public Optional<UserEntity> findByEmail(String email) {
        return userRepo.findByEmail(email);
    }

    public Optional<UserEntity> findByProviderId(String providerId) {
        return userRepo.findByProviderId(providerId);
    }

    public UserEntity save(UserEntity user) {
        return userRepo.save(user); 
    }

    public UserEntity processGoogleLogin(GoogleUserDTO googleUser) {
        UserEntity user = userRepo.findByEmail(googleUser.getEmail())
            .orElseGet(() -> {
                UserEntity newUser = new UserEntity(); // ✅ Fixed: Changed `User` to `UserEntity`
                newUser.setEmail(googleUser.getEmail());
                newUser.setFirstname(googleUser.getFirstname());
                newUser.setLastname(googleUser.getLastname() != null ? googleUser.getLastname() : "Unknown"); // ✅ Ensure lastname is not null
                newUser.setProviderId(googleUser.getProviderId());
                newUser.setAuthProvider("GOOGLE"); 
                newUser.setProfilePicture(googleUser.getProfilePicture());
                newUser.setCreatedAt(LocalDateTime.now());
                return userRepo.save(newUser);
            });

        return user;
    }

    @Transactional
    public UserEntity updateUser(String email, UserUpdateDTO updateData) {
        Optional<UserEntity> userOpt = userRepo.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new NoSuchElementException("User with email " + email + " not found");
        }

        UserEntity user = userOpt.get();
        
        // Update fields if they are provided
        if (updateData.getFirstname() != null && !updateData.getFirstname().trim().isEmpty()) {
            user.setFirstname(updateData.getFirstname().trim());
        }
        
        if (updateData.getLastname() != null && !updateData.getLastname().trim().isEmpty()) {
            user.setLastname(updateData.getLastname().trim());
        }
        
        if (updateData.getEmail() != null && !updateData.getEmail().trim().isEmpty() 
                && !updateData.getEmail().equals(email)) {
            // Check if new email is already in use
            if (userRepo.findByEmail(updateData.getEmail()).isPresent()) {
                throw new IllegalArgumentException("Email already in use by another account");
            }
            user.setEmail(updateData.getEmail().trim());
        }
        
        if (updateData.getCurrency() != null && !updateData.getCurrency().trim().isEmpty()) {
            user.setCurrency(updateData.getCurrency().trim());
        }
        
        return userRepo.save(user);
    }

    @Transactional
    public String uploadProfilePicture(String email, MultipartFile file) throws IOException {
        Optional<UserEntity> userOpt = userRepo.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new NoSuchElementException("User not found");
        }

        UserEntity user = userOpt.get();
        
        // Create upload directory if it doesn't exist
        String uploadDir = "user-profile-pictures";
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // Create a unique file name to prevent collisions
        String filename = user.getUserId() + "_" + UUID.randomUUID().toString();
        
        // Get file extension
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        Path filePath = uploadPath.resolve(filename + extension);
        
        // Copy the file to the upload directory
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        // Important: Use the correct URL format for the profile picture
        String profilePictureUrl = "/api/users/profile-pictures/" + filename + extension;
        user.setProfilePicture(profilePictureUrl);
        userRepo.save(user);
        
        return profilePictureUrl;
    }

    @Transactional
    public String uploadProfilePictureBase64(String email, byte[] imageBytes, String fileName, String contentType) {
        Optional<UserEntity> userOpt = userRepo.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new NoSuchElementException("User not found");
        }

        UserEntity user = userOpt.get();
        
        // Store the image bytes directly in the database
        user.setProfileImageData(imageBytes);
        
        // Keep track of the image content type
        user.setProfilePicture("data:" + contentType + ";base64,user-profile-picture");
        
        userRepo.save(user);
        
        return user.getProfileImageBase64();
    }

    // Add a method to retrieve profile picture as Base64
    public String getProfilePictureBase64(int userId) {
        UserEntity user = userRepo.findById(userId)
            .orElseThrow(() -> new NoSuchElementException("User not found"));
        
        if (user.getProfileImageData() != null) {
            return Base64.getEncoder().encodeToString(user.getProfileImageData());
        }
        
        return null;
    }

    @Transactional
    public boolean changePassword(String email, String currentPassword, String newPassword) {
        Optional<UserEntity> userOptional = userRepo.findByEmail(email);
        if (userOptional.isEmpty()) {
            throw new NoSuchElementException("User not found");
        }
        
        UserEntity user = userOptional.get();
        
        // For OAuth users who might not have a password set
        if (user.getPassword() == null && !("LOCAL".equals(user.getAuthProvider()))) {
            // Set password for OAuth user who doesn't have one yet
            user.setPassword(encoder.encode(newPassword));
            userRepo.save(user);
            return true;
        }
        
        // Check if the current password is correct
        if (!encoder.matches(currentPassword, user.getPassword())) {
            return false;
        }
        
        // Update with the new password
        user.setPassword(encoder.encode(newPassword));
        userRepo.save(user);
        
        return true;
    }
}
