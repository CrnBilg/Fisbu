package com.fisbu.api.budget.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fisbu.api.budget.application.port.in.CheckBudgetThresholdUseCase;
import com.fisbu.api.budget.application.port.in.CreateBudgetUseCase;
import com.fisbu.api.budget.application.port.in.DeleteBudgetUseCase;
import com.fisbu.api.budget.application.port.in.GetAllBudgetsUseCase;
import com.fisbu.api.budget.application.port.in.GetBudgetSuggestionUseCase;
import com.fisbu.api.budget.application.port.in.GetBudgetsUseCase;
import com.fisbu.api.budget.application.port.in.UpdateBudgetUseCase;
import com.fisbu.api.budget.application.port.out.DeleteBudgetPort;
import com.fisbu.api.budget.application.port.out.FindBudgetByCategoryAndPeriodPort;
import com.fisbu.api.budget.application.port.out.GenerateSuggestionCommentPort;
import com.fisbu.api.budget.application.port.out.LoadBudgetPort;
import com.fisbu.api.budget.application.port.out.LoadBudgetsPort;
import com.fisbu.api.budget.application.port.out.LoadOwnedCategoryPort;
import com.fisbu.api.budget.application.port.out.LoadUserNotificationProfilePort;
import com.fisbu.api.budget.application.port.out.ResolveUserIdPort;
import com.fisbu.api.budget.application.port.out.SaveBudgetPort;
import com.fisbu.api.budget.application.port.out.SendBudgetNotificationPort;
import com.fisbu.api.budget.application.port.out.SumReceiptSpendPort;
import com.fisbu.api.budget.application.port.out.UserNotificationProfile;
import com.fisbu.api.budget.domain.Budget;
import com.fisbu.api.budget.domain.exception.BudgetAccessDeniedException;
import com.fisbu.api.budget.domain.exception.BudgetNotFoundException;
import com.fisbu.api.budget.domain.exception.CategoryAccessDeniedException;
import com.fisbu.api.budget.domain.exception.CategoryNotFoundException;
import com.fisbu.api.budget.domain.exception.DuplicateBudgetException;
import com.fisbu.api.budget.domain.exception.UserNotFoundException;
import com.fisbu.api.category.domain.Category;
import com.fisbu.api.dto.BudgetRequest;
import com.fisbu.api.dto.BudgetResponse;
import com.fisbu.api.dto.BudgetSuggestionResponse;

