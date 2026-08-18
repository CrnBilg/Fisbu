package com.fisbu.api.receipt.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.fisbu.api.receipt.domain.Receipt;

public interface FindDuplicateReceiptPort {

    List<Receipt> findDuplicates(Long userId, String storeName, BigDecimal totalAmount, LocalDate receiptDate);
}
