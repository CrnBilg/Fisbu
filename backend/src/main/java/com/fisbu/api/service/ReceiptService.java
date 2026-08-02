package com.fisbu.api.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fisbu.api.dto.BulkImportError;
import com.fisbu.api.dto.BulkReceiptImportResponse;
import com.fisbu.api.dto.ReceiptItemRequest;
import com.fisbu.api.dto.ReceiptItemResponse;
import com.fisbu.api.dto.ReceiptRequest;
import com.fisbu.api.dto.ReceiptResponse;
import com.fisbu.api.entity.Category;
import com.fisbu.api.entity.Receipt;
import com.fisbu.api.entity.ReceiptItem;
import com.fisbu.api.entity.User;
import com.fisbu.api.repository.CategoryRepository;
import com.fisbu.api.repository.ReceiptRepository;
import com.fisbu.api.repository.UserRepository;

@Service
public class ReceiptService {

    private final ReceiptRepository receiptRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final BudgetService budgetService;
    private final ExportService exportService;

    public ReceiptService(ReceiptRepository receiptRepository,
                          UserRepository userRepository,
                          CategoryRepository categoryRepository,
                          BudgetService budgetService,
                          ExportService exportService) {
        this.receiptRepository = receiptRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.budgetService = budgetService;
        this.exportService = exportService;
    }

    // Kullanıcının tüm fişlerini listeler
    public List<ReceiptResponse> getReceipts(String email) {
        User user = getUserByEmail(email);
        return receiptRepository.findByUser(user)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // Yeni fiş ekler
    public ReceiptResponse createReceipt(String email, ReceiptRequest request) {
        User user = getUserByEmail(email);

        Receipt receipt = new Receipt();
        receipt.setUser(user);
        receipt.setStoreName(request.getStoreName());
        receipt.setTotalAmount(request.getTotalAmount());
        receipt.setReceiptDate(request.getReceiptDate());
        receipt.setImageUrl(request.getImageUrl());
        receipt.setRawOcrText(request.getRawOcrText());

        // Kategori opsiyonel — gönderilmişse set et
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Kategori bulunamadı"));

            if (!category.getUser().getId().equals(user.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bu kategoriye erişim yetkiniz yok");
            }

            receipt.setCategory(category);
        }

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
            budgetService.checkBudgetAndNotify(
                    user, saved.getCategory(),
                    saved.getReceiptDate().getYear(),
                    saved.getReceiptDate().getMonthValue());
        }

        return toResponse(saved);
    }

    // Ekstre içe aktarma onayı: her satır bağımsız denenir, bir satırın hatası diğerlerini etkilemez
    public BulkReceiptImportResponse createReceiptsBulk(String email, List<ReceiptRequest> requests) {
        List<ReceiptResponse> created = new ArrayList<>();
        List<BulkImportError> failed = new ArrayList<>();

        for (int i = 0; i < requests.size(); i++) {
            try {
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

        return response;
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Kullanıcı bulunamadı"));
    }
}