package com.fisbu.api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fisbu.api.dto.BadgeResponse;
import com.fisbu.api.dto.CategoryTotalResponse;
import com.fisbu.api.dto.MonthlyStatisticsResponse;
import com.fisbu.api.dto.SpendingPersonaResponse;
import com.fisbu.api.dto.SpendingPersonalityResponse;
import com.fisbu.api.dto.StoreStatResponse;
import com.fisbu.api.dto.SubscriptionCandidateResponse;
import com.fisbu.api.dto.TopProductResponse;
import com.fisbu.api.entity.Category;
import com.fisbu.api.entity.Receipt;
import com.fisbu.api.entity.ReceiptItem;
import com.fisbu.api.entity.SavingsGoal;
import com.fisbu.api.entity.User;
import com.fisbu.api.repository.ReceiptItemRepository;
import com.fisbu.api.repository.ReceiptRepository;
import com.fisbu.api.repository.SavingsGoalRepository;
import com.fisbu.api.repository.UserRepository;

@Service
public class StatisticsService {

    private static final int DEFAULT_TOP_PRODUCT_LIMIT = 10;

    // Abonelik tespiti eşikleri: aylık faturalandırma genelde 28-31 gün ama gecikme/erken
    // ödeme payı bırakmak için biraz geniş tutuldu
    private static final int MIN_SUBSCRIPTION_INTERVAL_DAYS = 20;
    private static final int MAX_SUBSCRIPTION_INTERVAL_DAYS = 40;
    private static final int MAX_SUBSCRIPTION_INTERVAL_SPREAD_DAYS = 15;
    private static final BigDecimal MAX_SUBSCRIPTION_AMOUNT_VARIANCE = BigDecimal.valueOf(0.15);

    private final ReceiptRepository receiptRepository;
    private final ReceiptItemRepository receiptItemRepository;
    private final UserRepository userRepository;
    private final SavingsGoalRepository savingsGoalRepository;

