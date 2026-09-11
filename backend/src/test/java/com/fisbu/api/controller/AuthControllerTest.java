package com.fisbu.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fisbu.api.config.SecurityConfig;
import com.fisbu.api.dto.ProfileResponse;
import com.fisbu.api.dto.RegisterResponse;
import com.fisbu.api.repository.UserRepository;
import com.fisbu.api.service.AuthService;
import com.fisbu.api.service.JwtService;

/**
 * AuthController için dar (@WebMvcTest) kapsamlı testler. Gerçek SecurityConfig import
 * edilir; böylece "/auth/register" ve "/auth/login" gibi permitAll uçlar ile diğer korumalı
 * uçlar için gerçek yetkilendirme davranışı doğrulanır. JwtAuthFilter'ın bağımlılıkları
 * (JwtService, UserRepository) mock'lanır ki gerçek DB'ye bağlanılmasın.
 */
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void register_permitAll_dondurur201benzeriBasariliYanit() throws Exception {
        RegisterResponse response = new RegisterResponse(1L, "test@fisbu.com", "Test Kullanici", false);
        when(authService.register(any())).thenReturn(response);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"test@fisbu.com","password":"Sifre123!","name":"Test Kullanici"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@fisbu.com"))
                .andExpect(jsonPath("$.name").value("Test Kullanici"));
    }

    @Test
    void register_authHeaderOlmadanBileErismeSerbest() throws Exception {
        // /auth/register SecurityConfig'de permitAll — auth header gerektirmemeli
        when(authService.register(any())).thenReturn(new RegisterResponse(1L, "a@b.com", "A", false));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"a@b.com","password":"Sifre123!","name":"A"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void login_permitAll_tokenIcerenYanitDoner() throws Exception {
        when(authService.login(any())).thenReturn("jwt-token-abc");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"test@fisbu.com","password":"Sifre123!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-abc"));
    }

    @Test
    void login_gecersizGovdeIcin400Doner() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void profile_authHeaderOlmadanErisim403Doner() throws Exception {
        // /auth/profile permitAll listesinde değil, anyRequest().authenticated() kuralına düşer
        mockMvc.perform(get("/auth/profile"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "test@fisbu.com")
    void profile_gecerliKimlikleProfilDoner() throws Exception {
        ProfileResponse profile = new ProfileResponse("test@fisbu.com", "Test", null, "2024-01-01T00:00:00");
        when(authService.getProfile("test@fisbu.com")).thenReturn(profile);

        mockMvc.perform(get("/auth/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@fisbu.com"));
    }

    @Test
    void changePassword_authHeaderOlmadan403Doner() throws Exception {
        mockMvc.perform(post("/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"Eski123!","newPassword":"Yeni123!"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "test@fisbu.com")
    void changePassword_gecerliKimlikleBasarili() throws Exception {
        mockMvc.perform(post("/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"Eski123!","newPassword":"Yeni123!"}
                                """))
                .andExpect(status().isOk());

        verify(authService).changePassword(anyString(), any());
    }

    @Test
    void deleteAccount_authHeaderOlmadan403Doner() throws Exception {
        mockMvc.perform(delete("/auth/account"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "test@fisbu.com")
    void deleteAccount_gecerliKimlikleBasarili() throws Exception {
        mockMvc.perform(delete("/auth/account"))
                .andExpect(status().isOk());

        verify(authService).deleteAccount("test@fisbu.com");
    }

    @Test
    @WithMockUser(username = "test@fisbu.com")
    void updateProfile_gecerliKimlikleGuncellenmisProfilDoner() throws Exception {
        ProfileResponse updated = new ProfileResponse("test@fisbu.com", "Yeni Ad", null, "2024-01-01T00:00:00");
        when(authService.updateProfile(anyString(), any())).thenReturn(updated);

        mockMvc.perform(put("/auth/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Yeni Ad"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Yeni Ad"));
    }
}
