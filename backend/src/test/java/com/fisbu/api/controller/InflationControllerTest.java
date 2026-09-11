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
import com.fisbu.api.dto.PersonalInflationResponse;
import com.fisbu.api.dto.ProductPriceHistoryResponse;
import com.fisbu.api.repository.UserRepository;
import com.fisbu.api.service.JwtService;
import com.fisbu.api.service.PersonalInflationService;

/**
 * InflationController için dar (@WebMvcTest) kapsamlı testler — TEST-003'teki desen izlenir.
 */
@WebMvcTest(InflationController.class)
@Import(SecurityConfig.class)
class InflationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PersonalInflationService personalInflationService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void getSummary_authHeaderOlmadan403Doner() throws Exception {
        mockMvc.perform(get("/inflation/summary"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "test@fisbu.com")
    void getSummary_gecerliKimlikleSonucDoner() throws Exception {
        PersonalInflationResponse response = new PersonalInflationResponse();
        response.setMonths(6);
        response.setTrackedProductCount(10);
        response.setPersonalInflationPercent(new BigDecimal("12.5"));
        response.setTopIncreasing(List.of());
        response.setTopDecreasing(List.of());
        when(personalInflationService.getPersonalInflationSummary("test@fisbu.com", null)).thenReturn(response);

        mockMvc.perform(get("/inflation/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackedProductCount").value(10));
    }

    @Test
    void getProductHistory_authHeaderOlmadan403Doner() throws Exception {
        mockMvc.perform(get("/inflation/products/sut/history"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "test@fisbu.com")
    void getProductHistory_gecerliKimlikleSonucDoner() throws Exception {
        ProductPriceHistoryResponse response = new ProductPriceHistoryResponse();
        response.setNormalizedName("sut");
        response.setDisplayName("Süt");
        response.setPoints(List.of());
        when(personalInflationService.getProductPriceHistory("test@fisbu.com", "sut")).thenReturn(response);

        mockMvc.perform(get("/inflation/products/sut/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Süt"));
    }
}
