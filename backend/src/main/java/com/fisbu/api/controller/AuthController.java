package com.fisbu.api.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fisbu.api.dto.AuthResponse;
import com.fisbu.api.dto.ChangePasswordRequest;
import com.fisbu.api.dto.EmailRequest;
import com.fisbu.api.dto.LoginRequest;
import com.fisbu.api.dto.RegisterRequest;
import com.fisbu.api.dto.ResetPasswordRequest;
import com.fisbu.api.dto.VerifyEmailRequest;
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

    @PostMapping("/change-password") // Giriş yapmış kullanıcının şifresini değiştirir.
    public void changePassword(@AuthenticationPrincipal UserDetails userDetails,
                                @RequestBody @Valid ChangePasswordRequest request) {
        authService.changePassword(userDetails.getUsername(), request);
    }

    @PostMapping("/forgot-password") // Şifre sıfırlama kodu e-posta ile gönderilir.
    public void forgotPassword(@RequestBody @Valid EmailRequest request) {
        authService.forgotPassword(request.getEmail());
    }

    @PostMapping("/reset-password") // Kod doğrulanır ve şifre güncellenir.
    public void resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        authService.resetPassword(request);
    }

    @PostMapping("/verify-email") // Kayıt sonrası gönderilen 6 haneli kod doğrulanır.
    public void verifyEmail(@RequestBody @Valid VerifyEmailRequest request) {
        authService.verifyEmail(request);
    }

    @PostMapping("/resend-verification") // Doğrulama kodu tekrar gönderilir.
    public void resendVerification(@RequestBody @Valid EmailRequest request) {
        authService.resendVerificationCode(request.getEmail());
    }

    @DeleteMapping("/account") // Giriş yapmış kullanıcının hesabını ve tüm verilerini siler.
    public void deleteAccount(@AuthenticationPrincipal UserDetails userDetails) {
        authService.deleteAccount(userDetails.getUsername());
    }
}
