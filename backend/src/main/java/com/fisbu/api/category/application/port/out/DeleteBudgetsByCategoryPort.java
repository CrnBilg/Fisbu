package com.fisbu.api.category.application.port.out;

// Budget modülü henüz hexagonal'a taşınmadığı için geçici köprü port'u.
// Budget migrate olduğunda bu port kaldırılıp Budget modülünün kendi out-port'u kullanılacak.
public interface DeleteBudgetsByCategoryPort {

    void deleteBudgetsByCategory(Long categoryId);
}
