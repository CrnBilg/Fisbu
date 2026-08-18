package com.fisbu.api.category.adapter.out.receipt;

import org.springframework.stereotype.Component;

import com.fisbu.api.category.application.port.out.UnlinkReceiptsFromCategoryPort;
import com.fisbu.api.receipt.application.port.out.UnlinkCategoryFromReceiptsPort;

// Receipt modülü artık hexagonal olduğu için burada legacy repository'ye değil,
// doğrudan Receipt modülünün kendi out-port'una (UnlinkCategoryFromReceiptsPort) delege ediyoruz
// (bkz. budget/adapter/out/category/BudgetCategoryAdapter — aynı desen).
@Component
public class CategoryReceiptAdapter implements UnlinkReceiptsFromCategoryPort {

    private final UnlinkCategoryFromReceiptsPort unlinkCategoryFromReceiptsPort;

    public CategoryReceiptAdapter(UnlinkCategoryFromReceiptsPort unlinkCategoryFromReceiptsPort) {
        this.unlinkCategoryFromReceiptsPort = unlinkCategoryFromReceiptsPort;
    }

    @Override
    public void unlinkReceiptsFromCategory(Long categoryId) {
        unlinkCategoryFromReceiptsPort.unlinkCategoryFromReceipts(categoryId);
    }
}
