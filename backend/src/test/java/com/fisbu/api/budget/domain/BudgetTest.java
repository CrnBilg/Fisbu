package com.fisbu.api.budget.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class BudgetTest {

    private Budget budget(boolean warningNotified, boolean overspendNotified) {
        return new Budget(1L, 1L, 1L, BigDecimal.valueOf(1000), 2026, 8, warningNotified, overspendNotified);
    }

    @Test
    void staysNone_whenSpendBelowWarningThreshold() {
        Budget.SpendEvaluation result = budget(false, false).evaluateSpend(BigDecimal.valueOf(500));

        assertThat(result.event()).isEqualTo(Budget.NotificationEvent.NONE);
        assertThat(result.changed()).isFalse();
        assertThat(result.percentRounded()).isEqualTo(50);
    }

    @Test
    void firesWarning_whenCrossing80PercentForTheFirstTime() {
        Budget.SpendEvaluation result = budget(false, false).evaluateSpend(BigDecimal.valueOf(850));

        assertThat(result.event()).isEqualTo(Budget.NotificationEvent.WARNING);
        assertThat(result.changed()).isTrue();
        assertThat(result.budget().warningNotified()).isTrue();
        assertThat(result.budget().overspendNotified()).isFalse();
    }

    @Test
    void doesNotRefireWarning_whenAlreadyWarnedAndStillInWarningBand() {
        Budget.SpendEvaluation result = budget(true, false).evaluateSpend(BigDecimal.valueOf(900));

        assertThat(result.event()).isEqualTo(Budget.NotificationEvent.NONE);
        assertThat(result.changed()).isFalse();
    }

    @Test
    void firesOverspend_whenCrossing100PercentForTheFirstTime() {
        Budget.SpendEvaluation result = budget(true, false).evaluateSpend(BigDecimal.valueOf(1200));

        assertThat(result.event()).isEqualTo(Budget.NotificationEvent.OVERSPEND);
        assertThat(result.changed()).isTrue();
        assertThat(result.budget().warningNotified()).isTrue();
        assertThat(result.budget().overspendNotified()).isTrue();
    }

    @Test
    void doesNotRefireOverspend_whenAlreadyOverspendNotified() {
        Budget.SpendEvaluation result = budget(true, true).evaluateSpend(BigDecimal.valueOf(1500));

        assertThat(result.event()).isEqualTo(Budget.NotificationEvent.NONE);
        assertThat(result.changed()).isFalse();
    }

    @Test
    void clearsOverspendFlag_whenDroppingBackIntoWarningBand() {
        // Örn. limit yükseltilince yüzde 100'ün altına, ama hâlâ 80'in üstüne düşer
        Budget.SpendEvaluation result = budget(true, true).evaluateSpend(BigDecimal.valueOf(850));

        assertThat(result.event()).isEqualTo(Budget.NotificationEvent.NONE);
        assertThat(result.changed()).isTrue();
        assertThat(result.budget().warningNotified()).isTrue();
        assertThat(result.budget().overspendNotified()).isFalse();
    }

    @Test
    void clearsBothFlags_whenDroppingBelowWarningThreshold() {
        Budget.SpendEvaluation result = budget(true, true).evaluateSpend(BigDecimal.valueOf(200));

        assertThat(result.event()).isEqualTo(Budget.NotificationEvent.NONE);
        assertThat(result.changed()).isTrue();
        assertThat(result.budget().warningNotified()).isFalse();
        assertThat(result.budget().overspendNotified()).isFalse();
    }

    @Test
    void doesNothing_whenAlreadyBelowThresholdAndNoFlagsSet() {
        Budget original = budget(false, false);
        Budget.SpendEvaluation result = original.evaluateSpend(BigDecimal.valueOf(100));

        assertThat(result.changed()).isFalse();
        assertThat(result.budget()).isSameAs(original);
    }
}
