package com.fisbu.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity // db tablosu oldugunu belirttik.
@Table(name = "users")
@Getter
@Setter //otomatik lombok ile getter-setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)// null olamaz,unique olmalı.
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}