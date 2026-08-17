package com.fisbu.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
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
import com.fisbu.api.budget.application.service.BudgetService;
import com.fisbu.api.category.domain.Category;
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
}
