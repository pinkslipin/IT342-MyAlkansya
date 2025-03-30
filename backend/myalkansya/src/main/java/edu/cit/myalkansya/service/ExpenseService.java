package edu.cit.myalkansya.service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.cit.myalkansya.entity.BudgetEntity;
import edu.cit.myalkansya.entity.ExpenseEntity;
import edu.cit.myalkansya.entity.UserEntity;
import edu.cit.myalkansya.repository.BudgetRepository;
import edu.cit.myalkansya.repository.ExpenseRepository;
import edu.cit.myalkansya.repository.UserRepository;

@Service
public class ExpenseService {

    @Autowired
    private ExpenseRepository expenseRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private BudgetRepository budgetRepository;

    // CREATE
    @Transactional
    public ExpenseEntity createExpense(ExpenseEntity expense, int userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User with ID " + userId + " not found."));
        expense.setUser(user);
        
        // Find if there's an existing budget for this category
        Optional<BudgetEntity> budgetOpt = budgetRepository.findFirstByUserUserIdAndCategory(userId, expense.getCategory());
        if (budgetOpt.isPresent()) {
            BudgetEntity budget = budgetOpt.get();
            budget.addExpense(expense);
            budgetRepository.save(budget);
        } else {
            // Optionally, create a new budget for this category
            BudgetEntity newBudget = new BudgetEntity();
            newBudget.setUser(user);
            newBudget.setCategory(expense.getCategory());
            newBudget.setMonthlyBudget(expense.getAmount() * 2); // Default budget as twice the first expense
            newBudget.setTotalSpent(expense.getAmount());
            newBudget.setCurrency(expense.getCurrency());
            budgetRepository.save(newBudget);
            
            // Set the relationship
            expense.setBudget(newBudget);
        }
        
        // Save the expense
        ExpenseEntity savedExpense = expenseRepository.save(expense);
        
        // Update user's total savings by subtracting the expense amount
        user.setTotalSavings(user.getTotalSavings() - expense.getAmount());
        userRepository.save(user);
        
        return savedExpense;
    }

    // READ methods remain unchanged
    public List<ExpenseEntity> getAllExpenses() {
        return expenseRepository.findAll();
    }
    
    public List<ExpenseEntity> getExpensesByUserId(int userId) {
        return expenseRepository.findByUserUserId(userId);
    }

    public ExpenseEntity getExpenseById(int expenseId) {
        return expenseRepository.findById(expenseId)
                .orElseThrow(() -> new NoSuchElementException("Expense with ID " + expenseId + " not found."));
    }
    
    // Verify expense belongs to user
    public boolean expenseExistsAndBelongsToUser(int expenseId, int userId) {
        ExpenseEntity expense = expenseRepository.findById(expenseId).orElse(null);
        return expense != null && expense.getUser().getUserId() == userId;
    }

    // UPDATE
    @Transactional
    public ExpenseEntity updateExpense(int expenseId, ExpenseEntity newExpenseDetails, int userId) {
        if (!expenseExistsAndBelongsToUser(expenseId, userId)) {
            throw new NoSuchElementException("Expense with ID " + expenseId + " not found for user with ID " + userId);
        }
        
        ExpenseEntity existingExpense = expenseRepository.findById(expenseId).get();
        UserEntity user = existingExpense.getUser();
        
        // Store old amount and category for budget updates
        double oldAmount = existingExpense.getAmount();
        String oldCategory = existingExpense.getCategory();
        
        // Calculate the difference between old and new amounts for user total savings
        double amountDifference = newExpenseDetails.getAmount() - oldAmount;
        
        // Check if the category changed
        boolean categoryChanged = !oldCategory.equals(newExpenseDetails.getCategory());
        
        // Handle budget updates
        if (categoryChanged) {
            // Category changed, handle both old and new budgets
            
            // Handle old budget (remove expense from old budget)
            if (existingExpense.getBudget() != null) {
                BudgetEntity oldBudget = existingExpense.getBudget();
                oldBudget.removeExpense(existingExpense);
                budgetRepository.save(oldBudget);
            }
            
            // Handle new budget (add expense to new budget)
            Optional<BudgetEntity> newBudgetOpt = budgetRepository.findFirstByUserUserIdAndCategory(userId, newExpenseDetails.getCategory());
            if (newBudgetOpt.isPresent()) {
                BudgetEntity newBudget = newBudgetOpt.get();
                existingExpense.setBudget(newBudget);
                // The amount will be added when we save the expense
            } else {
                // Create a new budget for this category
                BudgetEntity newBudget = new BudgetEntity();
                newBudget.setUser(user);
                newBudget.setCategory(newExpenseDetails.getCategory());
                newBudget.setMonthlyBudget(newExpenseDetails.getAmount() * 2); // Default budget as twice the expense
                newBudget.setTotalSpent(newExpenseDetails.getAmount());
                newBudget.setCurrency(newExpenseDetails.getCurrency());
                budgetRepository.save(newBudget);
                
                existingExpense.setBudget(newBudget);
            }
        } else if (existingExpense.getBudget() != null) {
            // Category didn't change, just update the amount in the existing budget
            BudgetEntity budget = existingExpense.getBudget();
            budget.updateExpense(existingExpense, oldAmount);
            budgetRepository.save(budget);
        }
        
        // Update expense details
        existingExpense.setSubject(newExpenseDetails.getSubject());
        existingExpense.setCategory(newExpenseDetails.getCategory());
        existingExpense.setDate(newExpenseDetails.getDate());
        existingExpense.setAmount(newExpenseDetails.getAmount());
        existingExpense.setCurrency(newExpenseDetails.getCurrency());
        
        // Save the updated expense
        ExpenseEntity updatedExpense = expenseRepository.save(existingExpense);
        
        // Update user's total savings based on the amount difference
        user.setTotalSavings(user.getTotalSavings() - amountDifference);
        userRepository.save(user);
        
        return updatedExpense;
    }

    // DELETE
    @Transactional
    public String deleteExpense(int expenseId, int userId) {
        if (!expenseExistsAndBelongsToUser(expenseId, userId)) {
            return "Expense with ID " + expenseId + " not found for user with ID " + userId;
        }
        
        // Get the expense and user before deletion
        ExpenseEntity expense = expenseRepository.findById(expenseId).get();
        UserEntity user = expense.getUser();
        double amountToAdd = expense.getAmount();
        
        // Update budget if associated
        if (expense.getBudget() != null) {
            BudgetEntity budget = expense.getBudget();
            budget.removeExpense(expense);
            budgetRepository.save(budget);
        }
        
        // Delete the expense
        expenseRepository.deleteById(expenseId);
        
        // Update user's total savings by adding back the deleted expense amount
        user.setTotalSavings(user.getTotalSavings() + amountToAdd);
        userRepository.save(user);
        
        return "Expense with ID " + expenseId + " successfully deleted.";
    }
}