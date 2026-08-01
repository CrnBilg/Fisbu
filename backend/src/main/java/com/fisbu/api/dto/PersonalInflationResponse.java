package com.fisbu.api.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PersonalInflationResponse {
    private int months;
    private int trackedProductCount;
    private BigDecimal personalInflationPercent;
    private List<ProductInflationResponse> topIncreasing;
    private List<ProductInflationResponse> topDecreasing;
}
