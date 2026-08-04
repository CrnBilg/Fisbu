package com.fisbu.api.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.fisbu.api.entity.Category;
import com.fisbu.api.entity.Receipt;
import com.fisbu.api.entity.User;

public interface ReceiptRepository extends JpaRepository<Receipt, Long>, JpaSpecificationExecutor<Receipt> {
    List<Receipt> findByUser(User user);
    List<Receipt> findByCategory(Category category);
    List<Receipt> findByUserAndReceiptDateBetween(User user, LocalDate start, LocalDate end);
    List<Receipt> findByUserAndCategoryAndReceiptDateBetween(User user, Category category, LocalDate start, LocalDate end);
    List<Receipt> findByUserAndStoreNameIgnoreCaseAndTotalAmountAndReceiptDate(
            User user, String storeName, BigDecimal totalAmount, LocalDate receiptDate);

    // Aile bütçe modu — household üyelerinin fişlerini tek sorguda toplamak için
    List<Receipt> findByUserInAndReceiptDateBetween(List<User> users, LocalDate start, LocalDate end);
}