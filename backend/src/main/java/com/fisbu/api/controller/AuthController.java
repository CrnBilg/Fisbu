package com.fisbu.api.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fisbu.api.dto.AuthResponse;
import com.fisbu.api.dto.ProfileResponse;
import com.fisbu.api.dto.UpdateProfileRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import com.fisbu.api.dto.ChangePasswordRequest;
import com.fisbu.api.dto.LoginRequest;
import com.fisbu.api.dto.RegisterRequest;
import com.fisbu.api.entity.User;
import com.fisbu.api.service.AuthService;

import jakarta.validation.Valid;


@RestController // AuthController, kullanıcı kayıt işlemlerini yönetir ve HTTP isteklerini işler
@RequestMapping("/auth") // Tüm isteklerin "/auth" ile başlamasını sağlar, örneğin "/auth/register"
public class AuthController {

    private final AuthService authService;// AuthService'i kullanarak kullanıcı kayıt işlemlerini yönetir

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register") // HTTP POST isteği ile "/auth/register" endpoint'ine gelen kayıt isteklerini işler.
public User register(@RequestBody @Valid RegisterRequest request) {        return authService.register(request);
    }

    @PostMapping("/login")// HTTP POST isteği ile "/auth/login" endpoint'ine gelen giriş isteklerini işler.
    public AuthResponse login(@RequestBody LoginRequest request) {//Gelen JSON'ı LoginRequest nesnesine dönüştürür ve authService.login() metodunu çağırarak kullanıcı giriş işlemini gerçekleştirir. Giriş başarılı ise bir token döndürür.
        String token = authService.login(request);
        return new AuthResponse(token);
    }

    @PostMapping("/change-password")
    public void changePassword(@AuthenticationPrincipal UserDetails userDetails,
                                @RequestBody @Valid ChangePasswordRequest request) {
        authService.changePassword(userDetails.getUsername(), request);
    }

    @GetMapping("/profile")
    public ProfileResponse getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        return authService.getProfile(userDetails.getUsername());
    }

    @PutMapping("/profile")
    public ProfileResponse updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                                          @RequestBody UpdateProfileRequest request) {
        return authService.updateProfile(userDetails.getUsername(), request);
    }
}