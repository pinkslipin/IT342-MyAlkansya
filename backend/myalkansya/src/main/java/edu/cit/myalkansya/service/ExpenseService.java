package edu.cit.myalkansya.service;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.cit.myalkansya.entity.ExpenseEntity;
import edu.cit.myalkansya.entity.UserEntity;
import edu.cit.myalkansya.repository.ExpenseRepository;
import edu.cit.myalkansya.repository.UserRepository;

@Service
public class ExpenseService {

    @Autowired
    private ExpenseRepository expenseRepository;
    
    @Autowired
    private UserRepository userRepository;

    // CREATE
    public ExpenseEntity createExpense(ExpenseEntity expense, int userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User with ID " + userId + " not found."));
        expense.setUser(user);
        return expenseRepository.save(expense);
    }

    // READ
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
    public ExpenseEntity updateExpense(int expenseId, ExpenseEntity newExpenseDetails, int userId) {
        if (!expenseExistsAndBelongsToUser(expenseId, userId)) {
            throw new NoSuchElementException("Expense with ID " + expenseId + " not found for user with ID " + userId);
        }
        
        ExpenseEntity existingExpense = expenseRepository.findById(expenseId).get();
        existingExpense.setSubject(newExpenseDetails.getSubject());
        existingExpense.setCategory(newExpenseDetails.getCategory());
        existingExpense.setDate(newExpenseDetails.getDate());
        existingExpense.setAmount(newExpenseDetails.getAmount());
        existingExpense.setCurrency(newExpenseDetails.getCurrency());
        return expenseRepository.save(existingExpense);
    }

    // DELETE
    public String deleteExpense(int expenseId, int userId) {
        if (!expenseExistsAndBelongsToUser(expenseId, userId)) {
            return "Expense with ID " + expenseId + " not found for user with ID " + userId;
        }
        
        expenseRepository.deleteById(expenseId);
        return "Expense with ID " + expenseId + " successfully deleted.";
    }
}