package com.fisbu.api.receipt.application.port.out;

/** ARCH-002/ADR-004: hesap silme akışında kullanıcının tüm receipt'lerini siler. */
public interface DeleteReceiptsByUserPort {

    void deleteAllByUserId(Long userId);
}
