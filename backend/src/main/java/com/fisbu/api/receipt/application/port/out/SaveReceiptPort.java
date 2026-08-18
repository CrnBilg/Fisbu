package com.fisbu.api.receipt.application.port.out;

import com.fisbu.api.receipt.domain.Receipt;

public interface SaveReceiptPort {

    Receipt save(Receipt receipt);
}
