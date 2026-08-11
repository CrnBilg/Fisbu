package com.fisbu.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fisbu.api.dto.BudgetSuggestionResponse;
import com.fisbu.api.entity.Category;
import com.fisbu.api.entity.Receipt;
import com.fisbu.api.entity.User;
import com.fisbu.api.repository.BudgetRepository;
import com.fisbu.api.repository.CategoryRepository;
import com.fisbu.api.repository.ReceiptRepository;
import com.fisbu.api.repository.UserRepository;

/**
 * "Al ile Öner" bütçe önerisinin, son 3 aydan biri hiç harcama içermediğinde
 * ortalamayı sabit 3'e değil gerçekten veri olan ay sayısına bölmesini doğrular
 * (bkz. BudgetService.getBudgetSuggestion — kullanıcı bildirimine konu olan bug).
 */
@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ReceiptRepository receiptRepository;
    @Mock
    private PushNotificationService pushService;
    @Mock
    private AiService aiService;

    private BudgetService newService() {
        return new BudgetService(budgetRepository, userRepository, categoryRepository,
                receiptRepository, pushService, aiService);
    }

    private User someUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@fisbu.com");
        return user;
    }

    private Category someCategory(User user) {
        Category category = new Category();
        category.setId(10L);
        category.setUser(user);
        category.setName("Restoran");
        return category;
    }

    private Receipt receiptOf(BigDecimal amount) {
        Receipt receipt = new Receipt();
        receipt.setTotalAmount(amount);
        return receipt;
    }

    @Test
    void averagesOverMonthsWithDataOnly_whenOneOfThreeMonthsHasNoSpend() {
        User user = someUser();
        Category category = someCategory(user);
        BudgetService service = newService();

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

        // targetMonth = 2026-03-01 → geriye dönük Şubat, Ocak, Aralık 2025 taranır.
        when(receiptRepository.findByUserAndCategoryAndReceiptDateBetween(
                eq(user), eq(category), eq(LocalDate.of(2026, 2, 1)), eq(LocalDate.of(2026, 2, 28))))
                .thenReturn(List.of(receiptOf(new BigDecimal("600.00"))));
        when(receiptRepository.findByUserAndCategoryAndReceiptDateBetween(
                eq(user), eq(category), eq(LocalDate.of(2026, 1, 1)), eq(LocalDate.of(2026, 1, 31))))
                .thenReturn(List.of(receiptOf(new BigDecimal("600.00"))));
        when(receiptRepository.findByUserAndCategoryAndReceiptDateBetween(
                eq(user), eq(category), eq(LocalDate.of(2025, 12, 1)), eq(LocalDate.of(2025, 12, 31))))
                .thenReturn(List.of());

        BudgetSuggestionResponse response =
                service.getBudgetSuggestion(user.getEmail(), category.getId(), 2026, 3);

        assertThat(response.getMonthsAnalyzed()).isEqualTo(2);
        // Toplam 1200 TL / 2 ay veri = 600 TL — sabit 3'e bölünseydi (bug) 400 TL çıkardı.
        assertThat(response.getAverageSpend()).isEqualByComparingTo("600.00");
        assertThat(response.getSuggestedLimit()).isEqualByComparingTo("600");
    }

    @Test
    void averagesOverAllThreeMonths_whenEveryMonthHasSpend() {
        User user = someUser();
        Category category = someCategory(user);
        BudgetService service = newService();

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(receiptRepository.findByUserAndCategoryAndReceiptDateBetween(
                eq(user), eq(category), any(), any()))
                .thenReturn(List.of(receiptOf(new BigDecimal("300.00"))));

        BudgetSuggestionResponse response =
                service.getBudgetSuggestion(user.getEmail(), category.getId(), 2026, 3);

        assertThat(response.getMonthsAnalyzed()).isEqualTo(3);
        assertThat(response.getAverageSpend()).isEqualByComparingTo("300.00");
    }
}
