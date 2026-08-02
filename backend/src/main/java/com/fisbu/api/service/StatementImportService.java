package com.fisbu.api.service;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.fisbu.api.dto.ImportedTransactionDto;
import com.fisbu.api.dto.ParsedStatementResponse;
import com.fisbu.api.entity.Category;
import com.fisbu.api.entity.User;
import com.fisbu.api.repository.CategoryRepository;
import com.fisbu.api.repository.UserRepository;

/**
 * Kullanıcının yüklediği banka/kredi kartı ekstresi (PDF/CSV) ya da uygulamanın
 * kendi CSV export formatındaki dosyaları harcama önerisi listesine dönüştürür.
 * Hiçbir şey veritabanına yazmaz — kullanıcı mobil tarafta gözden geçirip
 * POST /receipts/import/confirm ile onayladıklarını kaydeder.
 */
@Service
public class StatementImportService {

    private static final Logger log = LoggerFactory.getLogger(StatementImportService.class);
    private static final int MAX_AI_TEXT_LENGTH = 8000;

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ReceiptAiService receiptAiService;

    public StatementImportService(CategoryRepository categoryRepository,
                                   UserRepository userRepository,
                                   ReceiptAiService receiptAiService) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.receiptAiService = receiptAiService;
    }

    public ParsedStatementResponse parseStatement(String email, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dosya boş olamaz");
        }

        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase(Locale.ROOT) : "";
        List<String> warnings = new ArrayList<>();

        if (filename.endsWith(".csv")) {
            String text = readAsText(file);
            ParsedStatementResponse appCsvResult = tryParseAppCsv(email, text);
            if (appCsvResult != null) {
                return appCsvResult;
            }
            return parseWithAi(email, text, warnings);
        }

        if (filename.endsWith(".pdf")) {
            String text = extractPdfText(file);
            if (text.isBlank()) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Dosyadan metin okunamadı. Lütfen metin tabanlı bir PDF ekstre yükleyin.");
            }
            return parseWithAi(email, text, warnings);
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Desteklenmeyen dosya türü. Lütfen PDF veya CSV yükleyin.");
    }

    /** Uygulamanın kendi export CSV formatı (Mağaza,Tutar (TL),Tarih,Kategori) ile eşleşiyorsa deterministik parse eder. */
    private ParsedStatementResponse tryParseAppCsv(String email, String text) {
        String withoutBom = text.startsWith("﻿") ? text.substring(1) : text;

        try (CSVParser parser = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .build()
                .parse(new StringReader(withoutBom))) {

            List<String> actualHeaders = parser.getHeaderNames();
            if (actualHeaders.size() != ExportService.HEADERS.length) {
                return null;
            }
            for (int i = 0; i < ExportService.HEADERS.length; i++) {
                if (!ExportService.HEADERS[i].equals(actualHeaders.get(i))) {
                    return null;
                }
            }

            User user = getUserByEmail(email);
            List<Category> categories = categoryRepository.findByUser(user);

            List<ImportedTransactionDto> transactions = new ArrayList<>();
            for (CSVRecord record : parser) {
                String storeName = record.get("Mağaza");
                if ("TOPLAM".equals(storeName)) {
                    continue;
                }
                BigDecimal amount = parseAmount(record.get("Tutar (TL)"));
                if (amount == null) {
                    continue;
                }
                ImportedTransactionDto dto = new ImportedTransactionDto();
                dto.setDescription(storeName);
                dto.setAmount(amount);
                dto.setDate(parseAppCsvDate(record.get("Tarih")));
                String categoryName = record.get("Kategori");
                if (categoryName != null && !categoryName.isBlank()) {
                    dto.setSuggestedCategoryName(categoryName);
                    categories.stream()
                            .filter(c -> c.getName().equalsIgnoreCase(categoryName))
                            .findFirst()
                            .ifPresent(c -> dto.setMatchedCategoryId(c.getId()));
                }
                dto.setConfidenceScore(100);
                transactions.add(dto);
            }

            ParsedStatementResponse response = new ParsedStatementResponse();
            response.setSourceType("APP_CSV");
            response.setTransactions(transactions);
            response.setWarnings(List.of());
            return response;
        } catch (IOException e) {
            return null;
        }
    }

    private ParsedStatementResponse parseWithAi(String email, String rawText, List<String> warnings) {
        String textForAi = rawText;
        if (textForAi.length() > MAX_AI_TEXT_LENGTH) {
            textForAi = textForAi.substring(0, MAX_AI_TEXT_LENGTH);
            warnings.add("Ekstre metni çok uzun olduğu için yalnızca ilk " + MAX_AI_TEXT_LENGTH
                    + " karakter analiz edildi. Bazı işlemler eksik olabilir.");
        }

        List<ImportedTransactionDto> transactions = receiptAiService.extractTransactions(email, textForAi);
        if (transactions.isEmpty()) {
            warnings.add("Dosyada harcama olarak tanınan bir işlem bulunamadı.");
        }

        ParsedStatementResponse response = new ParsedStatementResponse();
        response.setSourceType("AI_EXTRACTED");
        response.setTransactions(transactions);
        response.setWarnings(warnings);
        return response;
    }

    private String extractPdfText(MultipartFile file) {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            return new PDFTextStripper().getText(document);
        } catch (IOException e) {
            log.error("PDF metni çıkarılamadı: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "PDF dosyası okunamadı. Dosyanın bozuk olmadığından emin olun.");
        }
    }

    private String readAsText(MultipartFile file) {
        try {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dosya okunamadı");
        }
    }

    private BigDecimal parseAmount(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(text.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDate parseAppCsvDate(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(text.trim(), ExportService.DATE_FORMAT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kullanıcı bulunamadı"));
    }
}
