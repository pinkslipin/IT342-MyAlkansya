package edu.cit.myalkansya.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory; // Using Gson instead of Jackson
import edu.cit.myalkansya.dto.GoogleUserDTO;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.logging.Logger;

@Component
public class GoogleTokenVerifier {

    private static final Logger logger = Logger.getLogger(GoogleTokenVerifier.class.getName());
    
    // Web client ID
    private static final String WEB_CLIENT_ID = "275880440953-akhnvpmdmm4hiutcji6mcjtbap9cq7q1.apps.googleusercontent.com";
    
    // Android client ID - add this line
    private static final String ANDROID_CLIENT_ID = "275880440953-bghdgma37revndr2vepueat789feq9gtk.apps.googleusercontent.com";

    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifier() {
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Arrays.asList(WEB_CLIENT_ID, ANDROID_CLIENT_ID))  // Accept both client IDs
                .build();
        
        logger.info("Google Token Verifier initialized with Web Client ID: " + WEB_CLIENT_ID);
        logger.info("Google Token Verifier initialized with Android Client ID: " + ANDROID_CLIENT_ID);
    }

    public GoogleUserDTO verifyGoogleToken(String idToken) {
        try {
            logger.info("Attempting to verify Google token...");
            GoogleIdToken googleIdToken = verifier.verify(idToken);
            
            if (googleIdToken == null) {
                logger.warning("Google token verification failed: Token is invalid");
                return null; // Token is invalid
            }

            GoogleIdToken.Payload payload = googleIdToken.getPayload();
            String firstname = (String) payload.get("given_name");
            String lastname = (String) payload.get("family_name"); // Can be null
            String email = payload.getEmail();
            String profilePicture = (String) payload.get("picture");
            String providerId = payload.getSubject();
            
            logger.info("Google token verified successfully for email: " + email);
            return new GoogleUserDTO(firstname, lastname, email, profilePicture, providerId);
        } catch (GeneralSecurityException | IOException e) {
            logger.severe("Exception during Google token verification: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
