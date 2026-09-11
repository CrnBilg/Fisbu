package com.fisbu.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fisbu.api.dto.CategoryTotalResponse;
import com.fisbu.api.dto.ImportedTransactionDto;
import com.fisbu.api.dto.MonthlyStatisticsResponse;
import com.fisbu.api.dto.RestoreReceiptRequest;
import com.fisbu.api.dto.RestoreReceiptResponse;
import com.fisbu.api.dto.SpendingAnalysisResponse;
import com.fisbu.api.entity.Category;
import com.fisbu.api.entity.User;
import com.fisbu.api.repository.CategoryRepository;
import com.fisbu.api.repository.UserRepository;

/**
 * TEST-001 — ReceiptAiService'in mevcut davranışını kilitleyen regresyon testleri.
 * Bu dosya ReceiptAiService.java'nın kodunu DEĞİŞTİRMEZ, sadece mevcut davranışı test eder.
 * AiService gerçek bir dış servise (Groq) bağlandığı için mock'lanır — gerçek AI çağrısı yapılmaz.
 */
@ExtendWith(MockitoExtension.class)
class ReceiptAiServiceTest {

    private static final String EMAIL = "test@fisbu.com";

    @Mock
    private AiService aiService;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private StatisticsService statisticsService;

    private ReceiptAiService receiptAiService;
    private User user;

    @BeforeEach
    void setUp() {
        receiptAiService = new ReceiptAiService(aiService, categoryRepository, userRepository,
                statisticsService, new ObjectMapper());
        user = new User();
        user.setId(1L);
        user.setEmail(EMAIL);
    }

    private Category category(Long id, String name) {
        Category c = new Category();
        c.setId(id);
        c.setName(name);
        return c;
    }

    // ---------- restoreReceipt ----------

    @Test
    void restoreReceipt_varOlmayanKullanici_404Firlatir() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        RestoreReceiptRequest request = new RestoreReceiptRequest();
        request.setRawOcrText("bazı ocr metni");

