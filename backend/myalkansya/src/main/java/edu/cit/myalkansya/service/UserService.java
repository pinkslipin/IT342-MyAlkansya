package edu.cit.myalkansya.service;

import edu.cit.myalkansya.entity.UserEntity;
import edu.cit.myalkansya.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private PasswordEncoder encoder;

    public UserEntity registerLocalUser(String firstname, String lastname, String email, String password, String currency) {
        UserEntity user = new UserEntity();
        user.setFirstname(firstname);
        user.setLastname(lastname);
        user.setEmail(email);
        user.setPassword(encoder.encode(password));
        user.setAuthProvider("LOCAL");
        user.setCurrency(currency);
        return userRepo.save(user);
    }

    public Optional<UserEntity> findByEmail(String email) {
        return userRepo.findByEmail(email);
    }

    public Optional<UserEntity> findByProviderId(String providerId) {
        return userRepo.findByProviderId(providerId);
    }
}