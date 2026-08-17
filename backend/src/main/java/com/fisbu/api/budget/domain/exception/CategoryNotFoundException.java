package com.fisbu.api.budget.domain.exception;

import com.fisbu.api.shared.domain.exception.NotFoundException;

public class CategoryNotFoundException extends NotFoundException {

    public CategoryNotFoundException() {
        super("Kategori bulunamadı");
    }
}