        assertThatThrownBy(() -> receiptAiService.restoreReceipt(EMAIL, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void restoreReceipt_gecerliJsonYanit_alanlariDoldurulmusResponseDoner() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        Category market = category(1L, "Market");
        when(categoryRepository.findByUser(user)).thenReturn(List.of(market));

        String aiJson = """
                {
                  "storeName": "A101",
                  "totalAmount": 125.50,
                  "receiptDate": "2024-05-10",
                  "suggestedCategoryName": "Market",
                  "confidenceScore": 90,
                  "items": [
                    {"productName": "Süt", "unitPrice": 20, "quantity": 2}
                  ]
                }
                """;
        when(aiService.generateJson(org.mockito.ArgumentMatchers.anyString())).thenReturn(aiJson);

        RestoreReceiptRequest request = new RestoreReceiptRequest();
        request.setRawOcrText("bazı ocr metni");

        RestoreReceiptResponse response = receiptAiService.restoreReceipt(EMAIL, request);

        assertThat(response.getStoreName()).isEqualTo("A101");
        assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("125.50"));
        assertThat(response.getReceiptDate()).isEqualTo(LocalDate.of(2024, 5, 10));
        assertThat(response.getSuggestedCategoryName()).isEqualTo("Market");
        assertThat(response.getMatchedCategoryId()).isEqualTo(1L);
        assertThat(response.getConfidenceScore()).isEqualTo(90); // 4/4 alan dolu -> %100 tavan, model 90 kalır
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getProductName()).isEqualTo("Süt");
        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(BigDecimal.valueOf(2));
    }

    @Test
    void restoreReceipt_tumAlanlarBosAmaModelYuksekGuvenVeriyor_guvenSkoruSifiraSinirlanir() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(categoryRepository.findByUser(user)).thenReturn(List.of());

        String aiJson = """
                {"storeName": null, "totalAmount": null, "receiptDate": null,
                 "suggestedCategoryName": null, "confidenceScore": 90, "items": []}
                """;
        when(aiService.generateJson(org.mockito.ArgumentMatchers.anyString())).thenReturn(aiJson);

        RestoreReceiptRequest request = new RestoreReceiptRequest();
        request.setRawOcrText("okunamayan metin");

        RestoreReceiptResponse response = receiptAiService.restoreReceipt(EMAIL, request);

        assertThat(response.getConfidenceScore()).isZero();
        assertThat(response.getItems()).isEmpty();
    }

    @Test
    void restoreReceipt_gecersizJsonYanit_502Firlatir() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(categoryRepository.findByUser(user)).thenReturn(List.of());
        when(aiService.generateJson(org.mockito.ArgumentMatchers.anyString())).thenReturn("bu json degil");

        RestoreReceiptRequest request = new RestoreReceiptRequest();
        request.setRawOcrText("ocr metni");

        assertThatThrownBy(() -> receiptAiService.restoreReceipt(EMAIL, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("502");
    }

    @Test
    void restoreReceipt_markdownKodBlogunaSarilmisJson_temizlenipAyristirilir() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(categoryRepository.findByUser(user)).thenReturn(List.of());

        String wrapped = "```json\n{\"storeName\": \"BIM\", \"totalAmount\": 50, \"receiptDate\": null, "
                + "\"suggestedCategoryName\": null, \"confidenceScore\": 50, \"items\": []}\n```";
        when(aiService.generateJson(org.mockito.ArgumentMatchers.anyString())).thenReturn(wrapped);

        RestoreReceiptRequest request = new RestoreReceiptRequest();
        request.setRawOcrText("ocr metni");

        RestoreReceiptResponse response = receiptAiService.restoreReceipt(EMAIL, request);

        assertThat(response.getStoreName()).isEqualTo("BIM");
    }

    @Test
    void restoreReceipt_quantitySifirGelirse_biraVarsayilanYapilir() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(categoryRepository.findByUser(user)).thenReturn(List.of());

        String aiJson = """
                {"storeName": null, "totalAmount": null, "receiptDate": null, "suggestedCategoryName": null,
                 "confidenceScore": 10,
                 "items": [{"productName": "Ekmek", "unitPrice": 5, "quantity": 0}]}
                """;
        when(aiService.generateJson(org.mockito.ArgumentMatchers.anyString())).thenReturn(aiJson);

        RestoreReceiptRequest request = new RestoreReceiptRequest();
        request.setRawOcrText("ocr metni");

        RestoreReceiptResponse response = receiptAiService.restoreReceipt(EMAIL, request);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(BigDecimal.ONE);
    }

    // ---------- extractTransactions ----------

    @Test
    void extractTransactions_varOlmayanKullanici_404Firlatir() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> receiptAiService.extractTransactions(EMAIL, "ekstre metni"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void extractTransactions_gecerliIslemler_dtoListesineDonusturulur() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        Category market = category(1L, "Market");
        when(categoryRepository.findByUser(user)).thenReturn(List.of(market));

        String aiJson = """
                {"transactions": [
                  {"date": "2024-05-01", "description": "A101", "amount": 100.0,
                   "suggestedCategoryName": "Market", "confidenceScore": 80},
                  {"date": "2024-05-02", "description": "Bilinmeyen", "amount": null,
                   "suggestedCategoryName": null, "confidenceScore": 10}
                ]}
                """;
        when(aiService.generateJson(org.mockito.ArgumentMatchers.anyString())).thenReturn(aiJson);

        List<ImportedTransactionDto> transactions = receiptAiService.extractTransactions(EMAIL, "ekstre metni");

        // amount null olan işlem atlanır
        assertThat(transactions).hasSize(1);
        ImportedTransactionDto dto = transactions.get(0);
        assertThat(dto.getDescription()).isEqualTo("A101");
        assertThat(dto.getAmount()).isEqualTo(BigDecimal.valueOf(100.0));
        assertThat(dto.getMatchedCategoryId()).isEqualTo(1L);
        assertThat(dto.getConfidenceScore()).isEqualTo(80);
    }

    @Test
    void extractTransactions_transactionsAlaniYoksaVeyaDiziDegilse_bosListeDoner() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(categoryRepository.findByUser(user)).thenReturn(List.of());
        when(aiService.generateJson(org.mockito.ArgumentMatchers.anyString())).thenReturn("{}");

        List<ImportedTransactionDto> transactions = receiptAiService.extractTransactions(EMAIL, "ekstre metni");

        assertThat(transactions).isEmpty();
    }

    @Test
    void extractTransactions_kategoriKismenEslesirse_yakinKategoriBulunur() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        Category restoranlar = category(2L, "Restoranlar");
        when(categoryRepository.findByUser(user)).thenReturn(List.of(restoranlar));

        String aiJson = """
                {"transactions": [
                  {"date": "2024-05-01", "description": "Lokanta", "amount": 40.0,
                   "suggestedCategoryName": "Restoran", "confidenceScore": 60}
                ]}
                """;
        when(aiService.generateJson(org.mockito.ArgumentMatchers.anyString())).thenReturn(aiJson);

        List<ImportedTransactionDto> transactions = receiptAiService.extractTransactions(EMAIL, "ekstre metni");

        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getMatchedCategoryId()).isEqualTo(2L);
    }

    // ---------- getSpendingAnalysis ----------

    @Test
    void getSpendingAnalysis_istatistikleriKullanirVeYorumDoner() {
        MonthlyStatisticsResponse stats = new MonthlyStatisticsResponse();
        stats.setYear(2024);
        stats.setMonth(5);
        stats.setTotalAmount(BigDecimal.valueOf(300));
        CategoryTotalResponse cat = new CategoryTotalResponse();
        cat.setCategoryName("Market");
        cat.setTotalAmount(BigDecimal.valueOf(300));
        stats.setCategories(List.of(cat));

        when(statisticsService.getMonthlyStatistics(EMAIL, 2024, 5)).thenReturn(stats);
        when(aiService.generateText(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn("  En çok market kategorisinde harcadın.  ");

        SpendingAnalysisResponse response = receiptAiService.getSpendingAnalysis(EMAIL, 2024, 5);

        assertThat(response.getYear()).isEqualTo(2024);
        assertThat(response.getMonth()).isEqualTo(5);
        assertThat(response.getTotalAmount()).isEqualTo(BigDecimal.valueOf(300));
        assertThat(response.getComment()).isEqualTo("En çok market kategorisinde harcadın.");
    }

    @Test
    void getSpendingAnalysis_hicKategoriYok_bosAyMesajiPromptaEklenir() {
        MonthlyStatisticsResponse stats = new MonthlyStatisticsResponse();
        stats.setYear(2024);
        stats.setMonth(6);
        stats.setTotalAmount(BigDecimal.ZERO);
        stats.setCategories(List.of());

        when(statisticsService.getMonthlyStatistics(EMAIL, 2024, 6)).thenReturn(stats);
        when(aiService.generateText(org.mockito.ArgumentMatchers.anyString())).thenReturn("Bu ay hiç harcama yok.");

        SpendingAnalysisResponse response = receiptAiService.getSpendingAnalysis(EMAIL, 2024, 6);

        assertThat(response.getComment()).isEqualTo("Bu ay hiç harcama yok.");
        assertThat(response.getTotalAmount()).isEqualTo(BigDecimal.ZERO);
    }
}
