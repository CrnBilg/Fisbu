package com.fisbu.api.category.application.port.in;

import com.fisbu.api.category.domain.Category;

public interface CreateCategoryUseCase {

    record CreateCategoryCommand(String email, String name, String color) {
    }

    Category createCategory(CreateCategoryCommand command);
}
