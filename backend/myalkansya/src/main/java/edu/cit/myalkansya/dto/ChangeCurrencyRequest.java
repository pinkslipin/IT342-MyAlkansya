package edu.cit.myalkansya.dto;

public class ChangeCurrencyRequest {
    private String newCurrency;
    private String oldCurrency;
    
    public String getNewCurrency() {
        return newCurrency;
    }
    
    public void setNewCurrency(String newCurrency) {
        this.newCurrency = newCurrency;
    }
    
    public String getOldCurrency() {
        return oldCurrency;
    }
    
    public void setOldCurrency(String oldCurrency) {
        this.oldCurrency = oldCurrency;
    }
}
