package com.fisbu.api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fisbu.api.dto.PersonalInflationResponse;
import com.fisbu.api.dto.ProductInflationResponse;
import com.fisbu.api.dto.ProductPriceHistoryResponse;
import com.fisbu.api.dto.ProductPricePointResponse;
import com.fisbu.api.entity.ReceiptItem;
import com.fisbu.api.entity.User;
import com.fisbu.api.repository.ReceiptItemRepository;
import com.fisbu.api.repository.UserRepository;

@Service
public class PersonalInflationService {

    private static final int TOP_LIST_SIZE = 5;

    private final ReceiptItemRepository receiptItemRepository;
    private final UserRepository userRepository;

    public PersonalInflationService(ReceiptItemRepository receiptItemRepository,
                                     UserRepository userRepository) {
        this.receiptItemRepository = receiptItemRepository;
        this.userRepository = userRepository;
    }

    public ProductPriceHistoryResponse getProductPriceHistory(String email, String normalizedName) {
        User user = getUserByEmail(email);

        List<ReceiptItem> items = receiptItemRepository
                .findByReceipt_User_IdAndNormalizedNameOrderByReceipt_ReceiptDateAsc(user.getId(), normalizedName);

        ProductPriceHistoryResponse response = new ProductPriceHistoryResponse();
        response.setNormalizedName(normalizedName);
        response.setDisplayName(items.isEmpty() ? normalizedName : items.get(items.size() - 1).getProductName());

        List<ProductPricePointResponse> points = new ArrayList<>();
        for (ReceiptItem item : items) {
            if (item.getReceipt().getReceiptDate() == null) {
                continue;
            }
            ProductPricePointResponse point = new ProductPricePointResponse();
            point.setDate(item.getReceipt().getReceiptDate());
            point.setUnitPrice(item.getUnitPrice());
            point.setStoreName(item.getReceipt().getStoreName());
            points.add(point);
        }
        response.setPoints(points);

        return response;
    }

    public PersonalInflationResponse getPersonalInflationSummary(String email, Integer months) {
        User user = getUserByEmail(email);
        int resolvedMonths = months != null && months > 0 ? months : 3;

        LocalDate windowStart = LocalDate.now().minusMonths(resolvedMonths);
        List<ReceiptItem> allItems = receiptItemRepository.findByReceipt_User_Id(user.getId());

        // Ürünleri normalized isme göre grupla, pencere dışındaki ve tarihsiz satırları ele
        Map<String, List<ReceiptItem>> grouped = new LinkedHashMap<>();
        for (ReceiptItem item : allItems) {
            LocalDate receiptDate = item.getReceipt().getReceiptDate();
            if (receiptDate == null || receiptDate.isBefore(windowStart)) {
                continue;
            }
            grouped.computeIfAbsent(item.getNormalizedName(), k -> new ArrayList<>()).add(item);
        }

        List<ProductInflationResponse> productChanges = new ArrayList<>();
        BigDecimal weightedChangeSum = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;

        for (Map.Entry<String, List<ReceiptItem>> entry : grouped.entrySet()) {
            List<ReceiptItem> productItems = entry.getValue();
            productItems.sort(Comparator.comparing(i -> i.getReceipt().getReceiptDate()));

            if (productItems.size() < 2) {
                continue;
            }

            BigDecimal firstPrice = productItems.get(0).getUnitPrice();
            BigDecimal lastPrice = productItems.get(productItems.size() - 1).getUnitPrice();
            if (firstPrice == null || lastPrice == null || firstPrice.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal changePercent = lastPrice.subtract(firstPrice)
                    .divide(firstPrice, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            BigDecimal weight = BigDecimal.ZERO;
            for (ReceiptItem item : productItems) {
                BigDecimal quantity = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ONE;
                weight = weight.add(item.getUnitPrice().multiply(quantity));
            }

            ProductInflationResponse productResponse = new ProductInflationResponse();
            productResponse.setNormalizedName(entry.getKey());
            productResponse.setDisplayName(productItems.get(productItems.size() - 1).getProductName());
            productResponse.setFirstPrice(firstPrice);
            productResponse.setLastPrice(lastPrice);
            productResponse.setChangePercent(changePercent.setScale(2, RoundingMode.HALF_UP));
            productChanges.add(productResponse);

            weightedChangeSum = weightedChangeSum.add(changePercent.multiply(weight));
            totalWeight = totalWeight.add(weight);
        }

        PersonalInflationResponse response = new PersonalInflationResponse();
        response.setMonths(resolvedMonths);
        response.setTrackedProductCount(productChanges.size());
        response.setPersonalInflationPercent(totalWeight.compareTo(BigDecimal.ZERO) > 0
                ? weightedChangeSum.divide(totalWeight, 2, RoundingMode.HALF_UP)
                : null);

        List<ProductInflationResponse> increasing = new ArrayList<>(productChanges);
        increasing.sort(Comparator.comparing(ProductInflationResponse::getChangePercent).reversed());
        response.setTopIncreasing(increasing.subList(0, Math.min(TOP_LIST_SIZE, increasing.size())));

        List<ProductInflationResponse> decreasing = new ArrayList<>(productChanges);
        decreasing.sort(Comparator.comparing(ProductInflationResponse::getChangePercent));
        response.setTopDecreasing(decreasing.subList(0, Math.min(TOP_LIST_SIZE, decreasing.size())));

        return response;
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Kullanıcı bulunamadı"));
    }
}
