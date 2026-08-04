package com.fisbu.api.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

// Kategori kimlikleri kullanıcı başına farklı olduğundan (her üye kendi Category'sine sahip)
// household istatistiğinde kategori adına göre gruplanır, ID taşınmaz
@Getter
@Setter
public class HouseholdCategoryTotalResponse {
    private String categoryName;
    private BigDecimal totalAmount;
}
