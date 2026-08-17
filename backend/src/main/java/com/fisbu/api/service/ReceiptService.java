package com.fisbu.api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fisbu.api.budget.application.port.in.CheckBudgetThresholdUseCase;
import com.fisbu.api.dto.BulkImportError;
import com.fisbu.api.dto.BulkReceiptImportResponse;
import com.fisbu.api.dto.CategorySuggestionResponse;
import com.fisbu.api.dto.PageResponse;
import com.fisbu.api.dto.ReceiptItemRequest;
import com.fisbu.api.dto.ReceiptItemResponse;
import com.fisbu.api.dto.ReceiptRequest;
import com.fisbu.api.dto.ReceiptResponse;
import com.fisbu.api.dto.SaveSplitRequest;
import com.fisbu.api.dto.SetReceiptRemindersRequest;
import com.fisbu.api.dto.SplitParticipantDto;
import com.fisbu.api.entity.Category;
import com.fisbu.api.entity.Receipt;
import com.fisbu.api.entity.ReceiptItem;
import com.fisbu.api.entity.User;
import com.fisbu.api.repository.CategoryRepository;
import com.fisbu.api.repository.ReceiptRepository;
import com.fisbu.api.repository.ReceiptSpecifications;
import com.fisbu.api.repository.UserRepository;

@Service
public class ReceiptService {

    private static final Logger log = LoggerFactory.getLogger(ReceiptService.class);

    // GET /receipts gerçek sayfalamaya geçene kadar (mobil taraf hâlâ tüm listeyi tek
    // seferde bekliyor) sınırsız büyümeyi önlemek için üst sınır — normal kullanım için yeterince geniş
    private static final int MAX_RECEIPTS_LIST = 2000;

    private final ReceiptRepository receiptRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final CheckBudgetThresholdUseCase checkBudgetThresholdUseCase;
    private final ExportService exportService;
    private final ObjectMapper objectMapper;

    public ReceiptService(ReceiptRepository receiptRepository,
                          UserRepository userRepository,
                          CategoryRepository categoryRepository,
                          CheckBudgetThresholdUseCase checkBudgetThresholdUseCase,
                          ExportService exportService,
                          ObjectMapper objectMapper) {
        this.receiptRepository = receiptRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.checkBudgetThresholdUseCase = checkBudgetThresholdUseCase;
        this.exportService = exportService;
        this.objectMapper = objectMapper;
    }

    // Kullanıcının fişlerini listeler (en yeni önce, MAX_RECEIPTS_LIST ile sınırlı)
    public List<ReceiptResponse> getReceipts(String email) {
        User user = getUserByEmail(email);
        Pageable pageable = PageRequest.of(0, MAX_RECEIPTS_LIST,
                Sort.by(Sort.Direction.DESC, "receiptDate").and(Sort.by(Sort.Direction.DESC, "id")));
        return receiptRepository.findByUser(user, pageable)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // Fiş listesini mağaza adına/kategoriye göre filtreleyip sayfalı döner (fiş listesi ekranı)
    public PageResponse<ReceiptResponse> searchReceipts(String email, String query, Long categoryId,
                                                          boolean uncategorized, int page, int size) {
        User user = getUserByEmail(email);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Direction.DESC, "receiptDate").and(Sort.by(Sort.Direction.DESC, "id")));

        Specification<Receipt> spec = ReceiptSpecifications.hasUser(user);
        if (query != null && !query.isBlank()) {
            spec = spec.and(ReceiptSpecifications.storeNameContains(query.trim()));
        }
        if (uncategorized) {
            spec = spec.and(ReceiptSpecifications.isUncategorized());
        } else if (categoryId != null) {
            spec = spec.and(ReceiptSpecifications.hasCategoryId(categoryId));
        }

