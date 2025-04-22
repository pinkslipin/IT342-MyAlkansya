package edu.cit.myalkansya.dto;

import java.util.Objects;

public class ProfileUpdateRequest {
    private String firstname;
    private String lastname;
    private String email;
    private String currency;

    // Default constructor
    public ProfileUpdateRequest() {
    }

    // Constructor with fields
    public ProfileUpdateRequest(String firstname, String lastname, String email, String currency) {
        this.firstname = firstname;
        this.lastname = lastname;
        this.email = email;
        this.currency = currency;
    }

    // Getters and setters
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

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    @Override
    public String toString() {
        return "ProfileUpdateRequest{" +
                "firstname='" + firstname + '\'' +
                ", lastname='" + lastname + '\'' +
                ", email='" + email + '\'' +
                ", currency='" + currency + '\'' +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProfileUpdateRequest that = (ProfileUpdateRequest) o;
        return Objects.equals(firstname, that.firstname) &&
                Objects.equals(lastname, that.lastname) &&
                Objects.equals(email, that.email) &&
                Objects.equals(currency, that.currency);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(firstname, lastname, email, currency);
    }
}
