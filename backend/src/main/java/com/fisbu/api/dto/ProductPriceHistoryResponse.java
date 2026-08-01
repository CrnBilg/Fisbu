package com.fisbu.api.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductPriceHistoryResponse {
    private String normalizedName;
    private String displayName;
    private List<ProductPricePointResponse> points;
}
