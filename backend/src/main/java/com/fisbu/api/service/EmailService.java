package com.fisbu.api.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final URI BREVO_ENDPOINT = URI.create("https://api.brevo.com/v3/smtp/email");

    @Value("${brevo.api-key:}")
    private String apiKey;

    @Value("${brevo.from-email:noreply@fisbu.app}")
    private String fromEmail;

    @Value("${brevo.from-name:FişBu}")
    private String fromName;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public EmailService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void sendVerificationCode(String toEmail, String code) {
        send(
                toEmail,
                "FişBu - E-posta Doğrulama Kodun",
                "<p>Merhaba,</p><p>FişBu hesabını doğrulamak için kodun: <b style=\"font-size:20px\">"
                        + code + "</b></p><p>Bu kod 15 dakika geçerlidir.</p>"
        );
    }

    public void sendPasswordResetCode(String toEmail, String code) {
        send(
                toEmail,
                "FişBu - Şifre Sıfırlama Kodun",
                "<p>Merhaba,</p><p>Şifreni sıfırlamak için kodun: <b style=\"font-size:20px\">"
                        + code + "</b></p><p>Bu kod 15 dakika geçerlidir. "
                        + "Bu isteği sen yapmadıysan bu e-postayı yok sayabilirsin.</p>"
        );
    }

    private void send(String toEmail, String subject, String htmlContent) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("BREVO_API_KEY tanımlı değil, e-posta gönderilmedi ({} -> {})", subject, toEmail);
            return;
        }
        try {
            Map<String, Object> payload = Map.of(
                    "sender", Map.of("name", fromName, "email", fromEmail),
                    "to", List.of(Map.of("email", toEmail)),
                    "subject", subject,
                    "htmlContent", htmlContent
            );
            String json = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(BREVO_ENDPOINT)
                    .header("api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                log.error("Brevo isteği başarısız: {} - {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("E-posta gönderilirken hata oluştu ({} -> {}): {}", subject, toEmail, e.getMessage());
        }
    }
}
