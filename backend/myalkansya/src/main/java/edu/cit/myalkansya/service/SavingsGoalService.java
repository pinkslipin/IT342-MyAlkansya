package edu.cit.myalkansya.service;

import edu.cit.myalkansya.entity.SavingsGoalEntity;
import edu.cit.myalkansya.entity.UserEntity;
import edu.cit.myalkansya.repository.SavingsGoalRepository;
import edu.cit.myalkansya.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class SavingsGoalService {

    @Autowired
    private SavingsGoalRepository savingsGoalRepository;
    
    @Autowired
    private UserRepository userRepository;

    public List<SavingsGoalEntity> getAllSavingsGoals() {
        return savingsGoalRepository.findAll();
    }
    
    public List<SavingsGoalEntity> getSavingsGoalsByUserId(int userId) {
        return savingsGoalRepository.findByUserUserId(userId);
    }

    public Optional<SavingsGoalEntity> getSavingsGoalById(int id) {
        return savingsGoalRepository.findById(id);
    }
    
    // Verify savings goal belongs to user
    public boolean savingsGoalExistsAndBelongsToUser(int goalId, int userId) {
        SavingsGoalEntity goal = savingsGoalRepository.findById(goalId).orElse(null);
        return goal != null && goal.getUser().getUserId() == userId;
    }

    @Transactional
    public SavingsGoalEntity createSavingsGoal(SavingsGoalEntity savingsGoal, int userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User with ID " + userId + " not found."));
        savingsGoal.setUser(user);
        return savingsGoalRepository.save(savingsGoal);
    }

    @Transactional
    public SavingsGoalEntity updateSavingsGoal(int id, SavingsGoalEntity updatedSavingsGoal, int userId) {
        if (!savingsGoalExistsAndBelongsToUser(id, userId)) {
            throw new NoSuchElementException("Savings goal with ID " + id + " not found for user with ID " + userId);
        }
        
        return savingsGoalRepository.findById(id).map(existingGoal -> {
            existingGoal.setGoal(updatedSavingsGoal.getGoal());
            existingGoal.setTargetAmount(updatedSavingsGoal.getTargetAmount());
            existingGoal.setCurrentAmount(updatedSavingsGoal.getCurrentAmount());
            existingGoal.setTargetDate(updatedSavingsGoal.getTargetDate());
            existingGoal.setCurrency(updatedSavingsGoal.getCurrency());
            return savingsGoalRepository.save(existingGoal);
        }).orElseThrow(() -> new RuntimeException("Savings goal not found with id " + id));
    }

    @Transactional
    public String deleteSavingsGoal(int id, int userId) {
        if (!savingsGoalExistsAndBelongsToUser(id, userId)) {
            return "Savings goal with ID " + id + " not found for user with ID " + userId;
        }
        
        savingsGoalRepository.deleteById(id);
        return "Savings goal with ID " + id + " has been deleted.";
    }
}