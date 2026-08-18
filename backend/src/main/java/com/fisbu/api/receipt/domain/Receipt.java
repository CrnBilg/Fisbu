package com.fisbu.api.receipt.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

// categoryName alanı bilinçli olarak denormalize: persistence adaptörü zaten
// entity.Receipt'i {category, user} EntityGraph'ıyla tek sorguda çekiyor, bu yüzden
// listeleme akışlarında (getReceipts/searchReceipts) her fiş için ayrı bir kategori
// sorgusu (N+1) açmak yerine ismi burada taşıyoruz.
public record Receipt(Long id, Long userId, Long categoryId, String categoryName, String storeName,
                       BigDecimal totalAmount, LocalDate receiptDate, String imageUrl, String rawOcrText,
                       String splitDetailsJson, LocalDateTime createdAt, LocalDate returnDeadline,
                       LocalDate warrantyExpiryDate, Boolean returnReminderSent, Boolean warrantyReminderSent,
                       List<ReceiptItem> items) {

    // "Duplicate fiş" kuralı: aynı mağaza (case-insensitive) + aynı tutar + aynı tarih.
    // Application service, port'tan gelen adayları bu kurala göre süzer — böylece "iki fiş ne
    // zaman aynı sayılır" tanımı sorgu ismine değil buraya, domain'e ait olur.
    public boolean matchesForDuplicate(String storeName, BigDecimal totalAmount, LocalDate receiptDate) {
        return this.storeName != null && this.storeName.equalsIgnoreCase(storeName)
                && Objects.equals(this.totalAmount, totalAmount)
                && Objects.equals(this.receiptDate, receiptDate);
    }
}
