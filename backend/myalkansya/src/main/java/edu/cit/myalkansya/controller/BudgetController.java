package edu.cit.myalkansya.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import edu.cit.myalkansya.entity.BudgetEntity;
import edu.cit.myalkansya.service.BudgetService;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    @Autowired
    private BudgetService budgetService;

    // CREATE
    @PostMapping("/postBudget")
    public BudgetEntity postBudget(@RequestBody BudgetEntity budget) {
        return budgetService.createBudget(budget);
    }

    // READ
    @GetMapping("/getBudgets")
    public List<BudgetEntity> getBudgets() {
        return budgetService.getAllBudgets();
    }

    @GetMapping("/getBudget/{budgetId}")
    public BudgetEntity getBudgetById(@PathVariable int budgetId) {
        return budgetService.getBudgetById(budgetId);
    }

    // UPDATE
    @PutMapping("/putBudget/{budgetId}")
    public BudgetEntity putBudget(@PathVariable int budgetId, @RequestBody BudgetEntity newBudgetDetails) {
        return budgetService.updateBudget(budgetId, newBudgetDetails);
    }

    // DELETE
    @DeleteMapping("/deleteBudget/{budgetId}")
    public String deleteBudget(@PathVariable int budgetId) {
        return budgetService.deleteBudget(budgetId);
    }
}