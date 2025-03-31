package edu.cit.myalkansya.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import edu.cit.myalkansya.entity.BudgetEntity;
import edu.cit.myalkansya.security.JwtUtil;
import edu.cit.myalkansya.service.BudgetService;
import edu.cit.myalkansya.service.UserService;

@RestController
@RequestMapping("/api/budgets")
@CrossOrigin(origins = "http://localhost:5173") // Allow frontend origins
public class BudgetController {

    @Autowired
    private BudgetService budgetService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtUtil jwtUtil;

    // CREATE
    @PostMapping("/postBudget")
    public ResponseEntity<?> postBudget(@RequestBody BudgetEntity budget, @RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            int userId = userService.findByEmail(email).get().getUserId();
            
            BudgetEntity savedBudget = budgetService.createBudget(budget, userId);
            return ResponseEntity.ok(savedBudget);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // READ
    @GetMapping("/getBudgets")
    public ResponseEntity<?> getBudgets(@RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            int userId = userService.findByEmail(email).get().getUserId();
            
            List<BudgetEntity> budgets = budgetService.getBudgetsByUserId(userId);
            return ResponseEntity.ok(budgets);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/getBudget/{budgetId}")
    public ResponseEntity<?> getBudgetById(@PathVariable int budgetId, @RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            int userId = userService.findByEmail(email).get().getUserId();
            
            if (!budgetService.budgetExistsAndBelongsToUser(budgetId, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Budget not found or does not belong to user");
            }
            
            BudgetEntity budget = budgetService.getBudgetById(budgetId);
            return ResponseEntity.ok(budget);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // UPDATE
    @PutMapping("/putBudget/{budgetId}")
    public ResponseEntity<?> putBudget(
            @PathVariable int budgetId, 
            @RequestBody BudgetEntity newBudgetDetails,
            @RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            int userId = userService.findByEmail(email).get().getUserId();
            
            BudgetEntity updatedBudget = budgetService.updateBudget(budgetId, newBudgetDetails, userId);
            return ResponseEntity.ok(updatedBudget);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // DELETE
    @DeleteMapping("/deleteBudget/{budgetId}")
    public ResponseEntity<?> deleteBudget(@PathVariable int budgetId, @RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            int userId = userService.findByEmail(email).get().getUserId();
            
            String result = budgetService.deleteBudget(budgetId, userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}