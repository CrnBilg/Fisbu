package com.fisbu.api.category.application.port.out;

import java.util.Optional;

import com.fisbu.api.category.domain.Category;

public interface FindCategoryByNamePort {

    Optional<Category> findByUserIdAndNameIgnoreCase(Long userId, String name);
}
