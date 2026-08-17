package com.fisbu.api.category.application.port.in;

public interface DeleteCategoryUseCase {

    record DeleteCategoryCommand(String email, Long categoryId) {
    }

    void deleteCategory(DeleteCategoryCommand command);
}
