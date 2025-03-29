package edu.cit.myalkansya.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import edu.cit.myalkansya.entity.IncomeEntity;
import edu.cit.myalkansya.security.JwtUtil;
import edu.cit.myalkansya.service.IncomeService;
import edu.cit.myalkansya.service.UserService;

@RestController
@RequestMapping("/api/incomes")
public class IncomeController {

    @Autowired
    private IncomeService incomeService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtUtil jwtUtil;

    // CREATE
    @PostMapping("/postIncome")
    public ResponseEntity<?> postIncome(@RequestBody IncomeEntity income, @RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            int userId = userService.findByEmail(email).get().getUserId();
            
            IncomeEntity savedIncome = incomeService.createIncome(income, userId);
            return ResponseEntity.ok(savedIncome);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // READ
    @GetMapping("/getIncomes")
    public ResponseEntity<?> getIncomes(@RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            int userId = userService.findByEmail(email).get().getUserId();
            
            List<IncomeEntity> incomes = incomeService.getIncomesByUserId(userId);
            return ResponseEntity.ok(incomes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/getIncome/{incomeId}")
    public ResponseEntity<?> getIncomeById(@PathVariable int incomeId, @RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            int userId = userService.findByEmail(email).get().getUserId();
            
            if (!incomeService.incomeExistsAndBelongsToUser(incomeId, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Income not found or does not belong to user");
            }
            
            IncomeEntity income = incomeService.getIncomeById(incomeId);
            return ResponseEntity.ok(income);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // UPDATE
    @PutMapping("/putIncome/{incomeId}")
    public ResponseEntity<?> putIncome(
            @PathVariable int incomeId, 
            @RequestBody IncomeEntity newIncomeDetails,
            @RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            int userId = userService.findByEmail(email).get().getUserId();
            
            IncomeEntity updatedIncome = incomeService.updateIncome(incomeId, newIncomeDetails, userId);
            return ResponseEntity.ok(updatedIncome);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // DELETE
    @DeleteMapping("/deleteIncome/{incomeId}")
    public ResponseEntity<?> deleteIncome(@PathVariable int incomeId, @RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            int userId = userService.findByEmail(email).get().getUserId();
            
            String result = incomeService.deleteIncome(incomeId, userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}