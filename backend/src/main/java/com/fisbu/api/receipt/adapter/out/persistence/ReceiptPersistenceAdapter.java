package com.fisbu.api.receipt.adapter.out.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fisbu.api.entity.Category;
import com.fisbu.api.entity.User;
import com.fisbu.api.receipt.application.port.out.AverageAmountByCategoryPort;
import com.fisbu.api.receipt.application.port.out.CountReceiptsByCategoryPort;
import com.fisbu.api.receipt.application.port.out.DeleteReceiptPort;
import com.fisbu.api.receipt.application.port.out.DeleteReceiptsByUserPort;
import com.fisbu.api.receipt.application.port.out.FindDuplicateReceiptPort;
import com.fisbu.api.receipt.application.port.out.FindReceiptsByStoreNameContainingPort;
import com.fisbu.api.receipt.application.port.out.LoadAllReceiptsByUserPort;
import com.fisbu.api.receipt.application.port.out.LoadReceiptItemsByUserPort;
import com.fisbu.api.receipt.application.port.out.LoadReceiptPort;
import com.fisbu.api.receipt.application.port.out.LoadReceiptsByUserAndDateRangePort;
import com.fisbu.api.receipt.application.port.out.LoadReceiptsByUserIdsAndDateRangePort;
import com.fisbu.api.receipt.application.port.out.LoadReceiptsPort;
import com.fisbu.api.receipt.application.port.out.ReceiptPage;
import com.fisbu.api.receipt.application.port.out.ResolveUserIdPort;
import com.fisbu.api.receipt.application.port.out.SaveReceiptPort;
import com.fisbu.api.receipt.application.port.out.SearchReceiptsPort;
import com.fisbu.api.receipt.application.port.out.SumReceiptSpendByCategoryPort;
import com.fisbu.api.receipt.application.port.out.UnlinkCategoryFromReceiptsPort;
import com.fisbu.api.receipt.domain.Receipt;
import com.fisbu.api.receipt.domain.ReceiptItem;
import com.fisbu.api.repository.CategoryRepository;
import com.fisbu.api.repository.ReceiptItemRepository;
import com.fisbu.api.repository.ReceiptRepository;
import com.fisbu.api.repository.ReceiptSpecifications;
import com.fisbu.api.repository.UserRepository;

