package com.fisbu.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CategorySuggestionResponse {
    private Long categoryId;
    private String categoryName;
}
