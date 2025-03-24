package edu.cit.myalkansya.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import edu.cit.myalkansya.entity.ExpenseEntity;
import edu.cit.myalkansya.service.ExpenseService;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    @Autowired
    private ExpenseService expenseService;

    // CREATE
    @PostMapping("/postExpense")
    public ExpenseEntity postExpense(@RequestBody ExpenseEntity expense) {
        return expenseService.createExpense(expense);
    }

    // READ
    @GetMapping("/getExpenses")
    public List<ExpenseEntity> getExpenses() {
        return expenseService.getAllExpenses();
    }

    @GetMapping("/getExpense/{expenseId}")
    public ExpenseEntity getExpenseById(@PathVariable int expenseId) {
        return expenseService.getExpenseById(expenseId);
    }

    // UPDATE
    @PutMapping("/putExpense/{expenseId}")
    public ExpenseEntity putExpense(@PathVariable int expenseId, @RequestBody ExpenseEntity newExpenseDetails) {
        return expenseService.updateExpense(expenseId, newExpenseDetails);
    }

    // DELETE
    @DeleteMapping("/deleteExpense/{expenseId}")
    public String deleteExpense(@PathVariable int expenseId) {
        return expenseService.deleteExpense(expenseId);
    }
}