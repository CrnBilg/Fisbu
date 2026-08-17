package com.fisbu.api.shared.domain.exception;

// İstenen kaynak bulunamadığında domain katmanında fırlatılır (framework'ten bağımsız).
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
