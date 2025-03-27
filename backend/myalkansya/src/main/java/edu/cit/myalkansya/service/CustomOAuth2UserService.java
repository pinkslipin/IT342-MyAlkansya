package edu.cit.myalkansya.service;

import edu.cit.myalkansya.entity.UserEntity;
import edu.cit.myalkansya.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepo;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        // Load user information from Google
        OAuth2User oauthUser = super.loadUser(userRequest);

        // Extract user details from Google attributes
        Map<String, Object> attributes = oauthUser.getAttributes();
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String picture = (String) attributes.get("picture");
        String providerId = (String) attributes.get("sub"); // Google unique ID

        // Check if the user already exists in the database
        Optional<UserEntity> userOpt = userRepo.findByProviderId(providerId);

        UserEntity user;
        if (userOpt.isPresent()) {
            // User already exists — update name, email, and profile picture if needed
            user = userOpt.get();
            user.setName(name);
            user.setEmail(email);
            user.setProfilePicture(picture);
        } else {
            // First-time login with Google — create a new user
            user = new UserEntity();
            user.setName(name);
            user.setEmail(email);
            user.setProfilePicture(picture);
            user.setProviderId(providerId);
            user.setAuthProvider("GOOGLE");
        }

        // Save the user in the database
        userRepo.save(user);

        return oauthUser;
    }

    public UserEntity getUserFromOAuth2User(OAuth2User oauthUser) {
        // Helper method to retrieve user details from OAuth2User
        Map<String, Object> attributes = oauthUser.getAttributes();
        String providerId = (String) attributes.get("sub"); // Google unique ID

        return userRepo.findByProviderId(providerId).orElse(null);
    }
}