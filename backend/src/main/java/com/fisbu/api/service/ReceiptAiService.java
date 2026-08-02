package com.fisbu.api.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fisbu.api.dto.ImportedTransactionDto;
import com.fisbu.api.dto.LineItemDto;
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
                  "confidenceScore": 0 ile 100 arasında tam sayı, verinin ne kadar güvenilir olduğunu belirtir,
                  "items": [
                    {"productName": "ürün adı", "unitPrice": birim fiyat (sayı), "quantity": adet (sayı, bulunamazsa 1)}
                  ]
                }
                Fişte tek tek ürün satırları okunabiliyorsa "items" dizisini doldur, okunamıyorsa boş dizi ver.
                KDV/toplam/indirim/ödeme satırlarını ürün sayma.

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
        response.setMatchedCategoryId(matchCategory(suggestedCategoryName, categories));

        response.setItems(parseItems(root.get("items")));

        return response;
    }

    /**
     * Banka/kredi kartı ekstresi veya genel harcama listesi metninden harcama işlemlerini çıkarır.
     * Ekstre içe aktarma akışında hem PDF hem de bilinmeyen formatlı CSV metinleri için kullanılır.
     */
    public List<ImportedTransactionDto> extractTransactions(String email, String rawText) {
        User user = getUserByEmail(email);
        List<Category> categories = categoryRepository.findByUser(user);
        String categoryNames = categories.stream()
                .map(Category::getName)
                .collect(Collectors.joining(", "));

        String prompt = """
                Sen bir banka ekstresi / harcama listesi ayrıştırma asistanısın. Aşağıda bir banka veya \
                kredi kartı ekstresinden ya da genel bir harcama listesinden çıkarılmış ham metin var. \
                Bu metindeki HARCAMA (gider) işlemlerini çıkar. Ödeme, tahsilat, kredi kartı ödemesi, \
                faiz iadesi, nakit avans, EFT/havale gelen para gibi harcama OLMAYAN satırları YOKSAY. \
                Sadece gerçek harcama/alışveriş işlemlerini pozitif tutar olarak döndür. SADECE aşağıdaki \
                JSON formatında yanıt ver, başka hiçbir açıklama ekleme:

                {
                  "transactions": [
                    {
                      "date": "yyyy-MM-dd formatında tarih (bulunamazsa null)",
                      "description": "işlem açıklaması / mağaza adı",
                      "amount": sayısal tutar (pozitif, TL, ondalık ayraç nokta),
                      "suggestedCategoryName": "şu listeden en uygun kategori adını seç: [%s] (hiçbiri uymuyorsa null)",
                      "confidenceScore": 0 ile 100 arasında tam sayı, verinin ne kadar güvenilir olduğunu belirtir
                    }
                  ]
                }
                Bir satırı harcama olarak tanımlayamıyorsan listeye ekleme.

                Metin:
                %s
                """.formatted(categoryNames, rawText);

        JsonNode root = parseJsonResponse(aiService.generateJson(prompt));
        return parseTransactions(root.get("transactions"), categories);
    }

    private List<ImportedTransactionDto> parseTransactions(JsonNode transactionsNode, List<Category> categories) {
        List<ImportedTransactionDto> transactions = new ArrayList<>();
        if (transactionsNode == null || !transactionsNode.isArray()) {
            return transactions;
        }
        for (JsonNode node : transactionsNode) {
            BigDecimal amount = decimalOrNull(node.get("amount"));
            if (amount == null) {
                continue;
            }
            ImportedTransactionDto dto = new ImportedTransactionDto();
            dto.setDate(dateOrNull(textOrNull(node, "date")));
            dto.setDescription(textOrNull(node, "description"));
            dto.setAmount(amount);
            String suggestedCategoryName = textOrNull(node, "suggestedCategoryName");
            dto.setSuggestedCategoryName(suggestedCategoryName);
            Long matchedCategoryId = matchCategory(suggestedCategoryName, categories);
            if (matchedCategoryId != null) {
                dto.setMatchedCategoryId(matchedCategoryId);
            }
            dto.setConfidenceScore(Math.max(0, Math.min(100, node.path("confidenceScore").asInt(0))));
            transactions.add(dto);
        }
        return transactions;
    }

    private Long matchCategory(String suggestedCategoryName, List<Category> categories) {
        if (suggestedCategoryName == null) {
            return null;
        }
        return categories.stream()
                .filter(c -> c.getName().equalsIgnoreCase(suggestedCategoryName))
                .findFirst()
                .map(Category::getId)
                .orElse(null);
    }

    private List<LineItemDto> parseItems(JsonNode itemsNode) {
        List<LineItemDto> items = new ArrayList<>();
        if (itemsNode == null || !itemsNode.isArray()) {
            return items;
        }
        for (JsonNode itemNode : itemsNode) {
            String productName = textOrNull(itemNode, "productName");
            BigDecimal unitPrice = decimalOrNull(itemNode.get("unitPrice"));
            if (productName == null || unitPrice == null) {
                continue;
            }
            LineItemDto item = new LineItemDto();
            item.setProductName(productName);
            item.setUnitPrice(unitPrice);
            BigDecimal quantity = decimalOrNull(itemNode.get("quantity"));
            item.setQuantity(quantity != null ? quantity : BigDecimal.ONE);
            items.add(item);
        }
        return items;
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

                ÖNEMLİ: Yanıtın SADECE Türkçe olmalı — İngilizce, Çince veya başka herhangi bir dilden \
                tek bir kelime ya da karakter bile kullanma (örn. "yıl" yerine "year" yazma). Ayrıca \
                birbiriyle çelişen iki gözlem yazma (örn. bir kategori harcamanın yarısını oluşturuyorsa \
                bunu hem "dikkat çekici" hem "dengesizlik yok" diye sunma).
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
