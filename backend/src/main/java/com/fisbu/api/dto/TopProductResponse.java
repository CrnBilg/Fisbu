package com.fisbu.api.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TopProductResponse {
    private String normalizedName;
    private String displayName;
    private int purchaseCount;
    private BigDecimal totalSpent;
}
