package edu.cit.myalkansya.security;

import edu.cit.myalkansya.entity.UserEntity;
import edu.cit.myalkansya.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

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
        
        if (oAuth2User instanceof CustomOAuth2User) {
            // If we have our custom OAuth2User, use it directly
            UserEntity user = ((CustomOAuth2User) oAuth2User).getUser();
            email = user.getEmail();
        } else if (oAuth2User instanceof DefaultOidcUser) {
            // For Google OIDC authentication
            DefaultOidcUser oidcUser = (DefaultOidcUser) oAuth2User;
            email = oidcUser.getEmail();
            
            // Ensure user is saved in database if using OIDC directly
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
        String name = oidcUser.getFullName();
        String picture = oidcUser.getAttribute("picture");
        String providerId = oidcUser.getName(); // Subject ID
        
        userRepository.findByEmail(email)
            .ifPresentOrElse(
                user -> {
                    // Update existing user
                    user.setName(name);
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
                    newUser.setName(name);
                    newUser.setProfilePicture(picture);
                    newUser.setAuthProvider("GOOGLE");
                    newUser.setProviderId(providerId);
                    userRepository.save(newUser);
                }
            );
    }
}