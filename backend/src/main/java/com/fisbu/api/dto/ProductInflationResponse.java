package com.fisbu.api.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductInflationResponse {
    private String normalizedName;
    private String displayName;
    private BigDecimal firstPrice;
    private BigDecimal lastPrice;
    private BigDecimal changePercent;
}
