package com.fisbu.api.budget.application.port.out;

import java.util.Optional;

import com.fisbu.api.category.domain.Category;

// Category modülü zaten hexagonal'a taşındığı için bu bir köprü değil: adaptörü doğrudan
// Category modülünün kendi LoadCategoryPort'unu sarmalar (bkz. adapter/out/category).
public interface LoadOwnedCategoryPort {

    Optional<Category> loadById(Long categoryId);
}
