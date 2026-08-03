package com.fisbu.api.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BudgetSuggestionResponse {
    private Long categoryId;
    private String categoryName;
    private int monthsAnalyzed;
    private BigDecimal averageSpend;
    private BigDecimal suggestedLimit;
    private String comment;
}
