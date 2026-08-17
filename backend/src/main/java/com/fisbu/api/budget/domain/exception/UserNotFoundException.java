package com.fisbu.api.budget.domain.exception;

import com.fisbu.api.shared.domain.exception.NotFoundException;

public class UserNotFoundException extends NotFoundException {

    public UserNotFoundException() {
        super("Kullanıcı bulunamadı");
    }
}
