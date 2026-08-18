package com.fisbu.api.receipt.domain.exception;

import com.fisbu.api.shared.domain.exception.NotFoundException;

public class ReceiptNotFoundException extends NotFoundException {

    public ReceiptNotFoundException() {
        super("Fiş bulunamadı");
    }
}
