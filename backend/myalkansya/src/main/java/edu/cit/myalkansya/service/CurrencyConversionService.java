package edu.cit.myalkansya.service;

import edu.cit.myalkansya.entity.BudgetEntity;
import edu.cit.myalkansya.entity.ExpenseEntity;
import edu.cit.myalkansya.entity.IncomeEntity;
import edu.cit.myalkansya.entity.SavingsGoalEntity;
import edu.cit.myalkansya.entity.UserEntity;
import edu.cit.myalkansya.repository.BudgetRepository;
import edu.cit.myalkansya.repository.ExpenseRepository;
import edu.cit.myalkansya.repository.IncomeRepository;
import edu.cit.myalkansya.repository.SavingsGoalRepository;
import edu.cit.myalkansya.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.logging.Logger;

@Service
public class CurrencyConversionService {
    
    private static final Logger logger = Logger.getLogger(CurrencyConversionService.class.getName());
    
    @Autowired
    private ExchangeRateService exchangeRateService;
    
    @Autowired
    private IncomeRepository incomeRepository;
    
    @Autowired
    private ExpenseRepository expenseRepository;
    
    @Autowired
    private SavingsGoalRepository savingsGoalRepository;
    
    @Autowired
    private BudgetRepository budgetRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Transactional
    public void convertUserCurrency(int userId, String fromCurrency, String toCurrency) {
        logger.info("Converting all financial data for user " + userId + " from " + fromCurrency + " to " + toCurrency);
        
        // Get the exchange rate
        double exchangeRate = exchangeRateService.getExchangeRate(fromCurrency, toCurrency);
        logger.info("Exchange rate: 1 " + fromCurrency + " = " + exchangeRate + " " + toCurrency);
        
        // Convert income records
        List<IncomeEntity> incomes = incomeRepository.findByUserUserId(userId);
        for (IncomeEntity income : incomes) {
            if (fromCurrency.equals(income.getCurrency())) {
                double convertedAmount = income.getAmount() * exchangeRate;
                income.setAmount(convertedAmount);
                income.setCurrency(toCurrency);
                incomeRepository.save(income);
            }
        }
        logger.info("Converted " + incomes.size() + " income records");
        
        // Convert expense records
        List<ExpenseEntity> expenses = expenseRepository.findByUserUserId(userId);
        for (ExpenseEntity expense : expenses) {
            if (fromCurrency.equals(expense.getCurrency())) {
                double convertedAmount = expense.getAmount() * exchangeRate;
                expense.setAmount(convertedAmount);
                expense.setCurrency(toCurrency);
                expenseRepository.save(expense);
            }
        }
        logger.info("Converted " + expenses.size() + " expense records");
        
        // Convert savings goals
        List<SavingsGoalEntity> savingsGoals = savingsGoalRepository.findByUserUserId(userId);
        for (SavingsGoalEntity goal : savingsGoals) {
            if (fromCurrency.equals(goal.getCurrency())) {
                double convertedTargetAmount = goal.getTargetAmount() * exchangeRate;
                double convertedCurrentAmount = goal.getCurrentAmount() * exchangeRate;
                
                goal.setTargetAmount(convertedTargetAmount);
                goal.setCurrentAmount(convertedCurrentAmount);
                goal.setCurrency(toCurrency);
                savingsGoalRepository.save(goal);
            }
        }
        logger.info("Converted " + savingsGoals.size() + " savings goals");
        
        // Convert budgets
        List<BudgetEntity> budgets = budgetRepository.findByUserUserId(userId);
        for (BudgetEntity budget : budgets) {
            if (fromCurrency.equals(budget.getCurrency())) {
                // Convert both monthlyBudget and totalSpent fields
                double convertedMonthlyBudget = budget.getMonthlyBudget() * exchangeRate;
                double convertedTotalSpent = budget.getTotalSpent() * exchangeRate;
                
                budget.setMonthlyBudget(convertedMonthlyBudget);
                budget.setTotalSpent(convertedTotalSpent);
                budget.setCurrency(toCurrency);
                budgetRepository.save(budget);
            }
        }
        logger.info("Converted " + budgets.size() + " budgets");
        
        // Convert the user's total savings - REMOVING THE INCORRECT CONDITION
        UserEntity user = userRepository.findById(userId).orElseThrow(() -> 
            new RuntimeException("User not found"));

        // Always convert the savings regardless of current currency
        double convertedSavings = user.getTotalSavings() * exchangeRate;
        user.setTotalSavings(convertedSavings);
        user.setCurrency(toCurrency); // Make sure the currency is updated
        userRepository.save(user);
        logger.info("Converted user's total savings from " + fromCurrency + " to " + toCurrency);
        
        logger.info("Currency conversion completed for user " + userId);
    }
}