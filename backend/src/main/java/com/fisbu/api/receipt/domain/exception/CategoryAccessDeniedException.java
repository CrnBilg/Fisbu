package com.fisbu.api.receipt.domain.exception;

import com.fisbu.api.shared.domain.exception.ForbiddenException;

public class CategoryAccessDeniedException extends ForbiddenException {

    public CategoryAccessDeniedException() {
        super("Bu kategoriye erişim yetkiniz yok");
    }
}
