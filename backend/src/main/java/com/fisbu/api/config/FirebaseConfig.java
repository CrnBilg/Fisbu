package com.fisbu.api.config;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import jakarta.annotation.PostConstruct;

/**
 * Firebase Admin SDK'yı service account JSON'ı ile başlatır.
 * JSON, FIREBASE_SERVICE_ACCOUNT ortam değişkeninde tam metin olarak tutulur.
 * Değişken boşsa Firebase başlatılmaz ve push bildirimleri sessizce atlanır
 * (uygulama yine de normal çalışır — EmailService ile aynı yaklaşım).
 */
@Component
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${firebase.service-account-json:}")
    private String serviceAccountJson;

    @PostConstruct
    public void init() {
        if (serviceAccountJson == null || serviceAccountJson.isBlank()) {
            log.warn("FIREBASE_SERVICE_ACCOUNT tanımlı değil — push bildirimleri devre dışı");
            return;
        }

        if (!FirebaseApp.getApps().isEmpty()) {
            return; // Zaten başlatılmış
        }

        try {
            GoogleCredentials credentials = GoogleCredentials.fromStream(
                    new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8)));
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build();
            FirebaseApp.initializeApp(options);
            log.info("Firebase Admin SDK başarıyla başlatıldı");
        } catch (Exception e) {
            log.error("Firebase başlatılamadı: {}", e.getMessage());
        }
    }
}
