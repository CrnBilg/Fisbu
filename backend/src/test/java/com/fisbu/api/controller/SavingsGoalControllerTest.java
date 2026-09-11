package com.fisbu.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import com.fisbu.api.dto.SavingsGoalResponse;
import com.fisbu.api.dto.SavingsGoalSuggestionResponse;
import com.fisbu.api.repository.UserRepository;
import com.fisbu.api.service.JwtService;
import com.fisbu.api.service.SavingsGoalService;

/**
 * SavingsGoalController için dar (@WebMvcTest) kapsamlı testler. TEST-003'teki desen izlenir:
 * gerçek SecurityConfig import edilir, JwtAuthFilter bağımlılıkları ve controller'ın kendi
 * service bağımlılığı mock'lanır — gerçek DB'ye bağlanılmaz.
 */
@WebMvcTest(SavingsGoalController.class)
@Import(SecurityConfig.class)
class SavingsGoalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SavingsGoalService savingsGoalService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    private SavingsGoalResponse sampleResponse() {
        SavingsGoalResponse r = new SavingsGoalResponse();
        r.setId(1L);
        r.setName("Tatil");
        r.setTargetAmount(new BigDecimal("1000"));
        r.setCurrentAmount(new BigDecimal("100"));
        r.setProgressPercent(10.0);
        r.setAchieved(false);
        return r;
    }

    @Test
    void createGoal_authHeaderOlmadan403Doner() throws Exception {
        mockMvc.perform(post("/savings-goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Tatil","targetAmount":1000}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "test@fisbu.com")
    void createGoal_gecerliKimlikle201Doner() throws Exception {
        when(savingsGoalService.createGoal(eq("test@fisbu.com"), any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/savings-goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Tatil","targetAmount":1000}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Tatil"));
    }

    @Test
    @WithMockUser(username = "test@fisbu.com")
    void createGoal_gecersizGovdeIcin400Doner() throws Exception {
        mockMvc.perform(post("/savings-goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getGoals_authHeaderOlmadan403Doner() throws Exception {
        mockMvc.perform(get("/savings-goals"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "test@fisbu.com")
    void getGoals_gecerliKimlikleListeDoner() throws Exception {
        when(savingsGoalService.getGoals("test@fisbu.com")).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/savings-goals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Tatil"));
    }

    @Test
    @WithMockUser(username = "test@fisbu.com")
    void contribute_gecerliKimlikleGuncellenmisHedefDoner() throws Exception {
        SavingsGoalResponse updated = sampleResponse();
        updated.setCurrentAmount(new BigDecimal("200"));
        when(savingsGoalService.contribute(eq("test@fisbu.com"), eq(1L), any())).thenReturn(updated);

        mockMvc.perform(put("/savings-goals/1/contribute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":100}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentAmount").value(200));
    }

    @Test
    void contribute_authHeaderOlmadan403Doner() throws Exception {
        mockMvc.perform(put("/savings-goals/1/contribute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":100}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "test@fisbu.com")
    void getSuggestion_gecerliKimlikleOneriDoner() throws Exception {
        SavingsGoalSuggestionResponse suggestion = new SavingsGoalSuggestionResponse();
        suggestion.setRequiredMonthlyContribution(new BigDecimal("50"));
        suggestion.setComment("İyi gidiyorsun");
        when(savingsGoalService.getSuggestion("test@fisbu.com", 1L)).thenReturn(suggestion);

        mockMvc.perform(get("/savings-goals/1/suggestion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comment").value("İyi gidiyorsun"));
    }

    @Test
    void deleteGoal_authHeaderOlmadan403Doner() throws Exception {
        mockMvc.perform(delete("/savings-goals/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "test@fisbu.com")
    void deleteGoal_gecerliKimlikle204Doner() throws Exception {
        mockMvc.perform(delete("/savings-goals/1"))
                .andExpect(status().isNoContent());

        verify(savingsGoalService).deleteGoal(eq("test@fisbu.com"), eq(1L));
    }
}
