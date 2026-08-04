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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Groq'un OpenAI-uyumlu chat completions API'sini kullanır (ücretsiz, kredi kartı gerektirmiyor).
 * Gün 12/13'teki fiş restorasyonu ve harcama analizi endpoint'leri bu servisi kullanacak.
 */
@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);
    private static final URI GROQ_ENDPOINT = URI.create("https://api.groq.com/openai/v1/chat/completions");

    @Value("${groq.api-key:}")
    private String apiKey;

    @Value("${groq.model:llama-3.3-70b-versatile}")
    private String model;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public AiService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /** Düz metin prompt gönderir, modelin ürettiği metni döner. */
    public String generateText(String prompt) {
        return chat(List.of(Map.of("role", "user", "content", prompt)), model, false);
    }

    /** Prompt gönderir, modelin ürettiği metni katı JSON olarak döndürmesini zorlar. */
    public String generateJson(String prompt) {
        return chat(List.of(Map.of("role", "user", "content", prompt)), model, true);
    }

    /** Sistem promptu + çok turlu konuşma geçmişiyle sohbet (finansal asistan gibi akışlar için). */
    public String chatConversation(List<Map<String, Object>> messages) {
        return chat(messages, model, false);
    }

    private String chat(List<Map<String, Object>> messages, String modelName, boolean jsonMode) {
        if (!isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI servisi henüz yapılandırılmadı");
        }

        try {
            // Düşük temperature: tutarlı çıktı, model diller arası karışma (code-switching) yapma riskini azaltır
            Map<String, Object> payload = jsonMode
                    ? Map.of(
                            "model", modelName,
                            "messages", messages,
                            "temperature", 0.3,
                            "response_format", Map.of("type", "json_object"))
                    : Map.of(
                            "model", modelName,
                            "messages", messages,
                            "temperature", 0.3
                    );
            String json = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(GROQ_ENDPOINT)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 300) {
                log.error("Groq isteği başarısız: {} - {}", response.statusCode(), response.body());
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI isteği başarısız oldu");
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode contentNode = root.path("choices").path(0).path("message").path("content");

            if (contentNode.isMissingNode()) {
                log.error("Groq yanıtı beklenmedik formatta: {}", response.body());
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI yanıtı işlenemedi");
            }

            return contentNode.asText();
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Groq isteği sırasında hata oluştu: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI isteği sırasında hata oluştu");
        }
    }
}
