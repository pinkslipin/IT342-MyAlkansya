package edu.cit.myalkansya.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory; // Using Gson instead of Jackson
import edu.cit.myalkansya.dto.GoogleUserDTO;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Component
public class GoogleTokenVerifier {

    private static final String CLIENT_ID = "275880440953-akhnvpmdmm4hiutcji6mcjtbap9cq7q1.apps.googleusercontent.com"; // Replace with your actual Google Client ID

    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifier() {
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory()) // Using Gson
                .setAudience(Collections.singletonList(CLIENT_ID))
                .build();
    }

    public GoogleUserDTO verifyGoogleToken(String idToken) {
        try {
            GoogleIdToken googleIdToken = verifier.verify(idToken);
            if (googleIdToken == null) {
                return null; // Token is invalid
            }

            GoogleIdToken.Payload payload = googleIdToken.getPayload();
            String firstname = (String) payload.get("given_name");
            String lastname = (String) payload.get("family_name"); // Can be null
            String email = payload.getEmail();
            String profilePicture = (String) payload.get("picture");
            String providerId = payload.getSubject();

            return new GoogleUserDTO(firstname, lastname, email, profilePicture, providerId);
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
