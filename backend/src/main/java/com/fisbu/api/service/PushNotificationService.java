package com.fisbu.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

/**
 * FCM üzerinden tek bir cihaza push bildirim gönderir.
 * Firebase yapılandırılmamışsa (bkz. FirebaseConfig) sessizce atlanır.
 */
@Service
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);

    public boolean isConfigured() {
        return !FirebaseApp.getApps().isEmpty();
    }

    /**
     * Verilen FCM token'ına başlık + gövde ile bildirim gönderir.
     * Hata durumunda exception fırlatmaz — çağıran akışı (ör. fiş ekleme) bozulmamalı.
     */
    public void send(String fcmToken, String title, String body) {
        if (fcmToken == null || fcmToken.isBlank()) {
            return;
        }
        if (!isConfigured()) {
            log.warn("Firebase yapılandırılmadı, bildirim gönderilmedi: {}", title);
            return;
        }

        try {
            Message message = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.info("Push bildirimi gönderildi: {}", response);
        } catch (Exception e) {
            log.error("Push bildirimi gönderilemedi ({}): {}", title, e.getMessage());
        }
    }
}
