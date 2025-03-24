package edu.cit.myalkansya.controller;

import edu.cit.myalkansya.entity.SavingsGoalEntity;
import edu.cit.myalkansya.service.SavingsGoalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/savings-goals")
public class SavingsGoalController {

    @Autowired
    private SavingsGoalService savingsGoalService;

    // CREATE
    @PostMapping("/postSavingsGoal")
    public SavingsGoalEntity postSavingsGoal(@RequestBody SavingsGoalEntity savingsGoal) {
        return savingsGoalService.createSavingsGoal(savingsGoal);
    }

    // READ
    @GetMapping("/getSavingsGoals")
    public List<SavingsGoalEntity> getSavingsGoals() {
        return savingsGoalService.getAllSavingsGoals();
    }

    @GetMapping("/getSavingsGoal/{goalId}")
    public Optional<SavingsGoalEntity> getSavingsGoalById(@PathVariable int goalId) {
        return savingsGoalService.getSavingsGoalById(goalId);
    }

    // UPDATE
    @PutMapping("/putSavingsGoal/{goalId}")
    public SavingsGoalEntity putSavingsGoal(@PathVariable int goalId, @RequestBody SavingsGoalEntity updatedSavingsGoal) {
        return savingsGoalService.updateSavingsGoal(goalId, updatedSavingsGoal);
    }

    // DELETE
    @DeleteMapping("/deleteSavingsGoal/{goalId}")
    public String deleteSavingsGoal(@PathVariable int goalId) {
        savingsGoalService.deleteSavingsGoal(goalId);
        return "Savings goal with ID " + goalId + " has been deleted.";
    }
}
