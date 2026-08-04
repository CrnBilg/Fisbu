package com.fisbu.api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fisbu.api.entity.SavingsGoal;
import com.fisbu.api.entity.User;

public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {
    List<SavingsGoal> findByUser(User user);
}
