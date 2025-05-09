package edu.cit.myalkansya.dto;

public class FacebookUserDTO {
    private String firstname;
    private String lastname;
    private String email;
    private String profilePicture;
    private String providerId;

    public FacebookUserDTO() {
    }

    public FacebookUserDTO(String firstname, String lastname, String email, String profilePicture, String providerId) {
        this.firstname = firstname;
        this.lastname = lastname;
        this.email = email;
        this.profilePicture = profilePicture;
        this.providerId = providerId;
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

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }
}
