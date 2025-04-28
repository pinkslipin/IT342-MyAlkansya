package edu.cit.myalkansya.dto;

public class ProfilePictureUploadDTO {
    private String profilePicture;  // base64 encoded image
    private String fileName;
    private String contentType;

    // Default constructor
    public ProfilePictureUploadDTO() {
    }

    // Getters and setters
    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}