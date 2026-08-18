package com.fisbu.api.receipt.application.port.in;

public interface SuggestCategoryUseCase {

    // Öneri yoksa null döner (mağaza adı çok kısa veya geçmişte eşleşme yok)
    CategorySuggestion suggestCategory(String email, String storeName);

    record CategorySuggestion(Long categoryId, String categoryName) {
    }
}
