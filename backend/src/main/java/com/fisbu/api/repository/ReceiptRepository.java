package com.fisbu.api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fisbu.api.entity.Receipt;
import com.fisbu.api.entity.User;

public interface ReceiptRepository extends JpaRepository<Receipt, Long> {
    List<Receipt> findByUser(User user);
}