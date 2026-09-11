package com.fisbu.api.shared.application.port.out;

import java.util.Optional;

import com.fisbu.api.category.domain.Category;

// ARCH-001: budget ve receipt modüllerinde ayrı ayrı tanımlanmış birebir aynı iki port
// buraya birleştirildi (bkz. ADR-001). Category modülü zaten hexagonal olduğu için bu bir
// köprü değil: adaptörü doğrudan Category modülünün kendi LoadCategoryPort'unu sarmalar.
public interface LoadOwnedCategoryPort {

    Optional<Category> loadById(Long categoryId);
}
