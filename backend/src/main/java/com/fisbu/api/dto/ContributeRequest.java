package com.fisbu.api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContributeRequest {
    // Negatif değer para çekmek (hedeften düşmek) için kullanılabilir
    @NotNull(message = "Tutar boş olamaz")
    private BigDecimal amount;
}
