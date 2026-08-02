package com.fisbu.api.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fisbu.api.dto.BulkReceiptImportRequest;
import com.fisbu.api.dto.BulkReceiptImportResponse;
import com.fisbu.api.dto.ParsedStatementResponse;
import com.fisbu.api.service.ReceiptService;
import com.fisbu.api.service.StatementImportService;

@RestController
@RequestMapping("/receipts/import")
public class StatementImportController {

    private final StatementImportService statementImportService;
    private final ReceiptService receiptService;

    public StatementImportController(StatementImportService statementImportService, ReceiptService receiptService) {
        this.statementImportService = statementImportService;
        this.receiptService = receiptService;
    }

    @PostMapping("/parse")
    public ParsedStatementResponse parseStatement(@AuthenticationPrincipal UserDetails userDetails,
                                                   @RequestParam("file") MultipartFile file) {
        return statementImportService.parseStatement(userDetails.getUsername(), file);
    }

    @PostMapping("/confirm")
    @ResponseStatus(HttpStatus.CREATED)
    public BulkReceiptImportResponse confirmImport(@AuthenticationPrincipal UserDetails userDetails,
                                                     @RequestBody @Valid BulkReceiptImportRequest request) {
        return receiptService.createReceiptsBulk(userDetails.getUsername(), request.getReceipts());
    }
}
