package com.fisbu.api.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity // db tablosu oldugunu belirttik.
@Table(name = "users")
@Getter
@Setter //otomatik lombok ile getter-setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)// null olamaz,unique olmali.
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "name")
    private String name;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    // Var olan kullanıcılar mevcut mekanizmayla giriş yapmaya devam edebilsin diye
    // varsayılan true; sadece yeni kayıtlarda register() ile false'a çekilir.
    @Column(name = "email_verified", nullable = false, columnDefinition = "boolean default true")
    private Boolean emailVerified = true;

    @Column(name = "verification_code")
    private String verificationCode;

    @Column(name = "verification_code_expiry")
    private LocalDateTime verificationCodeExpiry;

    @Column(name = "reset_password_code")
    private String resetPasswordCode;

    @Column(name = "reset_password_code_expiry")
    private LocalDateTime resetPasswordCodeExpiry;

    // Kod başına başarısız deneme sayacı — brute force koruması: eşik aşılırsa kod
    // süresi dolmamış olsa bile geçersiz sayılır, yeni kod istenmesi gerekir
    @Column(name = "verification_attempts", nullable = false, columnDefinition = "integer default 0")
    private Integer verificationAttempts = 0;

    @Column(name = "reset_password_attempts", nullable = false, columnDefinition = "integer default 0")
    private Integer resetPasswordAttempts = 0;

    // Firebase Cloud Messaging cihaz token'ı — bütçe push bildirimleri buraya gönderilir
    @Column(name = "fcm_token")
    private String fcmToken;

    // Şifre değişince/resetlenince artırılır — JWT'ye gömülen versiyonla eşleşmeyen
    // eski token'lar JwtAuthFilter tarafından reddedilir (oturum iptali/revocation)
    @Column(name = "token_version", nullable = false, columnDefinition = "integer default 0")
    private Integer tokenVersion = 0;

    // Bütçe push bildirim tercihleri — BudgetService.checkBudgetAndNotify bunlara bakarak
    // kapatılmış bildirim türünü göndermez (bkz. NotificationSettingsScreen)
    @Column(name = "notify_budget_warning", nullable = false, columnDefinition = "boolean default true")
    private Boolean notifyBudgetWarning = true;

    @Column(name = "notify_budget_overspend", nullable = false, columnDefinition = "boolean default true")
    private Boolean notifyBudgetOverspend = true;

    // Aile bütçe modu — kullanıcı en fazla bir Household'a üye olabilir; Category/Budget/Receipt
    // ownership'i etkilemez, sadece toplu istatistik görünümü için kullanılır
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_id")
    private Household household;
}