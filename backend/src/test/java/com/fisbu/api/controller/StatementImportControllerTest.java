package com.fisbu.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fisbu.api.config.SecurityConfig;
import com.fisbu.api.dto.ParsedStatementResponse;
import com.fisbu.api.receipt.adapter.in.web.BulkReceiptImportResponse;
import com.fisbu.api.receipt.adapter.in.web.ReceiptWebMapper;
import com.fisbu.api.receipt.application.port.in.CreateReceiptUseCase.CreateReceiptCommand;
import com.fisbu.api.receipt.application.port.in.CreateReceiptsBulkUseCase;
import com.fisbu.api.receipt.application.port.in.CreateReceiptsBulkUseCase.BulkCreateResult;
import com.fisbu.api.repository.UserRepository;
import com.fisbu.api.service.JwtService;
import com.fisbu.api.service.StatementImportService;

/**
 * StatementImportController için dar (@WebMvcTest) kapsamlı testler — TEST-003'teki desen
 * izlenir (gerçek SecurityConfig import, JwtAuthFilter bağımlılıkları ve controller'ın kendi
 * service/use-case bağımlılıkları mock'lanır).
 */
@WebMvcTest(StatementImportController.class)
@Import(SecurityConfig.class)
class StatementImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StatementImportService statementImportService;

    @MockitoBean
    private CreateReceiptsBulkUseCase createReceiptsBulkUseCase;

    @MockitoBean
    private ReceiptWebMapper mapper;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void parseStatement_authHeaderOlmadan403Doner() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "statement.csv", "text/csv", "a,b,c".getBytes());

        mockMvc.perform(multipart("/receipts/import/parse").file(file))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "test@fisbu.com")
    void parseStatement_gecerliKimlikleAyristirilmisSonucDoner() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "statement.csv", "text/csv", "a,b,c".getBytes());
        ParsedStatementResponse response = new ParsedStatementResponse();
        response.setSourceType("csv");
        response.setWarnings(List.of());
        when(statementImportService.parseStatement(anyString(), any())).thenReturn(response);

        mockMvc.perform(multipart("/receipts/import/parse").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceType").value("csv"));
    }

    @Test
    void confirmImport_authHeaderOlmadan403Doner() throws Exception {
        mockMvc.perform(post("/receipts/import/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receipts":[]}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "test@fisbu.com")
    void confirmImport_gecersizGovdeIcin400Doner() throws Exception {
        mockMvc.perform(post("/receipts/import/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receipts":[]}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "test@fisbu.com")
    void confirmImport_gecerliKimlikleOlusturulanSonucDoner() throws Exception {
        CreateReceiptCommand command = new CreateReceiptCommand("test@fisbu.com", "Migros",
                new java.math.BigDecimal("10"), java.time.LocalDate.of(2024, 1, 1), null, null, null, null, null,
                false, List.of());
        BulkCreateResult result = new BulkCreateResult(List.of(), List.of());
        BulkReceiptImportResponse mapped = new BulkReceiptImportResponse();
        mapped.setCreated(List.of());
        mapped.setFailed(List.of());

        when(mapper.toCommand(anyString(), any())).thenReturn(command);
        when(createReceiptsBulkUseCase.createReceiptsBulk(anyString(), any())).thenReturn(result);
        when(mapper.toBulkResponse(result)).thenReturn(mapped);

        mockMvc.perform(post("/receipts/import/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receipts":[{"storeName":"Migros","totalAmount":10,"receiptDate":"2024-01-01"}]}
                                """))
                .andExpect(status().isCreated());
    }
}