        Page<Receipt> result = receiptRepository.findAll(spec, pageable);
        return PageResponse.from(result, this::toResponse);
    }

    // Elle fiş eklerken mağaza adına göre kategori önerir — bu mağazadan daha önce
    // eklenmiş fişlerde en sık kullanılan kategoriyi döner (AI çağrısı gerektirmez)
    public CategorySuggestionResponse suggestCategory(String email, String storeName) {
        if (storeName == null || storeName.trim().length() < 2) {
            return null;
        }
        User user = getUserByEmail(email);

        Specification<Receipt> spec = ReceiptSpecifications.hasUser(user)
                .and(ReceiptSpecifications.storeNameContains(storeName.trim()));
        List<Receipt> matches = receiptRepository.findAll(spec);

        Map<Long, Category> categoryById = new java.util.HashMap<>();
        Map<Long, Long> countsByCategoryId = new java.util.HashMap<>();
        for (Receipt receipt : matches) {
            Category category = receipt.getCategory();
            if (category == null) {
                continue;
            }
            categoryById.putIfAbsent(category.getId(), category);
            countsByCategoryId.merge(category.getId(), 1L, Long::sum);
        }

        return countsByCategoryId.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> categoryById.get(entry.getKey()))
                .map(category -> new CategorySuggestionResponse(category.getId(), category.getName()))
                .orElse(null);
    }

    // Yeni fiş ekler
    public ReceiptResponse createReceipt(String email, ReceiptRequest request) {
        User user = getUserByEmail(email);

        // Aynı mağaza+tutar+tarih ile daha önce eklenmiş bir fiş varsa, kullanıcı
        // "yine de ekle" demediyse (allowDuplicate) yanlışlıkla tekrar eklemeyi önle
        if (!request.isAllowDuplicate()
                && request.getStoreName() != null
                && request.getTotalAmount() != null
                && request.getReceiptDate() != null) {
            List<Receipt> duplicates = receiptRepository
                    .findByUserAndStoreNameIgnoreCaseAndTotalAmountAndReceiptDate(
                            user, request.getStoreName().trim(), request.getTotalAmount(), request.getReceiptDate());
            if (!duplicates.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Bu fiş zaten kayıtlı görünüyor (aynı mağaza, tutar ve tarih)");
            }
        }

        Receipt receipt = new Receipt();
        receipt.setUser(user);
        receipt.setStoreName(request.getStoreName());
        receipt.setTotalAmount(request.getTotalAmount());
        receipt.setReceiptDate(request.getReceiptDate());
        receipt.setImageUrl(request.getImageUrl());
        receipt.setRawOcrText(request.getRawOcrText());
        receipt.setReturnDeadline(request.getReturnDeadline());
        receipt.setWarrantyExpiryDate(request.getWarrantyExpiryDate());

        // Kategori opsiyonel — gönderilmişse set et
        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Kategori bulunamadı"));

            if (!category.getUser().getId().equals(user.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bu kategoriye erişim yetkiniz yok");
            }

            receipt.setCategory(category);
        }

        // Kaydetmeden önce, aynı kategorideki geçmiş fişlere göre bu tutar aykırı mı bak
        // (sadece bilgilendirici — engellemez, 409 duplicate uyarısından farklı olarak)
        String anomalyWarning = category != null
                ? detectAmountAnomaly(category, request.getTotalAmount())
                : null;

        if (request.getItems() != null) {
            for (ReceiptItemRequest itemRequest : request.getItems()) {
                ReceiptItem item = new ReceiptItem();
                item.setReceipt(receipt);
                item.setProductName(itemRequest.getProductName());
                item.setNormalizedName(ProductNameNormalizer.normalize(itemRequest.getProductName()));
                item.setUnitPrice(itemRequest.getUnitPrice());
                if (itemRequest.getQuantity() != null) {
                    item.setQuantity(itemRequest.getQuantity());
                }
                receipt.getItems().add(item);
            }
        }

        Receipt saved = receiptRepository.save(receipt);

        // Fiş kategorili ve tarihliyse, o ayki bütçe eşiği geçildiyse push bildirimi gönder
        if (saved.getCategory() != null && saved.getReceiptDate() != null) {
            checkBudgetThresholdUseCase.checkAndNotify(
                    user.getId(), saved.getCategory().getId(),
                    saved.getReceiptDate().getYear(),
                    saved.getReceiptDate().getMonthValue());
        }

        ReceiptResponse response = toResponse(saved);
        response.setAnomalyWarning(anomalyWarning);
        return response;
    }

    private static final int ANOMALY_MIN_SAMPLE_SIZE = 3;
    private static final BigDecimal ANOMALY_HIGH_MULTIPLIER = BigDecimal.valueOf(2.5);
    private static final BigDecimal ANOMALY_LOW_MULTIPLIER = BigDecimal.valueOf(0.25);
    private static final BigDecimal ANOMALY_MIN_AVERAGE = BigDecimal.valueOf(20);

    // Bu kategorideki geçmiş fişlere kıyasla tutar normalden çok yüksek/düşükse uyarı metni döner
    private String detectAmountAnomaly(Category category, BigDecimal newAmount) {
        if (newAmount == null) {
            return null;
        }
        long previousCount = receiptRepository.countByCategoryAndTotalAmountIsNotNull(category);
        if (previousCount < ANOMALY_MIN_SAMPLE_SIZE) {
            return null;
        }

        BigDecimal average = receiptRepository.avgTotalAmountByCategory(category)
                .setScale(2, RoundingMode.HALF_UP);
        if (average.compareTo(ANOMALY_MIN_AVERAGE) < 0) {
            return null;
        }

        if (newAmount.compareTo(average.multiply(ANOMALY_HIGH_MULTIPLIER)) > 0) {
            return category.getName() + " kategorisinde her zamankinden çok daha yüksek bir tutar ("
                    + newAmount.toPlainString() + " TL) — ortalaman " + average.toPlainString() + " TL";
        }
        if (newAmount.compareTo(average.multiply(ANOMALY_LOW_MULTIPLIER)) < 0) {
            return category.getName() + " kategorisinde her zamankinden çok daha düşük bir tutar ("
                    + newAmount.toPlainString() + " TL) — ortalaman " + average.toPlainString() + " TL";
        }
        return null;
    }

    // Ekstre içe aktarma onayı: her satır bağımsız denenir, bir satırın hatası diğerlerini etkilemez.
    // Kullanıcı zaten parse edilen listeyi gözden geçirip onayladığı için duplicate kontrolü atlanır.
    public BulkReceiptImportResponse createReceiptsBulk(String email, List<ReceiptRequest> requests) {
        List<ReceiptResponse> created = new ArrayList<>();
        List<BulkImportError> failed = new ArrayList<>();

        for (int i = 0; i < requests.size(); i++) {
            try {
                requests.get(i).setAllowDuplicate(true);
                created.add(createReceipt(email, requests.get(i)));
            } catch (ResponseStatusException e) {
                failed.add(new BulkImportError(i, e.getReason() != null ? e.getReason() : "Hata oluştu"));
            } catch (Exception e) {
                failed.add(new BulkImportError(i, "Beklenmedik bir hata oluştu"));
            }
        }

        BulkReceiptImportResponse response = new BulkReceiptImportResponse();
        response.setCreated(created);
        response.setFailed(failed);
        return response;
    }

    public ReceiptResponse getReceiptById(String email, Long receiptId) {
        User user = getUserByEmail(email);
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Fiş bulunamadı"));

        if (!receipt.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bu fişe erişim yetkiniz yok");
        }

        return toResponse(receipt);
    }

    // Garanti/iade hatırlatıcı tarihlerini fiş eklendikten sonra da kurabilmek/güncelleyebilmek için
    public ReceiptResponse setReminders(String email, Long receiptId, SetReceiptRemindersRequest request) {
        User user = getUserByEmail(email);
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Fiş bulunamadı"));

        if (!receipt.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bu fişe erişim yetkiniz yok");
        }

        receipt.setReturnDeadline(request.getReturnDeadline());
        receipt.setWarrantyExpiryDate(request.getWarrantyExpiryDate());
        // Tarih değişmiş/yeniden kurulmuş olabilir — hatırlatma tekrar tetiklenebilsin
        receipt.setReturnReminderSent(false);
        receipt.setWarrantyReminderSent(false);

        return toResponse(receiptRepository.save(receipt));
    }

    // Fiş bölüştürme sonucunu kalıcı hale getirir (kimin ne kadar ödeyeceği)
    public ReceiptResponse saveSplit(String email, Long receiptId, SaveSplitRequest request) {
        User user = getUserByEmail(email);
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Fiş bulunamadı"));

        if (!receipt.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bu fişe erişim yetkiniz yok");
        }

        try {
            receipt.setSplitDetailsJson(objectMapper.writeValueAsString(request.getParticipants()));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Bölüştürme kaydedilemedi");
        }

        return toResponse(receiptRepository.save(receipt));
    }

    public void deleteReceipt(String email, Long receiptId) {
        User user = getUserByEmail(email);
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Fiş bulunamadı"));

        if (!receipt.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bu fişi silme yetkiniz yok");
        }

        receiptRepository.delete(receipt);
    }

    // Gün 14/15: tarih aralığındaki fişleri PDF/Excel/CSV olarak dışa aktarır
    public ResponseEntity<byte[]> exportReceipts(String email, String format, LocalDate start, LocalDate end) {
        User user = getUserByEmail(email);

        if (start == null || end == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "start ve end tarihleri zorunludur");
        }
        if (end.isBefore(start)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "end, start'tan önce olamaz");
        }

        List<Receipt> receipts = receiptRepository.findByUserAndReceiptDateBetween(user, start, end);
        String normalizedFormat = format == null ? "" : format.toLowerCase(Locale.ROOT);

        byte[] fileBytes;
        MediaType mediaType;
        String extension;

        switch (normalizedFormat) {
            case "pdf" -> {
                fileBytes = exportService.toPdf(receipts, start.toString(), end.toString());
                mediaType = MediaType.APPLICATION_PDF;
                extension = "pdf";
            }
            case "excel" -> {
                fileBytes = exportService.toExcel(receipts);
                mediaType = MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                extension = "xlsx";
            }
            case "csv" -> {
                fileBytes = exportService.toCsv(receipts);
                mediaType = MediaType.parseMediaType("text/csv");
                extension = "csv";
            }
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Geçersiz format — pdf, excel veya csv olmalı");
        }

        String filename = "fisler_" + start + "_" + end + "." + extension;
        ContentDisposition disposition = ContentDisposition.attachment().filename(filename).build();

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(fileBytes);
    }

    private ReceiptResponse toResponse(Receipt receipt) {
        ReceiptResponse response = new ReceiptResponse();
        response.setId(receipt.getId());
        response.setStoreName(receipt.getStoreName());
        response.setTotalAmount(receipt.getTotalAmount());
        response.setReceiptDate(receipt.getReceiptDate());
        response.setImageUrl(receipt.getImageUrl());
        response.setRawOcrText(receipt.getRawOcrText());
        response.setCreatedAt(receipt.getCreatedAt());
        response.setReturnDeadline(receipt.getReturnDeadline());
        response.setWarrantyExpiryDate(receipt.getWarrantyExpiryDate());

        if (receipt.getCategory() != null) {
            response.setCategoryId(receipt.getCategory().getId());
            response.setCategoryName(receipt.getCategory().getName());
        }

        List<ReceiptItemResponse> itemResponses = new ArrayList<>();
        for (ReceiptItem item : receipt.getItems()) {
            ReceiptItemResponse itemResponse = new ReceiptItemResponse();
            itemResponse.setId(item.getId());
            itemResponse.setProductName(item.getProductName());
            itemResponse.setUnitPrice(item.getUnitPrice());
            itemResponse.setQuantity(item.getQuantity());
            itemResponses.add(itemResponse);
        }
        response.setItems(itemResponses);

        if (receipt.getSplitDetailsJson() != null) {
            try {
                response.setSplitParticipants(objectMapper.readValue(
                        receipt.getSplitDetailsJson(), new TypeReference<List<SplitParticipantDto>>() {}));
            } catch (Exception e) {
                log.error("Fiş #{} için splitDetailsJson ayrıştırılamadı: {}", receipt.getId(), e.getMessage());
                response.setSplitParticipants(null);
            }
        }

        return response;
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Kullanıcı bulunamadı"));
    }
}