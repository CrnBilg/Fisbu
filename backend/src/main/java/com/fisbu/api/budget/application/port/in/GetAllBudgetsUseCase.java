package com.fisbu.api.budget.application.port.in;

import java.util.List;

import com.fisbu.api.dto.BudgetResponse;

// KVKK veri indirme (data export) için — ay filtresi olmadan kullanıcının tüm bütçe geçmişi
public interface GetAllBudgetsUseCase {

    List<BudgetResponse> getAllBudgets(String email);
}
