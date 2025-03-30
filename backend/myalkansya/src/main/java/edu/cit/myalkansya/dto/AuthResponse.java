package edu.cit.myalkansya.dto;

public class AuthResponse {
    private String token;
    private GoogleUserDTO user;

    public AuthResponse(GoogleUserDTO user, String token) {
        this.user = user;
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public GoogleUserDTO getUser() {
        return user;
    }
}
