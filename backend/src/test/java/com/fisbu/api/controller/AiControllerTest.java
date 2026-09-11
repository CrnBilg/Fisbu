package com.fisbu.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
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
import com.fisbu.api.dto.ChatResponse;
import com.fisbu.api.dto.RestoreReceiptResponse;
import com.fisbu.api.dto.SpendingAnalysisResponse;
import com.fisbu.api.repository.UserRepository;
import com.fisbu.api.service.FinancialChatService;
import com.fisbu.api.service.JwtService;
import com.fisbu.api.service.ReceiptAiService;

/**
 * AiController için dar (@WebMvcTest) kapsamlı testler — TEST-003'teki desen izlenir.
 */
@WebMvcTest(AiController.class)
@Import(SecurityConfig.class)
class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReceiptAiService receiptAiService;

    @MockitoBean
    private FinancialChatService financialChatService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void restoreReceipt_authHeaderOlmadan403Doner() throws Exception {
        mockMvc.perform(post("/ai/restore-receipt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rawOcrText":"Migros 10 TL"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "test@fisbu.com")
    void restoreReceipt_gecerliKimlikleSonucDoner() throws Exception {
        RestoreReceiptResponse response = new RestoreReceiptResponse();
        response.setStoreName("Migros");
        response.setTotalAmount(new BigDecimal("10"));
        response.setConfidenceScore(80);
        response.setItems(List.of());
        when(receiptAiService.restoreReceipt(anyString(), any())).thenReturn(response);

        mockMvc.perform(post("/ai/restore-receipt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rawOcrText":"Migros 10 TL"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeName").value("Migros"));
    }

    @Test
    @WithMockUser(username = "test@fisbu.com")
    void restoreReceipt_gecersizGovdeIcin400Doner() throws Exception {
        mockMvc.perform(post("/ai/restore-receipt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void spendingAnalysis_authHeaderOlmadan403Doner() throws Exception {
        mockMvc.perform(get("/ai/spending-analysis"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "test@fisbu.com")
    void spendingAnalysis_gecerliKimlikleSonucDoner() throws Exception {
        SpendingAnalysisResponse response = new SpendingAnalysisResponse();
        response.setYear(2024);
        response.setMonth(1);
        response.setTotalAmount(new BigDecimal("500"));
        response.setComment("Bu ay harcamalarınız arttı");
        when(receiptAiService.getSpendingAnalysis("test@fisbu.com", null, null)).thenReturn(response);

        mockMvc.perform(get("/ai/spending-analysis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2024));
    }

    @Test
    void chat_authHeaderOlmadan403Doner() throws Exception {
        mockMvc.perform(post("/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"Bu ay ne kadar tasarruf edebilirim?"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "test@fisbu.com")
    void chat_gecerliKimlikleYanitDoner() throws Exception {
        when(financialChatService.sendMessage(anyString(), any())).thenReturn(new ChatResponse("Merhaba!"));

        mockMvc.perform(post("/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"Bu ay ne kadar tasarruf edebilirim?"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Merhaba!"));
    }

    @Test
    @WithMockUser(username = "test@fisbu.com")
    void chat_gecersizGovdeIcin400Doner() throws Exception {
        mockMvc.perform(post("/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
