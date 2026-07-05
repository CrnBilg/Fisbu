package com.fisbu.api.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fisbu.api.dto.CategoryTotalResponse;
import com.fisbu.api.dto.MonthlyStatisticsResponse;
import com.fisbu.api.entity.Category;
import com.fisbu.api.entity.Receipt;
import com.fisbu.api.entity.User;
import com.fisbu.api.repository.ReceiptRepository;
import com.fisbu.api.repository.UserRepository;

@Service
public class StatisticsService {

    private final ReceiptRepository receiptRepository;
    private final UserRepository userRepository;

    public StatisticsService(ReceiptRepository receiptRepository, UserRepository userRepository) {
        this.receiptRepository = receiptRepository;
        this.userRepository = userRepository;
    }

    public MonthlyStatisticsResponse getMonthlyStatistics(String email, Integer year, Integer month) {
        User user = getUserByEmail(email);

        LocalDate today = LocalDate.now();
        int resolvedYear = year != null ? year : today.getYear();
        int resolvedMonth = month != null ? month : today.getMonthValue();

        LocalDate start = LocalDate.of(resolvedYear, resolvedMonth, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        List<Receipt> receipts = receiptRepository.findByUserAndReceiptDateBetween(user, start, end);

        Map<Long, CategoryTotalResponse> totals = new LinkedHashMap<>();
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (Receipt receipt : receipts) {
            BigDecimal amount = receipt.getTotalAmount() != null ? receipt.getTotalAmount() : BigDecimal.ZERO;
            grandTotal = grandTotal.add(amount);

            Category category = receipt.getCategory();
            Long key = category != null ? category.getId() : null;

            CategoryTotalResponse entry = totals.get(key);
            if (entry == null) {
                entry = new CategoryTotalResponse();
                entry.setCategoryId(key);
                entry.setCategoryName(category != null ? category.getName() : "Diğer");
                entry.setColor(category != null ? category.getColor() : null);
                entry.setTotalAmount(amount);
                totals.put(key, entry);
            } else {
                entry.setTotalAmount(entry.getTotalAmount().add(amount));
            }
        }

        List<CategoryTotalResponse> sortedCategories = new ArrayList<>(totals.values());
        sortedCategories.sort(Comparator.comparing(CategoryTotalResponse::getTotalAmount).reversed());

        MonthlyStatisticsResponse response = new MonthlyStatisticsResponse();
        response.setYear(resolvedYear);
        response.setMonth(resolvedMonth);
        response.setTotalAmount(grandTotal);
        response.setCategories(sortedCategories);
        return response;
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Kullanıcı bulunamadı"));
    }
}
