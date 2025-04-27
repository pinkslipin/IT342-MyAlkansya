package edu.cit.myalkansya.service;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.cit.myalkansya.entity.IncomeEntity;
import edu.cit.myalkansya.entity.UserEntity;
import edu.cit.myalkansya.repository.IncomeRepository;
import edu.cit.myalkansya.repository.UserRepository;

@Service
public class IncomeService {

    @Autowired
    private IncomeRepository incomeRepository;
    
    @Autowired
    private UserRepository userRepository;

    // CREATE
    @Transactional
    public IncomeEntity createIncome(IncomeEntity income, int userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User with ID " + userId + " not found."));
        income.setUser(user);
        
        // If original amount and currency are provided and different from main amount/currency,
        // store them in the respective fields
        if (income.getOriginalAmount() != null && income.getOriginalCurrency() != null) {
            // Original values were already set in controller from the request
        } else if (!user.getCurrency().equals(income.getCurrency())) {
            // No original values but currency differs from user's currency
            // Store the current values as original before they might be converted
            income.setOriginalAmount(income.getAmount());
            income.setOriginalCurrency(income.getCurrency());
            
            // No need to convert here as it should be done in frontend
            // Just ensuring we have original values stored
        }
        
        // Save the income first
        IncomeEntity savedIncome = incomeRepository.save(income);
        
        // Update user's total savings by adding the income amount
        user.setTotalSavings(user.getTotalSavings() + income.getAmount());
        userRepository.save(user);
        
        return savedIncome;
    }

    // READ methods remain unchanged
    public List<IncomeEntity> getAllIncomes() {
        return incomeRepository.findAll();
    }
    
    public List<IncomeEntity> getIncomesByUserId(int userId) {
        return incomeRepository.findByUserUserId(userId);
    }

    public IncomeEntity getIncomeById(int incomeId) {
        return incomeRepository.findById(incomeId)
                .orElseThrow(() -> new NoSuchElementException("Income with ID " + incomeId + " not found."));
    }
    
    // Verify income belongs to user
    public boolean incomeExistsAndBelongsToUser(int incomeId, int userId) {
        IncomeEntity income = incomeRepository.findById(incomeId).orElse(null);
        return income != null && income.getUser().getUserId() == userId;
    }

    // UPDATE
    @Transactional
    public IncomeEntity updateIncome(int incomeId, IncomeEntity newIncomeDetails, int userId) {
        if (!incomeExistsAndBelongsToUser(incomeId, userId)) {
            throw new NoSuchElementException("Income with ID " + incomeId + " not found for user with ID " + userId);
        }
        
        IncomeEntity existingIncome = incomeRepository.findById(incomeId).get();
        UserEntity user = existingIncome.getUser();
        
        // Calculate the difference between old and new amounts
        double amountDifference = newIncomeDetails.getAmount() - existingIncome.getAmount();
        
        // Update income details
        existingIncome.setSource(newIncomeDetails.getSource());
        existingIncome.setDate(newIncomeDetails.getDate());
        existingIncome.setAmount(newIncomeDetails.getAmount());
        existingIncome.setCurrency(newIncomeDetails.getCurrency());
        
        // Save the updated income
        IncomeEntity updatedIncome = incomeRepository.save(existingIncome);
        
        // Update user's total savings based on the amount difference
        user.setTotalSavings(user.getTotalSavings() + amountDifference);
        userRepository.save(user);
        
        return updatedIncome;
    }

    // DELETE
    @Transactional
    public String deleteIncome(int incomeId, int userId) {
        if (!incomeExistsAndBelongsToUser(incomeId, userId)) {
            return "Income with ID " + incomeId + " not found for user with ID " + userId;
        }
        
        // Get the income and user before deletion
        IncomeEntity income = incomeRepository.findById(incomeId).get();
        UserEntity user = income.getUser();
        double amountToSubtract = income.getAmount();
        
        // Delete the income
        incomeRepository.deleteById(incomeId);
        
        // Update user's total savings by subtracting the deleted income amount
        user.setTotalSavings(user.getTotalSavings() - amountToSubtract);
        userRepository.save(user);
        
        return "Income with ID " + incomeId + " successfully deleted.";
    }
}