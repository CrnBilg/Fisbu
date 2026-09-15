package com.fisbu.api.category.application.port.out;

/** ARCH-002/ADR-004: hesap silme akışında kullanıcının tüm kategorilerini siler. */
public interface DeleteCategoriesByUserPort {

    void deleteAllByUserId(Long userId);
}
