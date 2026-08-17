package com.fisbu.api.shared.domain.exception;

// İş kuralı çakışması olduğunda (örn. benzersizlik ihlali) domain katmanında fırlatılır.
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
