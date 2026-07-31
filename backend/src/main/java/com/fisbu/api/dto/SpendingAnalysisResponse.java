package com.fisbu.api.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SpendingAnalysisResponse {

    private int year;
    private int month;
    private BigDecimal totalAmount;
    private String comment;
}
