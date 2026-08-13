package com.fisbu.api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.fisbu.api.entity.ReceiptItem;

public interface ReceiptItemRepository extends JpaRepository<ReceiptItem, Long> {

    // receipt ManyToOne LAZY — çağıranlar item.getReceipt().getReceiptDate()/getStoreName() ile
    // döngü içinde erişiyor, EntityGraph olmadan her satır için ayrı sorgu tetiklenir (N+1)
    @EntityGraph(attributePaths = {"receipt"})
    List<ReceiptItem> findByReceipt_User_IdAndNormalizedNameOrderByReceipt_ReceiptDateAsc(
            Long userId, String normalizedName);

    @EntityGraph(attributePaths = {"receipt"})
    List<ReceiptItem> findByReceipt_User_Id(Long userId);
}
