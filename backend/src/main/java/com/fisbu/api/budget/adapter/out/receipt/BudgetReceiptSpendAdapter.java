package com.fisbu.api.budget.adapter.out.receipt;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.fisbu.api.budget.application.port.out.SumReceiptSpendPort;
import com.fisbu.api.receipt.application.port.out.SumReceiptSpendByCategoryPort;

// Receipt modülü artık hexagonal olduğu için burada legacy repository'ye değil,
// doğrudan Receipt modülünün kendi out-port'una (SumReceiptSpendByCategoryPort) delege ediyoruz
// (bkz. budget/adapter/out/category/BudgetCategoryAdapter — aynı desen).
@Component
public class BudgetReceiptSpendAdapter implements SumReceiptSpendPort {

    private final SumReceiptSpendByCategoryPort sumReceiptSpendByCategoryPort;

    public BudgetReceiptSpendAdapter(SumReceiptSpendByCategoryPort sumReceiptSpendByCategoryPort) {
        this.sumReceiptSpendByCategoryPort = sumReceiptSpendByCategoryPort;
    }

    @Override
    public BigDecimal sumSpend(Long userId, Long categoryId, int year, int month) {
        return sumReceiptSpendByCategoryPort.sumSpend(userId, categoryId, year, month);
    }
}
