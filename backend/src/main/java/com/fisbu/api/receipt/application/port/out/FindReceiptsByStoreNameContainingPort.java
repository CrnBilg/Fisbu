package com.fisbu.api.receipt.application.port.out;

import java.util.List;

import com.fisbu.api.receipt.domain.Receipt;

public interface FindReceiptsByStoreNameContainingPort {

    List<Receipt> findByUserIdAndStoreNameContaining(Long userId, String storeName);
}
