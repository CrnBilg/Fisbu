package com.fisbu.api.budget.domain.exception;

import com.fisbu.api.shared.domain.exception.ForbiddenException;

public class BudgetAccessDeniedException extends ForbiddenException {

    public BudgetAccessDeniedException() {
        super("Bu bütçeye erişim yetkiniz yok");
    }
}
