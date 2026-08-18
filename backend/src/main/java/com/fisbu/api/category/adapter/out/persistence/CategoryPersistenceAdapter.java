package com.fisbu.api.category.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.fisbu.api.category.application.port.out.DeleteBudgetsByCategoryPort;
import com.fisbu.api.category.application.port.out.DeleteCategoryPort;
import com.fisbu.api.category.application.port.out.FindCategoryByNamePort;
import com.fisbu.api.category.application.port.out.LoadCategoriesPort;
import com.fisbu.api.category.application.port.out.LoadCategoryPort;
import com.fisbu.api.category.application.port.out.ResolveUserIdPort;
import com.fisbu.api.category.application.port.out.SaveCategoryPort;
import com.fisbu.api.category.domain.Category;
import com.fisbu.api.entity.User;
import com.fisbu.api.repository.BudgetRepository;
import com.fisbu.api.repository.CategoryRepository;
import com.fisbu.api.repository.UserRepository;

// Category modülünün çıkış adaptörü. Budget henüz kendi hexagonal modülüne taşınmadığı için,
// bu adaptör geçici olarak onun legacy repository'sini de kullanır (bkz. DeleteBudgetsByCategoryPort
// javadoc'u — Budget migrate olduğunda buradaki doğrudan erişim, onun kendi out-port'uyla
// değiştirilecek). UnlinkReceiptsFromCategoryPort artık burada değil, CategoryReceiptAdapter'da
// (Receipt modülü migrate oldu, bkz. adapter/out/receipt).
@Component
public class CategoryPersistenceAdapter implements ResolveUserIdPort, LoadCategoriesPort, LoadCategoryPort,
        FindCategoryByNamePort, SaveCategoryPort, DeleteCategoryPort, DeleteBudgetsByCategoryPort {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final BudgetRepository budgetRepository;
    private final CategoryPersistenceMapper mapper;

    public CategoryPersistenceAdapter(CategoryRepository categoryRepository, UserRepository userRepository,
                                       BudgetRepository budgetRepository, CategoryPersistenceMapper mapper) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.budgetRepository = budgetRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Long> resolveUserIdByEmail(String email) {
        return userRepository.findByEmail(email).map(User::getId);
    }

    @Override
    public List<Category> loadByUserId(Long userId) {
        User user = requireUser(userId);
        return categoryRepository.findByUser(user).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Category> loadById(Long categoryId) {
        return categoryRepository.findById(categoryId).map(mapper::toDomain);
    }

    @Override
    public Optional<Category> findByUserIdAndNameIgnoreCase(Long userId, String name) {
        User user = requireUser(userId);
        return categoryRepository.findByUserAndNameIgnoreCase(user, name).map(mapper::toDomain);
    }

    @Override
    public Category save(Category category) {
        com.fisbu.api.entity.Category entity = category.id() != null
                ? categoryRepository.findById(category.id()).orElseGet(com.fisbu.api.entity.Category::new)
                : new com.fisbu.api.entity.Category();

        entity.setUser(requireUser(category.userId()));
        entity.setName(category.name());
        entity.setColor(category.color());

        return mapper.toDomain(categoryRepository.save(entity));
    }

    @Override
    public void deleteById(Long categoryId) {
        categoryRepository.deleteById(categoryId);
    }

    @Override
    public void deleteBudgetsByCategory(Long categoryId) {
        com.fisbu.api.entity.Category category = requireCategoryEntity(categoryId);
        budgetRepository.deleteAll(budgetRepository.findByCategory(category));
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId).orElseThrow();
    }

    private com.fisbu.api.entity.Category requireCategoryEntity(Long categoryId) {
        return categoryRepository.findById(categoryId).orElseThrow();
    }
}
