package com.easyride.notification_service.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FcmConfig {

    private static final Logger log = LoggerFactory.getLogger(FcmConfig.class);

    // Path to your Firebase service account key JSON file (src/main/resources)
    @Value("${fcm.service-account.file:firebase-service-account-key.json}") // Default path
    private String serviceAccountKeyPath;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) { // Check if already initialized
            log.info("Initializing FirebaseApp with service account key from: {}", serviceAccountKeyPath);
            InputStream serviceAccountStream = new ClassPathResource(serviceAccountKeyPath).getInputStream();

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccountStream))
                    // Optionally set database URL if using Firebase Realtime Database or Firestore
                    // .setDatabaseUrl("https://<YOUR_PROJECT_ID>.firebaseio.com")
                    .build();

            return FirebaseApp.initializeApp(options);
        } else {
            log.info("FirebaseApp already initialized.");
            return FirebaseApp.getInstance();
        }
    }
}