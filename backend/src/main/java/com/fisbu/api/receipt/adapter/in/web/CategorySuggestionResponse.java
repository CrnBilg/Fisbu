package com.fisbu.api.receipt.adapter.in.web;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CategorySuggestionResponse {
    private Long categoryId;
    private String categoryName;
}
