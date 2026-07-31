package com.fisbu.api.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fisbu.api.dto.RestoreReceiptRequest;
import com.fisbu.api.dto.RestoreReceiptResponse;
import com.fisbu.api.dto.SpendingAnalysisResponse;
import com.fisbu.api.dto.MonthlyStatisticsResponse;
import com.fisbu.api.entity.Category;
import com.fisbu.api.entity.User;
import com.fisbu.api.repository.CategoryRepository;
import com.fisbu.api.repository.UserRepository;

/**
 * Gün 12/13: yırtık fiş restorasyonu ve harcama analizi.
 * Groq'un döndürdüğü veriyi veritabanına yazmaz — sadece öneri döner,
 * kullanıcı mobil tarafta gözden geçirip mevcut POST /receipts ile kaydeder.
 */
@Service
public class ReceiptAiService {

    private static final Logger log = LoggerFactory.getLogger(ReceiptAiService.class);

    private final AiService aiService;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final StatisticsService statisticsService;
    private final ObjectMapper objectMapper;

    public ReceiptAiService(AiService aiService,
                             CategoryRepository categoryRepository,
                             UserRepository userRepository,
                             StatisticsService statisticsService,
                             ObjectMapper objectMapper) {
        this.aiService = aiService;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.statisticsService = statisticsService;
        this.objectMapper = objectMapper;
    }

    public RestoreReceiptResponse restoreReceipt(String email, RestoreReceiptRequest request) {
        User user = getUserByEmail(email);
        List<Category> categories = categoryRepository.findByUser(user);
        String categoryNames = categories.stream()
                .map(Category::getName)
                .collect(Collectors.joining(", "));

        String prompt = """
                Sen bir market/restoran fişi okuma asistanısın. Aşağıda OCR ile okunmuş, muhtemelen \
                hatalı veya eksik bir fiş metni var. Bu metinden bilgileri çıkar ve SADECE aşağıdaki \
                JSON formatında yanıt ver, başka hiçbir açıklama ekleme:

                {
                  "storeName": "mağaza adı (bulunamazsa null)",
                  "totalAmount": sayısal tutar (TL, ondalık ayraç nokta, bulunamazsa null),
                  "receiptDate": "yyyy-MM-dd formatında tarih (bulunamazsa null)",
                  "suggestedCategoryName": "şu listeden en uygun kategori adını seç: [%s] (hiçbiri uymuyorsa null)",
                  "confidenceScore": 0 ile 100 arasında tam sayı, verinin ne kadar güvenilir olduğunu belirtir
                }

                OCR metni:
                %s
                """.formatted(categoryNames, request.getRawOcrText());

        JsonNode root = parseJsonResponse(aiService.generateJson(prompt));

        RestoreReceiptResponse response = new RestoreReceiptResponse();
        response.setStoreName(textOrNull(root, "storeName"));
        response.setTotalAmount(decimalOrNull(root.get("totalAmount")));
        response.setReceiptDate(dateOrNull(textOrNull(root, "receiptDate")));
        response.setConfidenceScore(Math.max(0, Math.min(100, root.path("confidenceScore").asInt(0))));

        String suggestedCategoryName = textOrNull(root, "suggestedCategoryName");
        response.setSuggestedCategoryName(suggestedCategoryName);
        if (suggestedCategoryName != null) {
            categories.stream()
                    .filter(c -> c.getName().equalsIgnoreCase(suggestedCategoryName))
                    .findFirst()
                    .ifPresent(c -> response.setMatchedCategoryId(c.getId()));
        }

        return response;
    }

    public SpendingAnalysisResponse getSpendingAnalysis(String email, Integer year, Integer month) {
        MonthlyStatisticsResponse stats = statisticsService.getMonthlyStatistics(email, year, month);

        String breakdown = stats.getCategories().stream()
                .map(c -> "- " + c.getCategoryName() + ": " + c.getTotalAmount() + " TL")
                .collect(Collectors.joining("\n"));
        if (breakdown.isBlank()) {
            breakdown = "(bu ay hiç fiş eklenmemiş)";
        }

        String prompt = """
                Kullanıcının %d yılı %d ayı harcama özeti:
                Toplam: %s TL
                Kategori bazında:
                %s

                Bu verilere dayanarak kullanıcıya samimi, kısa (3-4 cümle), Türkçe bir harcama yorumu yaz. \
                En çok harcanan kategoriyi belirt, dikkat çekici bir gözlem yap ve varsa kısa bir tasarruf \
                önerisi ver. Sadece yorum metnini yaz, başlık veya madde işareti kullanma.
                """.formatted(stats.getYear(), stats.getMonth(), stats.getTotalAmount(), breakdown);

        String comment = aiService.generateText(prompt).trim();

        SpendingAnalysisResponse response = new SpendingAnalysisResponse();
        response.setYear(stats.getYear());
        response.setMonth(stats.getMonth());
        response.setTotalAmount(stats.getTotalAmount());
        response.setComment(comment);
        return response;
    }

    private JsonNode parseJsonResponse(String raw) {
        String cleaned = raw.trim();
        // Bazı modeller JSON modunda bile içeriği ```json ... ``` bloğuna sarabiliyor
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```[a-zA-Z]*", "").replaceFirst("```$", "").trim();
        }
        try {
            return objectMapper.readTree(cleaned);
        } catch (Exception e) {
            log.error("AI yanıtı JSON olarak ayrıştırılamadı: {}", raw);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI yanıtı işlenemedi");
        }
    }

    private String textOrNull(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull() || !node.isTextual()) {
            return null;
        }
        String text = node.asText().trim();
        return text.isEmpty() ? null : text;
    }

    private BigDecimal decimalOrNull(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        try {
            if (node.isNumber()) {
                return node.decimalValue();
            }
            return new BigDecimal(node.asText().replace(",", "."));
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDate dateOrNull(String text) {
        if (text == null) {
            return null;
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kullanıcı bulunamadı"));
    }
}
