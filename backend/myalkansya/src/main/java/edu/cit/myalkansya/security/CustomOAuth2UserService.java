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

        String email = (String) attributes.get("email");
        String firstname = (String) attributes.get("given_name");
        String lastname = (String) attributes.get("family_name");
        String picture = (String) attributes.get("picture");
        String providerId = (String) attributes.get("sub");

        UserEntity user = processOAuth2User(email, firstname, lastname, picture, providerId);

        return new CustomOAuth2User(oAuth2User, user);
    }

    private UserEntity processOAuth2User(String email, String firstname, String lastname, String picture, String providerId) {
        Optional<UserEntity> userOptional = userRepo.findByEmail(email);

        if (userOptional.isPresent()) {
            UserEntity existingUser = userOptional.get();
            if ("LOCAL".equals(existingUser.getAuthProvider())) {
                existingUser.setAuthProvider("GOOGLE");
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
            newUser.setAuthProvider("GOOGLE");
            newUser.setProviderId(providerId);
            return userRepo.save(newUser);
        }
    }
}