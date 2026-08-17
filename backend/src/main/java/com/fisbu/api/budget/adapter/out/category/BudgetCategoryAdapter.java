package com.fisbu.api.budget.adapter.out.category;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.fisbu.api.budget.application.port.out.LoadOwnedCategoryPort;
import com.fisbu.api.category.application.port.out.LoadCategoryPort;
import com.fisbu.api.category.domain.Category;

// Category modülü zaten hexagonal olduğu için burada legacy repository'ye değil,
// doğrudan Category modülünün kendi out-port'una (LoadCategoryPort) delege ediyoruz.
@Component
public class BudgetCategoryAdapter implements LoadOwnedCategoryPort {

    private final LoadCategoryPort loadCategoryPort;

    public BudgetCategoryAdapter(LoadCategoryPort loadCategoryPort) {
        this.loadCategoryPort = loadCategoryPort;
    }

    @Override
    public Optional<Category> loadById(Long categoryId) {
        return loadCategoryPort.loadById(categoryId);
    }
}
