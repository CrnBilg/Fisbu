package com.fisbu.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "categories", indexes = {
        @Index(name = "idx_categories_user_id", columnList = "user_id")
}, uniqueConstraints = {
        // Aynı kullanıcı için aynı isimde iki kategori oluşmasını DB seviyesinde engeller
        // (bkz. issue #46 — geçmişte varsayılan kategorilerin bir kullanıcı için iki kez
        // eklendiği ve tekrar edecek bir yolun kod tabanında engellenmediği görüldü)
        @UniqueConstraint(name = "uk_categories_user_name", columnNames = {"user_id", "name"})
})
@Getter
@Setter
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Column
    private String color;
}