@Component
public class ReceiptPersistenceAdapter implements LoadReceiptPort, LoadReceiptsPort, SearchReceiptsPort,
        FindReceiptsByStoreNameContainingPort, FindDuplicateReceiptPort, SaveReceiptPort, DeleteReceiptPort,
        CountReceiptsByCategoryPort, AverageAmountByCategoryPort, ResolveUserIdPort, SumReceiptSpendByCategoryPort,
        UnlinkCategoryFromReceiptsPort, DeleteReceiptsByUserPort, LoadReceiptsByUserAndDateRangePort,
        LoadReceiptsByUserIdsAndDateRangePort, LoadAllReceiptsByUserPort, LoadReceiptItemsByUserPort {

    private final ReceiptRepository receiptRepository;
    private final ReceiptItemRepository receiptItemRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ReceiptPersistenceMapper mapper;

    public ReceiptPersistenceAdapter(ReceiptRepository receiptRepository, ReceiptItemRepository receiptItemRepository,
                                      UserRepository userRepository, CategoryRepository categoryRepository,
                                      ReceiptPersistenceMapper mapper) {
        this.receiptRepository = receiptRepository;
        this.receiptItemRepository = receiptItemRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Receipt> loadById(Long receiptId) {
        return receiptRepository.findById(receiptId).map(mapper::toDomain);
    }

    @Override
    public List<Receipt> loadByUserId(Long userId, int limit) {
        User user = requireUser(userId);
        Pageable pageable = PageRequest.of(0, limit,
                Sort.by(Sort.Direction.DESC, "receiptDate").and(Sort.by(Sort.Direction.DESC, "id")));
        return receiptRepository.findByUser(user, pageable).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public ReceiptPage search(Long userId, String query, Long categoryId, boolean uncategorized, int page, int size) {
        User user = requireUser(userId);
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "receiptDate").and(Sort.by(Sort.Direction.DESC, "id")));

        Specification<com.fisbu.api.entity.Receipt> spec = ReceiptSpecifications.hasUser(user);
        if (query != null && !query.isBlank()) {
            spec = spec.and(ReceiptSpecifications.storeNameContains(query.trim()));
        }
        if (uncategorized) {
            spec = spec.and(ReceiptSpecifications.isUncategorized());
        } else if (categoryId != null) {
            spec = spec.and(ReceiptSpecifications.hasCategoryId(categoryId));
        }

        Page<com.fisbu.api.entity.Receipt> result = receiptRepository.findAll(spec, pageable);
        List<Receipt> content = result.getContent().stream().map(mapper::toDomain).collect(Collectors.toList());
        return new ReceiptPage(content, result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages(), result.hasNext());
    }

    // ARCH-006: kategori önerisi (suggestCategory) için TÜM eşleşen fişleri değil, en son
    // MAX_CATEGORY_SUGGESTION_SAMPLE kadarını örnekliyoruz — aynı mağazadan yüzlerce/binlerce
    // kez alışveriş yapan bir kullanıcı için "en sık kullanılan kategoriyi öner" amacına en
    // güncel N fiş yeterli, tüm geçmişi belleğe çekmek gerekmez. Davranış pratikte AYNI kalır
    // (çoğu kullanıcı bir mağazadan MAX_CATEGORY_SUGGESTION_SAMPLE'dan az fiş biriktirir).
    private static final int MAX_CATEGORY_SUGGESTION_SAMPLE = 50;

    @Override
    public List<Receipt> findByUserIdAndStoreNameContaining(Long userId, String storeName) {
        User user = requireUser(userId);
        Specification<com.fisbu.api.entity.Receipt> spec = ReceiptSpecifications.hasUser(user)
                .and(ReceiptSpecifications.storeNameContains(storeName));
        Pageable pageable = PageRequest.of(0, MAX_CATEGORY_SUGGESTION_SAMPLE,
                Sort.by(Sort.Direction.DESC, "receiptDate").and(Sort.by(Sort.Direction.DESC, "id")));
        return receiptRepository.findAll(spec, pageable).stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Receipt> findDuplicates(Long userId, String storeName, BigDecimal totalAmount, LocalDate receiptDate) {
        User user = requireUser(userId);
        return receiptRepository
                .findByUserAndStoreNameIgnoreCaseAndTotalAmountAndReceiptDate(user, storeName, totalAmount, receiptDate)
                .stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public Receipt save(Receipt receipt) {
        if (receipt.id() == null) {
            return saveNew(receipt);
        }
        return saveExistingScalarFields(receipt);
    }

    private Receipt saveNew(Receipt receipt) {
        com.fisbu.api.entity.Receipt entity = new com.fisbu.api.entity.Receipt();
        entity.setUser(requireUser(receipt.userId()));
        applyScalarFields(entity, receipt);

        if (receipt.items() != null) {
            for (ReceiptItem item : receipt.items()) {
                com.fisbu.api.entity.ReceiptItem itemEntity = new com.fisbu.api.entity.ReceiptItem();
                itemEntity.setReceipt(entity);
                itemEntity.setProductName(item.productName());
                itemEntity.setNormalizedName(item.normalizedName());
                itemEntity.setUnitPrice(item.unitPrice());
                if (item.quantity() != null) {
                    itemEntity.setQuantity(item.quantity());
                }
                entity.getItems().add(itemEntity);
            }
        }

        return mapper.toDomain(receiptRepository.save(entity));
    }

    // Güncellemede items koleksiyonuna hiç dokunulmuyor — orphanRemoval=true olduğu için
    // koleksiyonu domain'den yeniden inşa etmeye çalışmak yanlışlıkla item silinmesine yol açar.
    // setReminders/saveSplit zaten sadece skaler alanları değiştiriyor, items aynen korunmalı
    // (orijinal legacy ReceiptService de bu iki akışta items'a hiç dokunmuyordu).
    private Receipt saveExistingScalarFields(Receipt receipt) {
        com.fisbu.api.entity.Receipt entity = receiptRepository.findById(receipt.id()).orElseThrow();
        applyScalarFields(entity, receipt);
        return mapper.toDomain(receiptRepository.save(entity));
    }

    private void applyScalarFields(com.fisbu.api.entity.Receipt entity, Receipt receipt) {
        entity.setCategory(receipt.categoryId() != null ? requireCategory(receipt.categoryId()) : null);
        entity.setStoreName(receipt.storeName());
        entity.setTotalAmount(receipt.totalAmount());
        entity.setReceiptDate(receipt.receiptDate());
        entity.setImageUrl(receipt.imageUrl());
        entity.setRawOcrText(receipt.rawOcrText());
        entity.setSplitDetailsJson(receipt.splitDetailsJson());
        entity.setReturnDeadline(receipt.returnDeadline());
        entity.setWarrantyExpiryDate(receipt.warrantyExpiryDate());
        entity.setReturnReminderSent(Boolean.TRUE.equals(receipt.returnReminderSent()));
        entity.setWarrantyReminderSent(Boolean.TRUE.equals(receipt.warrantyReminderSent()));
    }

    @Override
    public void deleteById(Long receiptId) {
        receiptRepository.deleteById(receiptId);
    }

    @Override
    public long countByCategoryId(Long categoryId) {
        return receiptRepository.countByCategoryAndTotalAmountIsNotNull(requireCategory(categoryId));
    }

    @Override
    public BigDecimal averageByCategoryId(Long categoryId) {
        return receiptRepository.avgTotalAmountByCategory(requireCategory(categoryId));
    }

    @Override
    public Optional<Long> resolveUserIdByEmail(String email) {
        return userRepository.findByEmail(email).map(User::getId);
    }

    @Override
    public BigDecimal sumSpend(Long userId, Long categoryId, int year, int month) {
        User user = requireUser(userId);
        Category category = requireCategory(categoryId);

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        return receiptRepository.sumTotalAmountByUserAndCategoryAndReceiptDateBetween(user, category, start, end);
    }

    // @Modifying bulk UPDATE, çağıranın (CategoryService.deleteCategory) kendisi @Transactional
    // olmadığı için burada AÇIKÇA @Transactional gerekiyor — aksi halde Hibernate
    // "Executing an update/delete query" hatası fırlatır (aktif transaction yok).
    @Override
    @Transactional
    public void unlinkCategoryFromReceipts(Long categoryId) {
        Category category = requireCategory(categoryId);
        receiptRepository.unlinkCategoryFromAllReceipts(category);
    }

    @Override
    public void deleteAllByUserId(Long userId) {
        User user = requireUser(userId);
        receiptRepository.deleteAll(receiptRepository.findByUser(user));
    }

    @Override
    public List<Receipt> loadByUserIdAndDateRange(Long userId, LocalDate start, LocalDate end) {
        User user = requireUser(userId);
        return receiptRepository.findByUserAndReceiptDateBetween(user, start, end).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Receipt> loadByUserIdsAndDateRange(List<Long> userIds, LocalDate start, LocalDate end) {
        List<User> users = userRepository.findAllById(userIds);
        return receiptRepository.findByUserInAndReceiptDateBetween(users, start, end).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Receipt> loadAllByUserId(Long userId) {
        User user = requireUser(userId);
        return receiptRepository.findByUser(user).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReceiptItem> loadByUserId(Long userId) {
        return receiptItemRepository.findByReceipt_User_Id(userId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId).orElseThrow();
    }

    private Category requireCategory(Long categoryId) {
        return categoryRepository.findById(categoryId).orElseThrow();
    }
}
