package com.fisbu.api.budget.application.port.out;

import java.util.Optional;

import com.fisbu.api.budget.domain.Budget;

public interface LoadBudgetPort {

    Optional<Budget> loadById(Long budgetId);
}
