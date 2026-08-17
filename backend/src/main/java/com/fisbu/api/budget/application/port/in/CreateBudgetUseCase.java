package com.fisbu.api.budget.application.port.in;

import com.fisbu.api.dto.BudgetRequest;
import com.fisbu.api.dto.BudgetResponse;

public interface CreateBudgetUseCase {

    BudgetResponse createBudget(String email, BudgetRequest request);
}
