package edu.cit.myalkansya.controller;

import edu.cit.myalkansya.dto.AuthResponse;
import edu.cit.myalkansya.dto.GoogleUserDTO;
import edu.cit.myalkansya.dto.LoginRequest;
import edu.cit.myalkansya.dto.RegisterRequest;
import edu.cit.myalkansya.entity.UserEntity;
import edu.cit.myalkansya.repository.UserRepository;
import edu.cit.myalkansya.service.UserService;
import edu.cit.myalkansya.security.JwtUtil;
import edu.cit.myalkansya.security.GoogleTokenVerifier;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;

@RestController
@RequestMapping("/api/auth")
public class UserController {

    private final GoogleTokenVerifier googleTokenVerifier;
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

    public UserController(
            UserService userService,
            JwtUtil jwtUtil,
            GoogleTokenVerifier googleTokenVerifier,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.googleTokenVerifier = googleTokenVerifier;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/google")
    public ResponseEntity<?> authenticateWithGoogle(@RequestBody Map<String, String> request) {
        String idToken = request.get("idToken");
        if (idToken == null || idToken.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("ID Token is required for authentication");
        }
    
        GoogleUserDTO googleUser = googleTokenVerifier.verifyGoogleToken(idToken);
        if (googleUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Google ID Token");
        }
    
        String email = googleUser.getEmail();
    
        // Ensure email is not null
        if (email == null || email.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email is required for authentication");
        }
    
        // Find or create user
        UserEntity user = userService.findByEmail(email).orElse(null);
        if (user == null) {
            user = new UserEntity();
            user.setFirstname(googleUser.getFirstname());
            user.setLastname(googleUser.getLastname());
            user.setEmail(email);
            user.setProfilePicture(googleUser.getProfilePicture());
            user.setProviderId(googleUser.getProviderId());
            user.setAuthProvider("GOOGLE");
            user = userService.save(user);
        }
    
        // Generate JWT token
        String token = jwtUtil.generateToken(user.getEmail());
    
        AuthResponse authResponse = new AuthResponse(googleUser, token);
        return ResponseEntity.ok(authResponse);
    }
    
}
