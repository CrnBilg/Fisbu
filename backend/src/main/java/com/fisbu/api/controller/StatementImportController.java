package com.fisbu.api.controller;

import java.util.List;
import java.util.stream.Collectors;

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

import com.fisbu.api.dto.ParsedStatementResponse;
import com.fisbu.api.receipt.adapter.in.web.BulkReceiptImportRequest;
import com.fisbu.api.receipt.adapter.in.web.BulkReceiptImportResponse;
import com.fisbu.api.receipt.adapter.in.web.ReceiptWebMapper;
import com.fisbu.api.receipt.application.port.in.CreateReceiptsBulkUseCase;
import com.fisbu.api.service.StatementImportService;

@RestController
@RequestMapping("/receipts/import")
public class StatementImportController {

    private final StatementImportService statementImportService;
    private final CreateReceiptsBulkUseCase createReceiptsBulkUseCase;
    private final ReceiptWebMapper mapper;

    public StatementImportController(StatementImportService statementImportService,
                                      CreateReceiptsBulkUseCase createReceiptsBulkUseCase, ReceiptWebMapper mapper) {
        this.statementImportService = statementImportService;
        this.createReceiptsBulkUseCase = createReceiptsBulkUseCase;
        this.mapper = mapper;
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
        String email = userDetails.getUsername();
        List<com.fisbu.api.receipt.application.port.in.CreateReceiptUseCase.CreateReceiptCommand> commands =
                request.getReceipts().stream().map(r -> mapper.toCommand(email, r)).collect(Collectors.toList());
        return mapper.toBulkResponse(createReceiptsBulkUseCase.createReceiptsBulk(email, commands));
    }
}
