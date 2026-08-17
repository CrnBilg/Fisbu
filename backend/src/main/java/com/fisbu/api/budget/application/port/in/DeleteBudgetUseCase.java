package com.fisbu.api.budget.application.port.in;

public interface DeleteBudgetUseCase {

    void deleteBudget(String email, Long budgetId);
}
