package com.fisbu.api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fisbu.api.entity.ReceiptItem;

public interface ReceiptItemRepository extends JpaRepository<ReceiptItem, Long> {
    List<ReceiptItem> findByReceipt_User_IdAndNormalizedNameOrderByReceipt_ReceiptDateAsc(
            Long userId, String normalizedName);

    List<ReceiptItem> findByReceipt_User_Id(Long userId);
}
