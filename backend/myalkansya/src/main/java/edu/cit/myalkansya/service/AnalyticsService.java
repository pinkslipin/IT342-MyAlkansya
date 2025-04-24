package edu.cit.myalkansya.service;

import edu.cit.myalkansya.dto.CategorySummaryDTO;
import edu.cit.myalkansya.dto.MonthlySummaryDTO;
import edu.cit.myalkansya.entity.ExpenseEntity;
import edu.cit.myalkansya.entity.IncomeEntity;
import edu.cit.myalkansya.entity.BudgetEntity;
import edu.cit.myalkansya.entity.SavingsGoalEntity;
import edu.cit.myalkansya.entity.UserEntity;
import edu.cit.myalkansya.repository.ExpenseRepository;
import edu.cit.myalkansya.repository.IncomeRepository;
import edu.cit.myalkansya.repository.BudgetRepository;
import edu.cit.myalkansya.repository.SavingsGoalRepository;
import edu.cit.myalkansya.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class AnalyticsService {

    @Autowired
    private ExpenseRepository expenseRepository;
    
    @Autowired
    private IncomeRepository incomeRepository;
    
    @Autowired
    private BudgetRepository budgetRepository;
    
    @Autowired
    private SavingsGoalRepository savingsGoalRepository;
    
    @Autowired
    private UserRepository userRepository;

    public List<MonthlySummaryDTO> getMonthlySummary(int userId, int year) {
        Map<Integer, MonthlySummaryDTO> monthlySummaryMap = new HashMap<>();
        
        // Initialize all months
        for (int i = 1; i <= 12; i++) {
            MonthlySummaryDTO dto = new MonthlySummaryDTO();
            dto.setMonth(Month.of(i).getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
            dto.setIncome(0);
            dto.setExpenses(0);
            monthlySummaryMap.put(i, dto);
        }
        
        // Get expenses for the year
        List<ExpenseEntity> expenses = expenseRepository.findByUser_UserIdAndDateBetween(
            userId, 
            LocalDate.of(year, 1, 1), 
            LocalDate.of(year, 12, 31)
        );
        
        for (ExpenseEntity expense : expenses) {
            LocalDate date = expense.getDate();
            int month = date.getMonthValue();
            
            MonthlySummaryDTO dto = monthlySummaryMap.get(month);
            dto.setExpenses(dto.getExpenses() + expense.getAmount());
        }
        
        // Get incomes for the year
        List<IncomeEntity> incomes = incomeRepository.findByUser_UserIdAndDateBetween(
            userId,
            LocalDate.of(year, 1, 1), 
            LocalDate.of(year, 12, 31)
        );
        
        for (IncomeEntity income : incomes) {
            LocalDate date = income.getDate();
            int month = date.getMonthValue();
            
            MonthlySummaryDTO dto = monthlySummaryMap.get(month);
            dto.setIncome(dto.getIncome() + income.getAmount());
        }
        
        return new ArrayList<>(monthlySummaryMap.values());
    }
    
    public List<CategorySummaryDTO> getExpenseByCategory(int userId, Integer month, Integer year) {
        List<ExpenseEntity> expenses;
        
        if (month != null && year != null) {
            LocalDate startDate = LocalDate.of(year, month, 1);
            LocalDate endDate = startDate.plusMonths(1).minusDays(1);
            expenses = expenseRepository.findByUser_UserIdAndDateBetween(userId, startDate, endDate);
        } else if (year != null) {
            LocalDate startDate = LocalDate.of(year, 1, 1);
            LocalDate endDate = LocalDate.of(year, 12, 31);
            expenses = expenseRepository.findByUser_UserIdAndDateBetween(userId, startDate, endDate);
        } else {
            expenses = expenseRepository.findByUser_UserId(userId);
        }
        
        Map<String, Double> categoryMap = new HashMap<>();
        
        for (ExpenseEntity expense : expenses) {
            String category = expense.getCategory();
            Double currentAmount = categoryMap.getOrDefault(category, 0.0);
            categoryMap.put(category, currentAmount + expense.getAmount());
        }
        
        List<CategorySummaryDTO> result = new ArrayList<>();
        for (Map.Entry<String, Double> entry : categoryMap.entrySet()) {
            CategorySummaryDTO dto = new CategorySummaryDTO();
            dto.setCategory(entry.getKey());
            dto.setAmount(entry.getValue());
            result.add(dto);
        }
        
        return result;
    }
    
    /**
     * Get financial summary for a user for a specific month and year
     */
    public Map<String, Object> getFinancialSummary(int userId, Integer month, Integer year) {
        Map<String, Object> summary = new HashMap<>();
        
        // Get start and end dates for the specified month and year
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);
        
        // Get total income for the month
        List<IncomeEntity> incomes = incomeRepository.findByUser_UserIdAndDateBetween(
            userId, startDate, endDate);
        double totalIncome = incomes.stream()
            .mapToDouble(IncomeEntity::getAmount)
            .sum();
        
        // Get total expenses for the month
        List<ExpenseEntity> expenses = expenseRepository.findByUser_UserIdAndDateBetween(
            userId, startDate, endDate);
        double totalExpenses = expenses.stream()
            .mapToDouble(ExpenseEntity::getAmount)
            .sum();
        
        // Get total budget for the month
        List<BudgetEntity> budgets = budgetRepository.findByUserUserIdAndBudgetMonthAndBudgetYear(
            userId, month, year);
        double totalBudget = budgets.stream()
            .mapToDouble(BudgetEntity::getMonthlyBudget)
            .sum();
        
        // Get user's total savings
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        double totalSavings = userOpt.isPresent() ? userOpt.get().getTotalSavings() : 0.0;
        
        // Calculate net cashflow (income - expenses)
        double netCashflow = totalIncome - totalExpenses;
        
        // Calculate budget utilization (expenses / budget)
        double budgetUtilization = totalBudget > 0 ? totalExpenses / totalBudget : 0.0;
        
        // Calculate savings rate (net cashflow / income)
        double savingsRate = totalIncome > 0 ? netCashflow / totalIncome : 0.0;
        
        // Get user's currency
        String currency = userOpt.isPresent() ? userOpt.get().getCurrency() : "USD";
        
        // Populate the summary map
        summary.put("totalIncome", totalIncome);
        summary.put("totalExpenses", totalExpenses);
        summary.put("totalBudget", totalBudget);
        summary.put("totalSavings", totalSavings);
        summary.put("netCashflow", netCashflow);
        summary.put("budgetUtilization", budgetUtilization);
        summary.put("savingsRate", savingsRate);
        summary.put("currency", currency);
        
        return summary;
    }
    
    /**
     * Get progress information for all of a user's savings goals
     */
    public List<Map<String, Object>> getSavingsGoalsProgress(int userId) {
        List<SavingsGoalEntity> savingsGoals = savingsGoalRepository.findByUserUserId(userId);
        List<Map<String, Object>> progressList = new ArrayList<>();
        
        LocalDate today = LocalDate.now();
        
        for (SavingsGoalEntity goal : savingsGoals) {
            Map<String, Object> progressMap = new HashMap<>();
            
            // Basic goal info
            progressMap.put("goal", goal.getGoal());  // Using getGoal() instead of getName()
            progressMap.put("currentAmount", goal.getCurrentAmount());
            progressMap.put("targetAmount", goal.getTargetAmount());
            
            // Calculate progress percentage
            double progress = goal.getTargetAmount() > 0 
                ? (goal.getCurrentAmount() / goal.getTargetAmount()) * 100 
                : 0.0;
            progressMap.put("progress", progress);
            progressMap.put("savings", goal.getCurrentAmount());
            progressMap.put("percentage", progress);
            
            // Target date and days remaining
            LocalDate targetDate = goal.getTargetDate();
            progressMap.put("targetDate", targetDate.toString());
            
            long daysRemaining = 0;
            if (!targetDate.isBefore(today)) {
                daysRemaining = ChronoUnit.DAYS.between(today, targetDate);
            }
            progressMap.put("daysRemaining", daysRemaining);
            
            progressList.add(progressMap);
        }
        
        return progressList;
    }
}