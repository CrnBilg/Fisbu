package com.fisbu.api.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryTotalResponse {
    private Long categoryId;
    private String categoryName;
    private String color;
    private BigDecimal totalAmount;
}
