package com.fisbu.api.controller;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fisbu.api.dto.RestoreReceiptRequest;
import com.fisbu.api.dto.RestoreReceiptResponse;
import com.fisbu.api.dto.SpendingAnalysisResponse;
import com.fisbu.api.service.ReceiptAiService;

@RestController
@RequestMapping("/ai")
public class AiController {

    private final ReceiptAiService receiptAiService;

    public AiController(ReceiptAiService receiptAiService) {
        this.receiptAiService = receiptAiService;
    }

    @PostMapping("/restore-receipt")
    public RestoreReceiptResponse restoreReceipt(@AuthenticationPrincipal UserDetails userDetails,
                                                  @RequestBody @Valid RestoreReceiptRequest request) {
        return receiptAiService.restoreReceipt(userDetails.getUsername(), request);
    }

    @GetMapping("/spending-analysis")
    public SpendingAnalysisResponse spendingAnalysis(@AuthenticationPrincipal UserDetails userDetails,
                                                       @RequestParam(required = false) Integer year,
                                                       @RequestParam(required = false) Integer month) {
        return receiptAiService.getSpendingAnalysis(userDetails.getUsername(), year, month);
    }
}
