package edu.cit.myalkansya.service;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.cit.myalkansya.entity.BudgetEntity;
import edu.cit.myalkansya.entity.UserEntity;
import edu.cit.myalkansya.repository.BudgetRepository;
import edu.cit.myalkansya.repository.UserRepository;

@Service
public class BudgetService {

    @Autowired
    private BudgetRepository budgetRepository;
    
    @Autowired
    private UserRepository userRepository;

    // CREATE
    public BudgetEntity createBudget(BudgetEntity budget, int userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User with ID " + userId + " not found."));
        budget.setUser(user);
        return budgetRepository.save(budget);
    }

    // READ
    public List<BudgetEntity> getAllBudgets() {
        return budgetRepository.findAll();
    }
    
    public List<BudgetEntity> getBudgetsByUserId(int userId) {
        return budgetRepository.findByUserUserId(userId);
    }

    public BudgetEntity getBudgetById(int budgetId) {
        return budgetRepository.findById(budgetId)
                .orElseThrow(() -> new NoSuchElementException("Budget with ID " + budgetId + " not found."));
    }
    
    // Verify budget belongs to user
    public boolean budgetExistsAndBelongsToUser(int budgetId, int userId) {
        BudgetEntity budget = budgetRepository.findById(budgetId).orElse(null);
        return budget != null && budget.getUser().getUserId() == userId;
    }

    // UPDATE
    public BudgetEntity updateBudget(int budgetId, BudgetEntity newBudgetDetails, int userId) {
        if (!budgetExistsAndBelongsToUser(budgetId, userId)) {
            throw new NoSuchElementException("Budget with ID " + budgetId + " not found for user with ID " + userId);
        }
        
        BudgetEntity existingBudget = budgetRepository.findById(budgetId).get();
        existingBudget.setCategory(newBudgetDetails.getCategory());
        existingBudget.setMonthlyBudget(newBudgetDetails.getMonthlyBudget());
        existingBudget.setTotalSpent(newBudgetDetails.getTotalSpent());
        existingBudget.setCurrency(newBudgetDetails.getCurrency());
        return budgetRepository.save(existingBudget);
    }

    // DELETE
    public String deleteBudget(int budgetId, int userId) {
        if (!budgetExistsAndBelongsToUser(budgetId, userId)) {
            return "Budget with ID " + budgetId + " not found for user with ID " + userId;
        }
        
        budgetRepository.deleteById(budgetId);
        return "Budget with ID " + budgetId + " successfully deleted.";
    }
}