package com.fisbu.api.budget.application.port.in;

import java.util.List;

import com.fisbu.api.dto.BudgetResponse;

public interface GetBudgetsUseCase {

    List<BudgetResponse> getBudgets(String email, Integer year, Integer month);
}
