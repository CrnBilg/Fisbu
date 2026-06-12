package com.fisbu.api.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fisbu.api.dto.RegisterRequest;
import com.fisbu.api.entity.User;
import com.fisbu.api.service.AuthService;

@RestController // AuthController, kullanıcı kayıt işlemlerini yönetir ve HTTP isteklerini işler
@RequestMapping("/auth") // Tüm isteklerin "/auth" ile başlamasını sağlar, örneğin "/auth/register"
public class AuthController {

    private final AuthService authService;// AuthService'i kullanarak kullanıcı kayıt işlemlerini yönetir

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register") // HTTP POST isteği ile "/auth/register" endpoint'ine gelen kayıt isteklerini işler.
    public User register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }
}