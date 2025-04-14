package edu.cit.myalkansya.dto;

public class MonthlySummaryDTO {
    private String month;
    private double income;
    private double expenses;
    
    public String getMonth() {
        return month;
    }
    
    public void setMonth(String month) {
        this.month = month;
    }
    
    public double getIncome() {
        return income;
    }
    
    public void setIncome(double income) {
        this.income = income;
    }
    
    public double getExpenses() {
        return expenses;
    }
    
    public void setExpenses(double expenses) {
        this.expenses = expenses;
    }
}