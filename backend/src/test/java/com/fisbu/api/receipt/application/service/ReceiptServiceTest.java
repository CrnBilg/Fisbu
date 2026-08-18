package com.fisbu.api.receipt.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fisbu.api.budget.application.port.in.CheckBudgetThresholdUseCase;
import com.fisbu.api.category.domain.Category;
import com.fisbu.api.receipt.application.port.in.CreateReceiptUseCase.CreateReceiptCommand;
import com.fisbu.api.receipt.application.port.in.CreateReceiptUseCase.CreateReceiptResult;
import com.fisbu.api.receipt.application.port.in.CreateReceiptsBulkUseCase.BulkCreateResult;
import com.fisbu.api.receipt.application.port.out.AverageAmountByCategoryPort;
import com.fisbu.api.receipt.application.port.out.CountReceiptsByCategoryPort;
import com.fisbu.api.receipt.application.port.out.DeleteReceiptPort;
import com.fisbu.api.receipt.application.port.out.FindDuplicateReceiptPort;
import com.fisbu.api.receipt.application.port.out.FindReceiptsByStoreNameContainingPort;
import com.fisbu.api.receipt.application.port.out.GenerateReceiptExportPort;
import com.fisbu.api.receipt.application.port.out.LoadOwnedCategoryPort;
import com.fisbu.api.receipt.application.port.out.LoadReceiptPort;
import com.fisbu.api.receipt.application.port.out.LoadReceiptsPort;
import com.fisbu.api.receipt.application.port.out.ResolveUserIdPort;
import com.fisbu.api.receipt.application.port.out.SaveReceiptPort;
import com.fisbu.api.receipt.application.port.out.SearchReceiptsPort;
import com.fisbu.api.receipt.domain.Receipt;
import com.fisbu.api.receipt.domain.exception.CategoryAccessDeniedException;
import com.fisbu.api.receipt.domain.exception.CategoryNotFoundException;
import com.fisbu.api.receipt.domain.exception.DuplicateReceiptException;
import com.fisbu.api.receipt.domain.exception.ReceiptAccessDeniedException;
import com.fisbu.api.receipt.domain.exception.ReceiptNotFoundException;

@ExtendWith(MockitoExtension.class)
class ReceiptServiceTest {

    private static final String EMAIL = "test@fisbu.com";
    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long CATEGORY_ID = 10L;
    private static final Long RECEIPT_ID = 100L;

    @Mock
    private ResolveUserIdPort resolveUserIdPort;
    @Mock
    private LoadOwnedCategoryPort loadOwnedCategoryPort;
    @Mock
    private LoadReceiptPort loadReceiptPort;
    @Mock
    private LoadReceiptsPort loadReceiptsPort;
    @Mock
    private SearchReceiptsPort searchReceiptsPort;
    @Mock
    private FindReceiptsByStoreNameContainingPort findReceiptsByStoreNameContainingPort;
    @Mock
    private FindDuplicateReceiptPort findDuplicateReceiptPort;
    @Mock
    private SaveReceiptPort saveReceiptPort;
    @Mock
    private DeleteReceiptPort deleteReceiptPort;
    @Mock
    private CountReceiptsByCategoryPort countReceiptsByCategoryPort;
    @Mock
    private AverageAmountByCategoryPort averageAmountByCategoryPort;
    @Mock
    private GenerateReceiptExportPort generateReceiptExportPort;
    @Mock
    private CheckBudgetThresholdUseCase checkBudgetThresholdUseCase;

    private ReceiptService newService() {
        return new ReceiptService(resolveUserIdPort, loadOwnedCategoryPort, loadReceiptPort, loadReceiptsPort,
                searchReceiptsPort, findReceiptsByStoreNameContainingPort, findDuplicateReceiptPort, saveReceiptPort,
                deleteReceiptPort, countReceiptsByCategoryPort, averageAmountByCategoryPort, generateReceiptExportPort,
                checkBudgetThresholdUseCase, new ObjectMapper());
    }

    private Category category(Long id, Long userId, String name) {
        return new Category(id, userId, name, "#4CAF50");
    }

