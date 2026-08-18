package com.fisbu.api.receipt.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

// Bir kategorideki geçmiş fişlere kıyasla yeni bir tutarın normalden çok yüksek/düşük olup
// olmadığını değerlendiren saf domain kuralı — I/O yapmaz. İstatistikleri (adet, ortalama)
// application service sağlar (bkz. ReceiptService.detectAmountAnomaly — MIN_SAMPLE_SIZE'a
// göre ortalama sorgusunu atlayıp atlamayacağına orada karar verilir).
public final class AmountAnomalyPolicy {

    public static final int MIN_SAMPLE_SIZE = 3;

    private static final BigDecimal HIGH_MULTIPLIER = BigDecimal.valueOf(2.5);
    private static final BigDecimal LOW_MULTIPLIER = BigDecimal.valueOf(0.25);
    private static final BigDecimal MIN_AVERAGE = BigDecimal.valueOf(20);

    private AmountAnomalyPolicy() {
    }

    // Uyarı yoksa null döner.
    public static String evaluate(String categoryName, long previousCount, BigDecimal average, BigDecimal newAmount) {
        if (newAmount == null || previousCount < MIN_SAMPLE_SIZE) {
            return null;
        }

        BigDecimal roundedAverage = average.setScale(2, RoundingMode.HALF_UP);
        if (roundedAverage.compareTo(MIN_AVERAGE) < 0) {
            return null;
        }

        if (newAmount.compareTo(roundedAverage.multiply(HIGH_MULTIPLIER)) > 0) {
            return categoryName + " kategorisinde her zamankinden çok daha yüksek bir tutar ("
                    + newAmount.toPlainString() + " TL) — ortalaman " + roundedAverage.toPlainString() + " TL";
        }
        if (newAmount.compareTo(roundedAverage.multiply(LOW_MULTIPLIER)) < 0) {
            return categoryName + " kategorisinde her zamankinden çok daha düşük bir tutar ("
                    + newAmount.toPlainString() + " TL) — ortalaman " + roundedAverage.toPlainString() + " TL";
        }
        return null;
    }
}
