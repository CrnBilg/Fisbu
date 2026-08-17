package com.fisbu.api.category.domain.exception;

import com.fisbu.api.shared.domain.exception.NotFoundException;

public class UserNotFoundException extends NotFoundException {

    public UserNotFoundException() {
        super("Kullanıcı bulunamadı");
    }
}
