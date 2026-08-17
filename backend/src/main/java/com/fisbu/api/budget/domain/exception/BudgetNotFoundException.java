package com.fisbu.api.budget.domain.exception;

import com.fisbu.api.shared.domain.exception.NotFoundException;

public class BudgetNotFoundException extends NotFoundException {

    public BudgetNotFoundException() {
        super("Bütçe bulunamadı");
    }
}
