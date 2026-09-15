package com.fisbu.api.shared.application.port.out;

/**
 * ARCH-002/ADR-004: hesap silme akışında kullanıcının tüm tasarruf hedeflerini siler.
 * SavingsGoal modülü henüz hexagonal'a taşınmadığı için bu port geçici olarak
 * {@code shared} altında tanımlanıyor — modül migrate olduğunda kendi
 * {@code savingsgoal/application/port/out/} paketine taşınmalı (bkz. ADR-004).
 */
public interface DeleteSavingsGoalsByUserPort {

    void deleteAllByUserId(Long userId);
}
