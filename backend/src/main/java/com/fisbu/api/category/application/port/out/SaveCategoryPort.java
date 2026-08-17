package com.fisbu.api.category.application.port.out;

import com.fisbu.api.category.domain.Category;

public interface SaveCategoryPort {

    Category save(Category category);
}
