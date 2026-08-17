package com.fisbu.api.budget.application.port.out;

import java.math.BigDecimal;

// Receipt modülü henüz hexagonal'a taşınmadığı için geçici köprü port'u.
// Receipt migrate olduğunda bu port kaldırılıp Receipt modülünün kendi out-port'u kullanılacak.
public interface SumReceiptSpendPort {

    BigDecimal sumSpend(Long userId, Long categoryId, int year, int month);
}
