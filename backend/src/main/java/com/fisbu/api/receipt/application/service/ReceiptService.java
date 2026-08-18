package com.fisbu.api.receipt.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fisbu.api.budget.application.port.in.CheckBudgetThresholdUseCase;
import com.fisbu.api.category.domain.Category;
import com.fisbu.api.receipt.application.port.in.CreateReceiptUseCase;
import com.fisbu.api.receipt.application.port.in.CreateReceiptsBulkUseCase;
import com.fisbu.api.receipt.application.port.in.DeleteReceiptUseCase;
import com.fisbu.api.receipt.application.port.in.ExportReceiptsUseCase;
import com.fisbu.api.receipt.application.port.in.GetReceiptByIdUseCase;
import com.fisbu.api.receipt.application.port.in.GetReceiptsUseCase;
import com.fisbu.api.receipt.application.port.in.SaveSplitUseCase;
import com.fisbu.api.receipt.application.port.in.SearchReceiptsUseCase;
import com.fisbu.api.receipt.application.port.in.SetReceiptRemindersUseCase;
import com.fisbu.api.receipt.application.port.in.SuggestCategoryUseCase;
import com.fisbu.api.receipt.application.port.out.AverageAmountByCategoryPort;
import com.fisbu.api.receipt.application.port.out.CountReceiptsByCategoryPort;
import com.fisbu.api.receipt.application.port.out.DeleteReceiptPort;
import com.fisbu.api.receipt.application.port.out.FindDuplicateReceiptPort;
import com.fisbu.api.receipt.application.port.out.FindReceiptsByStoreNameContainingPort;
import com.fisbu.api.receipt.application.port.out.GenerateReceiptExportPort;
import com.fisbu.api.receipt.application.port.out.LoadOwnedCategoryPort;
import com.fisbu.api.receipt.application.port.out.LoadReceiptPort;
import com.fisbu.api.receipt.application.port.out.LoadReceiptsPort;
import com.fisbu.api.receipt.application.port.out.ReceiptPage;
import com.fisbu.api.receipt.application.port.out.ResolveUserIdPort;
import com.fisbu.api.receipt.application.port.out.SaveReceiptPort;
import com.fisbu.api.receipt.application.port.out.SearchReceiptsPort;
import com.fisbu.api.receipt.domain.AmountAnomalyPolicy;
import com.fisbu.api.receipt.domain.Receipt;
import com.fisbu.api.receipt.domain.ReceiptItem;
import com.fisbu.api.receipt.domain.exception.CategoryAccessDeniedException;
import com.fisbu.api.receipt.domain.exception.CategoryNotFoundException;
import com.fisbu.api.receipt.domain.exception.DuplicateReceiptException;
import com.fisbu.api.receipt.domain.exception.ReceiptAccessDeniedException;
import com.fisbu.api.receipt.domain.exception.ReceiptNotFoundException;
import com.fisbu.api.receipt.domain.exception.UserNotFoundException;
import com.fisbu.api.service.ProductNameNormalizer;

