package com.fisbu.api.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fisbu.api.dto.CategoryRequest;
import com.fisbu.api.dto.CategoryResponse;
import com.fisbu.api.entity.Category;
import com.fisbu.api.entity.Receipt;
import com.fisbu.api.entity.User;
import com.fisbu.api.repository.CategoryRepository;
import com.fisbu.api.repository.ReceiptRepository;
import com.fisbu.api.repository.UserRepository;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ReceiptRepository receiptRepository;

    public CategoryService(CategoryRepository categoryRepository, UserRepository userRepository,
                            ReceiptRepository receiptRepository) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.receiptRepository = receiptRepository;
    }

    public List<CategoryResponse> getCategories(String email) {
        User user = getUserByEmail(email);
        return categoryRepository.findByUser(user)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public CategoryResponse createCategory(String email, CategoryRequest request) {
        User user = getUserByEmail(email);

        Category category = new Category();
        category.setUser(user);
        category.setName(request.getName());
        category.setColor(request.getColor());

        return toResponse(categoryRepository.save(category));
    }

    public CategoryResponse updateCategory(String email, Long categoryId, CategoryRequest request) {
        User user = getUserByEmail(email);
        Category category = getOwnedCategory(user, categoryId);

        category.setName(request.getName());
        category.setColor(request.getColor());

        return toResponse(categoryRepository.save(category));
    }

    public void deleteCategory(String email, Long categoryId) {
        User user = getUserByEmail(email);
        Category category = getOwnedCategory(user, categoryId);

        // Bu kategoriyi kullanan fişleri kategorisiz bırak, FK hatasını önle
        List<Receipt> receipts = receiptRepository.findByCategory(category);
        receipts.forEach(receipt -> receipt.setCategory(null));
        receiptRepository.saveAll(receipts);

        categoryRepository.delete(category);
    }

    private Category getOwnedCategory(User user, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Kategori bulunamadı"));

        if (!category.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bu kategoriye erişim yetkiniz yok");
        }

        return category;
    }

    private CategoryResponse toResponse(Category category) {
        CategoryResponse response = new CategoryResponse();
        response.setId(category.getId());
        response.setName(category.getName());
        response.setColor(category.getColor());
        return response;
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Kullanıcı bulunamadı"));
    }
}