    public StatisticsService(ReceiptRepository receiptRepository,
                              ReceiptItemRepository receiptItemRepository,
                              UserRepository userRepository,
                              SavingsGoalRepository savingsGoalRepository) {
        this.receiptRepository = receiptRepository;
        this.receiptItemRepository = receiptItemRepository;
        this.userRepository = userRepository;
        this.savingsGoalRepository = savingsGoalRepository;
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

    // Aynı mağazaya ~aylık aralıklarla benzer tutarda ödeme yapılıyorsa "abonelik olabilir" diye işaretler
    public List<SubscriptionCandidateResponse> getPotentialSubscriptions(String email) {
        User user = getUserByEmail(email);
        List<Receipt> receipts = receiptRepository.findByUser(user);

        Map<String, List<Receipt>> byStoreKey = new LinkedHashMap<>();
        for (Receipt receipt : receipts) {
            if (receipt.getStoreName() == null || receipt.getReceiptDate() == null || receipt.getTotalAmount() == null) {
                continue;
            }
            String key = receipt.getStoreName().trim().toLowerCase(Locale.ROOT);
            byStoreKey.computeIfAbsent(key, k -> new ArrayList<>()).add(receipt);
        }

        List<SubscriptionCandidateResponse> candidates = new ArrayList<>();
        for (List<Receipt> group : byStoreKey.values()) {
            if (group.size() < 2) {
                continue;
            }
            group.sort(Comparator.comparing(Receipt::getReceiptDate));

            List<Long> gaps = new ArrayList<>();
            for (int i = 1; i < group.size(); i++) {
                gaps.add(ChronoUnit.DAYS.between(group.get(i - 1).getReceiptDate(), group.get(i).getReceiptDate()));
            }
            double averageGap = gaps.stream().mapToLong(Long::longValue).average().orElse(0);
            long minGap = gaps.stream().mapToLong(Long::longValue).min().orElse(0);
            long maxGap = gaps.stream().mapToLong(Long::longValue).max().orElse(0);

            if (averageGap < MIN_SUBSCRIPTION_INTERVAL_DAYS || averageGap > MAX_SUBSCRIPTION_INTERVAL_DAYS) {
                continue;
            }
            if ((maxGap - minGap) > MAX_SUBSCRIPTION_INTERVAL_SPREAD_DAYS) {
                continue;
            }

            BigDecimal total = BigDecimal.ZERO;
            for (Receipt receipt : group) {
                total = total.add(receipt.getTotalAmount());
            }
            BigDecimal averageAmount = total.divide(BigDecimal.valueOf(group.size()), 2, RoundingMode.HALF_UP);
            if (averageAmount.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            boolean amountsConsistent = group.stream().allMatch(r -> {
                BigDecimal deviation = r.getTotalAmount().subtract(averageAmount).abs()
                        .divide(averageAmount, 4, RoundingMode.HALF_UP);
                return deviation.compareTo(MAX_SUBSCRIPTION_AMOUNT_VARIANCE) <= 0;
            });
            if (!amountsConsistent) {
                continue;
            }

            LocalDate firstDate = group.get(0).getReceiptDate();
            LocalDate lastDate = group.get(group.size() - 1).getReceiptDate();
            int roundedInterval = (int) Math.round(averageGap);

            SubscriptionCandidateResponse candidate = new SubscriptionCandidateResponse();
            candidate.setStoreName(group.get(0).getStoreName().trim());
            candidate.setAverageAmount(averageAmount);
            candidate.setOccurrenceCount(group.size());
            candidate.setFirstDate(firstDate);
            candidate.setLastDate(lastDate);
            candidate.setAverageIntervalDays(roundedInterval);
            candidate.setEstimatedNextDate(lastDate.plusDays(roundedInterval));
            candidates.add(candidate);
        }

        candidates.sort(Comparator.comparing(SubscriptionCandidateResponse::getLastDate).reversed());
        return candidates;
    }

    // Harcama kişiliği + rozetler — mevcut veriden hesaplanır, ayrıca saklanmaz
    public SpendingPersonalityResponse getSpendingPersonality(String email) {
        User user = getUserByEmail(email);
        List<Receipt> receipts = receiptRepository.findByUser(user);

        SpendingPersonaResponse persona = computePersona(receipts);
        List<BadgeResponse> badges = computeBadges(email, user, receipts);
        return new SpendingPersonalityResponse(persona, badges);
    }

    private SpendingPersonaResponse computePersona(List<Receipt> receipts) {
        if (receipts.size() < 5) {
            return new SpendingPersonaResponse("Yeni Başlayan",
                    "Kişiliğini belirlemek için birkaç fiş daha ekle");
        }

        BigDecimal totalSpend = BigDecimal.ZERO;
        Map<String, BigDecimal> categoryTotals = new LinkedHashMap<>();
        Map<String, Integer> storeCounts = new LinkedHashMap<>();
        int weekendCount = 0;

        for (Receipt r : receipts) {
            BigDecimal amount = r.getTotalAmount() != null ? r.getTotalAmount() : BigDecimal.ZERO;
            totalSpend = totalSpend.add(amount);

            if (r.getCategory() != null) {
                categoryTotals.merge(r.getCategory().getName(), amount, BigDecimal::add);
            }
            if (r.getStoreName() != null) {
                storeCounts.merge(r.getStoreName().trim(), 1, Integer::sum);
            }
            if (r.getReceiptDate() != null) {
                var day = r.getReceiptDate().getDayOfWeek();
                if (day == java.time.DayOfWeek.SATURDAY || day == java.time.DayOfWeek.SUNDAY) {
                    weekendCount++;
                }
            }
        }

        if (totalSpend.signum() > 0) {
            var topCategory = categoryTotals.entrySet().stream()
                    .max(Map.Entry.comparingByValue()).orElse(null);
            if (topCategory != null) {
                double share = topCategory.getValue().divide(totalSpend, 4, RoundingMode.HALF_UP).doubleValue();
                if (share > 0.5) {
                    return new SpendingPersonaResponse(topCategory.getKey() + " Aşığı",
                            "Harcamalarının %" + Math.round(share * 100) + "'i " + topCategory.getKey()
                                    + " kategorisinde");
                }
            }
        }

        var topStore = storeCounts.entrySet().stream().max(Map.Entry.comparingByValue()).orElse(null);
        if (topStore != null) {
            double share = (double) topStore.getValue() / receipts.size();
            if (share > 0.4 && topStore.getValue() >= 5) {
                return new SpendingPersonaResponse(topStore.getKey() + " Sadığı",
                        "Fişlerinin %" + Math.round(share * 100) + "'i " + topStore.getKey() + "'den");
            }
        }

        double weekendShare = (double) weekendCount / receipts.size();
        if (weekendShare >= 0.6) {
            return new SpendingPersonaResponse("Hafta Sonu Harcayıcısı",
                    "Fişlerinin çoğu hafta sonuna ait");
        }

        BigDecimal averageAmount = totalSpend.divide(BigDecimal.valueOf(receipts.size()), 2, RoundingMode.HALF_UP);
        if (averageAmount.compareTo(BigDecimal.valueOf(500)) > 0) {
            return new SpendingPersonaResponse("Büyük Alışverişçi",
                    "Ortalama fiş tutarın " + averageAmount + " TL — az ama öz alışveriş yapıyorsun");
        }
        if (receipts.size() >= 15 && averageAmount.compareTo(BigDecimal.valueOf(100)) < 0) {
            return new SpendingPersonaResponse("Sık Alışverişçi",
                    "Küçük tutarlarla sık sık alışveriş yapıyorsun");
        }

        return new SpendingPersonaResponse("Dengeli Harcayıcı",
                "Harcamaların kategoriler arasında dengeli dağılıyor");
    }

    private List<BadgeResponse> computeBadges(String email, User user, List<Receipt> receipts) {
        int receiptCount = receipts.size();
        long distinctCategories = receipts.stream()
                .map(Receipt::getCategory).filter(java.util.Objects::nonNull)
                .map(Category::getId).distinct().count();
        Map<String, Long> storeCounts = receipts.stream()
                .filter(r -> r.getStoreName() != null)
                .collect(Collectors.groupingBy(r -> r.getStoreName().trim(), Collectors.counting()));
        boolean hasLoyalStore = storeCounts.values().stream().anyMatch(c -> c >= 10);
        boolean hasSplitReceipt = receipts.stream().anyMatch(r -> r.getSplitDetailsJson() != null);
        boolean hasSubscription = !getPotentialSubscriptions(email).isEmpty();
        boolean hasAchievedSavingsGoal = savingsGoalRepository.findByUser(user).stream()
                .anyMatch(g -> g.getCurrentAmount().compareTo(g.getTargetAmount()) >= 0);

        List<BadgeResponse> badges = new ArrayList<>();
        badges.add(new BadgeResponse("first_receipt", "İlk Adım", "İlk fişini ekle", receiptCount >= 1));
        badges.add(new BadgeResponse("receipts_10", "10 Fiş Kulübü", "10 fiş ekle", receiptCount >= 10));
        badges.add(new BadgeResponse("receipts_50", "50 Fiş Kulübü", "50 fiş ekle", receiptCount >= 50));
        badges.add(new BadgeResponse("receipts_100", "100 Fiş Kulübü", "100 fiş ekle", receiptCount >= 100));
        badges.add(new BadgeResponse("category_explorer", "Kategori Kaşifi",
                "5 farklı kategoride fiş ekle", distinctCategories >= 5));
        badges.add(new BadgeResponse("loyal_customer", "Sadık Müşteri",
                "Aynı mağazadan 10 kez fiş ekle", hasLoyalStore));
        badges.add(new BadgeResponse("subscription_hunter", "Abone Avcısı",
                "Bir abonelik/tekrarlayan ödeme tespit edilsin", hasSubscription));
        badges.add(new BadgeResponse("savings_champion", "Tasarruf Şampiyonu",
                "Bir tasarruf hedefine ulaş", hasAchievedSavingsGoal));
        badges.add(new BadgeResponse("bill_splitter", "Bölüşen", "Bir fişi böl/paylaştır", hasSplitReceipt));
        return badges;
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Kullanıcı bulunamadı"));
    }
}
