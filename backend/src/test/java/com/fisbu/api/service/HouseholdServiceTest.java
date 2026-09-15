package com.fisbu.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.fisbu.api.dto.HouseholdStatisticsResponse;
import com.fisbu.api.entity.Household;
import com.fisbu.api.entity.User;
import com.fisbu.api.receipt.application.port.out.LoadReceiptsByUserIdsAndDateRangePort;
import com.fisbu.api.receipt.domain.Receipt;
import com.fisbu.api.repository.HouseholdRepository;
import com.fisbu.api.repository.UserRepository;

/**
 * ARCH-003/ADR-005 — HouseholdService.getStatistics()'in davranışını doğrulayan testler.
 * Refactor öncesi (ReceiptRepository doğrudan enjekte edilirken) bu servis için hiç unit
 * test yoktu (sadece @WebMvcTest ile mock'lanan HouseholdControllerTest vardı) — bu dosya
 * hem yeni port tabanlı davranışı hem de mevcut hesaplama mantığını (üye/kategori bazlı
 * toplamlar) ilk kez kilitliyor.
 */
@ExtendWith(MockitoExtension.class)
class HouseholdServiceTest {

    private static final String EMAIL = "member1@fisbu.com";

    @Mock
    private HouseholdRepository householdRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private LoadReceiptsByUserIdsAndDateRangePort loadReceiptsByUserIdsAndDateRangePort;

    private HouseholdService householdService;

    @BeforeEach
    void setUp() {
        householdService = new HouseholdService(householdRepository, userRepository,
                loadReceiptsByUserIdsAndDateRangePort);
    }

    private User member(Long id, String email, String name, Household household) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setName(name);
        user.setHousehold(household);
        return user;
    }

    private Receipt receipt(Long userId, String categoryName, BigDecimal amount, LocalDate date) {
        return new Receipt(null, userId, null, categoryName, null, amount, date, null, null, null, null, null, null,
                null, null, null);
    }

    @Test
    void getStatistics_biraileyeUyeDegilse_404Firlatir() {
        User user = member(1L, EMAIL, "Ali", null);
        when(userRepository.findByEmail(EMAIL)).thenReturn(java.util.Optional.of(user));

        assertThatThrownBy(() -> householdService.getStatistics(EMAIL, 2024, 5))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void getStatistics_uyeVeKategoriBazindaToplarDogruHesaplar() {
        Household household = new Household();
        household.setId(10L);

        User member1 = member(1L, EMAIL, "Ali", household);
        User member2 = member(2L, "member2@fisbu.com", "Ayşe", household);

        when(userRepository.findByEmail(EMAIL)).thenReturn(java.util.Optional.of(member1));
        when(userRepository.findByHousehold(household)).thenReturn(List.of(member1, member2));

        List<Receipt> receipts = List.of(
                receipt(1L, "Market", BigDecimal.valueOf(100), LocalDate.of(2024, 5, 1)),
                receipt(2L, "Market", BigDecimal.valueOf(50), LocalDate.of(2024, 5, 10)),
                receipt(1L, "Ulaşım", BigDecimal.valueOf(30), LocalDate.of(2024, 5, 15)));
        when(loadReceiptsByUserIdsAndDateRangePort.loadByUserIdsAndDateRange(
                eq(List.of(1L, 2L)), any(), any())).thenReturn(receipts);

        HouseholdStatisticsResponse response = householdService.getStatistics(EMAIL, 2024, 5);

        assertThat(response.getYear()).isEqualTo(2024);
        assertThat(response.getMonth()).isEqualTo(5);
        assertThat(response.getTotalAmount()).isEqualTo(BigDecimal.valueOf(180));

        assertThat(response.getByMember()).hasSize(2);
        assertThat(response.getByMember().get(0).getUserId()).isEqualTo(1L);
        assertThat(response.getByMember().get(0).getTotalAmount()).isEqualTo(BigDecimal.valueOf(130));
        assertThat(response.getByMember().get(1).getUserId()).isEqualTo(2L);
        assertThat(response.getByMember().get(1).getTotalAmount()).isEqualTo(BigDecimal.valueOf(50));

        assertThat(response.getByCategory()).hasSize(2);
        assertThat(response.getByCategory().get(0).getCategoryName()).isEqualTo("Market");
        assertThat(response.getByCategory().get(0).getTotalAmount()).isEqualTo(BigDecimal.valueOf(150));
        assertThat(response.getByCategory().get(1).getCategoryName()).isEqualTo("Ulaşım");
    }

    @Test
    void getStatistics_kategorisizFisDigerOlarakGoruntulenir() {
        Household household = new Household();
        household.setId(10L);
        User member1 = member(1L, EMAIL, "Ali", household);

        when(userRepository.findByEmail(EMAIL)).thenReturn(java.util.Optional.of(member1));
        when(userRepository.findByHousehold(household)).thenReturn(List.of(member1));
        when(loadReceiptsByUserIdsAndDateRangePort.loadByUserIdsAndDateRange(eq(List.of(1L)), any(), any()))
                .thenReturn(List.of(receipt(1L, null, BigDecimal.valueOf(20), LocalDate.of(2024, 5, 1))));

        HouseholdStatisticsResponse response = householdService.getStatistics(EMAIL, 2024, 5);

        assertThat(response.getByCategory()).hasSize(1);
        assertThat(response.getByCategory().get(0).getCategoryName()).isEqualTo("Diğer");
    }
}
