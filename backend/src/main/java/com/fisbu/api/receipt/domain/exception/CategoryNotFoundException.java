package com.fisbu.api.receipt.domain.exception;

import com.fisbu.api.shared.domain.exception.NotFoundException;

public class CategoryNotFoundException extends NotFoundException {

    public CategoryNotFoundException() {
        super("Kategori bulunamadı");
    }
}
