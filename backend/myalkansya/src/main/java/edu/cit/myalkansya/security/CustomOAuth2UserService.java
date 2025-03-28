package edu.cit.myalkansya.security;

import edu.cit.myalkansya.entity.UserEntity;
import edu.cit.myalkansya.repository.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepo;

    public CustomOAuth2UserService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) {
        OAuth2User oAuth2User = super.loadUser(request);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // Extract necessary data from Google's response
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String picture = (String) attributes.get("picture");
        String providerId = (String) attributes.get("sub"); // Google ID

        UserEntity user = processOAuth2User(email, name, picture, providerId);

        // Return custom OAuth2User that links to our database entity
        return new CustomOAuth2User(oAuth2User, user);
    }

    private UserEntity processOAuth2User(String email, String name, String picture, String providerId) {
        // Check if user exists by email
        Optional<UserEntity> userOptional = userRepo.findByEmail(email);
        
        // If user exists, update their OAuth details
        if (userOptional.isPresent()) {
            UserEntity existingUser = userOptional.get();
            
            // If they initially registered locally but now using Google, update provider info
            if ("LOCAL".equals(existingUser.getAuthProvider())) {
                existingUser.setAuthProvider("GOOGLE");
                existingUser.setProviderId(providerId);
            }
            
            // Update profile information that might have changed on Google's side
            existingUser.setName(name);
            existingUser.setProfilePicture(picture);
            
            return userRepo.save(existingUser);
        } else {
            // Create new user if they don't exist
            UserEntity newUser = new UserEntity();
            newUser.setEmail(email);
            newUser.setName(name);
            newUser.setProfilePicture(picture);
            newUser.setAuthProvider("GOOGLE");
            newUser.setProviderId(providerId);
            return userRepo.save(newUser);
        }
    }
}