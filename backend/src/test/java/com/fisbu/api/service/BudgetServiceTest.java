package com.fisbu.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import com.fisbu.api.budget.application.service.BudgetService;
import com.fisbu.api.budget.domain.Budget;
import com.fisbu.api.budget.domain.exception.DuplicateBudgetException;
import com.fisbu.api.category.domain.Category;
import com.fisbu.api.dto.BudgetRequest;
import com.fisbu.api.dto.BudgetResponse;
import com.fisbu.api.dto.BudgetSuggestionResponse;

/**
 * "Al ile Öner" bütçe önerisinin, son 3 aydan biri hiç harcama içermediğinde
 * ortalamayı sabit 3'e değil gerçekten veri olan ay sayısına bölmesini doğrular
 * (bkz. BudgetService.getBudgetSuggestion — kullanıcı bildirimine konu olan bug).
 */
@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private ResolveUserIdPort resolveUserIdPort;
    @Mock
    private LoadUserNotificationProfilePort loadUserNotificationProfilePort;
    @Mock
    private LoadOwnedCategoryPort loadOwnedCategoryPort;
    @Mock
    private SumReceiptSpendPort sumReceiptSpendPort;
    @Mock
    private LoadBudgetsPort loadBudgetsPort;
    @Mock
    private LoadBudgetPort loadBudgetPort;
    @Mock
    private FindBudgetByCategoryAndPeriodPort findBudgetByCategoryAndPeriodPort;
    @Mock
    private SaveBudgetPort saveBudgetPort;
    @Mock
    private DeleteBudgetPort deleteBudgetPort;
    @Mock
    private SendBudgetNotificationPort sendBudgetNotificationPort;
    @Mock
    private GenerateSuggestionCommentPort generateSuggestionCommentPort;

    private BudgetService newService() {
        return new BudgetService(resolveUserIdPort, loadUserNotificationProfilePort, loadOwnedCategoryPort,
                sumReceiptSpendPort, loadBudgetsPort, loadBudgetPort, findBudgetByCategoryAndPeriodPort,
                saveBudgetPort, deleteBudgetPort, sendBudgetNotificationPort, generateSuggestionCommentPort);
    }

    private static final Long USER_ID = 1L;
    private static final String EMAIL = "test@fisbu.com";
    private static final Long CATEGORY_ID = 10L;

    private Category restoranCategory() {
        return new Category(CATEGORY_ID, USER_ID, "Restoran", "#ff0000");
    }

    @Test
    void averagesOverMonthsWithDataOnly_whenOneOfThreeMonthsHasNoSpend() {
        BudgetService service = newService();

        when(resolveUserIdPort.resolveUserIdByEmail(EMAIL)).thenReturn(Optional.of(USER_ID));
        when(loadOwnedCategoryPort.loadById(CATEGORY_ID)).thenReturn(Optional.of(restoranCategory()));

        // targetMonth = 2026-03-01 → geriye dönük Şubat, Ocak, Aralık 2025 taranır.
        when(sumReceiptSpendPort.sumSpend(eq(USER_ID), eq(CATEGORY_ID), eq(2026), eq(2)))
                .thenReturn(new BigDecimal("600.00"));
        when(sumReceiptSpendPort.sumSpend(eq(USER_ID), eq(CATEGORY_ID), eq(2026), eq(1)))
                .thenReturn(new BigDecimal("600.00"));
        when(sumReceiptSpendPort.sumSpend(eq(USER_ID), eq(CATEGORY_ID), eq(2025), eq(12)))
                .thenReturn(BigDecimal.ZERO);

        BudgetSuggestionResponse response =
                service.getBudgetSuggestion(EMAIL, CATEGORY_ID, 2026, 3);

        assertThat(response.getMonthsAnalyzed()).isEqualTo(2);
        // Toplam 1200 TL / 2 ay veri = 600 TL — sabit 3'e bölünseydi (bug) 400 TL çıkardı.
        assertThat(response.getAverageSpend()).isEqualByComparingTo("600.00");
        assertThat(response.getSuggestedLimit()).isEqualByComparingTo("600");
    }

    @Test
    void averagesOverAllThreeMonths_whenEveryMonthHasSpend() {
        BudgetService service = newService();

        when(resolveUserIdPort.resolveUserIdByEmail(EMAIL)).thenReturn(Optional.of(USER_ID));
        when(loadOwnedCategoryPort.loadById(CATEGORY_ID)).thenReturn(Optional.of(restoranCategory()));
        when(sumReceiptSpendPort.sumSpend(eq(USER_ID), eq(CATEGORY_ID), anyInt(), anyInt()))
                .thenReturn(new BigDecimal("300.00"));

        BudgetSuggestionResponse response =
                service.getBudgetSuggestion(EMAIL, CATEGORY_ID, 2026, 3);

        assertThat(response.getMonthsAnalyzed()).isEqualTo(3);
        assertThat(response.getAverageSpend()).isEqualByComparingTo("300.00");
    }

    private Budget budget(boolean warningNotified, boolean overspendNotified) {
        return new Budget(1L, USER_ID, CATEGORY_ID, BigDecimal.valueOf(1000), 2026, 8, warningNotified,
                overspendNotified);
    }

    private UserNotificationProfile profile(boolean warningEnabled, boolean overspendEnabled) {
        return new UserNotificationProfile("fcm-token", warningEnabled, overspendEnabled);
    }

    @Test
    void checkAndNotify_doesNothing_whenBudgetNotFound() {
        when(findBudgetByCategoryAndPeriodPort.findByUserIdAndCategoryIdAndYearAndMonth(USER_ID, CATEGORY_ID, 2026, 8))
                .thenReturn(Optional.empty());

        newService().checkAndNotify(USER_ID, CATEGORY_ID, 2026, 8);

        verify(sendBudgetNotificationPort, never()).send(any(), any(), any());
        verify(saveBudgetPort, never()).save(any());
    }

    @Test
    void checkAndNotify_doesNothing_whenLimitIsZeroOrNull() {
        Budget zeroLimitBudget = new Budget(1L, USER_ID, CATEGORY_ID, BigDecimal.ZERO, 2026, 8, false, false);
        when(findBudgetByCategoryAndPeriodPort.findByUserIdAndCategoryIdAndYearAndMonth(USER_ID, CATEGORY_ID, 2026, 8))
                .thenReturn(Optional.of(zeroLimitBudget));

        newService().checkAndNotify(USER_ID, CATEGORY_ID, 2026, 8);

        verify(sendBudgetNotificationPort, never()).send(any(), any(), any());
    }

    @Test
    void checkAndNotify_sendsWarningAndSaves_whenCrossing80PercentForTheFirstTime() {
        when(findBudgetByCategoryAndPeriodPort.findByUserIdAndCategoryIdAndYearAndMonth(USER_ID, CATEGORY_ID, 2026, 8))
                .thenReturn(Optional.of(budget(false, false)));
        when(loadOwnedCategoryPort.loadById(CATEGORY_ID)).thenReturn(Optional.of(restoranCategory()));
        when(loadUserNotificationProfilePort.loadProfileByUserId(USER_ID)).thenReturn(Optional.of(profile(true, true)));
        when(sumReceiptSpendPort.sumSpend(USER_ID, CATEGORY_ID, 2026, 8)).thenReturn(BigDecimal.valueOf(850));

        newService().checkAndNotify(USER_ID, CATEGORY_ID, 2026, 8);

        verify(sendBudgetNotificationPort).send(eq("fcm-token"), eq("Bütçe Uyarısı"), any());
        verify(sendBudgetNotificationPort, never()).send(any(), eq("Bütçe Aşıldı"), any());
        verify(saveBudgetPort).save(argThat(b -> b.warningNotified() && !b.overspendNotified()));
    }

    @Test
    void checkAndNotify_sendsOverspendAndSaves_whenCrossing100PercentForTheFirstTime() {
        when(findBudgetByCategoryAndPeriodPort.findByUserIdAndCategoryIdAndYearAndMonth(USER_ID, CATEGORY_ID, 2026, 8))
                .thenReturn(Optional.of(budget(true, false)));
        when(loadOwnedCategoryPort.loadById(CATEGORY_ID)).thenReturn(Optional.of(restoranCategory()));
        when(loadUserNotificationProfilePort.loadProfileByUserId(USER_ID)).thenReturn(Optional.of(profile(true, true)));
        when(sumReceiptSpendPort.sumSpend(USER_ID, CATEGORY_ID, 2026, 8)).thenReturn(BigDecimal.valueOf(1200));

        newService().checkAndNotify(USER_ID, CATEGORY_ID, 2026, 8);

        verify(sendBudgetNotificationPort).send(eq("fcm-token"), eq("Bütçe Aşıldı"), any());
        verify(saveBudgetPort).save(argThat(b -> b.warningNotified() && b.overspendNotified()));
    }

    @Test
    void checkAndNotify_updatesFlagsButDoesNotPush_whenUserDisabledThatNotificationType() {
        when(findBudgetByCategoryAndPeriodPort.findByUserIdAndCategoryIdAndYearAndMonth(USER_ID, CATEGORY_ID, 2026, 8))
                .thenReturn(Optional.of(budget(false, false)));
        when(loadOwnedCategoryPort.loadById(CATEGORY_ID)).thenReturn(Optional.of(restoranCategory()));
        // Kullanıcı uyarı bildirimini kapatmış — bayrak yine de güncellenmeli (tekrar tetiklenmesin),
        // ama push gönderilmemeli.
        when(loadUserNotificationProfilePort.loadProfileByUserId(USER_ID)).thenReturn(Optional.of(profile(false, true)));
        when(sumReceiptSpendPort.sumSpend(USER_ID, CATEGORY_ID, 2026, 8)).thenReturn(BigDecimal.valueOf(850));

        newService().checkAndNotify(USER_ID, CATEGORY_ID, 2026, 8);

        verify(sendBudgetNotificationPort, never()).send(any(), any(), any());
        verify(saveBudgetPort).save(argThat(Budget::warningNotified));
    }

    @Test
    void createBudget_savesNewBudget_whenNoDuplicateExists() {
        when(resolveUserIdPort.resolveUserIdByEmail(EMAIL)).thenReturn(Optional.of(USER_ID));
        when(loadOwnedCategoryPort.loadById(CATEGORY_ID)).thenReturn(Optional.of(restoranCategory()));
        when(findBudgetByCategoryAndPeriodPort.findByUserIdAndCategoryIdAndYearAndMonth(USER_ID, CATEGORY_ID, 2026, 8))
                .thenReturn(Optional.empty());
        when(saveBudgetPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(sumReceiptSpendPort.sumSpend(USER_ID, CATEGORY_ID, 2026, 8)).thenReturn(BigDecimal.ZERO);

        BudgetRequest request = new BudgetRequest();
        request.setCategoryId(CATEGORY_ID);
        request.setMonthlyLimit(BigDecimal.valueOf(1000));
        request.setYear(2026);
        request.setMonth(8);

        BudgetResponse response = newService().createBudget(EMAIL, request);

        assertThat(response.getCategoryId()).isEqualTo(CATEGORY_ID);
        assertThat(response.getMonthlyLimit()).isEqualByComparingTo("1000");
    }

    @Test
    void createBudget_throws_whenBudgetAlreadyExistsForSameCategoryAndPeriod() {
        when(resolveUserIdPort.resolveUserIdByEmail(EMAIL)).thenReturn(Optional.of(USER_ID));
        when(loadOwnedCategoryPort.loadById(CATEGORY_ID)).thenReturn(Optional.of(restoranCategory()));
        when(findBudgetByCategoryAndPeriodPort.findByUserIdAndCategoryIdAndYearAndMonth(USER_ID, CATEGORY_ID, 2026, 8))
                .thenReturn(Optional.of(budget(false, false)));

        BudgetRequest request = new BudgetRequest();
        request.setCategoryId(CATEGORY_ID);
        request.setMonthlyLimit(BigDecimal.valueOf(1000));
        request.setYear(2026);
        request.setMonth(8);

        assertThatThrownBy(() -> newService().createBudget(EMAIL, request))
                .isInstanceOf(DuplicateBudgetException.class);
        verify(saveBudgetPort, never()).save(any());
    }
}
