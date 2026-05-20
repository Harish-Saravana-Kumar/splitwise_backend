package com.splitwise.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import java.io.IOException;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.service-account-path:firebase/serviceAccountKey.json}")
    private String serviceAccountPath;

    @Bean
    @Lazy
    public FirebaseApp firebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        Resource resource = resolveServiceAccountResource();

        if (!resource.exists()) {
            String message = "Firebase service account JSON not found at " + serviceAccountPath
                    + ". Place the downloaded file at src/main/resources/firebase/serviceAccountKey.json "
                    + "or set FIREBASE_SERVICE_ACCOUNT_PATH to an absolute or classpath resource.";
            log.error(message);
            throw new IOException(message);
        }

        try (InputStream serviceAccountStream = resource.getInputStream()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccountStream))
                    .build();
            return FirebaseApp.initializeApp(options);
        } catch (IOException ex) {
            log.error("Failed to read Firebase service account JSON from {}", serviceAccountPath, ex);
            throw ex;
        }
    }

    @Bean
    @Lazy
    public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        return FirebaseAuth.getInstance(firebaseApp);
    }

    private Resource resolveServiceAccountResource() {
        if (serviceAccountPath.startsWith("classpath:")) {
            return new ClassPathResource(serviceAccountPath.substring("classpath:".length()));
        }

        return new ClassPathResource(serviceAccountPath);
    }
}