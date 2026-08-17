package com.fisbu.api.category.domain;

// Saf domain modeli — JPA/Spring'e bağımlı değil.
public record Category(Long id, Long userId, String name, String color) {
}
