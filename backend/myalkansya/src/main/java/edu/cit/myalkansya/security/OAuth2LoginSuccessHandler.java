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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private static final Logger logger = Logger.getLogger(OAuth2LoginSuccessHandler.class.getName());

    public OAuth2LoginSuccessHandler(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) 
            throws IOException, ServletException {
        
        String email = null;
        String provider = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
        logger.info("OAuth2 login success with provider: " + provider);
        
        if (authentication.getPrincipal() instanceof CustomOAuth2User) {
            // Our custom OAuth2User handling (works for both providers)
            CustomOAuth2User customOAuth2User = (CustomOAuth2User) authentication.getPrincipal();
            UserEntity user = customOAuth2User.getUser();
            email = user.getEmail();
            logger.info("Using CustomOAuth2User: " + email);
        } else if (authentication.getPrincipal() instanceof DefaultOidcUser) {
            // Google OIDC authentication
            DefaultOidcUser oidcUser = (DefaultOidcUser) authentication.getPrincipal();
            email = oidcUser.getEmail();
            logger.info("Using DefaultOidcUser: " + email);
            
            // Make sure user is saved/updated in our database
            processOidcUser(oidcUser);
        } else if (authentication.getPrincipal() instanceof OAuth2User) {
            // Generic OAuth2 (Facebook or possibly others)
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            email = oauth2User.getAttribute("email");
            logger.info("Using generic OAuth2User: " + email);
            
            if ("facebook".equals(provider)) {
                processFacebookUser(oauth2User);
            }
        }
        
        if (email != null) {
            String token = jwtUtil.generateToken(email);
            
            // Get the redirect_uri
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
            logger.warning("Email not found in OAuth2 user data");
            response.sendRedirect("http://localhost:5173/login?error=auth_failed");
        }
    }
    
    private void processFacebookUser(OAuth2User oAuth2User) {
        String email = oAuth2User.getAttribute("email");
        String firstname = oAuth2User.getAttribute("first_name");
        String lastname = oAuth2User.getAttribute("last_name");
        String providerId = oAuth2User.getAttribute("id");
        
        // Handle cases where name fields might be missing
        if (firstname == null) {
            String name = oAuth2User.getAttribute("name");
            if (name != null) {
                String[] parts = name.split("\\s+", 2);
                firstname = parts[0];
                lastname = parts.length > 1 ? parts[1] : "User";
            } else {
                firstname = "Facebook";
                lastname = "User";
            }
        }
        
        if (lastname == null || lastname.trim().isEmpty()) {
            lastname = "User";
        }
        
        logger.info("Facebook user: Email=" + email + ", First name=" + firstname + ", Last name=" + lastname);
                    
        // Extract picture URL from nested Facebook structure
        String picture = null;
        
        // Method 1: Try the standard nested structure
        if (oAuth2User.getAttribute("picture") != null) {
            logger.info("Facebook picture attribute found: " + oAuth2User.getAttribute("picture"));
            
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> pictureObj = (Map<String, Object>) oAuth2User.getAttribute("picture");
                if (pictureObj != null && pictureObj.containsKey("data")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> pictureData = (Map<String, Object>) pictureObj.get("data");
                    if (pictureData != null) {
                        picture = (String) pictureData.get("url");
                        logger.info("Extracted Facebook profile picture URL: " + picture);
                    }
                }
            } catch (Exception e) {
                logger.warning("Error parsing Facebook picture object: " + e.getMessage());
            }
        }
        
        // Method 2: If we still don't have a picture, try constructing a URL directly
        if (picture == null && providerId != null) {
            // This URL format requests a large profile picture directly from Facebook
            picture = "https://graph.facebook.com/" + providerId + "/picture?type=large";
            logger.info("Using constructed Facebook profile picture URL: " + picture);
        }
        
        saveOrUpdateUser(email, firstname, lastname, picture, providerId, "FACEBOOK");
    }
    
    private void processOidcUser(DefaultOidcUser oidcUser) {
        String email = oidcUser.getEmail();
        String firstname = oidcUser.getGivenName();
        String lastname = oidcUser.getFamilyName();
        String picture = oidcUser.getAttribute("picture");
        String providerId = oidcUser.getSubject();

        // Ensure firstname and lastname are not null
        if (firstname == null || firstname.trim().isEmpty()) {
            firstname = "Google";
        }
        if (lastname == null || lastname.trim().isEmpty()) {
            lastname = "User";
        }
        
        logger.info("Google OIDC user: Email=" + email + ", First name=" + firstname + ", Last name=" + lastname);
        
        saveOrUpdateUser(email, firstname, lastname, picture, providerId, "GOOGLE");
    }
    
    private void saveOrUpdateUser(String email, String firstname, String lastname, String picture, String providerId, String provider) {
        // Ensure we have non-null values for required fields
        firstname = (firstname != null && !firstname.trim().isEmpty()) ? firstname : "User";
        lastname = (lastname != null && !lastname.trim().isEmpty()) ? lastname : "Name";
        
        Optional<UserEntity> userOptional = userRepository.findByEmail(email);
        
        if (userOptional.isPresent()) {
            UserEntity existingUser = userOptional.get();
            if ("LOCAL".equals(existingUser.getAuthProvider())) {
                existingUser.setAuthProvider(provider);
                existingUser.setProviderId(providerId);
            }
            existingUser.setFirstname(firstname);
            existingUser.setLastname(lastname);
            existingUser.setProfilePicture(picture);
            userRepository.save(existingUser);
            logger.info("Updated user: " + email);
        } else {
            UserEntity newUser = new UserEntity();
            newUser.setEmail(email);
            newUser.setFirstname(firstname);
            newUser.setLastname(lastname);
            newUser.setProfilePicture(picture);
            newUser.setAuthProvider(provider);
            newUser.setProviderId(providerId);
            // No password is set for OAuth2 users
            userRepository.save(newUser);
            logger.info("Created new user: " + email);
        }
    }
}