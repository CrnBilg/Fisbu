package com.fisbu.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SavingsGoalRequest {
    @NotBlank(message = "Hedef adı boş olamaz")
    private String name;

    @NotNull(message = "Hedef tutar boş olamaz")
    @DecimalMin(value = "0.01", message = "Hedef tutar 0'dan büyük olmalıdır")
    private BigDecimal targetAmount;

    private LocalDate targetDate;

    // PERF-001: opsiyonel — client (mobil app) network retry'de aynı isteği tekrar gönderirse
    // bu alanla (bir UUID) aynı kaydı işaret eder; boş bırakılırsa idempotency garantisi yoktur
    // (geriye dönük uyumluluk için zorunlu değil).
    private String idempotencyKey;
}
