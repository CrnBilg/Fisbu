package com.fisbu.api.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BudgetResponse {
    private Long id;
    private Long categoryId;
    private String categoryName;
    private String categoryColor;
    private BigDecimal monthlyLimit;
    private Integer year;
    private Integer month;
    private BigDecimal currentSpend;
    private Double percentage;
    private Boolean overBudget;
}
