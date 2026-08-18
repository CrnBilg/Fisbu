package com.fisbu.api.receipt.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class ReceiptTest {

    private Receipt receipt(String storeName, BigDecimal totalAmount, LocalDate receiptDate) {
        return new Receipt(1L, 1L, null, null, storeName, totalAmount, receiptDate, null, null, null, null,
                null, null, false, false, List.of());
    }

    @Test
    void matches_whenStoreAmountAndDateAreIdentical() {
        Receipt receipt = receipt("Migros", BigDecimal.valueOf(150.50), LocalDate.of(2026, 8, 10));

        assertThat(receipt.matchesForDuplicate("Migros", BigDecimal.valueOf(150.50), LocalDate.of(2026, 8, 10)))
                .isTrue();
    }

    @Test
    void matches_whenStoreNameDiffersOnlyByCase() {
        Receipt receipt = receipt("Migros", BigDecimal.valueOf(150.50), LocalDate.of(2026, 8, 10));

        assertThat(receipt.matchesForDuplicate("MIGROS", BigDecimal.valueOf(150.50), LocalDate.of(2026, 8, 10)))
                .isTrue();
    }

    @Test
    void doesNotMatch_whenAmountDiffers() {
        Receipt receipt = receipt("Migros", BigDecimal.valueOf(150.50), LocalDate.of(2026, 8, 10));

        assertThat(receipt.matchesForDuplicate("Migros", BigDecimal.valueOf(150.51), LocalDate.of(2026, 8, 10)))
                .isFalse();
    }

    @Test
    void doesNotMatch_whenDateDiffers() {
        Receipt receipt = receipt("Migros", BigDecimal.valueOf(150.50), LocalDate.of(2026, 8, 10));

        assertThat(receipt.matchesForDuplicate("Migros", BigDecimal.valueOf(150.50), LocalDate.of(2026, 8, 11)))
                .isFalse();
    }

    @Test
    void doesNotMatch_whenStoreNameDiffers() {
        Receipt receipt = receipt("Migros", BigDecimal.valueOf(150.50), LocalDate.of(2026, 8, 10));

        assertThat(receipt.matchesForDuplicate("Bim", BigDecimal.valueOf(150.50), LocalDate.of(2026, 8, 10)))
                .isFalse();
    }

    @Test
    void doesNotMatch_whenReceiptHasNoStoreName() {
        Receipt receipt = receipt(null, BigDecimal.valueOf(150.50), LocalDate.of(2026, 8, 10));

        assertThat(receipt.matchesForDuplicate("Migros", BigDecimal.valueOf(150.50), LocalDate.of(2026, 8, 10)))
                .isFalse();
    }
}
