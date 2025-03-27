package edu.cit.myalkansya.dto;

public class RegisterRequest {
    private String name;
    private String email;
    private String password;

    // Getters and setters (Lombok also works if you're using it)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
}