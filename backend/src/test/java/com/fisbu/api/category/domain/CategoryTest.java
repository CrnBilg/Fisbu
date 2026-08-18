package com.fisbu.api.category.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CategoryTest {

    @Test
    void isNotDifferent_whenSameId() {
        Category category = new Category(5L, 1L, "Market", "#00ff00");

        assertThat(category.isDifferentCategoryFrom(5L)).isFalse();
    }

    @Test
    void isDifferent_whenDifferentId() {
        Category category = new Category(5L, 1L, "Market", "#00ff00");

        assertThat(category.isDifferentCategoryFrom(9L)).isTrue();
    }

    @Test
    void isDifferent_whenExcludingIdIsNull() {
        // Yeni kayıt oluşturuluyor senaryosu — hariç tutulacak "kendisi" yok,
        // bu yüzden bulunan her eşleşme çakışma sayılmalı
        Category category = new Category(5L, 1L, "Market", "#00ff00");

        assertThat(category.isDifferentCategoryFrom(null)).isTrue();
    }
}
