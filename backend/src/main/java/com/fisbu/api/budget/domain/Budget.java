package com.fisbu.api.budget.domain;

import java.math.BigDecimal;

public record Budget(Long id, Long userId, Long categoryId, BigDecimal monthlyLimit, Integer year, Integer month,
                      Boolean warningNotified, Boolean overspendNotified) {
}
