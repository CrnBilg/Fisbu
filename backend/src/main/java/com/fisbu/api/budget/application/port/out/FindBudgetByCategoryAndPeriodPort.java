package com.fisbu.api.budget.application.port.out;

import java.util.Optional;

import com.fisbu.api.budget.domain.Budget;

public interface FindBudgetByCategoryAndPeriodPort {

    Optional<Budget> findByUserIdAndCategoryIdAndYearAndMonth(Long userId, Long categoryId, int year, int month);
}
