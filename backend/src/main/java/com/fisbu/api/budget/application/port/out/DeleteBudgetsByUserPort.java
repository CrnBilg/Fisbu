package com.fisbu.api.budget.application.port.out;

/** ARCH-002/ADR-004: hesap silme akışında kullanıcının tüm bütçelerini siler. */
public interface DeleteBudgetsByUserPort {

    void deleteAllByUserId(Long userId);
}
