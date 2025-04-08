package edu.cit.myalkansya.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.cit.myalkansya.entity.BudgetEntity;
import edu.cit.myalkansya.security.JwtUtil;
import edu.cit.myalkansya.service.BudgetService;
import edu.cit.myalkansya.service.UserService;

@RestController
@RequestMapping("/api/budgets")
@CrossOrigin(origins = "http://localhost:5173")
public class BudgetController {

    @Autowired
    private BudgetService budgetService;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/create")
    public ResponseEntity<?> createBudget(@RequestBody BudgetEntity budget,
                                         @RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            int userId = userService.findByEmail(email).get().getUserId();

            // If month is not set, default to current month
            if (budget.getBudgetMonth() == 0) {
                LocalDate now = LocalDate.now();
                budget.setBudgetMonth(now.getMonthValue());
                budget.setBudgetYear(now.getYear());
            } else if (budget.getBudgetYear() == 0) {
                // If year is not set but month is, use current year
                budget.setBudgetYear(LocalDate.now().getYear());
            }

            BudgetEntity createdBudget = budgetService.createBudget(budget, userId);
            return ResponseEntity.ok(createdBudget);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/user")
    public ResponseEntity<?> getUserBudgets(@RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            int userId = userService.findByEmail(email).get().getUserId();
            
            List<BudgetEntity> budgets = budgetService.getBudgetsByUserId(userId);
            return ResponseEntity.ok(budgets);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBudgetById(@PathVariable int id,
                                         @RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            int userId = userService.findByEmail(email).get().getUserId();
            
            BudgetEntity budget = budgetService.getBudgetById(id);
            
            // Check if the budget belongs to the user
            if (budget.getUser().getUserId() != userId) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You don't have permission to view this budget");
            }
            
            return ResponseEntity.ok(budget);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateBudget(@PathVariable int id, 
                                         @RequestBody BudgetEntity budget,
                                         @RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            int userId = userService.findByEmail(email).get().getUserId();
            
            // Ensure the budget ID is set for update
            budget.setId(id);
            
            BudgetEntity updatedBudget = budgetService.updateBudget(budget, userId);
            return ResponseEntity.ok(updatedBudget);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteBudget(@PathVariable int id,
                                         @RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            int userId = userService.findByEmail(email).get().getUserId();
            
            budgetService.deleteBudget(id, userId);
            return ResponseEntity.ok("Budget deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
    
    // New endpoints for monthly filtering
    @GetMapping("/getBudgetsByMonth/{month}/{year}")
    public ResponseEntity<?> getBudgetsByMonth(
            @PathVariable int month, 
            @PathVariable int year,
            @RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            int userId = userService.findByEmail(email).get().getUserId();
            
            List<BudgetEntity> budgets = budgetService.getBudgetsByMonth(userId, month, year);
            return ResponseEntity.ok(budgets);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
    
    @GetMapping("/getBudgetsByMonth/{month}")
    public ResponseEntity<?> getBudgetsByMonthOnly(
            @PathVariable int month,
            @RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            int userId = userService.findByEmail(email).get().getUserId();
            
            List<BudgetEntity> budgets = budgetService.getBudgetsByMonth(userId, month);
            return ResponseEntity.ok(budgets);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
    
    @GetMapping("/getCurrentMonthBudgets")
    public ResponseEntity<?> getCurrentMonthBudgets(@RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            int userId = userService.findByEmail(email).get().getUserId();
            
            LocalDate now = LocalDate.now();
            List<BudgetEntity> budgets = budgetService.getBudgetsByMonth(userId, now.getMonthValue(), now.getYear());
            return ResponseEntity.ok(budgets);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}