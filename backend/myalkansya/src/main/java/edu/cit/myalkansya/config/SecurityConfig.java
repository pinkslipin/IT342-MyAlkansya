package edu.cit.myalkansya.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/error").permitAll()  // Public endpoints
                .anyRequest().authenticated()  // All others require auth
            )
            .oauth2Login(oauth2 -> oauth2
                .defaultSuccessUrl("http://localhost:8080/user-info", true)
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/")  // Redirect to home after logout
            )
            .csrf(csrf -> csrf.disable());  // Only disable for testing!

        return http.build();
    }
}