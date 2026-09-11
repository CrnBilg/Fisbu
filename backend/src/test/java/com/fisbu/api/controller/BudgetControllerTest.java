package com.fisbu.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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

import com.fisbu.api.budget.application.port.in.CreateBudgetUseCase;
import com.fisbu.api.budget.application.port.in.DeleteBudgetUseCase;
import com.fisbu.api.budget.application.port.in.GetBudgetSuggestionUseCase;
import com.fisbu.api.budget.application.port.in.GetBudgetsUseCase;
import com.fisbu.api.budget.application.port.in.UpdateBudgetUseCase;
import com.fisbu.api.config.SecurityConfig;
import com.fisbu.api.dto.BudgetResponse;
import com.fisbu.api.dto.BudgetSuggestionResponse;
import com.fisbu.api.repository.UserRepository;
import com.fisbu.api.service.JwtService;

/**
 * BudgetController için dar (@WebMvcTest) kapsamlı testler. Gerçek SecurityConfig import
 * edilir, tüm /budgets uçları "anyRequest().authenticated()" kuralına düşer — kimlik
 * doğrulama yoksa 403 döner (varsayılan AuthenticationEntryPoint). Hexagonal use-case port'ları mock'lanır.
 */
@WebMvcTest(BudgetController.class)
@Import(SecurityConfig.class)
class BudgetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetBudgetsUseCase getBudgetsUseCase;

    @MockitoBean
    private CreateBudgetUseCase createBudgetUseCase;

    @MockitoBean
    private UpdateBudgetUseCase updateBudgetUseCase;

    @MockitoBean
    private DeleteBudgetUseCase deleteBudgetUseCase;

    @MockitoBean
    private GetBudgetSuggestionUseCase getBudgetSuggestionUseCase;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void getBudgets_authHeaderOlmadan403Doner() throws Exception {
        mockMvc.perform(get("/budgets"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "test@fisbu.com")
    void getBudgets_gecerliKimlikleListeDoner() throws Exception {
        BudgetResponse budget = new BudgetResponse();
        budget.setId(1L);
        budget.setCategoryId(2L);
        budget.setMonthlyLimit(BigDecimal.valueOf(1000));
        when(getBudgetsUseCase.getBudgets(eq("test@fisbu.com"), isNull(), isNull()))
                .thenReturn(List.of(budget));

        mockMvc.perform(get("/budgets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].categoryId").value(2));
    }

    @Test
    @WithMockUser(username = "test@fisbu.com")
    void createBudget_gecerliGovdeIle201Doner() throws Exception {
        BudgetResponse created = new BudgetResponse();
        created.setId(5L);
        created.setCategoryId(2L);
        created.setMonthlyLimit(BigDecimal.valueOf(500));
        when(createBudgetUseCase.createBudget(eq("test@fisbu.com"), any())).thenReturn(created);

        mockMvc.perform(post("/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId":2,"monthlyLimit":500,"year":2026,"month":9}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void createBudget_authHeaderOlmadan403Doner() throws Exception {
        mockMvc.perform(post("/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId":2,"monthlyLimit":500,"year":2026,"month":9}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "test@fisbu.com")
    void createBudget_gecersizGovdeIcin400Doner() throws Exception {
        mockMvc.perform(post("/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "test@fisbu.com")
    void getBudgetSuggestion_gecerliKimlikleOneriDoner() throws Exception {
        BudgetSuggestionResponse suggestion = new BudgetSuggestionResponse();
        suggestion.setCategoryId(2L);
        suggestion.setSuggestedLimit(BigDecimal.valueOf(750));
        when(getBudgetSuggestionUseCase.getBudgetSuggestion(eq("test@fisbu.com"), eq(2L), isNull(), isNull()))
                .thenReturn(suggestion);

        mockMvc.perform(get("/budgets/suggestion").param("categoryId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(2));
    }

    @Test
    void getBudgetSuggestion_authHeaderOlmadan403Doner() throws Exception {
        mockMvc.perform(get("/budgets/suggestion").param("categoryId", "2"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "test@fisbu.com")
    void updateBudget_gecerliKimlikleGuncellenmisBudgetDoner() throws Exception {
        BudgetResponse updated = new BudgetResponse();
        updated.setId(1L);
        updated.setMonthlyLimit(BigDecimal.valueOf(1200));
        when(updateBudgetUseCase.updateBudget(eq("test@fisbu.com"), eq(1L), any())).thenReturn(updated);

        mockMvc.perform(put("/budgets/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId":2,"monthlyLimit":1200,"year":2026,"month":9}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyLimit").value(1200));
    }

    @Test
    void updateBudget_authHeaderOlmadan403Doner() throws Exception {
        mockMvc.perform(put("/budgets/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId":2,"monthlyLimit":1200,"year":2026,"month":9}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "test@fisbu.com")
    void deleteBudget_gecerliKimlikle204Doner() throws Exception {
        mockMvc.perform(delete("/budgets/1"))
                .andExpect(status().isNoContent());

        verify(deleteBudgetUseCase).deleteBudget("test@fisbu.com", 1L);
    }

    @Test
    void deleteBudget_authHeaderOlmadan403Doner() throws Exception {
        mockMvc.perform(delete("/budgets/1"))
                .andExpect(status().isForbidden());
    }
}
