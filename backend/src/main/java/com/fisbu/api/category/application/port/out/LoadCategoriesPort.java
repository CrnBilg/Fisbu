package com.fisbu.api.category.application.port.out;

import java.util.List;

import com.fisbu.api.category.domain.Category;

public interface LoadCategoriesPort {

    List<Category> loadByUserId(Long userId);
}
