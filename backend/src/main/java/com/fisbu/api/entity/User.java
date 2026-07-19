package com.fisbu.api.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
}