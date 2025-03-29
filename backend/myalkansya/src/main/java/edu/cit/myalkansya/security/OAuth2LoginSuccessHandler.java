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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public OAuth2LoginSuccessHandler(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        // No default target URL - we'll handle redirection explicitly
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
            
            // Get the redirect_uri from the request parameter or session attribute
            String redirectUri = request.getParameter("redirect_uri");
            
            // If no redirect_uri in parameters, check if it was stored in session
            if (redirectUri == null || redirectUri.isEmpty()) {
                redirectUri = (String) request.getSession().getAttribute("REDIRECT_URI");
            }
            
            // If still no redirect_uri, fallback to default frontend URL
            if (redirectUri == null || redirectUri.isEmpty()) {
                redirectUri = "http://localhost:5173"; // Default frontend URL
            }
            
            // Clean up any trailing slashes and add a token parameter
            if (redirectUri.endsWith("/")) {
                redirectUri = redirectUri.substring(0, redirectUri.length() - 1);
            }
            
            // Build the final redirect URL with the JWT token
            String finalRedirectUrl = redirectUri + "/?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
            
            // Set Authorization header and redirect
            response.setHeader("Authorization", "Bearer " + token);
            response.sendRedirect(finalRedirectUrl);
        } else {
            // If email is null, redirect to login page with error
            response.sendRedirect("http://localhost:5173/login?error=auth_failed");
        }
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