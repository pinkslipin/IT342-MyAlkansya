package edu.cit.myalkansya.service;

import edu.cit.myalkansya.dto.CategorySummaryDTO;
import edu.cit.myalkansya.dto.MonthlySummaryDTO;
import edu.cit.myalkansya.entity.ExpenseEntity;
import edu.cit.myalkansya.entity.IncomeEntity;
import edu.cit.myalkansya.repository.ExpenseRepository;
import edu.cit.myalkansya.repository.IncomeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;

@Service
public class AnalyticsService {

    @Autowired
    private ExpenseRepository expenseRepository;
    
    @Autowired
    private IncomeRepository incomeRepository;

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
}