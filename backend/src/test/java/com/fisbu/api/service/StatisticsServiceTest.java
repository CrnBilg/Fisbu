package com.fisbu.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.fisbu.api.dto.MonthlyStatisticsResponse;
import com.fisbu.api.dto.SpendingPersonalityResponse;
import com.fisbu.api.dto.StoreStatResponse;
import com.fisbu.api.dto.SubscriptionCandidateResponse;
import com.fisbu.api.dto.TopProductResponse;
import com.fisbu.api.entity.Category;
import com.fisbu.api.entity.Receipt;
import com.fisbu.api.entity.ReceiptItem;
import com.fisbu.api.entity.SavingsGoal;
import com.fisbu.api.entity.User;
import com.fisbu.api.repository.ReceiptItemRepository;
import com.fisbu.api.repository.ReceiptRepository;
import com.fisbu.api.repository.SavingsGoalRepository;
import com.fisbu.api.repository.UserRepository;

/**
 * TEST-001 — StatisticsService'in mevcut davranışını kilitleyen regresyon testleri.
 * Bu dosya StatisticsService.java'nın kodunu DEĞİŞTİRMEZ, sadece mevcut davranışı test eder.
 */
@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    private static final String EMAIL = "test@fisbu.com";

    @Mock
    private ReceiptRepository receiptRepository;
    @Mock
    private ReceiptItemRepository receiptItemRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SavingsGoalRepository savingsGoalRepository;

    private StatisticsService statisticsService;

    private User user;

    @BeforeEach
    void setUp() {
        statisticsService = new StatisticsService(receiptRepository, receiptItemRepository,
                userRepository, savingsGoalRepository);
        user = new User();
        user.setId(1L);
        user.setEmail(EMAIL);
    }

    private Category category(Long id, String name, String color) {
        Category c = new Category();
        c.setId(id);
        c.setName(name);
        c.setColor(color);
        return c;
    }

    private Receipt receipt(Category category, String storeName, BigDecimal amount, LocalDate date) {
        Receipt r = new Receipt();
        r.setUser(user);
        r.setCategory(category);
        r.setStoreName(storeName);
        r.setTotalAmount(amount);
        r.setReceiptDate(date);
        return r;
    }

    // ---------- getMonthlyStatistics ----------

    @Test
    void getMonthlyStatistics_varOlmayanKullanici_404Firlatir() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> statisticsService.getMonthlyStatistics(EMAIL, 2024, 5))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void getMonthlyStatistics_kategoriBazindaGruplarVeTutariBuyuktenKucugeSiralar() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        Category market = category(1L, "Market", "#FF0000");
        Category ulasim = category(2L, "Ulaşım", "#00FF00");

        List<Receipt> receipts = List.of(
                receipt(market, "A101", BigDecimal.valueOf(100), LocalDate.of(2024, 5, 1)),
                receipt(market, "A101", BigDecimal.valueOf(50), LocalDate.of(2024, 5, 10)),
                receipt(ulasim, "Metro", BigDecimal.valueOf(30), LocalDate.of(2024, 5, 15)));
        when(receiptRepository.findByUserAndReceiptDateBetween(user,
                LocalDate.of(2024, 5, 1), LocalDate.of(2024, 5, 31))).thenReturn(receipts);

        MonthlyStatisticsResponse response = statisticsService.getMonthlyStatistics(EMAIL, 2024, 5);

        assertThat(response.getYear()).isEqualTo(2024);
        assertThat(response.getMonth()).isEqualTo(5);
        assertThat(response.getTotalAmount()).isEqualTo(BigDecimal.valueOf(180));
        assertThat(response.getCategories()).hasSize(2);
        assertThat(response.getCategories().get(0).getCategoryName()).isEqualTo("Market");
        assertThat(response.getCategories().get(0).getTotalAmount()).isEqualTo(BigDecimal.valueOf(150));
        assertThat(response.getCategories().get(1).getCategoryName()).isEqualTo("Ulaşım");
    }

    @Test
    void getMonthlyStatistics_yilAyVerilmezseBugunkuAyKullanilir() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        LocalDate today = LocalDate.now();
        LocalDate start = today.withDayOfMonth(1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        when(receiptRepository.findByUserAndReceiptDateBetween(user, start, end)).thenReturn(List.of());

        MonthlyStatisticsResponse response = statisticsService.getMonthlyStatistics(EMAIL, null, null);

        assertThat(response.getYear()).isEqualTo(today.getYear());
        assertThat(response.getMonth()).isEqualTo(today.getMonthValue());
        assertThat(response.getTotalAmount()).isEqualTo(BigDecimal.ZERO);
        assertThat(response.getCategories()).isEmpty();
    }

    @Test
    void getMonthlyStatistics_kategorisizFisDigerOlarakGoruntulenir() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        List<Receipt> receipts = List.of(receipt(null, "Bilinmeyen", BigDecimal.valueOf(20), LocalDate.of(2024, 5, 1)));
        when(receiptRepository.findByUserAndReceiptDateBetween(user,
                LocalDate.of(2024, 5, 1), LocalDate.of(2024, 5, 31))).thenReturn(receipts);

        MonthlyStatisticsResponse response = statisticsService.getMonthlyStatistics(EMAIL, 2024, 5);

        assertThat(response.getCategories()).hasSize(1);
        assertThat(response.getCategories().get(0).getCategoryName()).isEqualTo("Diğer");
        assertThat(response.getCategories().get(0).getCategoryId()).isNull();
    }

    // ---------- getMonthlyStatisticsRange ----------

    @Test
    void getMonthlyStatisticsRange_varOlmayanKullanici_404Firlatir() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> statisticsService.getMonthlyStatisticsRange(EMAIL, 3))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void getMonthlyStatisticsRange_istenenAySayisiKadarSonucDoner_EskidenYeniyeSirali() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        LocalDate today = LocalDate.now();
        Category market = category(1L, "Market", "#FF0000");

        Receipt thisMonthReceipt = receipt(market, "A101", BigDecimal.valueOf(75), today.withDayOfMonth(1));
        when(receiptRepository.findByUserAndReceiptDateBetween(eq(user), any(), any()))
                .thenReturn(List.of(thisMonthReceipt));

        List<MonthlyStatisticsResponse> results = statisticsService.getMonthlyStatisticsRange(EMAIL, 3);

        assertThat(results).hasSize(3);
        MonthlyStatisticsResponse lastMonthInRange = results.get(2);
        assertThat(lastMonthInRange.getYear()).isEqualTo(today.getYear());
        assertThat(lastMonthInRange.getMonth()).isEqualTo(today.getMonthValue());
    }

    // ---------- getStoreStatistics ----------

    @Test
    void getStoreStatistics_varOlmayanKullanici_404Firlatir() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> statisticsService.getStoreStatistics(EMAIL))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void getStoreStatistics_magazaBazindaToplarVeOrtalamaHesaplar() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        List<Receipt> receipts = List.of(
                receipt(null, "A101", BigDecimal.valueOf(100), LocalDate.of(2024, 1, 1)),
                receipt(null, "A101", BigDecimal.valueOf(50), LocalDate.of(2024, 1, 5)),
                receipt(null, "  BIM  ", BigDecimal.valueOf(200), LocalDate.of(2024, 1, 10)));
        when(receiptRepository.findByUser(user)).thenReturn(receipts);

        List<StoreStatResponse> stats = statisticsService.getStoreStatistics(EMAIL);

        assertThat(stats).hasSize(2);
        StoreStatResponse top = stats.get(0);
        assertThat(top.getStoreName()).isEqualTo("BIM");
        assertThat(top.getTotalAmount()).isEqualTo(BigDecimal.valueOf(200));
        assertThat(top.getReceiptCount()).isEqualTo(1);
        assertThat(top.getAverageAmount()).isEqualTo(new BigDecimal("200.00"));

        StoreStatResponse a101 = stats.get(1);
        assertThat(a101.getReceiptCount()).isEqualTo(2);
        assertThat(a101.getTotalAmount()).isEqualTo(BigDecimal.valueOf(150));
        assertThat(a101.getAverageAmount()).isEqualTo(new BigDecimal("75.00"));
    }

    @Test
    void getStoreStatistics_hicFisYok_bosListeDoner() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(receiptRepository.findByUser(user)).thenReturn(List.of());

        List<StoreStatResponse> stats = statisticsService.getStoreStatistics(EMAIL);

        assertThat(stats).isEmpty();
    }

    // ---------- getTopProducts ----------

    @Test
    void getTopProducts_varOlmayanKullanici_404Firlatir() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> statisticsService.getTopProducts(EMAIL, 5))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void getTopProducts_satinAlmaSayisinaGoreSiralarVeLimitUygular() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        ReceiptItem sut = new ReceiptItem();
        sut.setNormalizedName("sut");
        sut.setProductName("Süt");
        sut.setUnitPrice(BigDecimal.valueOf(10));
        sut.setQuantity(BigDecimal.valueOf(2));

        ReceiptItem sut2 = new ReceiptItem();
        sut2.setNormalizedName("sut");
        sut2.setProductName("Süt");
        sut2.setUnitPrice(BigDecimal.valueOf(10));
        sut2.setQuantity(BigDecimal.ONE);

        ReceiptItem ekmek = new ReceiptItem();
        ekmek.setNormalizedName("ekmek");
        ekmek.setProductName("Ekmek");
        ekmek.setUnitPrice(BigDecimal.valueOf(5));
        ekmek.setQuantity(null);

        when(receiptItemRepository.findByReceipt_User_Id(user.getId())).thenReturn(List.of(sut, sut2, ekmek));

        List<TopProductResponse> topProducts = statisticsService.getTopProducts(EMAIL, 1);

        assertThat(topProducts).hasSize(1);
        assertThat(topProducts.get(0).getNormalizedName()).isEqualTo("sut");
        assertThat(topProducts.get(0).getPurchaseCount()).isEqualTo(2);
        assertThat(topProducts.get(0).getTotalSpent()).isEqualTo(BigDecimal.valueOf(30));
    }

    @Test
    void getTopProducts_limitNullVeyaNegatifse_varsayilanLimitKullanilir() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(receiptItemRepository.findByReceipt_User_Id(user.getId())).thenReturn(List.of());

        List<TopProductResponse> topProducts = statisticsService.getTopProducts(EMAIL, -5);

        assertThat(topProducts).isEmpty();
    }

    // ---------- getPotentialSubscriptions ----------

    @Test
    void getPotentialSubscriptions_varOlmayanKullanici_404Firlatir() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> statisticsService.getPotentialSubscriptions(EMAIL))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void getPotentialSubscriptions_aylikAraliklaAyniTutarOdenenMagaza_abonelikOlarakBulunur() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        List<Receipt> receipts = List.of(
                receipt(null, "Netflix", BigDecimal.valueOf(100), LocalDate.of(2024, 1, 1)),
                receipt(null, "Netflix", BigDecimal.valueOf(100), LocalDate.of(2024, 2, 1)),
                receipt(null, "Netflix", BigDecimal.valueOf(100), LocalDate.of(2024, 3, 1)));
        when(receiptRepository.findByUser(user)).thenReturn(receipts);

        List<SubscriptionCandidateResponse> candidates = statisticsService.getPotentialSubscriptions(EMAIL);

        assertThat(candidates).hasSize(1);
        assertThat(candidates.get(0).getStoreName()).isEqualTo("Netflix");
        assertThat(candidates.get(0).getOccurrenceCount()).isEqualTo(3);
        assertThat(candidates.get(0).getAverageAmount()).isEqualTo(new BigDecimal("100.00"));
    }

    @Test
    void getPotentialSubscriptions_tutarBuyukFarklilikGosterirse_abonelikSayilmaz() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        List<Receipt> receipts = List.of(
                receipt(null, "Market", BigDecimal.valueOf(50), LocalDate.of(2024, 1, 1)),
                receipt(null, "Market", BigDecimal.valueOf(500), LocalDate.of(2024, 2, 1)),
                receipt(null, "Market", BigDecimal.valueOf(50), LocalDate.of(2024, 3, 1)));
        when(receiptRepository.findByUser(user)).thenReturn(receipts);

        List<SubscriptionCandidateResponse> candidates = statisticsService.getPotentialSubscriptions(EMAIL);

        assertThat(candidates).isEmpty();
    }

    @Test
    void getPotentialSubscriptions_tekFis_abonelikSayilmaz() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(receiptRepository.findByUser(user)).thenReturn(
                List.of(receipt(null, "Netflix", BigDecimal.valueOf(100), LocalDate.of(2024, 1, 1))));

        List<SubscriptionCandidateResponse> candidates = statisticsService.getPotentialSubscriptions(EMAIL);

        assertThat(candidates).isEmpty();
    }

    // ---------- getSpendingPersonality ----------

    @Test
    void getSpendingPersonality_varOlmayanKullanici_404Firlatir() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> statisticsService.getSpendingPersonality(EMAIL))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void getSpendingPersonality_besFisAltinda_yeniBaslayanPersonasiDoner() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        List<Receipt> receipts = new ArrayList<>();
        receipts.add(receipt(null, "A", BigDecimal.TEN, LocalDate.of(2024, 1, 1)));
        when(receiptRepository.findByUser(user)).thenReturn(receipts);
        when(savingsGoalRepository.findByUser(user)).thenReturn(List.of());

        SpendingPersonalityResponse response = statisticsService.getSpendingPersonality(EMAIL);

        assertThat(response.getPersona().getTitle()).isEqualTo("Yeni Başlayan");
        assertThat(response.getBadges()).hasSize(9);
        assertThat(response.getBadges().get(0).isAchieved()).isTrue(); // first_receipt
        assertThat(response.getBadges().get(1).isAchieved()).isFalse(); // receipts_10
    }

    @Test
    void getSpendingPersonality_birKategoriYarininUzerindeHarcama_kategoriAsigiPersonasiDoner() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        Category market = category(1L, "Market", "#FF0000");
        List<Receipt> receipts = List.of(
                receipt(market, "A", BigDecimal.valueOf(100), LocalDate.of(2024, 1, 2)),
                receipt(market, "A", BigDecimal.valueOf(100), LocalDate.of(2024, 1, 3)),
                receipt(market, "A", BigDecimal.valueOf(100), LocalDate.of(2024, 1, 4)),
                receipt(null, "B", BigDecimal.valueOf(10), LocalDate.of(2024, 1, 5)),
                receipt(null, "C", BigDecimal.valueOf(10), LocalDate.of(2024, 1, 6)));
        when(receiptRepository.findByUser(user)).thenReturn(receipts);
        when(savingsGoalRepository.findByUser(user)).thenReturn(List.of());

        SpendingPersonalityResponse response = statisticsService.getSpendingPersonality(EMAIL);

        assertThat(response.getPersona().getTitle()).isEqualTo("Market Aşığı");
    }

    @Test
    void getSpendingPersonality_hedefeUlasilmisTasarrufVarsa_sampiyonRozetiAcilir() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        List<Receipt> receipts = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            receipts.add(receipt(null, "Store" + i, BigDecimal.valueOf(20), LocalDate.of(2024, 1, i + 1)));
        }
        when(receiptRepository.findByUser(user)).thenReturn(receipts);

        SavingsGoal goal = new SavingsGoal();
        goal.setTargetAmount(BigDecimal.valueOf(100));
        goal.setCurrentAmount(BigDecimal.valueOf(150));
        when(savingsGoalRepository.findByUser(user)).thenReturn(List.of(goal));

        SpendingPersonalityResponse response = statisticsService.getSpendingPersonality(EMAIL);

        boolean savingsBadgeAchieved = response.getBadges().stream()
                .anyMatch(b -> b.getId().equals("savings_champion") && b.isAchieved());
        assertThat(savingsBadgeAchieved).isTrue();
    }

    private static <T> T any() {
        return org.mockito.ArgumentMatchers.any();
    }

    private static <T> T eq(T value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }
}