    private Receipt receipt(Long id, Long userId, Long categoryId, String storeName, BigDecimal amount, LocalDate date) {
        return new Receipt(id, userId, categoryId, categoryId != null ? "Market" : null, storeName, amount, date,
                null, null, null, null, null, null, false, false, List.of());
    }

    private CreateReceiptCommand command(Long categoryId, boolean allowDuplicate) {
        return new CreateReceiptCommand(EMAIL, "Migros", BigDecimal.valueOf(150), LocalDate.of(2026, 8, 10),
                null, null, categoryId, null, null, allowDuplicate, List.of());
    }

    private Receipt withId(Receipt r, Long id) {
        return new Receipt(id, r.userId(), r.categoryId(), r.categoryName(), r.storeName(), r.totalAmount(),
                r.receiptDate(), r.imageUrl(), r.rawOcrText(), r.splitDetailsJson(), r.createdAt(),
                r.returnDeadline(), r.warrantyExpiryDate(), r.returnReminderSent(), r.warrantyReminderSent(),
                r.items());
    }

    @Test
    void createReceipt_savesReceipt_whenNoCategoryGiven() {
        when(resolveUserIdPort.resolveUserIdByEmail(EMAIL)).thenReturn(Optional.of(USER_ID));
        when(saveReceiptPort.save(any())).thenAnswer(inv -> withId(inv.getArgument(0), RECEIPT_ID));

        CreateReceiptResult result = newService().createReceipt(command(null, false));

        assertThat(result.receipt().id()).isEqualTo(RECEIPT_ID);
        assertThat(result.anomalyWarning()).isNull();
        verify(checkBudgetThresholdUseCase, never()).checkAndNotify(any(), any(), anyInt(), anyInt());
    }

    @Test
    void createReceipt_throws_whenDuplicateExists() {
        when(resolveUserIdPort.resolveUserIdByEmail(EMAIL)).thenReturn(Optional.of(USER_ID));
        when(findDuplicateReceiptPort.findDuplicates(USER_ID, "Migros", BigDecimal.valueOf(150), LocalDate.of(2026, 8, 10)))
                .thenReturn(List.of(receipt(1L, USER_ID, null, "Migros", BigDecimal.valueOf(150), LocalDate.of(2026, 8, 10))));

        assertThatThrownBy(() -> newService().createReceipt(command(null, false)))
                .isInstanceOf(DuplicateReceiptException.class);
        verify(saveReceiptPort, never()).save(any());
    }

    @Test
    void createReceipt_skipsDuplicateCheck_whenAllowDuplicateIsTrue() {
        when(resolveUserIdPort.resolveUserIdByEmail(EMAIL)).thenReturn(Optional.of(USER_ID));
        when(saveReceiptPort.save(any())).thenAnswer(inv -> withId(inv.getArgument(0), RECEIPT_ID));

        newService().createReceipt(command(null, true));

        verify(findDuplicateReceiptPort, never()).findDuplicates(any(), any(), any(), any());
    }

