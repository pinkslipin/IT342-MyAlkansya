package edu.cit.myalkansya.entity;

import java.time.Month;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "budgets")
public class BudgetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String category;
    private double monthlyBudget;
    private double totalSpent;
    private String currency;
    
    // Add these fields to track budget month/year
    private int budgetMonth; // 1-12 for January-December
    private int budgetYear;  // e.g., 2025
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"budgets", "expenses", "incomes"}) // Prevents infinite recursion in JSON response
    private UserEntity user;
    
    @OneToMany(mappedBy = "budget", cascade = CascadeType.ALL)
    @JsonIgnoreProperties("budget") // Prevents infinite recursion in JSON response
    private List<ExpenseEntity> expenses = new ArrayList<>();

    public BudgetEntity() {
        super();
    }

    public BudgetEntity(String category, double monthlyBudget, double totalSpent, String currency, UserEntity user) {
        this.category = category;
        this.monthlyBudget = monthlyBudget;
        this.totalSpent = totalSpent;
        this.currency = currency;
        this.user = user;
        
        // Default to current month and year
        YearMonth now = YearMonth.now();
        this.budgetMonth = now.getMonthValue();
        this.budgetYear = now.getYear();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getMonthlyBudget() {
        return monthlyBudget;
    }

    public void setMonthlyBudget(double monthlyBudget) {
        this.monthlyBudget = monthlyBudget;
    }

    public double getTotalSpent() {
        return totalSpent;
    }

    public void setTotalSpent(double totalSpent) {
        this.totalSpent = totalSpent;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public int getBudgetMonth() {
        return budgetMonth;
    }

    public void setBudgetMonth(int budgetMonth) {
        this.budgetMonth = budgetMonth;
    }

    public int getBudgetYear() {
        return budgetYear;
    }

    public void setBudgetYear(int budgetYear) {
        this.budgetYear = budgetYear;
    }
    
    // Helper method to get the month name
    public String getMonthName() {
        return Month.of(budgetMonth).name();
    }
    
    // Helper method to determine if this is the current month budget
    public boolean isCurrentMonth() {
        YearMonth current = YearMonth.now();
        return current.getYear() == budgetYear && current.getMonthValue() == budgetMonth;
    }
    
    public UserEntity getUser() {
        return user;
    }
    
    public void setUser(UserEntity user) {
        this.user = user;
    }
    
    public List<ExpenseEntity> getExpenses() {
        return expenses;
    }
    
    public void setExpenses(List<ExpenseEntity> expenses) {
        this.expenses = expenses;
    }
    
    // Helper methods for managing the relationship
    public void addExpense(ExpenseEntity expense) {
        expenses.add(expense);
        expense.setBudget(this);
        // Update the totalSpent amount
        this.totalSpent += expense.getAmount();
    }
    
    public void removeExpense(ExpenseEntity expense) {
        expenses.remove(expense);
        expense.setBudget(null);
        // Update the totalSpent amount
        this.totalSpent -= expense.getAmount();
    }
    
    public void updateExpense(ExpenseEntity expense, double oldAmount) {
        // Calculate the difference and update the totalSpent
        this.totalSpent = this.totalSpent - oldAmount + expense.getAmount();
    }
}