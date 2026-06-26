package com.fisbu.api.controller;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping
    public List<ReceiptResponse> getReceipts(@AuthenticationPrincipal UserDetails userDetails) {
        return receiptService.getReceipts(userDetails.getUsername());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReceiptResponse createReceipt(@AuthenticationPrincipal UserDetails userDetails,
                                          @RequestBody @Valid ReceiptRequest request) {
        return receiptService.createReceipt(userDetails.getUsername(), request);
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

}