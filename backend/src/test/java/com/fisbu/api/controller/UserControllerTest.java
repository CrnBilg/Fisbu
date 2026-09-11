package com.fisbu.api.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
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
import com.fisbu.api.dto.NotificationPrefsResponse;
import com.fisbu.api.dto.ProfileResponse;
import com.fisbu.api.dto.UserDataExportResponse;
import com.fisbu.api.repository.UserRepository;
import com.fisbu.api.service.JwtService;
import com.fisbu.api.service.UserService;

/**
 * UserController için dar (@WebMvcTest) kapsamlı testler — TEST-003/TEST-004 deseni izlenir.
 */
@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void saveFcmToken_authHeaderOlmadan403Doner() throws Exception {
        mockMvc.perform(post("/users/fcm-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fcmToken\":\"abc\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "test@fisbu.com")
    void saveFcmToken_gecerliIstekte204Doner() throws Exception {
        mockMvc.perform(post("/users/fcm-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fcmToken\":\"abc123\"}"))
                .andExpect(status().isNoContent());

        verify(userService).updateFcmToken("test@fisbu.com", "abc123");
    }

    @Test
    @WithMockUser(username = "test@fisbu.com")
    void saveFcmToken_bosTokenIle400Doner() throws Exception {
        mockMvc.perform(post("/users/fcm-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fcmToken\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void exportMyData_authHeaderOlmadan403Doner() throws Exception {
        mockMvc.perform(get("/users/me/export"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "test@fisbu.com")
    void exportMyData_gecerliKimlikleSonucDoner() throws Exception {
        ProfileResponse profile = new ProfileResponse("test@fisbu.com", "Test Kullanıcı", null,
                LocalDateTime.now().toString());
        UserDataExportResponse response = new UserDataExportResponse(
                LocalDateTime.now(), profile, List.of(), List.of(), List.of());
        when(userService.exportMyData("test@fisbu.com")).thenReturn(response);

        mockMvc.perform(get("/users/me/export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.email").value("test@fisbu.com"));
    }

    @Test
    void getNotificationPrefs_authHeaderOlmadan403Doner() throws Exception {
        mockMvc.perform(get("/users/notification-prefs"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "test@fisbu.com")
    void getNotificationPrefs_gecerliKimlikleSonucDoner() throws Exception {
        when(userService.getNotificationPrefs("test@fisbu.com"))
                .thenReturn(new NotificationPrefsResponse(true, false));

        mockMvc.perform(get("/users/notification-prefs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.budgetWarningEnabled").value(true))
                .andExpect(jsonPath("$.budgetOverspendEnabled").value(false));
    }

    @Test
    void updateNotificationPrefs_authHeaderOlmadan403Doner() throws Exception {
        mockMvc.perform(put("/users/notification-prefs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"budgetWarningEnabled\":true,\"budgetOverspendEnabled\":true}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "test@fisbu.com")
    void updateNotificationPrefs_gecerliIstekteGuncellenmisTercihleriDoner() throws Exception {
        when(userService.updateNotificationPrefs(eq("test@fisbu.com"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new NotificationPrefsResponse(false, true));

        mockMvc.perform(put("/users/notification-prefs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"budgetWarningEnabled\":false,\"budgetOverspendEnabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.budgetWarningEnabled").value(false))
                .andExpect(jsonPath("$.budgetOverspendEnabled").value(true));
    }

    @Test
    @WithMockUser(username = "test@fisbu.com")
    void updateNotificationPrefs_alanEksikse400Doner() throws Exception {
        mockMvc.perform(put("/users/notification-prefs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"budgetWarningEnabled\":true}"))
                .andExpect(status().isBadRequest());
    }
}
