package com.fisbu.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RestoreReceiptResponse {

    private String storeName;
    private BigDecimal totalAmount;
    private LocalDate receiptDate;
    private String suggestedCategoryName;
    private Long matchedCategoryId;
    private int confidenceScore;
}
