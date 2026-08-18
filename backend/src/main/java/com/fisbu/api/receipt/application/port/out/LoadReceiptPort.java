package com.fisbu.api.receipt.application.port.out;

import java.util.Optional;

import com.fisbu.api.receipt.domain.Receipt;

public interface LoadReceiptPort {

    Optional<Receipt> loadById(Long receiptId);
}