@Service
public class ReceiptService implements GetReceiptsUseCase, SearchReceiptsUseCase, SuggestCategoryUseCase,
        CreateReceiptUseCase, CreateReceiptsBulkUseCase, GetReceiptByIdUseCase, SetReceiptRemindersUseCase,
        SaveSplitUseCase, DeleteReceiptUseCase, ExportReceiptsUseCase {

    // GET /receipts gerçek sayfalamaya geçene kadar (mobil taraf hâlâ tüm listeyi tek
    // seferde bekliyor) sınırsız büyümeyi önlemek için üst sınır — normal kullanım için yeterince geniş
    private static final int MAX_RECEIPTS_LIST = 2000;

    private final ResolveUserIdPort resolveUserIdPort;
    private final LoadOwnedCategoryPort loadOwnedCategoryPort;
    private final LoadReceiptPort loadReceiptPort;
    private final LoadReceiptsPort loadReceiptsPort;
    private final SearchReceiptsPort searchReceiptsPort;
    private final FindReceiptsByStoreNameContainingPort findReceiptsByStoreNameContainingPort;
    private final FindDuplicateReceiptPort findDuplicateReceiptPort;
    private final SaveReceiptPort saveReceiptPort;
    private final DeleteReceiptPort deleteReceiptPort;
    private final CountReceiptsByCategoryPort countReceiptsByCategoryPort;
    private final AverageAmountByCategoryPort averageAmountByCategoryPort;
    private final GenerateReceiptExportPort generateReceiptExportPort;
    private final CheckBudgetThresholdUseCase checkBudgetThresholdUseCase;
    private final ObjectMapper objectMapper;

    public ReceiptService(ResolveUserIdPort resolveUserIdPort, LoadOwnedCategoryPort loadOwnedCategoryPort,
                           LoadReceiptPort loadReceiptPort, LoadReceiptsPort loadReceiptsPort,
                           SearchReceiptsPort searchReceiptsPort,
                           FindReceiptsByStoreNameContainingPort findReceiptsByStoreNameContainingPort,
                           FindDuplicateReceiptPort findDuplicateReceiptPort, SaveReceiptPort saveReceiptPort,
                           DeleteReceiptPort deleteReceiptPort, CountReceiptsByCategoryPort countReceiptsByCategoryPort,
                           AverageAmountByCategoryPort averageAmountByCategoryPort,
                           GenerateReceiptExportPort generateReceiptExportPort,
                           CheckBudgetThresholdUseCase checkBudgetThresholdUseCase, ObjectMapper objectMapper) {
        this.resolveUserIdPort = resolveUserIdPort;
        this.loadOwnedCategoryPort = loadOwnedCategoryPort;
        this.loadReceiptPort = loadReceiptPort;
        this.loadReceiptsPort = loadReceiptsPort;
        this.searchReceiptsPort = searchReceiptsPort;
        this.findReceiptsByStoreNameContainingPort = findReceiptsByStoreNameContainingPort;
        this.findDuplicateReceiptPort = findDuplicateReceiptPort;
        this.saveReceiptPort = saveReceiptPort;
        this.deleteReceiptPort = deleteReceiptPort;
        this.countReceiptsByCategoryPort = countReceiptsByCategoryPort;
        this.averageAmountByCategoryPort = averageAmountByCategoryPort;
        this.generateReceiptExportPort = generateReceiptExportPort;
        this.checkBudgetThresholdUseCase = checkBudgetThresholdUseCase;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<Receipt> getReceipts(String email) {
        Long userId = resolveUserId(email);
        return loadReceiptsPort.loadByUserId(userId, MAX_RECEIPTS_LIST);
    }

    @Override
    public ReceiptPage searchReceipts(String email, String query, Long categoryId, boolean uncategorized,
                                       int page, int size) {
        Long userId = resolveUserId(email);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return searchReceiptsPort.search(userId, query, categoryId, uncategorized, safePage, safeSize);
    }

    @Override
    public CategorySuggestion suggestCategory(String email, String storeName) {
        if (storeName == null || storeName.trim().length() < 2) {
            return null;
        }
        Long userId = resolveUserId(email);
        List<Receipt> matches = findReceiptsByStoreNameContainingPort
                .findByUserIdAndStoreNameContaining(userId, storeName.trim());

        Map<Long, String> categoryNameById = new HashMap<>();
        Map<Long, Long> countsByCategoryId = new HashMap<>();
        for (Receipt receipt : matches) {
            if (receipt.categoryId() == null) {
                continue;
            }
            categoryNameById.putIfAbsent(receipt.categoryId(), receipt.categoryName());
            countsByCategoryId.merge(receipt.categoryId(), 1L, Long::sum);
        }

        return countsByCategoryId.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> new CategorySuggestion(entry.getKey(), categoryNameById.get(entry.getKey())))
                .orElse(null);
    }

    @Override
    public CreateReceiptResult createReceipt(CreateReceiptCommand command) {
        Long userId = resolveUserId(command.email());

        if (!command.allowDuplicate()
                && command.storeName() != null
                && command.totalAmount() != null
                && command.receiptDate() != null) {
            List<Receipt> candidates = findDuplicateReceiptPort.findDuplicates(
                    userId, command.storeName().trim(), command.totalAmount(), command.receiptDate());
            boolean isDuplicate = candidates.stream().anyMatch(candidate -> candidate.matchesForDuplicate(
                    command.storeName().trim(), command.totalAmount(), command.receiptDate()));
            if (isDuplicate) {
                throw new DuplicateReceiptException();
            }
        }

        Category category = null;
        if (command.categoryId() != null) {
            category = loadOwnedCategoryPort.loadById(command.categoryId())
                    .orElseThrow(CategoryNotFoundException::new);

            if (!category.userId().equals(userId)) {
                throw new CategoryAccessDeniedException();
            }
        }

        String anomalyWarning = category != null
                ? detectAmountAnomaly(category, command.totalAmount())
                : null;

        List<ReceiptItem> items = new ArrayList<>();
        if (command.items() != null) {
            for (ReceiptItemCommand itemCommand : command.items()) {
                items.add(new ReceiptItem(null, itemCommand.productName(),
                        ProductNameNormalizer.normalize(itemCommand.productName()),
                        itemCommand.unitPrice(), itemCommand.quantity()));
            }
        }

        Receipt receipt = new Receipt(null, userId, command.categoryId(),
                category != null ? category.name() : null, command.storeName(), command.totalAmount(),
                command.receiptDate(), command.imageUrl(), command.rawOcrText(), null, null,
                command.returnDeadline(), command.warrantyExpiryDate(), false, false, items);

        Receipt saved = saveReceiptPort.save(receipt);

        // Fiş kategorili ve tarihliyse, o ayki bütçe eşiği geçildiyse push bildirimi gönder
        if (saved.categoryId() != null && saved.receiptDate() != null) {
            checkBudgetThresholdUseCase.checkAndNotify(
                    userId, saved.categoryId(), saved.receiptDate().getYear(), saved.receiptDate().getMonthValue());
        }

        return new CreateReceiptResult(saved, anomalyWarning);
    }

    // Bu kategorideki geçmiş fişlere kıyasla tutar normalden çok yüksek/düşükse uyarı metni döner.
    // Asıl eşik/çarpan mantığı AmountAnomalyPolicy'de (domain); burada sadece MIN_SAMPLE_SIZE'a
    // göre gereksiz bir ortalama sorgusundan kaçınılıyor (performans, iş kuralı değil).
    private String detectAmountAnomaly(Category category, BigDecimal newAmount) {
        if (newAmount == null) {
            return null;
        }
        long previousCount = countReceiptsByCategoryPort.countByCategoryId(category.id());
        if (previousCount < AmountAnomalyPolicy.MIN_SAMPLE_SIZE) {
            return null;
        }

        BigDecimal average = averageAmountByCategoryPort.averageByCategoryId(category.id());
        return AmountAnomalyPolicy.evaluate(category.name(), previousCount, average, newAmount);
    }

    // Ekstre içe aktarma onayı: her satır bağımsız denenir, bir satırın hatası diğerlerini etkilemez.
    // Kullanıcı zaten parse edilen listeyi gözden geçirip onayladığı için duplicate kontrolü atlanır.
    @Override
    public BulkCreateResult createReceiptsBulk(String email, List<CreateReceiptCommand> commands) {
        List<CreateReceiptResult> created = new ArrayList<>();
        List<BulkImportError> failed = new ArrayList<>();

        for (int i = 0; i < commands.size(); i++) {
            try {
                created.add(createReceipt(withAllowDuplicate(commands.get(i))));
            } catch (ResponseStatusException e) {
                failed.add(new BulkImportError(i, e.getReason() != null ? e.getReason() : "Hata oluştu"));
            } catch (RuntimeException e) {
                failed.add(new BulkImportError(i, e.getMessage() != null ? e.getMessage() : "Beklenmedik bir hata oluştu"));
            }
        }

        return new BulkCreateResult(created, failed);
    }

    private CreateReceiptCommand withAllowDuplicate(CreateReceiptCommand command) {
        return new CreateReceiptCommand(command.email(), command.storeName(), command.totalAmount(),
                command.receiptDate(), command.imageUrl(), command.rawOcrText(), command.categoryId(),
                command.returnDeadline(), command.warrantyExpiryDate(), true, command.items());
    }

    @Override
    public Receipt getReceiptById(String email, Long receiptId) {
        Long userId = resolveUserId(email);
        return getOwnedReceipt(userId, receiptId);
    }

    // Garanti/iade hatırlatıcı tarihlerini fiş eklendikten sonra da kurabilmek/güncelleyebilmek için
    @Override
    public Receipt setReminders(String email, Long receiptId, LocalDate returnDeadline, LocalDate warrantyExpiryDate) {
        Long userId = resolveUserId(email);
        Receipt receipt = getOwnedReceipt(userId, receiptId);

        Receipt updated = new Receipt(receipt.id(), receipt.userId(), receipt.categoryId(), receipt.categoryName(),
                receipt.storeName(), receipt.totalAmount(), receipt.receiptDate(), receipt.imageUrl(),
                receipt.rawOcrText(), receipt.splitDetailsJson(), receipt.createdAt(), returnDeadline,
                warrantyExpiryDate, false, false, receipt.items());

        return saveReceiptPort.save(updated);
    }

    // Fiş bölüştürme sonucunu kalıcı hale getirir (kimin ne kadar ödeyeceği)
    @Override
    public Receipt saveSplit(String email, Long receiptId, List<SplitParticipant> participants) {
        Long userId = resolveUserId(email);
        Receipt receipt = getOwnedReceipt(userId, receiptId);

        Receipt updated = new Receipt(receipt.id(), receipt.userId(), receipt.categoryId(), receipt.categoryName(),
                receipt.storeName(), receipt.totalAmount(), receipt.receiptDate(), receipt.imageUrl(),
                receipt.rawOcrText(), serializeSplitParticipants(participants), receipt.createdAt(),
                receipt.returnDeadline(), receipt.warrantyExpiryDate(), receipt.returnReminderSent(),
                receipt.warrantyReminderSent(), receipt.items());

        return saveReceiptPort.save(updated);
    }

    private String serializeSplitParticipants(List<SplitParticipant> participants) {
        try {
            return objectMapper.writeValueAsString(participants);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Bölüştürme kaydedilemedi");
        }
    }

    @Override
    public void deleteReceipt(String email, Long receiptId) {
        Long userId = resolveUserId(email);
        Receipt receipt = loadReceiptPort.loadById(receiptId).orElseThrow(ReceiptNotFoundException::new);

        if (!receipt.userId().equals(userId)) {
            throw new ReceiptAccessDeniedException("Bu fişi silme yetkiniz yok");
        }

        deleteReceiptPort.deleteById(receiptId);
    }

    // Gün 14/15: tarih aralığındaki fişleri PDF/Excel/CSV olarak dışa aktarır
    @Override
    public ExportResult exportReceipts(String email, String format, LocalDate start, LocalDate end) {
        Long userId = resolveUserId(email);

        if (start == null || end == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "start ve end tarihleri zorunludur");
        }
        if (end.isBefore(start)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "end, start'tan önce olamaz");
        }

        String normalizedFormat = format == null ? "" : format.toLowerCase(Locale.ROOT);

        byte[] fileBytes;
        String mediaType;
        String extension;

        switch (normalizedFormat) {
            case "pdf" -> {
                fileBytes = generateReceiptExportPort.toPdf(userId, start, end, start.toString(), end.toString());
                mediaType = "application/pdf";
                extension = "pdf";
            }
            case "excel" -> {
                fileBytes = generateReceiptExportPort.toExcel(userId, start, end);
                mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                extension = "xlsx";
            }
            case "csv" -> {
                fileBytes = generateReceiptExportPort.toCsv(userId, start, end);
                mediaType = "text/csv";
                extension = "csv";
            }
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Geçersiz format — pdf, excel veya csv olmalı");
        }

        String filename = "fisler_" + start + "_" + end + "." + extension;
        return new ExportResult(fileBytes, mediaType, filename);
    }

    private Receipt getOwnedReceipt(Long userId, Long receiptId) {
        Receipt receipt = loadReceiptPort.loadById(receiptId).orElseThrow(ReceiptNotFoundException::new);

        if (!receipt.userId().equals(userId)) {
            throw new ReceiptAccessDeniedException();
        }

        return receipt;
    }

    private Long resolveUserId(String email) {
        return resolveUserIdPort.resolveUserIdByEmail(email).orElseThrow(UserNotFoundException::new);
    }
}
