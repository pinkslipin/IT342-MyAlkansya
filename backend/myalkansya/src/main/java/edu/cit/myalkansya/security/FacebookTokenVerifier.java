package edu.cit.myalkansya.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.cit.myalkansya.dto.FacebookUserDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class FacebookTokenVerifier {
    
    private static final Logger logger = Logger.getLogger(FacebookTokenVerifier.class.getName());
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();
    
    /**
     * Verify Facebook access token and get user information
     * @param accessToken The Facebook access token to verify
     * @return FacebookUserDTO with user information or null if verification fails
     */
    public FacebookUserDTO verifyFacebookToken(String accessToken) {
        try {
            logger.info("Verifying Facebook token...");
            
            // Step 1: Verify token with Facebook Graph API
            String verifyUrl = "https://graph.facebook.com/v19.0/me?access_token=" + accessToken + 
                    "&fields=id,name,email,first_name,last_name,picture";
            
            ResponseEntity<String> response = restTemplate.getForEntity(verifyUrl, String.class);
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                logger.warning("Facebook token verification failed with status: " + response.getStatusCode());
                return null;
            }
            
            // Step 2: Parse the Facebook response
            JsonNode node = objectMapper.readTree(response.getBody());
            
            // Extract user info
            String id = node.path("id").asText();
            String email = node.path("email").asText();
            String firstName = node.path("first_name").asText();
            String lastName = node.path("last_name").asText();
            String name = node.path("name").asText();
            
            // Handle cases where name might be missing or incomplete
            if (firstName == null || firstName.isEmpty()) {
                if (name != null && !name.isEmpty()) {
                    String[] parts = name.split("\\s+", 2);
                    firstName = parts[0];
                    lastName = parts.length > 1 ? parts[1] : "User";
                } else {
                    firstName = "Facebook";
                    lastName = "User";
                }
            }
            
            if (lastName == null || lastName.isEmpty()) {
                lastName = "User";
            }
            
            // Get profile picture URL
            String pictureUrl = null;
            if (node.has("picture") && node.path("picture").has("data") && 
                node.path("picture").path("data").has("url")) {
                pictureUrl = node.path("picture").path("data").path("url").asText();
            } else {
                // Construct URL directly if not available
                pictureUrl = "https://graph.facebook.com/" + id + "/picture?type=large";
            }
            
            logger.info("Facebook token verified successfully for user: " + firstName + " " + lastName + 
                       (email != null ? " (" + email + ")" : " (no email)"));
            
            return new FacebookUserDTO(firstName, lastName, email, pictureUrl, id);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error verifying Facebook token: " + e.getMessage(), e);
            return null;
        }
    }
}