    @Test
    void createReceipt_throws_whenCategoryDoesNotExist() {
        when(resolveUserIdPort.resolveUserIdByEmail(EMAIL)).thenReturn(Optional.of(USER_ID));
        when(loadOwnedCategoryPort.loadById(CATEGORY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> newService().createReceipt(command(CATEGORY_ID, true)))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void createReceipt_throws_whenCategoryBelongsToAnotherUser() {
        when(resolveUserIdPort.resolveUserIdByEmail(EMAIL)).thenReturn(Optional.of(USER_ID));
        when(loadOwnedCategoryPort.loadById(CATEGORY_ID))
                .thenReturn(Optional.of(category(CATEGORY_ID, OTHER_USER_ID, "Market")));

        assertThatThrownBy(() -> newService().createReceipt(command(CATEGORY_ID, true)))
                .isInstanceOf(CategoryAccessDeniedException.class);
    }

    @Test
    void createReceipt_includesAnomalyWarning_whenAmountIsAnomalous() {
        when(resolveUserIdPort.resolveUserIdByEmail(EMAIL)).thenReturn(Optional.of(USER_ID));
        when(loadOwnedCategoryPort.loadById(CATEGORY_ID)).thenReturn(Optional.of(category(CATEGORY_ID, USER_ID, "Market")));
        when(countReceiptsByCategoryPort.countByCategoryId(CATEGORY_ID)).thenReturn(5L);
        when(averageAmountByCategoryPort.averageByCategoryId(CATEGORY_ID)).thenReturn(BigDecimal.valueOf(50));
        when(saveReceiptPort.save(any())).thenAnswer(inv -> withId(inv.getArgument(0), RECEIPT_ID));

        // command() tutarı 150 TL, ortalama 50 TL'nin 2.5 katından fazla → anomali beklenir
        CreateReceiptResult result = newService().createReceipt(command(CATEGORY_ID, true));

        assertThat(result.anomalyWarning()).contains("çok daha yüksek");
    }

    @Test
    void createReceipt_skipsAverageQuery_whenSampleSizeTooSmall() {
        when(resolveUserIdPort.resolveUserIdByEmail(EMAIL)).thenReturn(Optional.of(USER_ID));
        when(loadOwnedCategoryPort.loadById(CATEGORY_ID)).thenReturn(Optional.of(category(CATEGORY_ID, USER_ID, "Market")));
        when(countReceiptsByCategoryPort.countByCategoryId(CATEGORY_ID)).thenReturn(2L);
        when(saveReceiptPort.save(any())).thenAnswer(inv -> withId(inv.getArgument(0), RECEIPT_ID));

        CreateReceiptResult result = newService().createReceipt(command(CATEGORY_ID, true));

        assertThat(result.anomalyWarning()).isNull();
        verify(averageAmountByCategoryPort, never()).averageByCategoryId(any());
    }

    @Test
    void createReceipt_triggersBudgetThresholdCheck_whenCategorizedAndDated() {
        when(resolveUserIdPort.resolveUserIdByEmail(EMAIL)).thenReturn(Optional.of(USER_ID));
        when(loadOwnedCategoryPort.loadById(CATEGORY_ID)).thenReturn(Optional.of(category(CATEGORY_ID, USER_ID, "Market")));
        when(countReceiptsByCategoryPort.countByCategoryId(CATEGORY_ID)).thenReturn(0L);
        when(saveReceiptPort.save(any())).thenAnswer(inv -> withId(inv.getArgument(0), RECEIPT_ID));

        newService().createReceipt(command(CATEGORY_ID, true));

        verify(checkBudgetThresholdUseCase).checkAndNotify(USER_ID, CATEGORY_ID, 2026, 8);
    }

    @Test
    void createReceiptsBulk_continuesAfterOneRowFails() {
        when(resolveUserIdPort.resolveUserIdByEmail(EMAIL)).thenReturn(Optional.of(USER_ID));
        when(loadOwnedCategoryPort.loadById(999L)).thenReturn(Optional.empty());
        when(saveReceiptPort.save(any())).thenAnswer(inv -> withId(inv.getArgument(0), RECEIPT_ID));

        BulkCreateResult result = newService().createReceiptsBulk(EMAIL, List.of(
                command(null, false),
                command(999L, false)));

        assertThat(result.created()).hasSize(1);
        assertThat(result.failed()).hasSize(1);
        assertThat(result.failed().get(0).index()).isEqualTo(1);
        assertThat(result.failed().get(0).error()).isEqualTo("Kategori bulunamadı");
    }

    @Test
    void deleteReceipt_deletes_whenOwner() {
        when(resolveUserIdPort.resolveUserIdByEmail(EMAIL)).thenReturn(Optional.of(USER_ID));
        when(loadReceiptPort.loadById(RECEIPT_ID))
                .thenReturn(Optional.of(receipt(RECEIPT_ID, USER_ID, null, "Migros", BigDecimal.TEN, LocalDate.now())));

        newService().deleteReceipt(EMAIL, RECEIPT_ID);

        verify(deleteReceiptPort).deleteById(RECEIPT_ID);
    }

    @Test
    void deleteReceipt_throwsWithDeleteSpecificMessage_whenNotOwner() {
        when(resolveUserIdPort.resolveUserIdByEmail(EMAIL)).thenReturn(Optional.of(USER_ID));
        when(loadReceiptPort.loadById(RECEIPT_ID))
                .thenReturn(Optional.of(receipt(RECEIPT_ID, OTHER_USER_ID, null, "Migros", BigDecimal.TEN, LocalDate.now())));

        assertThatThrownBy(() -> newService().deleteReceipt(EMAIL, RECEIPT_ID))
                .isInstanceOf(ReceiptAccessDeniedException.class)
                .hasMessage("Bu fişi silme yetkiniz yok");
        verify(deleteReceiptPort, never()).deleteById(any());
    }

    @Test
    void getReceiptById_throwsGenericAccessMessage_whenNotOwner() {
        when(resolveUserIdPort.resolveUserIdByEmail(EMAIL)).thenReturn(Optional.of(USER_ID));
        when(loadReceiptPort.loadById(RECEIPT_ID))
                .thenReturn(Optional.of(receipt(RECEIPT_ID, OTHER_USER_ID, null, "Migros", BigDecimal.TEN, LocalDate.now())));

        assertThatThrownBy(() -> newService().getReceiptById(EMAIL, RECEIPT_ID))
                .isInstanceOf(ReceiptAccessDeniedException.class)
                .hasMessage("Bu fişe erişim yetkiniz yok");
    }

    @Test
    void getReceiptById_throws_whenReceiptDoesNotExist() {
        when(resolveUserIdPort.resolveUserIdByEmail(EMAIL)).thenReturn(Optional.of(USER_ID));
        when(loadReceiptPort.loadById(RECEIPT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> newService().getReceiptById(EMAIL, RECEIPT_ID))
                .isInstanceOf(ReceiptNotFoundException.class);
    }

    @Test
    void setReminders_updatesDatesAndResetsSentFlags() {
        Receipt existing = receipt(RECEIPT_ID, USER_ID, null, "Migros", BigDecimal.TEN, LocalDate.now());
        when(resolveUserIdPort.resolveUserIdByEmail(EMAIL)).thenReturn(Optional.of(USER_ID));
        when(loadReceiptPort.loadById(RECEIPT_ID)).thenReturn(Optional.of(existing));
        when(saveReceiptPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Receipt result = newService().setReminders(EMAIL, RECEIPT_ID, LocalDate.of(2026, 9, 1), LocalDate.of(2027, 1, 1));

        assertThat(result.returnDeadline()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(result.warrantyExpiryDate()).isEqualTo(LocalDate.of(2027, 1, 1));
        assertThat(result.returnReminderSent()).isFalse();
        assertThat(result.warrantyReminderSent()).isFalse();
        assertThat(result.items()).isEqualTo(existing.items());
    }

    @Test
    void exportReceipts_throws_whenDatesMissing() {
        when(resolveUserIdPort.resolveUserIdByEmail(EMAIL)).thenReturn(Optional.of(USER_ID));

        assertThatThrownBy(() -> newService().exportReceipts(EMAIL, "csv", null, LocalDate.now()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("start ve end tarihleri zorunludur");
    }

    @Test
    void exportReceipts_throws_whenEndBeforeStart() {
        when(resolveUserIdPort.resolveUserIdByEmail(EMAIL)).thenReturn(Optional.of(USER_ID));

        assertThatThrownBy(() -> newService()
                .exportReceipts(EMAIL, "csv", LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 1)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("end, start'tan önce olamaz");
    }

    @Test
    void exportReceipts_throws_whenFormatInvalid() {
        when(resolveUserIdPort.resolveUserIdByEmail(EMAIL)).thenReturn(Optional.of(USER_ID));

        assertThatThrownBy(() -> newService()
                .exportReceipts(EMAIL, "xml", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 10)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Geçersiz format");
    }

    @Test
    void exportReceipts_returnsCsv_whenFormatIsCsv() {
        when(resolveUserIdPort.resolveUserIdByEmail(EMAIL)).thenReturn(Optional.of(USER_ID));
        when(generateReceiptExportPort.toCsv(eq(USER_ID), any(), any())).thenReturn("csv-content".getBytes());

        var result = newService().exportReceipts(EMAIL, "csv", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 10));

        assertThat(result.mediaType()).isEqualTo("text/csv");
        assertThat(new String(result.content())).isEqualTo("csv-content");
        assertThat(result.filename()).isEqualTo("fisler_2026-08-01_2026-08-10.csv");
    }
}
