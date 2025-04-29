package edu.cit.myalkansya.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.logging.Logger;

@Service
public class CloudinaryService {
    private final Cloudinary cloudinary;
    private static final Logger logger = Logger.getLogger(CloudinaryService.class.getName());
    
    public CloudinaryService() {
        // Initialize Cloudinary with your actual credentials
        cloudinary = new Cloudinary(ObjectUtils.asMap(
            "cloud_name", "dmq7l0qec",
            "api_key", "237479364428163",
            "api_secret", "mXs8DeSapDRceTSK_6VL-C4qn3I"
        ));
    }
    
    public String uploadImage(byte[] imageData, String filename) throws IOException {
        try {
            Map uploadResult = cloudinary.uploader().upload(
                imageData, 
                ObjectUtils.asMap(
                    "public_id", "profile_pictures/" + filename.replace(".", "_"),
                    "resource_type", "image"
                )
            );
            
            // Get the secure URL from the response
            String secureUrl = (String) uploadResult.get("secure_url");
            logger.info("Image uploaded to Cloudinary: " + secureUrl);
            return secureUrl;
            
        } catch (Exception e) {
            logger.severe("Cloudinary upload error: " + e.getMessage());
            throw e;
        }
    }
}