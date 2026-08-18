package com.fisbu.api.category.domain;

// Saf domain modeli — JPA/Spring'e bağımlı değil.
public record Category(Long id, Long userId, String name, String color) {

    // Aynı isimde bulunan bir kategorinin, güncellenmekte olan kategorinin kendisi mi yoksa
    // başka bir kayıt mı olduğunu belirler (isim tekilliği kuralı). excludingCategoryId null ise
    // (yeni kayıt oluşturuluyorsa) hariç tutulacak bir "kendisi" olmadığından her eşleşme çakışmadır.
    public boolean isDifferentCategoryFrom(Long excludingCategoryId) {
        return !id.equals(excludingCategoryId);
    }
}
