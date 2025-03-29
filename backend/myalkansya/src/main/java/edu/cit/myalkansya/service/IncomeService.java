package edu.cit.myalkansya.service;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    public IncomeEntity createIncome(IncomeEntity income, int userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User with ID " + userId + " not found."));
        income.setUser(user);
        return incomeRepository.save(income);
    }

    // READ
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
    public IncomeEntity updateIncome(int incomeId, IncomeEntity newIncomeDetails, int userId) {
        if (!incomeExistsAndBelongsToUser(incomeId, userId)) {
            throw new NoSuchElementException("Income with ID " + incomeId + " not found for user with ID " + userId);
        }
        
        IncomeEntity existingIncome = incomeRepository.findById(incomeId).get();
        existingIncome.setSource(newIncomeDetails.getSource());
        existingIncome.setDate(newIncomeDetails.getDate());
        existingIncome.setAmount(newIncomeDetails.getAmount());
        existingIncome.setCurrency(newIncomeDetails.getCurrency());
        return incomeRepository.save(existingIncome);
    }

    // DELETE
    public String deleteIncome(int incomeId, int userId) {
        if (!incomeExistsAndBelongsToUser(incomeId, userId)) {
            return "Income with ID " + incomeId + " not found for user with ID " + userId;
        }
        
        incomeRepository.deleteById(incomeId);
        return "Income with ID " + incomeId + " successfully deleted.";
    }
}