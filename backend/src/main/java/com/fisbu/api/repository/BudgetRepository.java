package com.fisbu.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fisbu.api.entity.Budget;
import com.fisbu.api.entity.Category;
import com.fisbu.api.entity.User;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByUserAndYearAndMonth(User user, Integer year, Integer month);
    Optional<Budget> findByUserAndCategoryAndYearAndMonth(User user, Category category, Integer year, Integer month);
}
