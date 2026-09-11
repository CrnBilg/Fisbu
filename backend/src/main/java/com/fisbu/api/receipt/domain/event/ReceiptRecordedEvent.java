package com.fisbu.api.receipt.domain.event;

// ARCH-001/ADR-001: receipt modülünün budget modülünün in-port'unu doğrudan çağırmasını
// (döngüsel bağımlılık) ortadan kaldırmak için — receipt artık sadece bu event'i yayınlar,
// hangi modülün dinlediğini bilmez/bilmesi gerekmez.
public record ReceiptRecordedEvent(Long userId, Long categoryId, int year, int month) {
}
