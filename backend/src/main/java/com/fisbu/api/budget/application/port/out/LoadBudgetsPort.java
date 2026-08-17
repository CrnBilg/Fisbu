package com.fisbu.api.budget.application.port.out;

import java.util.List;

import com.fisbu.api.budget.domain.Budget;

public interface LoadBudgetsPort {

    List<Budget> loadByUserIdAndYearAndMonth(Long userId, int year, int month);

    List<Budget> loadByUserId(Long userId);
}
