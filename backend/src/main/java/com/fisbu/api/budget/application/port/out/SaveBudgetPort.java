package com.fisbu.api.budget.application.port.out;

import com.fisbu.api.budget.domain.Budget;

public interface SaveBudgetPort {

    Budget save(Budget budget);
}
