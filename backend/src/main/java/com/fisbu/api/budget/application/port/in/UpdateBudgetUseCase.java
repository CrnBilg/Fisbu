package com.fisbu.api.budget.application.port.in;

import com.fisbu.api.dto.BudgetRequest;
import com.fisbu.api.dto.BudgetResponse;

public interface UpdateBudgetUseCase {

    BudgetResponse updateBudget(String email, Long budgetId, BudgetRequest request);
}
