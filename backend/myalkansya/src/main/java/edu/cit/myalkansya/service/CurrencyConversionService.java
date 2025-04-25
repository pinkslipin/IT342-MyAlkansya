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
import java.util.NoSuchElementException;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    
    /**
     * Round a double value to exactly 2 decimal places with proper banker's rounding
     * 
     * @param value The value to round
     * @return The rounded value with exactly 2 decimal places
     */
    private double roundToTwoDecimals(double value) {
        // Use BigDecimal for more precise rounding
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(2, RoundingMode.HALF_EVEN); // Banker's rounding
        return bd.doubleValue();
    }
    
    /**
     * Store original values and currencies to enable exact conversion back
     * 
     * @param userId User ID
     * @param originalCurrency The original currency code
     */
    @Transactional
    public void storeOriginalValues(int userId, String originalCurrency) {
        // Store original values for each entity type
        storeOriginalIncomeValues(userId, originalCurrency);
        storeOriginalExpenseValues(userId, originalCurrency);
        storeOriginalSavingsGoalValues(userId, originalCurrency);
        storeOriginalBudgetValues(userId, originalCurrency);
        storeOriginalUserValues(userId, originalCurrency);
    }
    
    /**
     * Convert user currency while preserving original values
     */
    @Transactional
    public void convertUserCurrency(int userId, String fromCurrency, String toCurrency) {
        logger.info("Converting all financial data for user " + userId + " from " + fromCurrency + " to " + toCurrency);
        
        // First, store original values if not already stored
        storeOriginalValues(userId, fromCurrency);
        
        // Get the exchange rate
        double exchangeRate = exchangeRateService.getExchangeRate(fromCurrency, toCurrency);
        logger.info("Exchange rate: 1 " + fromCurrency + " = " + exchangeRate + " " + toCurrency);
        
        // Convert income records
        convertIncomeRecords(userId, fromCurrency, toCurrency, exchangeRate);
        
        // Convert expense records
        convertExpenseRecords(userId, fromCurrency, toCurrency, exchangeRate);
        
        // Convert savings goals
        convertSavingsGoalRecords(userId, fromCurrency, toCurrency, exchangeRate);
        
        // Convert budgets
        convertBudgetRecords(userId, fromCurrency, toCurrency, exchangeRate);
        
        // Convert the user's total savings
        convertUserTotalSavings(userId, fromCurrency, toCurrency, exchangeRate);
        
        logger.info("Currency conversion completed for user " + userId);
    }
    
    // IMPLEMENTATION START: Income Records
    private void convertIncomeRecords(int userId, String fromCurrency, String toCurrency, double exchangeRate) {
        List<IncomeEntity> incomes = incomeRepository.findByUserUserId(userId);
        int convertedCount = 0;
        
        for (IncomeEntity income : incomes) {
            if (fromCurrency.equals(income.getCurrency())) {
                // If converting back to original currency, use the original amount
                if (income.getOriginalCurrency() != null && toCurrency.equals(income.getOriginalCurrency())) {
                    income.setAmount(income.getOriginalAmount());
                    income.setCurrency(toCurrency);
                } else {
                    // Otherwise, do regular conversion
                    double convertedAmount = income.getAmount() * exchangeRate;
                    income.setAmount(roundToTwoDecimals(convertedAmount));
                    income.setCurrency(toCurrency);
                }
                incomeRepository.save(income);
                convertedCount++;
            }
        }
        logger.info("Converted " + convertedCount + " income records");
    }
    
    private void storeOriginalIncomeValues(int userId, String currency) {
        List<IncomeEntity> incomes = incomeRepository.findByUserUserId(userId);
        for (IncomeEntity income : incomes) {
            // Only store original values if they haven't been stored yet
            if (income.getOriginalAmount() == null && income.getCurrency().equals(currency)) {
                income.setOriginalAmount(income.getAmount());
                income.setOriginalCurrency(currency);
                incomeRepository.save(income);
            }
        }
        logger.info("Stored original values for " + incomes.size() + " income records");
    }
    // IMPLEMENTATION END: Income Records
    
    // IMPLEMENTATION START: Expense Records
    private void convertExpenseRecords(int userId, String fromCurrency, String toCurrency, double exchangeRate) {
        List<ExpenseEntity> expenses = expenseRepository.findByUserUserId(userId);
        int convertedCount = 0;
        
        for (ExpenseEntity expense : expenses) {
            if (fromCurrency.equals(expense.getCurrency())) {
                // If converting back to original currency, use the original amount
                if (expense.getOriginalCurrency() != null && toCurrency.equals(expense.getOriginalCurrency())) {
                    expense.setAmount(expense.getOriginalAmount());
                    expense.setCurrency(toCurrency);
                } else {
                    // Otherwise, do regular conversion
                    double convertedAmount = expense.getAmount() * exchangeRate;
                    expense.setAmount(roundToTwoDecimals(convertedAmount));
                    expense.setCurrency(toCurrency);
                }
                expenseRepository.save(expense);
                convertedCount++;
            }
        }
        logger.info("Converted " + convertedCount + " expense records");
    }
    
    private void storeOriginalExpenseValues(int userId, String currency) {
        List<ExpenseEntity> expenses = expenseRepository.findByUserUserId(userId);
        for (ExpenseEntity expense : expenses) {
            // Only store original values if they haven't been stored yet
            if (expense.getOriginalAmount() == null && expense.getCurrency().equals(currency)) {
                expense.setOriginalAmount(expense.getAmount());
                expense.setOriginalCurrency(currency);
                expenseRepository.save(expense);
            }
        }
        logger.info("Stored original values for " + expenses.size() + " expense records");
    }
    // IMPLEMENTATION END: Expense Records
    
    // IMPLEMENTATION START: Savings Goal Records
    private void convertSavingsGoalRecords(int userId, String fromCurrency, String toCurrency, double exchangeRate) {
        List<SavingsGoalEntity> savingsGoals = savingsGoalRepository.findByUserUserId(userId);
        int convertedCount = 0;
        
        for (SavingsGoalEntity goal : savingsGoals) {
            if (fromCurrency.equals(goal.getCurrency())) {
                // If converting back to original currency, use the original amounts
                if (goal.getOriginalCurrency() != null && toCurrency.equals(goal.getOriginalCurrency())) {
                    goal.setTargetAmount(goal.getOriginalTargetAmount());
                    goal.setCurrentAmount(goal.getOriginalCurrentAmount());
                    goal.setCurrency(toCurrency);
                } else {
                    // Otherwise, do regular conversion
                    double convertedTargetAmount = goal.getTargetAmount() * exchangeRate;
                    double convertedCurrentAmount = goal.getCurrentAmount() * exchangeRate;
                    
                    goal.setTargetAmount(roundToTwoDecimals(convertedTargetAmount));
                    goal.setCurrentAmount(roundToTwoDecimals(convertedCurrentAmount));
                    goal.setCurrency(toCurrency);
                }
                savingsGoalRepository.save(goal);
                convertedCount++;
            }
        }
        logger.info("Converted " + convertedCount + " savings goals");
    }
    
    private void storeOriginalSavingsGoalValues(int userId, String currency) {
        List<SavingsGoalEntity> savingsGoals = savingsGoalRepository.findByUserUserId(userId);
        for (SavingsGoalEntity goal : savingsGoals) {
            // Only store original values if they haven't been stored yet
            if (goal.getOriginalTargetAmount() == null && goal.getCurrency().equals(currency)) {
                goal.setOriginalTargetAmount(goal.getTargetAmount());
                goal.setOriginalCurrentAmount(goal.getCurrentAmount());
                goal.setOriginalCurrency(currency);
                savingsGoalRepository.save(goal);
            }
        }
        logger.info("Stored original values for " + savingsGoals.size() + " savings goals");
    }
    // IMPLEMENTATION END: Savings Goal Records
    
    // IMPLEMENTATION START: Budget Records
    private void convertBudgetRecords(int userId, String fromCurrency, String toCurrency, double exchangeRate) {
        List<BudgetEntity> budgets = budgetRepository.findByUserUserId(userId);
        int convertedCount = 0;
        
        for (BudgetEntity budget : budgets) {
            if (fromCurrency.equals(budget.getCurrency())) {
                // If converting back to original currency, use the original amounts
                if (budget.getOriginalCurrency() != null && toCurrency.equals(budget.getOriginalCurrency())) {
                    budget.setMonthlyBudget(budget.getOriginalMonthlyBudget());
                    budget.setTotalSpent(budget.getOriginalTotalSpent());
                    budget.setCurrency(toCurrency);
                } else {
                    // Otherwise, do regular conversion
                    double convertedMonthlyBudget = budget.getMonthlyBudget() * exchangeRate;
                    double convertedTotalSpent = budget.getTotalSpent() * exchangeRate;
                    
                    budget.setMonthlyBudget(roundToTwoDecimals(convertedMonthlyBudget));
                    budget.setTotalSpent(roundToTwoDecimals(convertedTotalSpent));
                    budget.setCurrency(toCurrency);
                }
                budgetRepository.save(budget);
                convertedCount++;
            }
        }
        logger.info("Converted " + convertedCount + " budgets");
    }
    
    private void storeOriginalBudgetValues(int userId, String currency) {
        List<BudgetEntity> budgets = budgetRepository.findByUserUserId(userId);
        for (BudgetEntity budget : budgets) {
            // Only store original values if they haven't been stored yet
            if (budget.getOriginalMonthlyBudget() == null && budget.getCurrency().equals(currency)) {
                budget.setOriginalMonthlyBudget(budget.getMonthlyBudget());
                budget.setOriginalTotalSpent(budget.getTotalSpent());
                budget.setOriginalCurrency(currency);
                budgetRepository.save(budget);
            }
        }
        logger.info("Stored original values for " + budgets.size() + " budgets");
    }
    // IMPLEMENTATION END: Budget Records
    
    // IMPLEMENTATION START: User Savings
    private void convertUserTotalSavings(int userId, String fromCurrency, String toCurrency, double exchangeRate) {
        UserEntity user = userRepository.findById(userId).orElseThrow(() ->
            new RuntimeException("User not found"));

        // Sum all incomes in the new currency
        List<IncomeEntity> incomes = incomeRepository.findByUserUserId(userId);
        double totalIncome = 0.0;
        for (IncomeEntity income : incomes) {
            double amount = income.getAmount();
            if (!income.getCurrency().equals(toCurrency)) {
                double rate = exchangeRateService.getExchangeRate(income.getCurrency(), toCurrency);
                amount = roundToTwoDecimals(amount * rate);
            }
            totalIncome += amount;
        }

        // Sum all expenses in the new currency
        List<ExpenseEntity> expenses = expenseRepository.findByUserUserId(userId);
        double totalExpense = 0.0;
        for (ExpenseEntity expense : expenses) {
            double amount = expense.getAmount();
            if (!expense.getCurrency().equals(toCurrency)) {
                double rate = exchangeRateService.getExchangeRate(expense.getCurrency(), toCurrency);
                amount = roundToTwoDecimals(amount * rate);
            }
            totalExpense += amount;
        }

        double newTotal = roundToTwoDecimals(totalIncome - totalExpense);
        user.setTotalSavings(newTotal);
        user.setCurrency(toCurrency);

        userRepository.save(user);
        logger.info("Recalculated user's total savings in " + toCurrency + ": " + newTotal);
    }
    
    private void storeOriginalUserValues(int userId, String currency) {
        UserEntity user = userRepository.findById(userId).orElseThrow(() -> 
            new RuntimeException("User not found"));

        // Only store original value if it hasn't been stored yet
        if (user.getOriginalTotalSavings() == null && user.getCurrency().equals(currency)) {
            user.setOriginalTotalSavings(user.getTotalSavings());
            user.setOriginalCurrency(currency);
            userRepository.save(user);
            logger.info("Stored original savings value for user " + userId);
        }
    }
    // IMPLEMENTATION END: User Savings

    @Transactional
    public ExpenseEntity createExpense(ExpenseEntity expense, int userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        expense.setUser(user);
        ExpenseEntity savedExpense = expenseRepository.save(expense);

        // Update totalSavings in the user's current currency
        user.setTotalSavings(user.getTotalSavings() - expense.getAmount());

        // Recalculate originalTotalSavings from all transactions
        recalculateOriginalTotalSavings(user);

        userRepository.save(user);
        return savedExpense;
    }

    @Transactional
    public IncomeEntity createIncome(IncomeEntity income, int userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        income.setUser(user);
        IncomeEntity savedIncome = incomeRepository.save(income);

        // Update totalSavings in the user's current currency
        user.setTotalSavings(user.getTotalSavings() + income.getAmount());

        // Recalculate originalTotalSavings from all transactions
        recalculateOriginalTotalSavings(user);

        userRepository.save(user);
        return savedIncome;
    }

    // Helper method to recalculate originalTotalSavings
    private void recalculateOriginalTotalSavings(UserEntity user) {
        String originalCurrency = user.getOriginalCurrency();
        if (originalCurrency == null) {
            // If not set, use current currency as original
            originalCurrency = user.getCurrency();
            user.setOriginalCurrency(originalCurrency);
        }

        // Sum all incomes in original currency
        List<IncomeEntity> incomes = incomeRepository.findByUserUserId(user.getUserId());
        double totalIncome = 0.0;
        for (IncomeEntity income : incomes) {
            double amount = income.getAmount();
            if (!income.getCurrency().equals(originalCurrency)) {
                double rate = exchangeRateService.getExchangeRate(income.getCurrency(), originalCurrency);
                amount = roundToTwoDecimals(amount * rate);
            }
            totalIncome += amount;
        }

        // Sum all expenses in original currency
        List<ExpenseEntity> expenses = expenseRepository.findByUserUserId(user.getUserId());
        double totalExpense = 0.0;
        for (ExpenseEntity expense : expenses) {
            double amount = expense.getAmount();
            if (!expense.getCurrency().equals(originalCurrency)) {
                double rate = exchangeRateService.getExchangeRate(expense.getCurrency(), originalCurrency);
                amount = roundToTwoDecimals(amount * rate);
            }
            totalExpense += amount;
        }

        double originalTotalSavings = roundToTwoDecimals(totalIncome - totalExpense);
        user.setOriginalTotalSavings(originalTotalSavings);
    }
}