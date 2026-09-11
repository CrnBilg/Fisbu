package com.fisbu.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fisbu.api.entity.SavingsGoal;
import com.fisbu.api.entity.User;

public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {
    List<SavingsGoal> findByUser(User user);

    // PERF-001: idempotency kontrolü — aynı kullanıcı+key ile önceden oluşturulmuş hedef var mı
    Optional<SavingsGoal> findByUserAndIdempotencyKey(User user, String idempotencyKey);
}
