package com.fisbu.api.receipt.domain.exception;

import com.fisbu.api.shared.domain.exception.NotFoundException;

public class UserNotFoundException extends NotFoundException {

    public UserNotFoundException() {
        super("Kullanıcı bulunamadı");
    }
}
