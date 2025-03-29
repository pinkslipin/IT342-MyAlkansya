package edu.cit.myalkansya.security;

import edu.cit.myalkansya.entity.UserEntity;
import edu.cit.myalkansya.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public OAuth2LoginSuccessHandler(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        setDefaultTargetUrl("/user-info");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) 
            throws IOException, ServletException {
        
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = null;
        
        if (oAuth2User instanceof DefaultOidcUser) {
            // For Google OIDC authentication
            DefaultOidcUser oidcUser = (DefaultOidcUser) oAuth2User;
            email = oidcUser.getEmail();
            processOidcUser(oidcUser);
        } else {
            // Fallback for regular OAuth2 providers
            email = oAuth2User.getAttribute("email");
        }
        
        if (email != null) {
            String token = jwtUtil.generateToken(email);
            response.setHeader("Authorization", "Bearer " + token);
        }
        
        super.onAuthenticationSuccess(request, response, authentication);
    }
    
    private void processOidcUser(DefaultOidcUser oidcUser) {
        String email = oidcUser.getEmail();
        String firstname = oidcUser.getGivenName();
        String lastname = oidcUser.getFamilyName();
        String picture = oidcUser.getAttribute("picture");
        String providerId = oidcUser.getSubject();

        userRepository.findByEmail(email)
            .ifPresentOrElse(
                user -> {
                    // Update existing user
                    user.setFirstname(firstname);
                    user.setLastname(lastname);
                    user.setProfilePicture(picture);
                    if (!"GOOGLE".equals(user.getAuthProvider())) {
                        user.setAuthProvider("GOOGLE");
                        user.setProviderId(providerId);
                    }
                    userRepository.save(user);
                },
                () -> {
                    // Create new user
                    UserEntity newUser = new UserEntity();
                    newUser.setEmail(email);
                    newUser.setFirstname(firstname);
                    newUser.setLastname(lastname);
                    newUser.setProfilePicture(picture);
                    newUser.setAuthProvider("GOOGLE");
                    newUser.setProviderId(providerId);
                    newUser.setPassword("oauth2_user"); // Set a default password for OAuth2 users
                    userRepository.save(newUser);
                }
            );
    }
}