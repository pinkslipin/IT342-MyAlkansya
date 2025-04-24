package edu.cit.myalkansya.controller;

import edu.cit.myalkansya.dto.CategorySummaryDTO;
import edu.cit.myalkansya.dto.MonthlySummaryDTO;
import edu.cit.myalkansya.service.AnalyticsService;
import edu.cit.myalkansya.service.UserService;
import edu.cit.myalkansya.security.JwtUtil; // Updated import to use your existing JwtUtil
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "http://localhost:5173")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/monthly-summary")
    public ResponseEntity<List<MonthlySummaryDTO>> getMonthlySummary(
            @RequestParam(required = false) Integer year,
            @RequestHeader("Authorization") String token) {
        
        String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
        int userId = userService.findByEmail(email).get().getUserId();
        
        // If year is not provided, use current year
        if (year == null) {
            year = java.time.Year.now().getValue();
        }
        
        List<MonthlySummaryDTO> monthlySummary = analyticsService.getMonthlySummary(userId, year);
        return ResponseEntity.ok(monthlySummary);
    }
    
    @GetMapping("/expense-categories")
    public ResponseEntity<List<CategorySummaryDTO>> getExpenseCategories(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestHeader("Authorization") String token) {
        
        String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
        int userId = userService.findByEmail(email).get().getUserId();
        
        List<CategorySummaryDTO> categorySummary = analyticsService.getExpenseByCategory(userId, month, year);
        return ResponseEntity.ok(categorySummary);
    }

    @GetMapping("/financial-summary")
    public ResponseEntity<?> getFinancialSummary(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestHeader("Authorization") String token) {
        
        String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
        int userId = userService.findByEmail(email).get().getUserId();
        
        // If month/year are not provided, use current month/year
        if (month == null) {
            month = java.time.LocalDate.now().getMonthValue();
        }
        if (year == null) {
            year = java.time.Year.now().getValue();
        }
        
        Map<String, Object> financialSummary = analyticsService.getFinancialSummary(userId, month, year);
        return ResponseEntity.ok(financialSummary);
    }

    @GetMapping("/savings-goals-progress")
    public ResponseEntity<?> getSavingsGoalsProgress(
            @RequestHeader("Authorization") String token) {
        
        String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
        int userId = userService.findByEmail(email).get().getUserId();
        
        List<Map<String, Object>> savingsGoalsProgress = analyticsService.getSavingsGoalsProgress(userId);
        return ResponseEntity.ok(savingsGoalsProgress);
    }
}