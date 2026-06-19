package com.fisbu.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fisbu.api.dto.ReceiptRequest;
import com.fisbu.api.dto.ReceiptResponse;
import com.fisbu.api.service.ReceiptService;

@RestController
@RequestMapping("/receipts")
public class ReceiptController {

    private final ReceiptService receiptService;

    public ReceiptController(ReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    // GET /receipts — sadece login olan kullanıcının fişleri
    @GetMapping
    public List<ReceiptResponse> getReceipts(@AuthenticationPrincipal UserDetails userDetails) {
        return receiptService.getReceipts(userDetails.getUsername());
    }

    // POST /receipts — yeni fiş ekle
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReceiptResponse createReceipt(@AuthenticationPrincipal UserDetails userDetails,
                                          @RequestBody ReceiptRequest request) {
        return receiptService.createReceipt(userDetails.getUsername(), request);
    }
}