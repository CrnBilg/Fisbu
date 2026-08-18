package com.fisbu.api.receipt.domain.exception;

import com.fisbu.api.shared.domain.exception.ConflictException;

public class DuplicateReceiptException extends ConflictException {

    public DuplicateReceiptException() {
        super("Bu fiş zaten kayıtlı görünüyor (aynı mağaza, tutar ve tarih)");
    }
}
