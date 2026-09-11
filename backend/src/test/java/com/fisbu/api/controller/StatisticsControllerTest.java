package com.fisbu.api.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fisbu.api.config.SecurityConfig;
import com.fisbu.api.dto.BadgeResponse;
import com.fisbu.api.dto.MonthlyStatisticsResponse;
import com.fisbu.api.dto.SpendingPersonaResponse;
import com.fisbu.api.dto.SpendingPersonalityResponse;
import com.fisbu.api.dto.StoreStatResponse;
import com.fisbu.api.dto.SubscriptionCandidateResponse;
import com.fisbu.api.dto.TopProductResponse;
import com.fisbu.api.repository.UserRepository;
import com.fisbu.api.service.JwtService;
import com.fisbu.api.service.StatisticsService;

/**
 * StatisticsController için dar (@WebMvcTest) kapsamlı testler — TEST-003'teki desen izlenir.
 */
@WebMvcTest(StatisticsController.class)
@Import(SecurityConfig.class)
class StatisticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StatisticsService statisticsService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void getMonthlyStatistics_authHeaderOlmadan403Doner() throws Exception {
        mockMvc.perform(get("/statistics/monthly"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "test@fisbu.com")
    void getMonthlyStatistics_gecerliKimlikleSonucDoner() throws Exception {
        MonthlyStatisticsResponse response = new MonthlyStatisticsResponse();
        response.setYear(2024);
        response.setMonth(1);
        response.setTotalAmount(new BigDecimal("500"));
        response.setCategories(List.of());
        when(statisticsService.getMonthlyStatistics("test@fisbu.com", null, null)).thenReturn(response);

        mockMvc.perform(get("/statistics/monthly"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2024));
    }

    @Test
    void getStoreStatistics_authHeaderOlmadan403Doner() throws Exception {
        mockMvc.perform(get("/statistics/stores"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "test@fisbu.com")
    void getStoreStatistics_gecerliKimlikleListeDoner() throws Exception {
        StoreStatResponse store = new StoreStatResponse();
        store.setStoreName("Migros");
        store.setTotalAmount(new BigDecimal("300"));
        store.setAverageAmount(new BigDecimal("100"));
        store.setReceiptCount(3);
        when(statisticsService.getStoreStatistics("test@fisbu.com")).thenReturn(List.of(store));

        mockMvc.perform(get("/statistics/stores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].storeName").value("Migros"));
    }

    @Test
    @WithMockUser(username = "test@fisbu.com")
    void getTopProducts_gecerliKimlikleListeDoner() throws Exception {
        TopProductResponse product = new TopProductResponse();
        product.setNormalizedName("sut");
        product.setDisplayName("Süt");
        product.setPurchaseCount(5);
        product.setTotalSpent(new BigDecimal("150"));
        when(statisticsService.getTopProducts("test@fisbu.com", null)).thenReturn(List.of(product));

        mockMvc.perform(get("/statistics/top-products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].displayName").value("Süt"));
    }

    @Test
    void getTopProducts_authHeaderOlmadan403Doner() throws Exception {
        mockMvc.perform(get("/statistics/top-products"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "test@fisbu.com")
    void getPotentialSubscriptions_gecerliKimlikleListeDoner() throws Exception {
        SubscriptionCandidateResponse candidate = new SubscriptionCandidateResponse();
        candidate.setStoreName("Netflix");
        candidate.setAverageAmount(new BigDecimal("100"));
        candidate.setOccurrenceCount(3);
        when(statisticsService.getPotentialSubscriptions("test@fisbu.com")).thenReturn(List.of(candidate));

        mockMvc.perform(get("/statistics/subscriptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].storeName").value("Netflix"));
    }

    @Test
    void getPotentialSubscriptions_authHeaderOlmadan403Doner() throws Exception {
        mockMvc.perform(get("/statistics/subscriptions"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "test@fisbu.com")
    void getSpendingPersonality_gecerliKimlikleSonucDoner() throws Exception {
        SpendingPersonaResponse persona = new SpendingPersonaResponse("Tasarrufçu", "Harcamalarını iyi yönetiyorsun");
        SpendingPersonalityResponse response = new SpendingPersonalityResponse(persona, List.<BadgeResponse>of());
        when(statisticsService.getSpendingPersonality("test@fisbu.com")).thenReturn(response);

        mockMvc.perform(get("/statistics/spending-personality"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.persona.title").value("Tasarrufçu"));
    }

    @Test
    void getSpendingPersonality_authHeaderOlmadan403Doner() throws Exception {
        mockMvc.perform(get("/statistics/spending-personality"))
                .andExpect(status().isForbidden());
    }
}
