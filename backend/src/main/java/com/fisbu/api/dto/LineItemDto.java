package com.fisbu.api.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LineItemDto {
    private String productName;
    private BigDecimal unitPrice;
    private BigDecimal quantity;
}
