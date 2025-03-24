package edu.cit.myalkansya.service;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.cit.myalkansya.entity.ExpenseEntity;
import edu.cit.myalkansya.repository.ExpenseRepository;

@Service
public class ExpenseService {

    @Autowired
    private ExpenseRepository expenseRepository;

    // CREATE
    public ExpenseEntity createExpense(ExpenseEntity expense) {
        return expenseRepository.save(expense);
    }

    // READ
    public List<ExpenseEntity> getAllExpenses() {
        return expenseRepository.findAll();
    }

    public ExpenseEntity getExpenseById(int expenseId) {
        return expenseRepository.findById(expenseId)
                .orElseThrow(() -> new NoSuchElementException("Expense with ID " + expenseId + " not found."));
    }

    // UPDATE
    public ExpenseEntity updateExpense(int expenseId, ExpenseEntity newExpenseDetails) {
        ExpenseEntity existingExpense = getExpenseById(expenseId);
        existingExpense.setSubject(newExpenseDetails.getSubject());
        existingExpense.setCategory(newExpenseDetails.getCategory());
        existingExpense.setDate(newExpenseDetails.getDate());
        existingExpense.setAmount(newExpenseDetails.getAmount());
        existingExpense.setCurrency(newExpenseDetails.getCurrency()); // Update currency
        return expenseRepository.save(existingExpense);
    }

    // DELETE
    public String deleteExpense(int expenseId) {
        if (expenseRepository.existsById(expenseId)) {
            expenseRepository.deleteById(expenseId);
            return "Expense with ID " + expenseId + " successfully deleted.";
        } else {
            return "Expense with ID " + expenseId + " not found.";
        }
    }
}