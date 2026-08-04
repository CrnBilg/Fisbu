package com.fisbu.api.repository;

import org.springframework.data.jpa.domain.Specification;

import com.fisbu.api.entity.Receipt;
import com.fisbu.api.entity.User;

// GET /receipts/search'teki opsiyonel filtreleri (mağaza adı, kategori) birleştirmek için
public final class ReceiptSpecifications {

    private ReceiptSpecifications() {
    }

    public static Specification<Receipt> hasUser(User user) {
        return (root, query, cb) -> cb.equal(root.get("user"), user);
    }

    public static Specification<Receipt> storeNameContains(String query) {
        String pattern = "%" + query.toLowerCase() + "%";
        return (root, cq, cb) -> cb.like(cb.lower(root.get("storeName")), pattern);
    }

    public static Specification<Receipt> hasCategoryId(Long categoryId) {
        return (root, cq, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Receipt> isUncategorized() {
        return (root, cq, cb) -> cb.isNull(root.get("category"));
    }
}
