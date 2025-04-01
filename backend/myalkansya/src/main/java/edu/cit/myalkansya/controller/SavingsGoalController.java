package edu.cit.myalkansya.controller;

import edu.cit.myalkansya.entity.SavingsGoalEntity;
import edu.cit.myalkansya.security.JwtUtil;
import edu.cit.myalkansya.service.SavingsGoalService;
import edu.cit.myalkansya.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/savings-goals")
public class SavingsGoalController {

    @Autowired
    private SavingsGoalService savingsGoalService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtUtil jwtUtil;

    // CREATE
    @PostMapping("/postSavingsGoal")
    public ResponseEntity<?> postSavingsGoal(@RequestBody SavingsGoalEntity savingsGoal, @RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            int userId = userService.findByEmail(email).get().getUserId();
            
            SavingsGoalEntity savedGoal = savingsGoalService.createSavingsGoal(savingsGoal, userId);
            return ResponseEntity.ok(savedGoal);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // READ
    @GetMapping("/getSavingsGoals")
    public ResponseEntity<?> getSavingsGoals(@RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            int userId = userService.findByEmail(email).get().getUserId();
            
            List<SavingsGoalEntity> goals = savingsGoalService.getSavingsGoalsByUserId(userId);
            return ResponseEntity.ok(goals);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/getSavingsGoal/{goalId}")
    public ResponseEntity<?> getSavingsGoalById(@PathVariable int goalId, @RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            int userId = userService.findByEmail(email).get().getUserId();
            
            if (!savingsGoalService.savingsGoalExistsAndBelongsToUser(goalId, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Savings goal not found or does not belong to user");
            }
            
            Optional<SavingsGoalEntity> goal = savingsGoalService.getSavingsGoalById(goalId);
            return ResponseEntity.ok(goal);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // UPDATE
    @PutMapping("/putSavingsGoal/{goalId}")
    public ResponseEntity<?> putSavingsGoal(@PathVariable int goalId, @RequestBody SavingsGoalEntity updatedGoal, @RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            int userId = userService.findByEmail(email).get().getUserId();
            
            SavingsGoalEntity updated = savingsGoalService.updateSavingsGoal(goalId, updatedGoal, userId);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // DELETE
    @DeleteMapping("/deleteSavingsGoal/{goalId}")
    public ResponseEntity<?> deleteSavingsGoal(@PathVariable int goalId, @RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            int userId = userService.findByEmail(email).get().getUserId();
            
            String result = savingsGoalService.deleteSavingsGoal(goalId, userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}