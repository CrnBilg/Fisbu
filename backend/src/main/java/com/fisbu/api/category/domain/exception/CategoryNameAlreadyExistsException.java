package com.fisbu.api.category.domain.exception;

import com.fisbu.api.shared.domain.exception.ConflictException;

public class CategoryNameAlreadyExistsException extends ConflictException {

    public CategoryNameAlreadyExistsException() {
        super("Bu isimde bir kategori zaten var");
    }
}
