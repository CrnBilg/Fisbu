package com.fisbu.api.receipt.application.port.in;

public interface DeleteReceiptUseCase {

    void deleteReceipt(String email, Long receiptId);
}
