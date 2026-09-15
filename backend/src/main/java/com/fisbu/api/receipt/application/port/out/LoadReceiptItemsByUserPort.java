package com.fisbu.api.receipt.application.port.out;

import java.util.List;

import com.fisbu.api.receipt.domain.ReceiptItem;

/** ARCH-003/ADR-005: kullanıcının tüm fişlerindeki fiş kalemlerini (ReceiptItem) yükler. */
public interface LoadReceiptItemsByUserPort {

    List<ReceiptItem> loadByUserId(Long userId);
}
