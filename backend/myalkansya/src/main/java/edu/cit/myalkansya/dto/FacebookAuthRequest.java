package edu.cit.myalkansya.dto;

public class FacebookAuthRequest {
    private String accessToken;

    // Default constructor
    public FacebookAuthRequest() {
    }

    public FacebookAuthRequest(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}

