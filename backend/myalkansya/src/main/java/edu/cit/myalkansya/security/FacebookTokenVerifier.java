package edu.cit.myalkansya.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.cit.myalkansya.dto.FacebookUserDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.logging.Logger;

@Component
public class FacebookTokenVerifier {
    private static final Logger logger = Logger.getLogger(FacebookTokenVerifier.class.getName());
    
    @Value("${spring.security.oauth2.client.registration.facebook.client-id}")
    private String appId;
    
    @Value("${spring.security.oauth2.client.registration.facebook.client-secret}")
    private String appSecret;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public FacebookTokenVerifier() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Verifies a Facebook access token and returns user information if valid
     */
    public FacebookUserDTO verifyFacebookToken(String accessToken) {
        try {
            // First, verify the token with Facebook
            String verifyUrl = "https://graph.facebook.com/debug_token?input_token=" + accessToken 
                             + "&access_token=" + appId + "|" + appSecret;
            
            ResponseEntity<String> verifyResponse = restTemplate.getForEntity(verifyUrl, String.class);
            if (verifyResponse.getStatusCode() != HttpStatus.OK) {
                logger.warning("Facebook token verification failed with status: " + verifyResponse.getStatusCode());
                return null;
            }
            
            JsonNode verifyData = objectMapper.readTree(verifyResponse.getBody());
            if (!verifyData.path("data").path("is_valid").asBoolean()) {
                logger.warning("Facebook token is not valid: " + verifyData.toString());
                return null;
            }
            
            String userId = verifyData.path("data").path("user_id").asText();
            if (userId == null || userId.isEmpty()) {
                logger.warning("No user ID in Facebook token response");
                return null;
            }
            
            // Now fetch user data using the verified token
            String userDataUrl = "https://graph.facebook.com/v17.0/me?fields=id,email,first_name,last_name,picture&access_token=" + accessToken;
            ResponseEntity<String> userResponse = restTemplate.getForEntity(userDataUrl, String.class);
            
            if (userResponse.getStatusCode() != HttpStatus.OK) {
                logger.warning("Failed to fetch Facebook user data with status: " + userResponse.getStatusCode());
                return null;
            }
            
            JsonNode userData = objectMapper.readTree(userResponse.getBody());
            
            // Extract user information
            FacebookUserDTO facebookUser = new FacebookUserDTO();
            facebookUser.setProviderId(userData.path("id").asText());
            facebookUser.setEmail(userData.path("email").asText(""));
            facebookUser.setFirstname(userData.path("first_name").asText(""));
            facebookUser.setLastname(userData.path("last_name").asText(""));
            
            // Extract profile picture URL and use a shorter version if available
            if (userData.has("picture") && userData.path("picture").has("data")) {
                String pictureUrl = userData.path("picture").path("data").path("url").asText("");
                
                if (pictureUrl.length() > 1000) {
                    // If URL is too long, use a simplified URL based on user ID
                    logger.info("Facebook profile picture URL is too long, using simplified version");
                    pictureUrl = "https://graph.facebook.com/" + userId + "/picture?type=large";
                }
                
                facebookUser.setProfilePicture(pictureUrl);
                logger.info("Using profile picture URL: " + pictureUrl);
            } else {
                // Use default picture URL based on user ID
                String defaultPicture = "https://graph.facebook.com/" + userId + "/picture?type=large";
                facebookUser.setProfilePicture(defaultPicture);
                logger.info("Using default profile picture URL: " + defaultPicture);
            }
            
            return facebookUser;
        } catch (IOException e) {
            logger.severe("Error parsing Facebook response: " + e.getMessage());
            return null;
        } catch (Exception e) {
            logger.severe("Error verifying Facebook token: " + e.getMessage());
            return null;
        }
    }
}
