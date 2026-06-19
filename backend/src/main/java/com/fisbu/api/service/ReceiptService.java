package com.fisbu.api.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fisbu.api.dto.ReceiptRequest;
import com.fisbu.api.dto.ReceiptResponse;
import com.fisbu.api.entity.Category;
import com.fisbu.api.entity.Receipt;
import com.fisbu.api.entity.User;
import com.fisbu.api.repository.CategoryRepository;
import com.fisbu.api.repository.ReceiptRepository;
import com.fisbu.api.repository.UserRepository;

@Service
public class ReceiptService {

    private final ReceiptRepository receiptRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    public ReceiptService(ReceiptRepository receiptRepository,
                          UserRepository userRepository,
                          CategoryRepository categoryRepository) {
        this.receiptRepository = receiptRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
    }

    // Kullanıcının tüm fişlerini listeler
    public List<ReceiptResponse> getReceipts(String email) {
        User user = getUserByEmail(email);
        return receiptRepository.findByUser(user)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // Yeni fiş ekler
    public ReceiptResponse createReceipt(String email, ReceiptRequest request) {
        User user = getUserByEmail(email);

        Receipt receipt = new Receipt();
        receipt.setUser(user);
        receipt.setStoreName(request.getStoreName());
        receipt.setTotalAmount(request.getTotalAmount());
        receipt.setReceiptDate(request.getReceiptDate());
        receipt.setImageUrl(request.getImageUrl());
        receipt.setRawOcrText(request.getRawOcrText());

        // Kategori opsiyonel — gönderilmişse set et
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Kategori bulunamadı"));
            receipt.setCategory(category);
        }

        return toResponse(receiptRepository.save(receipt));
    }

    // Entity'den DTO'ya dönüştürme
    private ReceiptResponse toResponse(Receipt receipt) {
        ReceiptResponse response = new ReceiptResponse();
        response.setId(receipt.getId());
        response.setStoreName(receipt.getStoreName());
        response.setTotalAmount(receipt.getTotalAmount());
        response.setReceiptDate(receipt.getReceiptDate());
        response.setImageUrl(receipt.getImageUrl());
        response.setRawOcrText(receipt.getRawOcrText());
        response.setCreatedAt(receipt.getCreatedAt());

        if (receipt.getCategory() != null) {
            response.setCategoryId(receipt.getCategory().getId());
            response.setCategoryName(receipt.getCategory().getName());
        }

        return response;
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Kullanıcı bulunamadı"));
    }
}