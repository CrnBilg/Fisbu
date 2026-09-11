package com.fisbu.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fisbu.api.config.SecurityConfig;
import com.fisbu.api.dto.HouseholdResponse;
import com.fisbu.api.dto.HouseholdStatisticsResponse;
import com.fisbu.api.repository.UserRepository;
import com.fisbu.api.service.HouseholdService;
import com.fisbu.api.service.JwtService;

/**
 * HouseholdController için dar (@WebMvcTest) kapsamlı testler. Gerçek SecurityConfig import
 * edilir; "/households/join" hem korumalı hem de RateLimitFilter'da IP başına sınırlı bir
 * uçtur ama SecurityConfig'de permitAll değildir (rate limit != auth) — bu yüzden auth header
 * olmadan 403 beklenir (Spring Security'nin varsayılan AuthenticationEntryPoint'i). HouseholdService mock'lanır.
 */
@WebMvcTest(HouseholdController.class)
@Import(SecurityConfig.class)
class HouseholdControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HouseholdService householdService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @WithMockUser(username = "test@fisbu.com")
    void createHousehold_gecerliKimlikle201Doner() throws Exception {
        HouseholdResponse response = new HouseholdResponse(1L, "Test Ailesi", "ABC123", List.of());
        when(householdService.createHousehold(eq("test@fisbu.com"), any())).thenReturn(response);

        mockMvc.perform(post("/households")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Test Ailesi"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Ailesi"))
                .andExpect(jsonPath("$.inviteCode").value("ABC123"));
    }

    @Test
    void createHousehold_authHeaderOlmadan403Doner() throws Exception {
        mockMvc.perform(post("/households")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Test Ailesi"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "test@fisbu.com")
    void createHousehold_gecersizGovdeIcin400Doner() throws Exception {
        mockMvc.perform(post("/households")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "test@fisbu.com")
    void joinHousehold_gecerliKimlikleAileDoner() throws Exception {
        HouseholdResponse response = new HouseholdResponse(2L, "Baska Aile", "XYZ789", List.of());
        when(householdService.joinHousehold(eq("test@fisbu.com"), any())).thenReturn(response);

        mockMvc.perform(post("/households/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"inviteCode":"XYZ789"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inviteCode").value("XYZ789"));
    }

    @Test
    void joinHousehold_authHeaderOlmadan403Doner() throws Exception {
        // /households/join rate-limit'e tabidir ama SecurityConfig'de permitAll değildir
        mockMvc.perform(post("/households/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"inviteCode":"XYZ789"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "test@fisbu.com")
    void leaveHousehold_gecerliKimlikle204Doner() throws Exception {
        mockMvc.perform(delete("/households/me"))
                .andExpect(status().isNoContent());

        verify(householdService).leaveHousehold("test@fisbu.com");
    }

    @Test
    void leaveHousehold_authHeaderOlmadan403Doner() throws Exception {
        mockMvc.perform(delete("/households/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "test@fisbu.com")
    void getMyHousehold_gecerliKimlikleAileDoner() throws Exception {
        HouseholdResponse response = new HouseholdResponse(1L, "Test Ailesi", "ABC123", List.of());
        when(householdService.getMyHousehold("test@fisbu.com")).thenReturn(response);

        mockMvc.perform(get("/households/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getMyHousehold_authHeaderOlmadan403Doner() throws Exception {
        mockMvc.perform(get("/households/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "test@fisbu.com")
    void getStatistics_gecerliKimlikleIstatistikDoner() throws Exception {
        HouseholdStatisticsResponse stats = new HouseholdStatisticsResponse();
        stats.setYear(2026);
        stats.setMonth(9);
        stats.setTotalAmount(BigDecimal.valueOf(1000));
        stats.setByMember(List.of());
        stats.setByCategory(List.of());
        when(householdService.getStatistics(eq("test@fisbu.com"), isNull(), isNull())).thenReturn(stats);

        mockMvc.perform(get("/households/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.totalAmount").value(1000));
    }

    @Test
    void getStatistics_authHeaderOlmadan403Doner() throws Exception {
        mockMvc.perform(get("/households/statistics"))
                .andExpect(status().isForbidden());
    }
}
