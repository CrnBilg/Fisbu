package com.fisbu.api.receipt.domain.exception;

import com.fisbu.api.shared.domain.exception.ForbiddenException;

public class ReceiptAccessDeniedException extends ForbiddenException {

    public ReceiptAccessDeniedException() {
        super("Bu fişe erişim yetkiniz yok");
    }

    public ReceiptAccessDeniedException(String message) {
        super(message);
    }
}
