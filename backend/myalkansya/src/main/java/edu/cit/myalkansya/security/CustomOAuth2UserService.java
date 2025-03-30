package edu.cit.myalkansya.security;

import edu.cit.myalkansya.entity.UserEntity;
import edu.cit.myalkansya.repository.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepo;
    private static final Logger logger = Logger.getLogger(CustomOAuth2UserService.class.getName());

    public CustomOAuth2UserService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) {
        OAuth2User oAuth2User = super.loadUser(request);
        Map<String, Object> attributes = oAuth2User.getAttributes();
        
        // Get provider ID (Google or Facebook)
        String registrationId = request.getClientRegistration().getRegistrationId();
        
        String email = null;
        String firstname = null;
        String lastname = null;
        String picture = null;
        String providerId = null;
        
        if ("google".equals(registrationId)) {
            email = (String) attributes.get("email");
            firstname = (String) attributes.get("given_name");
            lastname = (String) attributes.get("family_name");
            picture = (String) attributes.get("picture");
            providerId = (String) attributes.get("sub");
        } else if ("facebook".equals(registrationId)) {
            email = (String) attributes.get("email");
            providerId = (String) attributes.get("id");
            
            // Try to get first_name and last_name directly
            firstname = (String) attributes.get("first_name");
            lastname = (String) attributes.get("last_name");
            
            // If firstname is missing, parse the full name
            if (firstname == null) {
                String name = (String) attributes.get("name");
                logger.info("Facebook returned full name: " + name);
                
                if (name != null && !name.trim().isEmpty()) {
                    // Split at first space to separate first and last name
                    String[] nameParts = name.split("\\s+", 2);
                    firstname = nameParts[0];
                    lastname = (nameParts.length > 1) ? nameParts[1] : ""; 
                    
                    logger.info("Parsed name into: firstname='" + firstname + "', lastname='" + lastname + "'");
                }
            }
            
            // Ensure we have non-null values
            if (firstname == null || firstname.trim().isEmpty()) {
                firstname = "Facebook";
            }
            
            if (lastname == null || lastname.trim().isEmpty()) {
                lastname = "User";
            }
            
            // Extract profile picture URL from Facebook's nested structure
            if (attributes.containsKey("picture")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> pictureObj = (Map<String, Object>) attributes.get("picture");
                if (pictureObj != null && pictureObj.containsKey("data")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> pictureData = (Map<String, Object>) pictureObj.get("data");
                    if (pictureData != null) {
                        picture = (String) pictureData.get("url");
                    }
                }
            }
        }
        
        UserEntity user = processOAuth2User(email, firstname, lastname, picture, providerId, registrationId.toUpperCase());

        return new CustomOAuth2User(oAuth2User, user);
    }

    private UserEntity processOAuth2User(String email, String firstname, String lastname, String picture, String providerId, String provider) {
        // Ensure we have non-null values for required fields
        firstname = (firstname != null && !firstname.trim().isEmpty()) ? firstname : "User";
        lastname = (lastname != null && !lastname.trim().isEmpty()) ? lastname : "Name";
        
        logger.info("Processing OAuth2 user: email=" + email + ", firstname=" + firstname + 
                    ", lastname=" + lastname + ", provider=" + provider);
        
        Optional<UserEntity> userOptional = userRepo.findByEmail(email);

        if (userOptional.isPresent()) {
            UserEntity existingUser = userOptional.get();
            if ("LOCAL".equals(existingUser.getAuthProvider())) {
                existingUser.setAuthProvider(provider);
                existingUser.setProviderId(providerId);
            }
            existingUser.setFirstname(firstname);
            existingUser.setLastname(lastname);
            existingUser.setProfilePicture(picture);
            return userRepo.save(existingUser);
        } else {
            UserEntity newUser = new UserEntity();
            newUser.setEmail(email);
            newUser.setFirstname(firstname);
            newUser.setLastname(lastname);
            newUser.setProfilePicture(picture);
            newUser.setAuthProvider(provider);
            newUser.setProviderId(providerId);
            return userRepo.save(newUser);
        }
    }
}