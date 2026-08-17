package com.fisbu.api.budget.application.port.in;

import com.fisbu.api.dto.BudgetSuggestionResponse;

public interface GetBudgetSuggestionUseCase {

    BudgetSuggestionResponse getBudgetSuggestion(String email, Long categoryId, Integer year, Integer month);
}
