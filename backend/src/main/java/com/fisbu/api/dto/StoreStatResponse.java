package com.fisbu.api.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StoreStatResponse {
    private String storeName;
    private BigDecimal totalAmount;
    private BigDecimal averageAmount;
    private int receiptCount;
}
