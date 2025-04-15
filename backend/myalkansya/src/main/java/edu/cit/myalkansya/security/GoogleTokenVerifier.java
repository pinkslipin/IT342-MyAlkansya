package edu.cit.myalkansya.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory; // Using Gson instead of Jackson
import edu.cit.myalkansya.dto.GoogleUserDTO;

import org.aspectj.weaver.ast.And;
import org.springframework.stereotype.Component;
import java.util.Arrays; 
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.logging.Logger;

@Component
public class GoogleTokenVerifier {

    private static final String CLIENT_ID = "275880440953-akhnvpmdmm4hiutcji6mcjtbap9cq7q1.apps.googleusercontent.com"; // Replace with your actual Google Client ID
    private static final String AndroidCLIENT_ID = "1002381152451-vnvb9324bmvonje971dtsr2gq6dk90kk.apps.googleusercontent.com";

    private final GoogleIdTokenVerifier verifier;
    private static final Logger logger = Logger.getLogger(GoogleTokenVerifier.class.getName());

    public GoogleTokenVerifier() {
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory()) // Using Gson
                .setAudience(Arrays.asList(CLIENT_ID,AndroidCLIENT_ID)) 
                .build();
    }

    public GoogleUserDTO verifyGoogleToken(String idToken) {
        try {
            logger.info("Verifying Google ID Token: " + idToken);
            GoogleIdToken googleIdToken = verifier.verify(idToken);
            if (googleIdToken == null) {
                logger.warning("Invalid Google ID Token");
                return null; // Token is invalid
            }

            GoogleIdToken.Payload payload = googleIdToken.getPayload();
            logger.info("Google ID Token verified: Email=" + payload.getEmail());
            String firstname = (String) payload.get("given_name");
            String lastname = (String) payload.get("family_name"); // Can be null
            String email = payload.getEmail();
            String profilePicture = (String) payload.get("picture");
            String providerId = payload.getSubject();

            return new GoogleUserDTO(firstname, lastname, email, profilePicture, providerId);
        } catch (GeneralSecurityException | IOException e) {
            logger.severe("Error verifying Google ID Token: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
