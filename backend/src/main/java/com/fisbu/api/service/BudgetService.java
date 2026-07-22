package com.fisbu.api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fisbu.api.dto.BudgetRequest;
import com.fisbu.api.dto.BudgetResponse;
import com.fisbu.api.entity.Budget;
import com.fisbu.api.entity.Category;
import com.fisbu.api.entity.Receipt;
import com.fisbu.api.entity.User;
import com.fisbu.api.repository.BudgetRepository;
import com.fisbu.api.repository.CategoryRepository;
import com.fisbu.api.repository.ReceiptRepository;
import com.fisbu.api.repository.UserRepository;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ReceiptRepository receiptRepository;

    public BudgetService(BudgetRepository budgetRepository, UserRepository userRepository,
                          CategoryRepository categoryRepository, ReceiptRepository receiptRepository) {
        this.budgetRepository = budgetRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.receiptRepository = receiptRepository;
    }

    public List<BudgetResponse> getBudgets(String email, Integer year, Integer month) {
        User user = getUserByEmail(email);

        LocalDate today = LocalDate.now();
        int resolvedYear = year != null ? year : today.getYear();
        int resolvedMonth = month != null ? month : today.getMonthValue();

        return budgetRepository.findByUserAndYearAndMonth(user, resolvedYear, resolvedMonth)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public BudgetResponse createBudget(String email, BudgetRequest request) {
        User user = getUserByEmail(email);
        Category category = getOwnedCategory(user, request.getCategoryId());

        budgetRepository.findByUserAndCategoryAndYearAndMonth(user, category, request.getYear(), request.getMonth())
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Bu kategori için bu ay zaten bir bütçe tanımlı");
                });

        Budget budget = new Budget();
        budget.setUser(user);
        budget.setCategory(category);
        budget.setMonthlyLimit(request.getMonthlyLimit());
        budget.setYear(request.getYear());
        budget.setMonth(request.getMonth());

        return toResponse(budgetRepository.save(budget));
    }

    public BudgetResponse updateBudget(String email, Long budgetId, BudgetRequest request) {
        User user = getUserByEmail(email);
        Budget budget = getOwnedBudget(user, budgetId);
        Category category = getOwnedCategory(user, request.getCategoryId());

        budget.setCategory(category);
        budget.setMonthlyLimit(request.getMonthlyLimit());
        budget.setYear(request.getYear());
        budget.setMonth(request.getMonth());

        return toResponse(budgetRepository.save(budget));
    }

    public void deleteBudget(String email, Long budgetId) {
        User user = getUserByEmail(email);
        Budget budget = getOwnedBudget(user, budgetId);
        budgetRepository.delete(budget);
    }

    private BigDecimal calculateSpend(User user, Category category, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        List<Receipt> receipts = receiptRepository.findByUserAndCategoryAndReceiptDateBetween(
                user, category, start, end);

        BigDecimal total = BigDecimal.ZERO;
        for (Receipt receipt : receipts) {
            if (receipt.getTotalAmount() != null) {
                total = total.add(receipt.getTotalAmount());
            }
        }
        return total;
    }

    private BudgetResponse toResponse(Budget budget) {
        BigDecimal spend = calculateSpend(budget.getUser(), budget.getCategory(), budget.getYear(), budget.getMonth());
        BigDecimal limit = budget.getMonthlyLimit();

        double percentage = limit.signum() > 0
                ? spend.divide(limit, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue()
                : 0.0;

        BudgetResponse response = new BudgetResponse();
        response.setId(budget.getId());
        response.setCategoryId(budget.getCategory().getId());
        response.setCategoryName(budget.getCategory().getName());
        response.setCategoryColor(budget.getCategory().getColor());
        response.setMonthlyLimit(limit);
        response.setYear(budget.getYear());
        response.setMonth(budget.getMonth());
        response.setCurrentSpend(spend);
        response.setPercentage(percentage);
        response.setOverBudget(spend.compareTo(limit) > 0);
        return response;
    }

    private Category getOwnedCategory(User user, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kategori bulunamadı"));

        if (!category.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bu kategoriye erişim yetkiniz yok");
        }

        return category;
    }

    private Budget getOwnedBudget(User user, Long budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bütçe bulunamadı"));

        if (!budget.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bu bütçeye erişim yetkiniz yok");
        }

        return budget;
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kullanıcı bulunamadı"));
    }
}
