package edu.cit.myalkansya.controller;

import edu.cit.myalkansya.dto.LoginRequest;
import edu.cit.myalkansya.dto.RegisterRequest;
import edu.cit.myalkansya.entity.UserEntity;
import edu.cit.myalkansya.service.UserService;
import edu.cit.myalkansya.security.JwtUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder encoder;

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

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Optional<UserEntity> userOpt = userService.findByEmail(request.getEmail());

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password");
        }

        UserEntity user = userOpt.get();

        if (!encoder.matches(request.getPassword(), user.getPassword())) {
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
}