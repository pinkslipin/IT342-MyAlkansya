package edu.cit.myalkansya.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "users") // optional, to avoid reserved word conflict
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int userId;

    @Column(nullable = false)
    private String firstname;

    @Column(nullable = true)
    private String lastname;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = true) // nullable = true is default
    private String password; // renamed from passwordHash

    private String authProvider; // "LOCAL", "GOOGLE"

    private String providerId; // Google 'sub' ID

    @Column(length = 1024)
    private String profilePicture;

    private double totalSavings = 0.0;

    private String currency = "PHP"; // Default currency

    // New fields for currency conversion tracking
    private Double originalTotalSavings;
    private String originalCurrency;

    private LocalDateTime createdAt = LocalDateTime.now();
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("user") // Prevents infinite recursion in JSON response
    private List<IncomeEntity> incomes = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("user") // Prevents infinite recursion in JSON response
    private List<ExpenseEntity> expenses = new ArrayList<>();
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("user") // Prevents infinite recursion in JSON response
    private List<BudgetEntity> budgets = new ArrayList<>();
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("user") // Prevents infinite recursion in JSON response
    private List<SavingsGoalEntity> savingsGoals = new ArrayList<>();
    
    // Getters and Setters
    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAuthProvider() {
        return authProvider;
    }

    public void setAuthProvider(String authProvider) {
        this.authProvider = authProvider;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public double getTotalSavings() {
        return totalSavings;
    }

    public void setTotalSavings(double totalSavings) {
        this.totalSavings = totalSavings;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public List<IncomeEntity> getIncomes() {
        return incomes;
    }
    
    public void setIncomes(List<IncomeEntity> incomes) {
        this.incomes = incomes;
    }
    
    // Helper method to add income
    public void addIncome(IncomeEntity income) {
        incomes.add(income);
        income.setUser(this);
    }
    
    // Helper method to remove income
    public void removeIncome(IncomeEntity income) {
        incomes.remove(income);
        income.setUser(null);
    }
    
    public List<ExpenseEntity> getExpenses() {
        return expenses;
    }
    
    public void setExpenses(List<ExpenseEntity> expenses) {
        this.expenses = expenses;
    }
    
    // Helper method to add expense
    public void addExpense(ExpenseEntity expense) {
        expenses.add(expense);
        expense.setUser(this);
    }
    
    // Helper method to remove expense
    public void removeExpense(ExpenseEntity expense) {
        expenses.remove(expense);
        expense.setUser(null);
    }
    
    public List<BudgetEntity> getBudgets() {
        return budgets;
    }
    
    public void setBudgets(List<BudgetEntity> budgets) {
        this.budgets = budgets;
    }
    
    // Helper method to add budget
    public void addBudget(BudgetEntity budget) {
        budgets.add(budget);
        budget.setUser(this);
    }
    
    // Helper method to remove budget
    public void removeBudget(BudgetEntity budget) {
        budgets.remove(budget);
        budget.setUser(null);
    }
    
    // Getters and setters for savingsGoals
    public List<SavingsGoalEntity> getSavingsGoals() {
        return savingsGoals;
    }
    
    public void setSavingsGoals(List<SavingsGoalEntity> savingsGoals) {
        this.savingsGoals = savingsGoals;
    }
    
    // Helper method to add savings goal
    public void addSavingsGoal(SavingsGoalEntity savingsGoal) {
        savingsGoals.add(savingsGoal);
        savingsGoal.setUser(this);
    }
    
    // Helper method to remove savings goal
    public void removeSavingsGoal(SavingsGoalEntity savingsGoal) {
        savingsGoals.remove(savingsGoal);
        savingsGoal.setUser(null);
    }

    // Getters and setters for new fields
    public Double getOriginalTotalSavings() {
        return originalTotalSavings;
    }

    public void setOriginalTotalSavings(Double originalTotalSavings) {
        this.originalTotalSavings = originalTotalSavings;
    }

    public String getOriginalCurrency() {
        return originalCurrency;
    }

    public void setOriginalCurrency(String originalCurrency) {
        this.originalCurrency = originalCurrency;
    }
}