package com.fisbu.api.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.cloudinary.Api;
import com.cloudinary.Cloudinary;
import com.fisbu.api.budget.application.port.out.DeleteBudgetsByUserPort;
import com.fisbu.api.category.application.port.out.DeleteCategoriesByUserPort;
import com.fisbu.api.entity.User;
import com.fisbu.api.receipt.application.port.out.DeleteReceiptsByUserPort;
import com.fisbu.api.repository.UserRepository;
import com.fisbu.api.shared.CloudinaryPaths;
import com.fisbu.api.shared.application.port.out.DeleteSavingsGoalsByUserPort;

/**
 * ARCH-002/ADR-004 — AuthService'ten taşınan hesap silme davranışını kilitleyen
 * regresyon testleri (bkz. eski AuthServiceTest.deleteAccount_* testleri, SEC-003).
 * Davranış birebir korunmalı: 404 varOlmayan kullanıcı için, FK sırasına göre
 * (budget -> savingsGoal -> receipt -> category -> user) silme başarılı durumda.
 */
@ExtendWith(MockitoExtension.class)
class AccountDeletionServiceTest {

    private static final String EMAIL = "test@fisbu.com";

    @Mock
    private UserRepository userRepository;
    @Mock
    private DeleteBudgetsByUserPort deleteBudgetsByUserPort;
    @Mock
    private DeleteSavingsGoalsByUserPort deleteSavingsGoalsByUserPort;
    @Mock
    private DeleteReceiptsByUserPort deleteReceiptsByUserPort;
    @Mock
    private DeleteCategoriesByUserPort deleteCategoriesByUserPort;
    @Mock
    private Cloudinary cloudinary;
    @Mock
    private Api cloudinaryApi;

    private AccountDeletionService accountDeletionService;

    @BeforeEach
    void setUp() {
        accountDeletionService = new AccountDeletionService(userRepository, deleteBudgetsByUserPort,
                deleteSavingsGoalsByUserPort, deleteReceiptsByUserPort, deleteCategoriesByUserPort, cloudinary);
    }

    private User verifiedUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail(EMAIL);
        return user;
    }

    @Test
    void deleteAccount_varOlmayanKullanici_404Doner() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountDeletionService.deleteAccount(EMAIL))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void deleteAccount_basarili_FKSirasinaGoreSilmeYapar() {
        User user = verifiedUser();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        accountDeletionService.deleteAccount(EMAIL);

        // Budget -> SavingsGoal -> Receipt -> Category -> User sırası: Budget'ın
        // Category'ye NOT NULL FK ile bağlı olması nedeniyle bu sıra kritik (bkz. ADR-004).
        InOrder inOrder = inOrder(deleteBudgetsByUserPort, deleteSavingsGoalsByUserPort,
                deleteReceiptsByUserPort, deleteCategoriesByUserPort, userRepository);
        inOrder.verify(deleteBudgetsByUserPort).deleteAllByUserId(user.getId());
        inOrder.verify(deleteSavingsGoalsByUserPort).deleteAllByUserId(user.getId());
        inOrder.verify(deleteReceiptsByUserPort).deleteAllByUserId(user.getId());
        inOrder.verify(deleteCategoriesByUserPort).deleteAllByUserId(user.getId());
        inOrder.verify(userRepository).delete(user);

        verify(deleteBudgetsByUserPort, times(1)).deleteAllByUserId(user.getId());
        verify(deleteCategoriesByUserPort, times(1)).deleteAllByUserId(user.getId());
    }

    @Test
    void deleteAccount_basarili_CloudinaryGorselleriniDeUploadIleAyniKlasordenSiler() throws Exception {
        User user = verifiedUser();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(cloudinary.api()).thenReturn(cloudinaryApi);

        accountDeletionService.deleteAccount(EMAIL);

        verify(cloudinaryApi).deleteResourcesByPrefix(
                eq(CloudinaryPaths.userReceiptsFolder(EMAIL)), anyMap());
    }

    @Test
    void deleteAccount_cloudinaryHataVerseDeHesapSilmeAkisiBasarisizOlmaz() {
        User user = verifiedUser();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(cloudinary.api()).thenThrow(new RuntimeException("Cloudinary erişilemedi (simüle edilmiş ağ hatası)"));

        // ARCH-005: Cloudinary temizliği best-effort — DB silme işlemleri zaten tamamlandığı
        // için bu hata dışarı fırlatılmamalı (kullanıcıya "hesabın silinemedi" YANLIŞ mesajı
        // gösterilmemeli, DB'deki silme aslında başarılı oldu).
        assertThatCode(() -> accountDeletionService.deleteAccount(EMAIL)).doesNotThrowAnyException();

        verify(userRepository).delete(user);
    }
}
