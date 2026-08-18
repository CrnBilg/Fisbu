package com.fisbu.api.receipt.adapter.out.category;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.fisbu.api.category.application.port.out.LoadCategoryPort;
import com.fisbu.api.category.domain.Category;
import com.fisbu.api.receipt.application.port.out.LoadOwnedCategoryPort;

// Category modülü zaten hexagonal olduğu için burada legacy repository'ye değil,
// doğrudan Category modülünün kendi out-port'una (LoadCategoryPort) delege ediyoruz
// (bkz. budget/adapter/out/category/BudgetCategoryAdapter — aynı desen).
@Component
public class ReceiptCategoryAdapter implements LoadOwnedCategoryPort {

    private final LoadCategoryPort loadCategoryPort;

    public ReceiptCategoryAdapter(LoadCategoryPort loadCategoryPort) {
        this.loadCategoryPort = loadCategoryPort;
    }

    @Override
    public Optional<Category> loadById(Long categoryId) {
        return loadCategoryPort.loadById(categoryId);
    }
}