@Service
public class BudgetService implements GetBudgetsUseCase, GetAllBudgetsUseCase, CreateBudgetUseCase,
        UpdateBudgetUseCase, DeleteBudgetUseCase, GetBudgetSuggestionUseCase, CheckBudgetThresholdUseCase {

    private static final int SUGGESTION_LOOKBACK_MONTHS = 3;
    private static final BigDecimal ROUNDING_STEP = BigDecimal.valueOf(50);

    private final ResolveUserIdPort resolveUserIdPort;
    private final LoadUserNotificationProfilePort loadUserNotificationProfilePort;
    private final LoadOwnedCategoryPort loadOwnedCategoryPort;
    private final SumReceiptSpendPort sumReceiptSpendPort;
    private final LoadBudgetsPort loadBudgetsPort;
    private final LoadBudgetPort loadBudgetPort;
    private final FindBudgetByCategoryAndPeriodPort findBudgetByCategoryAndPeriodPort;
    private final SaveBudgetPort saveBudgetPort;
    private final DeleteBudgetPort deleteBudgetPort;
    private final SendBudgetNotificationPort sendBudgetNotificationPort;
    private final GenerateSuggestionCommentPort generateSuggestionCommentPort;

    public BudgetService(ResolveUserIdPort resolveUserIdPort,
                          LoadUserNotificationProfilePort loadUserNotificationProfilePort,
                          LoadOwnedCategoryPort loadOwnedCategoryPort, SumReceiptSpendPort sumReceiptSpendPort,
                          LoadBudgetsPort loadBudgetsPort, LoadBudgetPort loadBudgetPort,
                          FindBudgetByCategoryAndPeriodPort findBudgetByCategoryAndPeriodPort,
                          SaveBudgetPort saveBudgetPort, DeleteBudgetPort deleteBudgetPort,
                          SendBudgetNotificationPort sendBudgetNotificationPort,
                          GenerateSuggestionCommentPort generateSuggestionCommentPort) {
        this.resolveUserIdPort = resolveUserIdPort;
        this.loadUserNotificationProfilePort = loadUserNotificationProfilePort;
        this.loadOwnedCategoryPort = loadOwnedCategoryPort;
        this.sumReceiptSpendPort = sumReceiptSpendPort;
        this.loadBudgetsPort = loadBudgetsPort;
        this.loadBudgetPort = loadBudgetPort;
        this.findBudgetByCategoryAndPeriodPort = findBudgetByCategoryAndPeriodPort;
        this.saveBudgetPort = saveBudgetPort;
        this.deleteBudgetPort = deleteBudgetPort;
        this.sendBudgetNotificationPort = sendBudgetNotificationPort;
        this.generateSuggestionCommentPort = generateSuggestionCommentPort;
    }

    @Override
    public List<BudgetResponse> getBudgets(String email, Integer year, Integer month) {
        Long userId = resolveUserId(email);

        LocalDate today = LocalDate.now();
        int resolvedYear = year != null ? year : today.getYear();
        int resolvedMonth = month != null ? month : today.getMonthValue();

        return loadBudgetsPort.loadByUserIdAndYearAndMonth(userId, resolvedYear, resolvedMonth)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<BudgetResponse> getAllBudgets(String email) {
        Long userId = resolveUserId(email);
        return loadBudgetsPort.loadByUserId(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public BudgetResponse createBudget(String email, BudgetRequest request) {
        Long userId = resolveUserId(email);
        Category category = getOwnedCategory(userId, request.getCategoryId());

        findBudgetByCategoryAndPeriodPort
                .findByUserIdAndCategoryIdAndYearAndMonth(userId, category.id(), request.getYear(), request.getMonth())
                .ifPresent(existing -> {
                    throw new DuplicateBudgetException();
                });

        Budget budget = new Budget(null, userId, category.id(), request.getMonthlyLimit(), request.getYear(),
                request.getMonth(), false, false);

        return toResponse(saveBudgetPort.save(budget));
    }

    @Override
    public BudgetResponse updateBudget(String email, Long budgetId, BudgetRequest request) {
        Long userId = resolveUserId(email);
        Budget existing = getOwnedBudget(userId, budgetId);
        Category category = getOwnedCategory(userId, request.getCategoryId());

        Budget updated = new Budget(existing.id(), userId, category.id(), request.getMonthlyLimit(),
                request.getYear(), request.getMonth(), existing.warningNotified(), existing.overspendNotified());

        return toResponse(saveBudgetPort.save(updated));
    }

    @Override
    public void deleteBudget(String email, Long budgetId) {
        Long userId = resolveUserId(email);
        Budget budget = getOwnedBudget(userId, budgetId);
        deleteBudgetPort.deleteById(budget.id());
    }

    @Override
    public void checkAndNotify(Long userId, Long categoryId, int year, int month) {
        Optional<Budget> budgetOpt =
                findBudgetByCategoryAndPeriodPort.findByUserIdAndCategoryIdAndYearAndMonth(userId, categoryId, year, month);
        if (budgetOpt.isEmpty()) return;

        Budget budget = budgetOpt.get();
        BigDecimal limit = budget.monthlyLimit();
        if (limit == null || limit.signum() <= 0) return;

        Category category = loadOwnedCategoryPort.loadById(categoryId).orElseThrow(CategoryNotFoundException::new);
        UserNotificationProfile profile = loadUserNotificationProfilePort.loadProfileByUserId(userId)
                .orElseThrow(UserNotFoundException::new);

        BigDecimal spend = sumReceiptSpendPort.sumSpend(userId, categoryId, year, month);
        Budget.SpendEvaluation evaluation = budget.evaluateSpend(spend);

        switch (evaluation.event()) {
            case OVERSPEND -> {
                if (profile.notifyBudgetOverspend()) {
                    sendBudgetNotificationPort.send(profile.fcmToken(), "Bütçe Aşıldı",
                            category.name() + " bütçeni aştın! (şu an %" + evaluation.percentRounded() + " kullanıldı)");
                }
            }
            case WARNING -> {
                if (profile.notifyBudgetWarning()) {
                    sendBudgetNotificationPort.send(profile.fcmToken(), "Bütçe Uyarısı",
                            category.name() + " bütçenin %80'ine ulaştın (şu an %" + evaluation.percentRounded() + ")");
                }
            }
            case NONE -> {
            }
        }

        if (evaluation.changed()) {
            saveBudgetPort.save(evaluation.budget());
        }
    }

    /**
     * Son 3 ayda harcama olan ayların ortalamasına göre bu kategori için bir bütçe önerisi üretir
     * (3 aydan biri hiç harcama içermiyorsa ortalama sadece veri olan aylara bölünür, aksi halde
     * ortalama olması gerekenden düşük çıkar). Öneri tutarı deterministik hesaplanır (AI'a bırakılmaz,
     * hatalı/tutarsız rakam riskini önler); AI sadece bu rakamı açıklayan kısa bir not yazmak için
     * kullanılır ve o adım başarısız olursa (Groq yapılandırılmamış/kesinti) sabit bir metne düşülür,
     * öneri yine de döner.
     */
    @Override
    public BudgetSuggestionResponse getBudgetSuggestion(String email, Long categoryId, Integer year, Integer month) {
        Long userId = resolveUserId(email);
        Category category = getOwnedCategory(userId, categoryId);

        LocalDate today = LocalDate.now();
        int resolvedYear = year != null ? year : today.getYear();
        int resolvedMonth = month != null ? month : today.getMonthValue();
        LocalDate targetMonth = LocalDate.of(resolvedYear, resolvedMonth, 1);

        BigDecimal total = BigDecimal.ZERO;
        int monthsWithData = 0;
        for (int i = 1; i <= SUGGESTION_LOOKBACK_MONTHS; i++) {
            LocalDate pastMonth = targetMonth.minusMonths(i);
            BigDecimal spend = sumReceiptSpendPort.sumSpend(userId, category.id(), pastMonth.getYear(), pastMonth.getMonthValue());
            if (spend.signum() > 0) {
                monthsWithData++;
            }
            total = total.add(spend);
        }

        BudgetSuggestionResponse response = new BudgetSuggestionResponse();
        response.setCategoryId(category.id());
        response.setCategoryName(category.name());
        response.setMonthsAnalyzed(monthsWithData);

        if (monthsWithData == 0) {
            response.setComment("Bu kategori için son " + SUGGESTION_LOOKBACK_MONTHS
                    + " ayda yeterli harcama verisi yok, öneri sunulamıyor");
            return response;
        }

        BigDecimal average = total.divide(BigDecimal.valueOf(monthsWithData), 2, RoundingMode.HALF_UP);
        BigDecimal suggestedLimit = average
                .divide(ROUNDING_STEP, 0, RoundingMode.UP)
                .multiply(ROUNDING_STEP);

        response.setAverageSpend(average);
        response.setSuggestedLimit(suggestedLimit);
        response.setComment(buildSuggestionComment(category.name(), average, suggestedLimit));
        return response;
    }

    private String buildSuggestionComment(String categoryName, BigDecimal average, BigDecimal suggestedLimit) {
        String fallback = "Son " + SUGGESTION_LOOKBACK_MONTHS + " ayın ortalamasına göre " + categoryName
                + " için " + suggestedLimit + " TL bütçe öneriyoruz";

        if (!generateSuggestionCommentPort.isConfigured()) {
            return fallback;
        }

        try {
            String prompt = """
                    Kullanıcının son %d ayda "%s" kategorisindeki ortalama aylık harcaması %s TL. \
                    Buna göre %s TL'lik bir aylık bütçe öneriyoruz. Kullanıcıya bunu açıklayan, \
                    samimi ve kısa (tek cümle), Türkçe bir not yaz. Sadece cümleyi yaz, başka \
                    açıklama ekleme. Yanıtın SADECE Türkçe olmalı, başka dilden tek kelime bile kullanma.
                    """.formatted(SUGGESTION_LOOKBACK_MONTHS, categoryName, average, suggestedLimit);
            String comment = generateSuggestionCommentPort.generateText(prompt).trim();
            return comment.isEmpty() ? fallback : comment;
        } catch (Exception e) {
            return fallback;
        }
    }

    private BudgetResponse toResponse(Budget budget) {
        Category category = loadOwnedCategoryPort.loadById(budget.categoryId()).orElseThrow(CategoryNotFoundException::new);
        BigDecimal spend = sumReceiptSpendPort.sumSpend(budget.userId(), budget.categoryId(), budget.year(), budget.month());
        BigDecimal limit = budget.monthlyLimit();

        double percentage = limit.signum() > 0
                ? spend.divide(limit, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue()
                : 0.0;

        BudgetResponse response = new BudgetResponse();
        response.setId(budget.id());
        response.setCategoryId(category.id());
        response.setCategoryName(category.name());
        response.setCategoryColor(category.color());
        response.setMonthlyLimit(limit);
        response.setYear(budget.year());
        response.setMonth(budget.month());
        response.setCurrentSpend(spend);
        response.setPercentage(percentage);
        response.setOverBudget(spend.compareTo(limit) > 0);
        return response;
    }

    private Category getOwnedCategory(Long userId, Long categoryId) {
        Category category = loadOwnedCategoryPort.loadById(categoryId)
                .orElseThrow(CategoryNotFoundException::new);

        if (!category.userId().equals(userId)) {
            throw new CategoryAccessDeniedException();
        }

        return category;
    }

    private Budget getOwnedBudget(Long userId, Long budgetId) {
        Budget budget = loadBudgetPort.loadById(budgetId)
                .orElseThrow(BudgetNotFoundException::new);

        if (!budget.userId().equals(userId)) {
            throw new BudgetAccessDeniedException();
        }

        return budget;
    }

    private Long resolveUserId(String email) {
        return resolveUserIdPort.resolveUserIdByEmail(email)
                .orElseThrow(UserNotFoundException::new);
    }
}
