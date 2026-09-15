package com.fisbu.api.shared.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.fisbu.api.entity.User;
import com.fisbu.api.repository.SavingsGoalRepository;
import com.fisbu.api.repository.UserRepository;
import com.fisbu.api.shared.application.port.out.DeleteSavingsGoalsByUserPort;

/**
 * ARCH-002/ADR-004: SavingsGoal modülü henüz hexagonal'a taşınmadığı için bu
 * adaptör legacy {@code SavingsGoalRepository}'yi geçici olarak {@code shared}
 * altında bir port arkasına sarmalıyor. Modül migrate olduğunda bu sınıf
 * {@code savingsgoal/adapter/out/persistence/}'a taşınmalı.
 */
@Component
public class SavingsGoalDeletionAdapter implements DeleteSavingsGoalsByUserPort {

    private final SavingsGoalRepository savingsGoalRepository;
    private final UserRepository userRepository;

    public SavingsGoalDeletionAdapter(SavingsGoalRepository savingsGoalRepository, UserRepository userRepository) {
        this.savingsGoalRepository = savingsGoalRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void deleteAllByUserId(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        savingsGoalRepository.deleteAll(savingsGoalRepository.findByUser(user));
    }
}
