package com.fisbu.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fisbu.api.dto.FcmTokenRequest;
import com.fisbu.api.dto.UserDataExportResponse;
import com.fisbu.api.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** Mobil uygulama giriş sonrası FCM token'ını buraya kaydeder. */
    @PostMapping("/fcm-token")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveFcmToken(@AuthenticationPrincipal UserDetails userDetails,
                             @RequestBody @Valid FcmTokenRequest request) {
        userService.updateFcmToken(userDetails.getUsername(), request.getFcmToken());
    }

    // KVKK "verilerimi indir" — kullanıcının profil/kategori/bütçe/fiş verilerinin tamamını JSON olarak döner
    @GetMapping("/me/export")
    public UserDataExportResponse exportMyData(@AuthenticationPrincipal UserDetails userDetails) {
        return userService.exportMyData(userDetails.getUsername());
    }
}
