package com.fisbu.api.budget.domain.exception;

import com.fisbu.api.shared.domain.exception.ConflictException;

public class DuplicateBudgetException extends ConflictException {

    public DuplicateBudgetException() {
        super("Bu kategori için bu ay zaten bir bütçe tanımlı");
    }
}
