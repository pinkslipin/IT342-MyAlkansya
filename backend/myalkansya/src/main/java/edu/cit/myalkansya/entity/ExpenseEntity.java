package edu.cit.myalkansya.entity;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "expenses")
public class ExpenseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String subject;
    private String category;
    private LocalDate date;
    private double amount;
    private String currency;
    
    // New fields for currency conversion tracking
    private Double originalAmount;
    private String originalCurrency;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"expenses", "incomes", "budgets"}) // Prevents infinite recursion in JSON response
    private UserEntity user;
    
    @ManyToOne
    @JoinColumn(name = "budget_id")
    @JsonIgnoreProperties({"expenses"}) // Prevents infinite recursion in JSON response
    private BudgetEntity budget;

    public ExpenseEntity() {
        super();
    }

    public ExpenseEntity(int id, String subject, String category, LocalDate date, double amount, String currency) {
        this.id = id;
        this.subject = subject;
        this.category = category;
        this.date = date;
        this.amount = amount;
        this.currency = currency;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public UserEntity getUser() {
        return user;
    }
    
    public void setUser(UserEntity user) {
        this.user = user;
    }
    
    public BudgetEntity getBudget() {
        return budget;
    }
    
    public void setBudget(BudgetEntity budget) {
        this.budget = budget;
    }

    // Getters and setters including for new fields
    public Double getOriginalAmount() {
        return originalAmount;
    }

    public void setOriginalAmount(Double originalAmount) {
        this.originalAmount = originalAmount;
    }

    public String getOriginalCurrency() {
        return originalCurrency;
    }

    public void setOriginalCurrency(String originalCurrency) {
        this.originalCurrency = originalCurrency;
    }
}