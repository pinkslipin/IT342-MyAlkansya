package edu.cit.myalkansya.controller;

import edu.cit.myalkansya.security.CustomOAuth2User;
import edu.cit.myalkansya.entity.UserEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
public class UserInfoController {

    @GetMapping("/user-info")
    public Map<String, Object> userInfo(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return Collections.singletonMap("error", "Not authenticated");
        }
        
        if (principal instanceof CustomOAuth2User) {
            UserEntity user = ((CustomOAuth2User) principal).getUser();
            return Map.of(
                "userId", user.getUserId(),
                "firstname", user.getFirstname(),
                "lastname", user.getLastname(),
                "email", user.getEmail(),
                "picture", user.getProfilePicture(),
                "authProvider", user.getAuthProvider(),
                "totalSavings", user.getTotalSavings()
            );
        }
        
        return Map.of(
            "name", principal.getAttribute("name"),
            "email", principal.getAttribute("email"),
            "picture", principal.getAttribute("picture")
        );
    }
}