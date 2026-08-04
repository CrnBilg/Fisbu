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

// Aile bütçe modu: kullanıcılar kendi fiş/kategori/bütçelerini yönetmeye devam eder,
// Household sadece davet koduyla katılınan bir grup olup toplu istatistik görünümü sağlar
@Entity
@Table(name = "households")
@Getter
@Setter
public class Household {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "invite_code", nullable = false, unique = true)
    private String inviteCode;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
