package edu.cit.myalkansya.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Map the user-profile-pictures directory to be served statically
        Path uploadDir = Paths.get("user-profile-pictures");
        String uploadPath = uploadDir.toFile().getAbsolutePath();
        
        registry.addResourceHandler("/user-profile-pictures/**")
                .addResourceLocations("file:" + uploadPath + "/")
                .setCachePeriod(0); // Disable caching
    }
}