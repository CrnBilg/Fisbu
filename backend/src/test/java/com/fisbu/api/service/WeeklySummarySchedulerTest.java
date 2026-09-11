package com.fisbu.api.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fisbu.api.entity.User;
import com.fisbu.api.repository.ReceiptRepository;
import com.fisbu.api.repository.UserRepository;

/**
 * PERF-002: WeeklySummaryScheduler'ın N+1 sorgu deseninden agregasyon sorgusuna
 * geçişinin davranışını kilitleyen regresyon testleri.
 */
@ExtendWith(MockitoExtension.class)
class WeeklySummarySchedulerTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ReceiptRepository receiptRepository;
    @Mock
    private PushNotificationService pushService;

    private WeeklySummaryScheduler scheduler;

    private User user(Long id, String email, String fcmToken) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setFcmToken(fcmToken);
        return u;
    }

    private void setUp() {
        scheduler = new WeeklySummaryScheduler(userRepository, receiptRepository, pushService);
        when(pushService.isConfigured()).thenReturn(true);
    }

    @Test
    void sendWeeklySummaries_pushYapilandirilmamissa_hicSorguAtmaz() {
        scheduler = new WeeklySummaryScheduler(userRepository, receiptRepository, pushService);
        when(pushService.isConfigured()).thenReturn(false);

        scheduler.sendWeeklySummaries();

        verify(userRepository, never()).findByFcmTokenIsNotNull();
        verify(receiptRepository, never()).sumTotalAmountGroupedByUserForDateRange(any(), any());
    }

    @Test
    void sendWeeklySummaries_ikiKullaniciIcinToplamIkiAgregasyonSorgusuAtar_NPlus1Degil() {
        setUp();
        User u1 = user(1L, "a@fisbu.com", "token1");
        User u2 = user(2L, "b@fisbu.com", "token2");
        when(userRepository.findByFcmTokenIsNotNull()).thenReturn(List.of(u1, u2));
        when(receiptRepository.sumTotalAmountGroupedByUserForDateRange(any(), any()))
                .thenReturn(List.of(
                        new Object[] {1L, BigDecimal.valueOf(100)},
                        new Object[] {2L, BigDecimal.valueOf(200)}));

        scheduler.sendWeeklySummaries();

        // Kritik: kullanıcı sayısı (2) ne olursa olsun, TOPLAM sorgu sayısı sabit (2 hafta = 2 çağrı) —
        // N+1 deseninde bu sayı kullanıcı sayısıyla birlikte büyürdü.
        verify(receiptRepository, times(2)).sumTotalAmountGroupedByUserForDateRange(any(), any());
        verify(pushService, times(2)).send(anyString(), anyString(), anyString());
    }

    @Test
    void sendWeeklySummaries_lastWeekToplamiSifirsa_bildirimGondermez() {
        setUp();
        User u1 = user(1L, "a@fisbu.com", "token1");
        when(userRepository.findByFcmTokenIsNotNull()).thenReturn(List.of(u1));
        when(receiptRepository.sumTotalAmountGroupedByUserForDateRange(any(), any()))
                .thenReturn(Collections.emptyList());

        scheduler.sendWeeklySummaries();

        verify(pushService, never()).send(any(), any(), any());
    }

    @Test
    void sendWeeklySummaries_kullaniciHaritadaYoksa_sifirVarsayilirVeBildirimGondermez() {
        setUp();
        User u1 = user(1L, "a@fisbu.com", "token1");
        User u2 = user(2L, "b@fisbu.com", "token2");
        when(userRepository.findByFcmTokenIsNotNull()).thenReturn(List.of(u1, u2));
        // Sadece u1 için harcama var — u2 haritada hiç yok (0 varsayılmalı, hata fırlatmamalı)
        when(receiptRepository.sumTotalAmountGroupedByUserForDateRange(any(), any()))
                .thenReturn(Collections.singletonList(new Object[] {1L, BigDecimal.valueOf(50)}));

        scheduler.sendWeeklySummaries();

        verify(pushService, times(1)).send(eq("token1"), anyString(), anyString());
        verify(pushService, never()).send(eq("token2"), anyString(), anyString());
    }

    @Test
    void sendWeeklySummaries_birKullanicidaHataOlursaDigerleriEtkilenmez() {
        setUp();
        User u1 = user(1L, "a@fisbu.com", "token1");
        User u2 = user(2L, "b@fisbu.com", "token2");
        when(userRepository.findByFcmTokenIsNotNull()).thenReturn(List.of(u1, u2));
        when(receiptRepository.sumTotalAmountGroupedByUserForDateRange(any(), any()))
                .thenReturn(List.of(
                        new Object[] {1L, BigDecimal.valueOf(50)},
                        new Object[] {2L, BigDecimal.valueOf(75)}));
        org.mockito.Mockito.doThrow(new RuntimeException("FCM hatası"))
                .when(pushService).send(eq("token1"), anyString(), anyString());

        scheduler.sendWeeklySummaries();

        verify(pushService).send(eq("token2"), anyString(), anyString());
    }
}
