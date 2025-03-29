package edu.cit.myalkansya.security;

import edu.cit.myalkansya.entity.UserEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

public class CustomOAuth2User implements OAuth2User {

    private final OAuth2User oauth2User;
    private final UserEntity user;

    public CustomOAuth2User(OAuth2User oauth2User, UserEntity user) {
        this.oauth2User = oauth2User;
        this.user = user;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return oauth2User.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return oauth2User.getAuthorities();
    }

    @Override
    public String getName() {
        return user.getFirstname() + " " + user.getLastname();
    }

    public UserEntity getUser() {
        return user;
    }
}