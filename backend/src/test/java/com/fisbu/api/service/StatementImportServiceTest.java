package com.fisbu.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.fisbu.api.dto.ImportedTransactionDto;
import com.fisbu.api.dto.ParsedStatementResponse;
import com.fisbu.api.entity.Category;
import com.fisbu.api.entity.User;
import com.fisbu.api.repository.CategoryRepository;
import com.fisbu.api.repository.UserRepository;

/**
 * TEST-002 — StatementImportService'in mevcut davranışını kilitleyen regresyon testleri.
 * SEC-003 deseniyle tutarlı: bu dosya StatementImportService.java'nın kodunu DEĞİŞTİRMEZ,
 * sadece mevcut davranışı test eder.
 */
@ExtendWith(MockitoExtension.class)
class StatementImportServiceTest {

    private static final String EMAIL = "test@fisbu.com";

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ReceiptAiService receiptAiService;

    private StatementImportService service;

    @BeforeEach
    void setUp() {
        service = new StatementImportService(categoryRepository, userRepository, receiptAiService);
    }

    private User user() {
        User user = new User();
        user.setId(1L);
        user.setEmail(EMAIL);
        return user;
    }

    private Category category(Long id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        return category;
    }

    private void stubUserFound() {
        lenient().when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));
    }

    // ---------- parseStatement: genel dosya doğrulama ----------

    @Test
    void parseStatement_dosyaNull_400Doner() {
        assertThatThrownBy(() -> service.parseStatement(EMAIL, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400")
                .hasMessageContaining("Dosya boş olamaz");
    }

    @Test
    void parseStatement_bosDosya_400Doner() {
        MultipartFile file = new MockMultipartFile("file", "ekstre.csv", "text/csv", new byte[0]);

        assertThatThrownBy(() -> service.parseStatement(EMAIL, file))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400")
                .hasMessageContaining("Dosya boş olamaz");
    }

    @Test
    void parseStatement_desteklenmeyenUzanti_400Doner() {
        MultipartFile file = new MockMultipartFile("file", "ekstre.txt", "text/plain",
                "herhangi bir metin".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.parseStatement(EMAIL, file))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400")
                .hasMessageContaining("Desteklenmeyen dosya türü");
    }

    @Test
    void parseStatement_uzantisizDosyaAdi_400Doner() {
        MultipartFile file = new MockMultipartFile("file", null, "text/plain",
                "herhangi bir metin".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.parseStatement(EMAIL, file))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");
    }

    // ---------- parseStatement: uygulamanın kendi CSV formatı ----------

    @Test
    void parseStatement_uygulamaCsvFormati_deterministikOlarakEslesirVeAiCagirmaz() {
        stubUserFound();
        when(categoryRepository.findByUser(any(User.class)))
                .thenReturn(List.of(category(10L, "Market")));

        String csv = "Mağaza,Tutar (TL),Tarih,Kategori\r\n"
                + "Migros,150.50,01.03.2024,Market\r\n"
                + "TOPLAM,150.50,,\r\n";
        MultipartFile file = new MockMultipartFile("file", "ekstre.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));

        ParsedStatementResponse response = service.parseStatement(EMAIL, file);

        assertThat(response.getSourceType()).isEqualTo("APP_CSV");
        assertThat(response.getTransactions()).hasSize(1);
        ImportedTransactionDto dto = response.getTransactions().get(0);
        assertThat(dto.getDescription()).isEqualTo("Migros");
        assertThat(dto.getAmount()).isEqualTo(new BigDecimal("150.50"));
        assertThat(dto.getDate()).isEqualTo(LocalDate.of(2024, 3, 1));
        assertThat(dto.getSuggestedCategoryName()).isEqualTo("Market");
        assertThat(dto.getMatchedCategoryId()).isEqualTo(10L);
        assertThat(dto.getConfidenceScore()).isEqualTo(100);
        assertThat(response.getWarnings()).isEmpty();

        verify(receiptAiService, never()).extractTransactions(any(), any());
    }

    @Test
    void parseStatement_uygulamaCsvFormati_UTF8BomIleBasliyorsaDaDogruParseEder() {
        stubUserFound();
        when(categoryRepository.findByUser(any(User.class))).thenReturn(List.of());

        String csv = "﻿" + "Mağaza,Tutar (TL),Tarih,Kategori\r\n"
                + "Şok,20,05.01.2024,\r\n";
        MultipartFile file = new MockMultipartFile("file", "ekstre.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));

        ParsedStatementResponse response = service.parseStatement(EMAIL, file);

        assertThat(response.getSourceType()).isEqualTo("APP_CSV");
        assertThat(response.getTransactions()).hasSize(1);
        assertThat(response.getTransactions().get(0).getDescription()).isEqualTo("Şok");
    }

    @Test
    void parseStatement_uygulamaCsvFormati_TOPLAMSatiriIslemOlarakEklenmez() {
        stubUserFound();
        when(categoryRepository.findByUser(any(User.class))).thenReturn(List.of());

        String csv = "Mağaza,Tutar (TL),Tarih,Kategori\r\n"
                + "A101,30,10.02.2024,\r\n"
                + "TOPLAM,30,,\r\n";
        MultipartFile file = new MockMultipartFile("file", "ekstre.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));

        ParsedStatementResponse response = service.parseStatement(EMAIL, file);

        assertThat(response.getTransactions()).hasSize(1);
        assertThat(response.getTransactions().get(0).getDescription()).isEqualTo("A101");
    }

    @Test
    void parseStatement_uygulamaCsvFormati_gecersizTutarSatiriAtlanir() {
        stubUserFound();
        when(categoryRepository.findByUser(any(User.class))).thenReturn(List.of());

        String csv = "Mağaza,Tutar (TL),Tarih,Kategori\r\n"
                + "GecersizSatir,abc,10.02.2024,\r\n"
                + "GecerliSatir,15,10.02.2024,\r\n";
        MultipartFile file = new MockMultipartFile("file", "ekstre.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));

        ParsedStatementResponse response = service.parseStatement(EMAIL, file);

        assertThat(response.getTransactions()).hasSize(1);
        assertThat(response.getTransactions().get(0).getDescription()).isEqualTo("GecerliSatir");
    }

    @Test
    void parseStatement_uygulamaCsvFormati_kullaniciBulunamazsa404Doner() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        String csv = "Mağaza,Tutar (TL),Tarih,Kategori\r\n"
                + "Migros,150.50,01.03.2024,Market\r\n";
        MultipartFile file = new MockMultipartFile("file", "ekstre.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.parseStatement(EMAIL, file))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    // ---------- parseStatement: bilinmeyen formatlı CSV -> AI ----------

    @Test
    void parseStatement_bilinmeyenCsvFormati_AiIleParseEder() {
        List<ImportedTransactionDto> aiTransactions = List.of(new ImportedTransactionDto());
        when(receiptAiService.extractTransactions(eq(EMAIL), any())).thenReturn(aiTransactions);

        String csv = "Tarih;Aciklama;Tutar\r\n05/01/2024;Migros;150,50\r\n";
        MultipartFile file = new MockMultipartFile("file", "banka_ekstresi.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));

        ParsedStatementResponse response = service.parseStatement(EMAIL, file);

        assertThat(response.getSourceType()).isEqualTo("AI_EXTRACTED");
        assertThat(response.getTransactions()).isEqualTo(aiTransactions);
        assertThat(response.getWarnings()).isEmpty();
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void parseStatement_bilinmeyenCsvFormati_AiHicIslemBulamazsaUyariEkler() {
        when(receiptAiService.extractTransactions(eq(EMAIL), any())).thenReturn(List.of());

        String csv = "rastgele,baslik,satiri\r\nveri1,veri2,veri3\r\n";
        MultipartFile file = new MockMultipartFile("file", "banka_ekstresi.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));

        ParsedStatementResponse response = service.parseStatement(EMAIL, file);

        assertThat(response.getTransactions()).isEmpty();
        assertThat(response.getWarnings())
                .containsExactly("Dosyada harcama olarak tanınan bir işlem bulunamadı.");
    }

    @Test
    void parseStatement_cokUzunCsvMetni_AiIcinKirpilirVeUyariEklenir() {
        when(receiptAiService.extractTransactions(eq(EMAIL), any())).thenReturn(List.of());

        String longText = "rastgele,baslik,satiri\r\n" + "a".repeat(9000);
        MultipartFile file = new MockMultipartFile("file", "banka_ekstresi.csv", "text/csv",
                longText.getBytes(StandardCharsets.UTF_8));

        ParsedStatementResponse response = service.parseStatement(EMAIL, file);

        verify(receiptAiService).extractTransactions(eq(EMAIL), argThatLengthIs(8000));
        assertThat(response.getWarnings())
                .anyMatch(w -> w.contains("çok uzun"));
    }

    private String argThatLengthIs(int length) {
        return org.mockito.ArgumentMatchers.argThat(s -> s != null && s.length() == length);
    }

    // ---------- parseStatement: PDF ----------

    @Test
    void parseStatement_pdfMagicByteYok_400Doner() {
        MultipartFile file = new MockMultipartFile("file", "sahte.pdf", "application/pdf",
                "bu bir pdf degil".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.parseStatement(EMAIL, file))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400")
                .hasMessageContaining("geçerli bir PDF değil");

        verify(receiptAiService, never()).extractTransactions(any(), any());
    }

    @Test
    void parseStatement_bozukPdfMagicByteVarAmaGecersizIcerik_422Doner() throws Exception {
        // "%PDF" ile başlıyor (magic-byte kontrolünü geçer) ama gerçek bir PDF yapısı değil
        byte[] content = "%PDF-fake-content-not-a-real-pdf-structure".getBytes(StandardCharsets.UTF_8);
        MultipartFile file = new MockMultipartFile("file", "bozuk.pdf", "application/pdf", content);

        assertThatThrownBy(() -> service.parseStatement(EMAIL, file))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("422")
                .hasMessageContaining("PDF dosyası okunamadı");
    }

    @Test
    void parseStatement_gecerliMetinIcerenPdf_AiIleParseEder() throws Exception {
        List<ImportedTransactionDto> aiTransactions = List.of(new ImportedTransactionDto());
        when(receiptAiService.extractTransactions(eq(EMAIL), any())).thenReturn(aiTransactions);

        byte[] pdfBytes = buildSimplePdfWithText("Migros harcama 150.50 TL");
        MultipartFile file = new MockMultipartFile("file", "ekstre.pdf", "application/pdf", pdfBytes);

        ParsedStatementResponse response = service.parseStatement(EMAIL, file);

        assertThat(response.getSourceType()).isEqualTo("AI_EXTRACTED");
        assertThat(response.getTransactions()).isEqualTo(aiTransactions);
    }

    @Test
    void parseStatement_metinIcermeyenBosPdf_422Doner() throws Exception {
        byte[] pdfBytes = buildEmptyPdf();
        MultipartFile file = new MockMultipartFile("file", "bos.pdf", "application/pdf", pdfBytes);

        assertThatThrownBy(() -> service.parseStatement(EMAIL, file))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("422")
                .hasMessageContaining("Dosyadan metin okunamadı");

        verify(receiptAiService, never()).extractTransactions(any(), any());
    }

    private byte[] buildSimplePdfWithText(String text) throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText(text);
                contentStream.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    private byte[] buildEmptyPdf() throws Exception {
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }
}
