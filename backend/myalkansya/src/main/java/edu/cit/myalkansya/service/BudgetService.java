package edu.cit.myalkansya.service;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.cit.myalkansya.entity.BudgetEntity;
import edu.cit.myalkansya.entity.ExpenseEntity;
import edu.cit.myalkansya.entity.UserEntity;
import edu.cit.myalkansya.repository.BudgetRepository;
import edu.cit.myalkansya.repository.ExpenseRepository;
import edu.cit.myalkansya.repository.UserRepository;

@Service
public class BudgetService {

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private UserRepository userRepository;

    // CREATE
    @Transactional
    public BudgetEntity createBudget(BudgetEntity budget, int userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User with ID " + userId + " not found."));
        budget.setUser(user);
        
        // If month/year not set, default to current month/year
        if (budget.getBudgetMonth() == 0) {
            LocalDate now = LocalDate.now();
            budget.setBudgetMonth(now.getMonthValue());
            budget.setBudgetYear(now.getYear());
        } else if (budget.getBudgetYear() == 0) {
            // If year is not set but month is, use current year
            budget.setBudgetYear(LocalDate.now().getYear());
        }
        
        // Check if a budget for this category and month already exists
        Optional<BudgetEntity> existingBudget = budgetRepository.findByUserUserIdAndCategoryAndBudgetMonthAndBudgetYear(
            userId, budget.getCategory(), budget.getBudgetMonth(), budget.getBudgetYear());
        
        if (existingBudget.isPresent()) {
            throw new IllegalStateException("A budget for category '" + budget.getCategory() + 
                "' already exists for month " + budget.getBudgetMonth() + "/" + budget.getBudgetYear());
        }
        
        BudgetEntity savedBudget = budgetRepository.save(budget);
        
        // Find all expenses for this category and link them to this budget
        List<ExpenseEntity> expenses = expenseRepository.findByUserUserIdAndCategory(userId, budget.getCategory());
        double totalSpent = 0.0;
        
        for (ExpenseEntity expense : expenses) {
            // Only link expenses from the same month/year
            LocalDate expenseDate = expense.getDate();
            if (expenseDate.getMonthValue() == budget.getBudgetMonth() && 
                expenseDate.getYear() == budget.getBudgetYear() && 
                expense.getBudget() == null) {
                expense.setBudget(savedBudget);
                expenseRepository.save(expense);
                totalSpent += expense.getAmount();
            }
        }
        
        // Update the total spent in the budget
        savedBudget.setTotalSpent(totalSpent);
        return budgetRepository.save(savedBudget);
    }
    
    // READ
    public List<BudgetEntity> getAllBudgets() {
        return budgetRepository.findAll();
    }

    public BudgetEntity getBudgetById(int id) {
        return budgetRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Budget not found with ID: " + id));
    }

    public List<BudgetEntity> getBudgetsByUserId(int userId) {
        return budgetRepository.findByUserUserId(userId);
    }

    // New methods for filtering
    public List<BudgetEntity> getBudgetsByMonth(int userId, int month, int year) {
        return budgetRepository.findByUserUserIdAndBudgetMonthAndBudgetYear(userId, month, year);
    }
    
    public List<BudgetEntity> getBudgetsByMonth(int userId, int month) {
        return budgetRepository.findByUserUserIdAndBudgetMonth(userId, month);
    }
    
    // UPDATE
    @Transactional
    public BudgetEntity updateBudget(BudgetEntity budget, int userId) {
        // Get the existing budget to update
        BudgetEntity existingBudget = budgetRepository.findById(budget.getId())
            .orElseThrow(() -> new NoSuchElementException("Budget not found with ID: " + budget.getId()));
        
        // Check if this budget belongs to the user
        if (existingBudget.getUser().getUserId() != userId) {
            throw new AccessDeniedException("You don't have permission to update this budget");
        }
        
        // If the category, month, or year is changing, check for conflicts
        if (!existingBudget.getCategory().equals(budget.getCategory()) ||
            existingBudget.getBudgetMonth() != budget.getBudgetMonth() ||
            existingBudget.getBudgetYear() != budget.getBudgetYear()) {
                
            Optional<BudgetEntity> conflictingBudget = budgetRepository.findByUserUserIdAndCategoryAndBudgetMonthAndBudgetYear(
                userId, budget.getCategory(), budget.getBudgetMonth(), budget.getBudgetYear());
            
            if (conflictingBudget.isPresent() && conflictingBudget.get().getId() != budget.getId()) {
                throw new IllegalStateException("A budget for category '" + budget.getCategory() + 
                    "' already exists for month " + budget.getBudgetMonth() + "/" + budget.getBudgetYear());
            }
        }
        
        // Store current totalSpent before updating
        double currentTotalSpent = existingBudget.getTotalSpent();
        
        // Update fields (but maintain the totalSpent value)
        existingBudget.setCategory(budget.getCategory());
        existingBudget.setMonthlyBudget(budget.getMonthlyBudget());
        existingBudget.setCurrency(budget.getCurrency());
        
        // Only recalculate expenses if month/year or category changed
        boolean needsToRecalculateExpenses = existingBudget.getBudgetMonth() != budget.getBudgetMonth() ||
                                             existingBudget.getBudgetYear() != budget.getBudgetYear() ||
                                             !existingBudget.getCategory().equals(budget.getCategory());
        
        if (needsToRecalculateExpenses) {
            existingBudget.setBudgetMonth(budget.getBudgetMonth());
            existingBudget.setBudgetYear(budget.getBudgetYear());
            
            // Unlink all current expenses
            List<ExpenseEntity> oldExpenses = expenseRepository.findByBudgetId(existingBudget.getId());
            for (ExpenseEntity expense : oldExpenses) {
                expense.setBudget(null);
                expenseRepository.save(expense);
            }
            
            // Link expenses for the new month/year and category
            List<ExpenseEntity> newExpenses = expenseRepository.findByUserUserIdAndCategory(
                userId, existingBudget.getCategory());
            
            double totalSpent = 0.0;
            for (ExpenseEntity expense : newExpenses) {
                LocalDate expenseDate = expense.getDate();
                if (expenseDate.getMonthValue() == existingBudget.getBudgetMonth() && 
                    expenseDate.getYear() == existingBudget.getBudgetYear()) {
                    expense.setBudget(existingBudget);
                    expenseRepository.save(expense);
                    totalSpent += expense.getAmount();
                }
            }
            
            existingBudget.setTotalSpent(totalSpent);
        } else {
            // If we're not changing category/month/year, keep the existing totalSpent
            existingBudget.setTotalSpent(currentTotalSpent);
        }
        
        return budgetRepository.save(existingBudget);
    }
    
    // DELETE
    @Transactional
    public void deleteBudget(int id, int userId) {
        BudgetEntity budget = budgetRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Budget not found with ID: " + id));
        
        // Check if this budget belongs to the user
        if (budget.getUser().getUserId() != userId) {
            throw new AccessDeniedException("You don't have permission to delete this budget");
        }
        
        // Unlink all expenses from this budget
        List<ExpenseEntity> expenses = expenseRepository.findByBudgetId(id);
        for (ExpenseEntity expense : expenses) {
            expense.setBudget(null);
            expenseRepository.save(expense);
        }
        
        // Delete the budget
        budgetRepository.deleteById(id);
    }
}