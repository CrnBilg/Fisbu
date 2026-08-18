package com.fisbu.api.receipt.application.port.in;

import com.fisbu.api.receipt.domain.Receipt;

public interface GetReceiptByIdUseCase {

    Receipt getReceiptById(String email, Long receiptId);
}
