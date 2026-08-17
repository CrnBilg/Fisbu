package com.fisbu.api.budget.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.fisbu.api.budget.application.port.out.DeleteBudgetPort;
import com.fisbu.api.budget.application.port.out.FindBudgetByCategoryAndPeriodPort;
import com.fisbu.api.budget.application.port.out.LoadBudgetPort;
import com.fisbu.api.budget.application.port.out.LoadBudgetsPort;
import com.fisbu.api.budget.application.port.out.LoadUserNotificationProfilePort;
import com.fisbu.api.budget.application.port.out.ResolveUserIdPort;
import com.fisbu.api.budget.application.port.out.SaveBudgetPort;
import com.fisbu.api.budget.application.port.out.UserNotificationProfile;
import com.fisbu.api.budget.domain.Budget;
import com.fisbu.api.entity.Category;
import com.fisbu.api.entity.User;
import com.fisbu.api.repository.BudgetRepository;
import com.fisbu.api.repository.CategoryRepository;
import com.fisbu.api.repository.UserRepository;

@Component
public class BudgetPersistenceAdapter implements LoadBudgetsPort, LoadBudgetPort, FindBudgetByCategoryAndPeriodPort,
        SaveBudgetPort, DeleteBudgetPort, ResolveUserIdPort, LoadUserNotificationProfilePort {

    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final BudgetPersistenceMapper mapper;

    public BudgetPersistenceAdapter(BudgetRepository budgetRepository, UserRepository userRepository,
                                     CategoryRepository categoryRepository, BudgetPersistenceMapper mapper) {
        this.budgetRepository = budgetRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.mapper = mapper;
    }

    @Override
    public List<Budget> loadByUserIdAndYearAndMonth(Long userId, int year, int month) {
        User user = requireUser(userId);
        return budgetRepository.findByUserAndYearAndMonth(user, year, month).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Budget> loadByUserId(Long userId) {
        User user = requireUser(userId);
        return budgetRepository.findByUser(user).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Budget> loadById(Long budgetId) {
        return budgetRepository.findById(budgetId).map(mapper::toDomain);
    }

    @Override
    public Optional<Budget> findByUserIdAndCategoryIdAndYearAndMonth(Long userId, Long categoryId, int year, int month) {
        User user = requireUser(userId);
        Category category = requireCategory(categoryId);
        return budgetRepository.findByUserAndCategoryAndYearAndMonth(user, category, year, month).map(mapper::toDomain);
    }

    @Override
    public Budget save(Budget budget) {
        com.fisbu.api.entity.Budget entity = budget.id() != null
                ? budgetRepository.findById(budget.id()).orElseGet(com.fisbu.api.entity.Budget::new)
                : new com.fisbu.api.entity.Budget();

        entity.setUser(requireUser(budget.userId()));
        entity.setCategory(requireCategory(budget.categoryId()));
        entity.setMonthlyLimit(budget.monthlyLimit());
        entity.setYear(budget.year());
        entity.setMonth(budget.month());
        entity.setWarningNotified(Boolean.TRUE.equals(budget.warningNotified()));
        entity.setOverspendNotified(Boolean.TRUE.equals(budget.overspendNotified()));

        return mapper.toDomain(budgetRepository.save(entity));
    }

    @Override
    public void deleteById(Long budgetId) {
        budgetRepository.deleteById(budgetId);
    }

    @Override
    public Optional<Long> resolveUserIdByEmail(String email) {
        return userRepository.findByEmail(email).map(User::getId);
    }

    @Override
    public Optional<UserNotificationProfile> loadProfileByUserId(Long userId) {
        return userRepository.findById(userId).map(user -> new UserNotificationProfile(
                user.getFcmToken(),
                Boolean.TRUE.equals(user.getNotifyBudgetWarning()),
                Boolean.TRUE.equals(user.getNotifyBudgetOverspend())));
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId).orElseThrow();
    }

    private Category requireCategory(Long categoryId) {
        return categoryRepository.findById(categoryId).orElseThrow();
    }
}
