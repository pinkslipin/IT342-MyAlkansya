package edu.cit.myalkansya.service;

import edu.cit.myalkansya.entity.SavingsGoalEntity;
import edu.cit.myalkansya.repository.SavingsGoalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SavingsGoalService {

    @Autowired
    private SavingsGoalRepository savingsGoalRepository;

    public List<SavingsGoalEntity> getAllSavingsGoals() {
        return savingsGoalRepository.findAll();
    }

    public Optional<SavingsGoalEntity> getSavingsGoalById(int id) {
        return savingsGoalRepository.findById(id);
    }

    public SavingsGoalEntity createSavingsGoal(SavingsGoalEntity savingsGoal) {
        return savingsGoalRepository.save(savingsGoal);
    }

    public SavingsGoalEntity updateSavingsGoal(int id, SavingsGoalEntity updatedSavingsGoal) {
        return savingsGoalRepository.findById(id).map(existingGoal -> {
            existingGoal.setGoal(updatedSavingsGoal.getGoal());
            existingGoal.setTargetAmount(updatedSavingsGoal.getTargetAmount());
            existingGoal.setCurrentAmount(updatedSavingsGoal.getCurrentAmount());
            existingGoal.setTargetDate(updatedSavingsGoal.getTargetDate());
            existingGoal.setCurrency(updatedSavingsGoal.getCurrency());
            return savingsGoalRepository.save(existingGoal);
        }).orElseThrow(() -> new RuntimeException("Savings goal not found with id " + id));
    }

    public void deleteSavingsGoal(int id) {
        savingsGoalRepository.deleteById(id);
    }
}
