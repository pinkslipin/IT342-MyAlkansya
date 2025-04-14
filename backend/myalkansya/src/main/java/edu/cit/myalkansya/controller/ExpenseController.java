package edu.cit.myalkansya.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import edu.cit.myalkansya.entity.ExpenseEntity;
import edu.cit.myalkansya.security.JwtUtil;
import edu.cit.myalkansya.service.ExpenseService;
import edu.cit.myalkansya.service.UserService;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    @Autowired
    private ExpenseService expenseService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtUtil jwtUtil;

    // CREATE
    @PostMapping("/postExpense")
    public ResponseEntity<?> postExpense(@RequestBody ExpenseEntity expense, @RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            int userId = userService.findByEmail(email).get().getUserId();
            
            ExpenseEntity savedExpense = expenseService.createExpense(expense, userId);
            return ResponseEntity.ok(savedExpense);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // READ
    @GetMapping("/getExpenses")
    public ResponseEntity<?> getExpenses(@RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            int userId = userService.findByEmail(email).get().getUserId();
            
            List<ExpenseEntity> expenses = expenseService.getExpensesByUserId(userId);
            return ResponseEntity.ok(expenses);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/getExpense/{expenseId}")
    public ResponseEntity<?> getExpenseById(@PathVariable int expenseId, @RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            int userId = userService.findByEmail(email).get().getUserId();
            
            if (!expenseService.expenseExistsAndBelongsToUser(expenseId, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Expense not found or does not belong to user");
            }
            
            ExpenseEntity expense = expenseService.getExpenseById(expenseId);
            return ResponseEntity.ok(expense);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/getExpensesByCategory/{category}")
    public ResponseEntity<?> getExpensesByCategory(
            @PathVariable String category,
            @RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            int userId = userService.findByEmail(email).get().getUserId();
            
            List<ExpenseEntity> expenses = expenseService.getExpensesByCategoryAndUserId(category, userId);
            return ResponseEntity.ok(expenses);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // UPDATE
    @PutMapping("/putExpense/{expenseId}")
    public ResponseEntity<?> putExpense(
            @PathVariable int expenseId, 
            @RequestBody ExpenseEntity newExpenseDetails,
            @RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            int userId = userService.findByEmail(email).get().getUserId();
            
            ExpenseEntity updatedExpense = expenseService.updateExpense(expenseId, newExpenseDetails, userId);
            return ResponseEntity.ok(updatedExpense);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // DELETE
    @DeleteMapping("/deleteExpense/{expenseId}")
    public ResponseEntity<?> deleteExpense(@PathVariable int expenseId, @RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            int userId = userService.findByEmail(email).get().getUserId();
            
            String result = expenseService.deleteExpense(expenseId, userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}