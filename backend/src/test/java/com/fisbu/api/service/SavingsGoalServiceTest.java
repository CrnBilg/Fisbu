package com.fisbu.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.fisbu.api.dto.ContributeRequest;
import com.fisbu.api.dto.SavingsGoalRequest;
import com.fisbu.api.dto.SavingsGoalResponse;
import com.fisbu.api.entity.SavingsGoal;
import com.fisbu.api.entity.User;
import com.fisbu.api.repository.SavingsGoalRepository;
import com.fisbu.api.repository.UserRepository;

/**
 * PERF-001: SavingsGoal create endpoint'ine eklenen idempotency-key mekanizmasının
 * regresyon testleri (mevcut davranış + yeni idempotency davranışı).
 */
@ExtendWith(MockitoExtension.class)
class SavingsGoalServiceTest {

    private static final String EMAIL = "test@fisbu.com";

    @Mock
    private SavingsGoalRepository savingsGoalRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PushNotificationService pushService;
    @Mock
    private AiService aiService;

    private SavingsGoalService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new SavingsGoalService(savingsGoalRepository, userRepository, pushService, aiService);
        user = new User();
        user.setId(1L);
        user.setEmail(EMAIL);
    }

    private SavingsGoalRequest request(String name, BigDecimal target, String idempotencyKey) {
        SavingsGoalRequest r = new SavingsGoalRequest();
        r.setName(name);
        r.setTargetAmount(target);
        r.setIdempotencyKey(idempotencyKey);
        return r;
    }

    @Test
    void createGoal_keyGonderilmezse_herZamanYeniKayitOlusturur() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(savingsGoalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.createGoal(EMAIL, request("Ev", BigDecimal.valueOf(50000), null));
        service.createGoal(EMAIL, request("Ev", BigDecimal.valueOf(50000), null));

        // Kritik: key yoksa idempotency kontrolü ATLANIR — mevcut (eski) davranış korunur
        verify(savingsGoalRepository, never()).findByUserAndIdempotencyKey(any(), any());
        verify(savingsGoalRepository, org.mockito.Mockito.times(2)).save(any());
    }

    @Test
    void createGoal_ayniKeyIleIkinciIstek_yeniKayitOlusturmazVarOlaniDoner() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        SavingsGoal existing = new SavingsGoal();
        existing.setId(42L);
        existing.setUser(user);
        existing.setName("Ev");
        existing.setTargetAmount(BigDecimal.valueOf(50000));
        existing.setCurrentAmount(BigDecimal.ZERO);
        existing.setIdempotencyKey("retry-key-1");
        when(savingsGoalRepository.findByUserAndIdempotencyKey(user, "retry-key-1"))
                .thenReturn(Optional.of(existing));

        SavingsGoalResponse response = service.createGoal(EMAIL, request("Ev", BigDecimal.valueOf(50000), "retry-key-1"));

        assertThat(response.getId()).isEqualTo(42L);
        verify(savingsGoalRepository, never()).save(any());
    }

    @Test
    void createGoal_yeniKeyIleIlkIstek_kaydiKeyIleBirlikteKaydeder() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(savingsGoalRepository.findByUserAndIdempotencyKey(user, "retry-key-2")).thenReturn(Optional.empty());
        when(savingsGoalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.createGoal(EMAIL, request("Araba", BigDecimal.valueOf(300000), "retry-key-2"));

        ArgumentCaptor<SavingsGoal> captor = ArgumentCaptor.forClass(SavingsGoal.class);
        verify(savingsGoalRepository).save(captor.capture());
        assertThat(captor.getValue().getIdempotencyKey()).isEqualTo("retry-key-2");
    }

    @Test
    void createGoal_bosStringKey_nullOlarakDavranir() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(savingsGoalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.createGoal(EMAIL, request("Ev", BigDecimal.valueOf(50000), "   "));

        verify(savingsGoalRepository, never()).findByUserAndIdempotencyKey(any(), any());
        ArgumentCaptor<SavingsGoal> captor = ArgumentCaptor.forClass(SavingsGoal.class);
        verify(savingsGoalRepository).save(captor.capture());
        assertThat(captor.getValue().getIdempotencyKey()).isNull();
    }

    @Test
    void createGoal_kullaniciBulunamazsa_404Doner() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createGoal(EMAIL, request("Ev", BigDecimal.valueOf(50000), null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void deleteGoal_baskaKullanicininHedefi_403Doner() {
        User otherUser = new User();
        otherUser.setId(2L);
        SavingsGoal goal = new SavingsGoal();
        goal.setId(5L);
        goal.setUser(otherUser);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(savingsGoalRepository.findById(5L)).thenReturn(Optional.of(goal));

        assertThatThrownBy(() -> service.deleteGoal(EMAIL, 5L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    @Test
    void contribute_negatifSonucaDusurmeyeCalisirsa_400Doner() {
        SavingsGoal goal = new SavingsGoal();
        goal.setId(5L);
        goal.setUser(user);
        goal.setCurrentAmount(BigDecimal.valueOf(10));
        goal.setTargetAmount(BigDecimal.valueOf(100));

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(savingsGoalRepository.findById(5L)).thenReturn(Optional.of(goal));

        ContributeRequest request = new ContributeRequest();
        request.setAmount(BigDecimal.valueOf(-20));

        assertThatThrownBy(() -> service.contribute(EMAIL, 5L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");
    }
}
