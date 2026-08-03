package com.fisbu.api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fisbu.api.dto.CategoryTotalResponse;
import com.fisbu.api.dto.MonthlyStatisticsResponse;
import com.fisbu.api.dto.StoreStatResponse;
import com.fisbu.api.dto.TopProductResponse;
import com.fisbu.api.entity.Category;
import com.fisbu.api.entity.Receipt;
import com.fisbu.api.entity.ReceiptItem;
import com.fisbu.api.entity.User;
import com.fisbu.api.repository.ReceiptItemRepository;
import com.fisbu.api.repository.ReceiptRepository;
import com.fisbu.api.repository.UserRepository;

@Service
public class StatisticsService {

    private static final int DEFAULT_TOP_PRODUCT_LIMIT = 10;

    private final ReceiptRepository receiptRepository;
    private final ReceiptItemRepository receiptItemRepository;
    private final UserRepository userRepository;

    public StatisticsService(ReceiptRepository receiptRepository,
                              ReceiptItemRepository receiptItemRepository,
                              UserRepository userRepository) {
        this.receiptRepository = receiptRepository;
        this.receiptItemRepository = receiptItemRepository;
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

    // Mağaza bazlı harcama özeti — hangi markette ne kadar/ortalama harcandığını gösterir
    public List<StoreStatResponse> getStoreStatistics(String email) {
        User user = getUserByEmail(email);
        List<Receipt> receipts = receiptRepository.findByUser(user);

        Map<String, StoreStatResponse> byStore = new LinkedHashMap<>();
        for (Receipt receipt : receipts) {
            String storeName = receipt.getStoreName() != null ? receipt.getStoreName().trim() : "Diğer";
            BigDecimal amount = receipt.getTotalAmount() != null ? receipt.getTotalAmount() : BigDecimal.ZERO;

            StoreStatResponse entry = byStore.get(storeName);
            if (entry == null) {
                entry = new StoreStatResponse();
                entry.setStoreName(storeName);
                entry.setTotalAmount(amount);
                entry.setReceiptCount(1);
                byStore.put(storeName, entry);
            } else {
                entry.setTotalAmount(entry.getTotalAmount().add(amount));
                entry.setReceiptCount(entry.getReceiptCount() + 1);
            }
        }

        List<StoreStatResponse> stores = new ArrayList<>(byStore.values());
        for (StoreStatResponse store : stores) {
            store.setAverageAmount(store.getTotalAmount()
                    .divide(BigDecimal.valueOf(store.getReceiptCount()), 2, RoundingMode.HALF_UP));
        }
        stores.sort(Comparator.comparing(StoreStatResponse::getTotalAmount).reversed());
        return stores;
    }

    // En sık alınan ürünler — ReceiptItem verisinden normalized isme göre gruplanır
    public List<TopProductResponse> getTopProducts(String email, Integer limit) {
        User user = getUserByEmail(email);
        int resolvedLimit = limit != null && limit > 0 ? limit : DEFAULT_TOP_PRODUCT_LIMIT;

        List<ReceiptItem> items = receiptItemRepository.findByReceipt_User_Id(user.getId());

        Map<String, TopProductResponse> byProduct = new LinkedHashMap<>();
        for (ReceiptItem item : items) {
            BigDecimal quantity = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ONE;
            BigDecimal spent = item.getUnitPrice() != null ? item.getUnitPrice().multiply(quantity) : BigDecimal.ZERO;

            TopProductResponse entry = byProduct.get(item.getNormalizedName());
            if (entry == null) {
                entry = new TopProductResponse();
                entry.setNormalizedName(item.getNormalizedName());
                entry.setDisplayName(item.getProductName());
                entry.setPurchaseCount(1);
                entry.setTotalSpent(spent);
                byProduct.put(item.getNormalizedName(), entry);
            } else {
                entry.setPurchaseCount(entry.getPurchaseCount() + 1);
                entry.setTotalSpent(entry.getTotalSpent().add(spent));
            }
        }

        return byProduct.values().stream()
                .sorted(Comparator.comparing(TopProductResponse::getPurchaseCount).reversed())
                .limit(resolvedLimit)
                .collect(Collectors.toList());
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Kullanıcı bulunamadı"));
    }
}
