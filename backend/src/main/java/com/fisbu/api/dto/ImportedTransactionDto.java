package com.fisbu.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImportedTransactionDto {
    private LocalDate date;
    private String description;
    private BigDecimal amount;
    private String suggestedCategoryName;
    private Long matchedCategoryId;
    private int confidenceScore;
}
