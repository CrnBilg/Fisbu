package com.fisbu.api.controller;

import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fisbu.api.dto.CategorySuggestionResponse;
import com.fisbu.api.dto.PageResponse;
import com.fisbu.api.dto.ReceiptRequest;
import com.fisbu.api.dto.ReceiptResponse;
import com.fisbu.api.dto.SaveSplitRequest;
import com.fisbu.api.dto.SetReceiptRemindersRequest;
import com.fisbu.api.service.ReceiptService;

@RestController
@RequestMapping("/receipts")
public class ReceiptController {

    private final ReceiptService receiptService;

    public ReceiptController(ReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    @GetMapping
    public List<ReceiptResponse> getReceipts(@AuthenticationPrincipal UserDetails userDetails) {
        return receiptService.getReceipts(userDetails.getUsername());
    }

    // Fiş listesi ekranı: mağaza adına göre arama + kategori filtresi + sayfalama (infinite scroll)
    @GetMapping("/search")
    public PageResponse<ReceiptResponse> searchReceipts(@AuthenticationPrincipal UserDetails userDetails,
                                                          @RequestParam(required = false) String query,
                                                          @RequestParam(required = false) Long categoryId,
                                                          @RequestParam(defaultValue = "false") boolean uncategorized,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "20") int size) {
        return receiptService.searchReceipts(userDetails.getUsername(), query, categoryId, uncategorized, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReceiptResponse createReceipt(@AuthenticationPrincipal UserDetails userDetails,
                                          @RequestBody @Valid ReceiptRequest request) {
        return receiptService.createReceipt(userDetails.getUsername(), request);
    }

    // Elle fiş eklerken mağaza adına göre kategori önerisi — öneri yoksa 204 döner
    @GetMapping("/category-suggestion")
    public ResponseEntity<CategorySuggestionResponse> suggestCategory(@AuthenticationPrincipal UserDetails userDetails,
                                                                        @RequestParam String storeName) {
        CategorySuggestionResponse suggestion = receiptService.suggestCategory(userDetails.getUsername(), storeName);
        return suggestion != null ? ResponseEntity.ok(suggestion) : ResponseEntity.noContent().build();
    }
    @GetMapping("/{id}")
    public ReceiptResponse getReceiptById(@AuthenticationPrincipal UserDetails userDetails,
                                           @PathVariable Long id) {
        return receiptService.getReceiptById(userDetails.getUsername(), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReceipt(@AuthenticationPrincipal UserDetails userDetails,
                               @PathVariable Long id) {
        receiptService.deleteReceipt(userDetails.getUsername(), id);
    }

    @PutMapping("/{id}/split")
    public ReceiptResponse saveSplit(@AuthenticationPrincipal UserDetails userDetails,
                                      @PathVariable Long id,
                                      @RequestBody @Valid SaveSplitRequest request) {
        return receiptService.saveSplit(userDetails.getUsername(), id, request);
    }

    // Garanti/iade hatırlatıcı tarihlerini fiş eklendikten sonra kurar/günceller
    @PutMapping("/{id}/reminders")
    public ReceiptResponse setReminders(@AuthenticationPrincipal UserDetails userDetails,
                                         @PathVariable Long id,
                                         @RequestBody SetReceiptRemindersRequest request) {
        return receiptService.setReminders(userDetails.getUsername(), id, request);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportReceipts(@AuthenticationPrincipal UserDetails userDetails,
                                                  @RequestParam String format,
                                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return receiptService.exportReceipts(userDetails.getUsername(), format, start, end);
    }

}