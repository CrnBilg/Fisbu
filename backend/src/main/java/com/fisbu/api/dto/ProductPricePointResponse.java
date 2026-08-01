package com.fisbu.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductPricePointResponse {
    private LocalDate date;
    private BigDecimal unitPrice;
    private String storeName;
}
