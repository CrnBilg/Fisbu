package com.fisbu.api.shared.domain.exception;

// Kullanıcının erişim yetkisi olmayan bir kaynağa ulaşmaya çalıştığında fırlatılır.
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
