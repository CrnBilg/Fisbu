package com.fisbu.api.receipt.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class AmountAnomalyPolicyTest {

    @Test
    void returnsNull_whenSampleSizeIsBelowMinimum() {
        String warning = AmountAnomalyPolicy.evaluate("Market", 2, BigDecimal.valueOf(100), BigDecimal.valueOf(500));

        assertThat(warning).isNull();
    }

    @Test
    void returnsNull_whenAverageIsBelowMinimumAverage() {
        // Ortalama 15 TL (MIN_AVERAGE=20'nin altı) — küçük tutarlarda anomali sinyali gürültülü olur
        String warning = AmountAnomalyPolicy.evaluate("Kafe", 5, BigDecimal.valueOf(15), BigDecimal.valueOf(50));

        assertThat(warning).isNull();
    }

    @Test
    void returnsNull_whenAmountIsNull() {
        String warning = AmountAnomalyPolicy.evaluate("Market", 5, BigDecimal.valueOf(100), null);

        assertThat(warning).isNull();
    }

    @Test
    void returnsNull_whenAmountIsWithinNormalRange() {
        String warning = AmountAnomalyPolicy.evaluate("Market", 5, BigDecimal.valueOf(100), BigDecimal.valueOf(150));

        assertThat(warning).isNull();
    }

    @Test
    void warnsHigh_whenAmountExceedsHighMultiplier() {
        // Ortalamanın 2.5 katından fazla
        String warning = AmountAnomalyPolicy.evaluate("Market", 5, BigDecimal.valueOf(100), BigDecimal.valueOf(300));

        assertThat(warning).contains("çok daha yüksek").contains("Market");
    }

    @Test
    void warnsLow_whenAmountIsBelowLowMultiplier() {
        // Ortalamanın 0.25 katından az
        String warning = AmountAnomalyPolicy.evaluate("Market", 5, BigDecimal.valueOf(100), BigDecimal.valueOf(20));

        assertThat(warning).contains("çok daha düşük").contains("Market");
    }

    @Test
    void doesNotWarn_exactlyAtHighMultiplierBoundary() {
        // Tam 2.5 kat — sıkı sınır ("büyük", eşit değil), bu yüzden anomali sayılmamalı
        String warning = AmountAnomalyPolicy.evaluate("Market", 5, BigDecimal.valueOf(100), BigDecimal.valueOf(250));

        assertThat(warning).isNull();
    }
}
