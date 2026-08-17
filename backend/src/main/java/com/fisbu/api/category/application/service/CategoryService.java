package com.fisbu.api.category.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.fisbu.api.category.application.port.in.CreateCategoryUseCase;
import com.fisbu.api.category.application.port.in.DeleteCategoryUseCase;
import com.fisbu.api.category.application.port.in.GetCategoriesUseCase;
import com.fisbu.api.category.application.port.in.UpdateCategoryUseCase;
import com.fisbu.api.category.application.port.out.DeleteBudgetsByCategoryPort;
import com.fisbu.api.category.application.port.out.DeleteCategoryPort;
import com.fisbu.api.category.application.port.out.FindCategoryByNamePort;
import com.fisbu.api.category.application.port.out.LoadCategoriesPort;
import com.fisbu.api.category.application.port.out.LoadCategoryPort;
import com.fisbu.api.category.application.port.out.ResolveUserIdPort;
import com.fisbu.api.category.application.port.out.SaveCategoryPort;
import com.fisbu.api.category.application.port.out.UnlinkReceiptsFromCategoryPort;
import com.fisbu.api.category.domain.Category;
import com.fisbu.api.category.domain.exception.CategoryAccessDeniedException;
import com.fisbu.api.category.domain.exception.CategoryNameAlreadyExistsException;
import com.fisbu.api.category.domain.exception.CategoryNotFoundException;
import com.fisbu.api.category.domain.exception.UserNotFoundException;

@Service
public class CategoryService implements GetCategoriesUseCase, CreateCategoryUseCase, UpdateCategoryUseCase, DeleteCategoryUseCase {

    private final ResolveUserIdPort resolveUserIdPort;
    private final LoadCategoriesPort loadCategoriesPort;
    private final LoadCategoryPort loadCategoryPort;
    private final FindCategoryByNamePort findCategoryByNamePort;
    private final SaveCategoryPort saveCategoryPort;
    private final DeleteCategoryPort deleteCategoryPort;
    private final DeleteBudgetsByCategoryPort deleteBudgetsByCategoryPort;
    private final UnlinkReceiptsFromCategoryPort unlinkReceiptsFromCategoryPort;

    public CategoryService(ResolveUserIdPort resolveUserIdPort, LoadCategoriesPort loadCategoriesPort,
                            LoadCategoryPort loadCategoryPort, FindCategoryByNamePort findCategoryByNamePort,
                            SaveCategoryPort saveCategoryPort, DeleteCategoryPort deleteCategoryPort,
                            DeleteBudgetsByCategoryPort deleteBudgetsByCategoryPort,
                            UnlinkReceiptsFromCategoryPort unlinkReceiptsFromCategoryPort) {
        this.resolveUserIdPort = resolveUserIdPort;
        this.loadCategoriesPort = loadCategoriesPort;
        this.loadCategoryPort = loadCategoryPort;
        this.findCategoryByNamePort = findCategoryByNamePort;
        this.saveCategoryPort = saveCategoryPort;
        this.deleteCategoryPort = deleteCategoryPort;
        this.deleteBudgetsByCategoryPort = deleteBudgetsByCategoryPort;
        this.unlinkReceiptsFromCategoryPort = unlinkReceiptsFromCategoryPort;
    }

    @Override
    public List<Category> getCategories(String email) {
        Long userId = resolveUserId(email);
        return loadCategoriesPort.loadByUserId(userId);
    }

    @Override
    public Category createCategory(CreateCategoryCommand command) {
        Long userId = resolveUserId(command.email());
        ensureNameNotTaken(userId, command.name(), null);

        Category category = new Category(null, userId, command.name(), command.color());
        return saveCategoryPort.save(category);
    }

    @Override
    public Category updateCategory(UpdateCategoryCommand command) {
        Long userId = resolveUserId(command.email());
        Category existing = getOwnedCategory(userId, command.categoryId());
        ensureNameNotTaken(userId, command.name(), command.categoryId());

        Category updated = new Category(existing.id(), userId, command.name(), command.color());
        return saveCategoryPort.save(updated);
    }

    @Override
    public void deleteCategory(DeleteCategoryCommand command) {
        Long userId = resolveUserId(command.email());
        Category category = getOwnedCategory(userId, command.categoryId());

        // Budget, category'ye NOT NULL FK ile bağlı — önce silinmeli, yoksa kategori silme FK hatası verir
        deleteBudgetsByCategoryPort.deleteBudgetsByCategory(category.id());

        // Bu kategoriyi kullanan fişleri kategorisiz bırak, FK hatasını önle
        unlinkReceiptsFromCategoryPort.unlinkReceiptsFromCategory(category.id());

        deleteCategoryPort.deleteById(category.id());
    }

    // Aynı kullanıcı için aynı isimde başka bir kategori var mı kontrol eder (case-insensitive).
    // DB'deki unique constraint'in son çare olduğu durumlarda da (ör. race condition) ham
    // DataIntegrityViolationException/500 yerine düzgün bir 409 dönmesini sağlar.
    private void ensureNameNotTaken(Long userId, String name, Long excludingCategoryId) {
        findCategoryByNamePort.findByUserIdAndNameIgnoreCase(userId, name).ifPresent(existing -> {
            if (!existing.id().equals(excludingCategoryId)) {
                throw new CategoryNameAlreadyExistsException();
            }
        });
    }

    private Category getOwnedCategory(Long userId, Long categoryId) {
        Category category = loadCategoryPort.loadById(categoryId)
                .orElseThrow(CategoryNotFoundException::new);

        if (!category.userId().equals(userId)) {
            throw new CategoryAccessDeniedException();
        }

        return category;
    }

    private Long resolveUserId(String email) {
        return resolveUserIdPort.resolveUserIdByEmail(email)
                .orElseThrow(UserNotFoundException::new);
    }
}
