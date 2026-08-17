package com.fisbu.api.category.application.port.in;

import java.util.List;

import com.fisbu.api.category.domain.Category;

public interface GetCategoriesUseCase {

    List<Category> getCategories(String email);
}
