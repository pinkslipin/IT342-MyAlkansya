package edu.cit.myalkansya.service;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.cit.myalkansya.entity.BudgetEntity;
import edu.cit.myalkansya.repository.BudgetRepository;

@Service
public class BudgetService {

    @Autowired
    private BudgetRepository budgetRepository;

    // CREATE
    public BudgetEntity createBudget(BudgetEntity budget) {
        return budgetRepository.save(budget);
    }

    // READ
    public List<BudgetEntity> getAllBudgets() {
        return budgetRepository.findAll();
    }

    public BudgetEntity getBudgetById(int budgetId) {
        return budgetRepository.findById(budgetId)
                .orElseThrow(() -> new NoSuchElementException("Budget with ID " + budgetId + " not found."));
    }

    // UPDATE
    public BudgetEntity updateBudget(int budgetId, BudgetEntity newBudgetDetails) {
        BudgetEntity existingBudget = getBudgetById(budgetId);
        existingBudget.setCategory(newBudgetDetails.getCategory());
        existingBudget.setMonthlyBudget(newBudgetDetails.getMonthlyBudget());
        existingBudget.setTotalSpent(newBudgetDetails.getTotalSpent());
        existingBudget.setCurrency(newBudgetDetails.getCurrency());
        return budgetRepository.save(existingBudget);
    }

    // DELETE
    public String deleteBudget(int budgetId) {
        if (budgetRepository.existsById(budgetId)) {
            budgetRepository.deleteById(budgetId);
            return "Budget with ID " + budgetId + " successfully deleted.";
        } else {
            return "Budget with ID " + budgetId + " not found.";
        }
    }
}