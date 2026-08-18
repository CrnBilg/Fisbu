package com.fisbu.api.receipt.application.port.out;

import java.math.BigDecimal;

// Budget modülünün SumReceiptSpendPort'u için gerçek implementasyonu sağlayan port
// (bkz. budget/adapter/out/receipt/BudgetReceiptSpendAdapter) — artık legacy ReceiptRepository'ye
// değil, buraya delege ediyor.
public interface SumReceiptSpendByCategoryPort {

    BigDecimal sumSpend(Long userId, Long categoryId, int year, int month);
}
