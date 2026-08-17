package com.fisbu.api.category.application.port.in;

import com.fisbu.api.category.domain.Category;

public interface UpdateCategoryUseCase {

    record UpdateCategoryCommand(String email, Long categoryId, String name, String color) {
    }

    Category updateCategory(UpdateCategoryCommand command);
}
