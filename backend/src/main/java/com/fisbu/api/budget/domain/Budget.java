package com.fisbu.api.budget.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Budget(Long id, Long userId, Long categoryId, BigDecimal monthlyLimit, Integer year, Integer month,
                      Boolean warningNotified, Boolean overspendNotified) {

    private static final double WARNING_THRESHOLD_PERCENT = 80.0;
    private static final double OVERSPEND_THRESHOLD_PERCENT = 100.0;

    public enum NotificationEvent {
        NONE, WARNING, OVERSPEND
    }

    // Bir harcama güncellemesinden sonra bu bütçenin bildirim durumunun nasıl değişmesi
    // gerektiğini hesaplayan saf domain kuralı — I/O yapmaz, hangi bildirimin fiilen
    // gönderileceğine (kullanıcı tercihi vb.) karar vermek application service'e ait.
    // Çağıran, monthlyLimit'in pozitif olduğunu garanti etmelidir (limitsiz bütçe için
    // bu metodun çağrılmasına gerek yoktur).
    public SpendEvaluation evaluateSpend(BigDecimal spend) {
        double pct = spend.divide(monthlyLimit, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).doubleValue();
        long pctRounded = Math.round(pct);

        boolean warning = Boolean.TRUE.equals(warningNotified);
        boolean overspend = Boolean.TRUE.equals(overspendNotified);
        NotificationEvent event = NotificationEvent.NONE;
        boolean changed = false;

        if (pct >= OVERSPEND_THRESHOLD_PERCENT) {
            if (!overspend) {
                event = NotificationEvent.OVERSPEND;
                overspend = true;
                warning = true;
                changed = true;
            }
        } else if (pct >= WARNING_THRESHOLD_PERCENT) {
            if (!warning) {
                event = NotificationEvent.WARNING;
                warning = true;
                changed = true;
            }
            // Aşımdan uyarı bandına düştüyse aşım bayrağını sıfırla
            if (overspend) {
                overspend = false;
                changed = true;
            }
        } else {
            // %80 altına döndü → sonraki eşik geçişinde tekrar bildirebilmek için bayrakları sıfırla
            if (warning || overspend) {
                warning = false;
                overspend = false;
                changed = true;
            }
        }

        Budget updated = changed
                ? new Budget(id, userId, categoryId, monthlyLimit, year, month, warning, overspend)
                : this;

        return new SpendEvaluation(updated, changed, event, pctRounded);
    }

    public record SpendEvaluation(Budget budget, boolean changed, NotificationEvent event, long percentRounded) {
    }
}
