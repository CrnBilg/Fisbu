package com.fisbu.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fisbu.api.dto.CategoryRequest;
import com.fisbu.api.dto.CategoryResponse;
import com.fisbu.api.service.CategoryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<CategoryResponse> getCategories(@AuthenticationPrincipal UserDetails userDetails) {
        return categoryService.getCategories(userDetails.getUsername());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse createCategory(@AuthenticationPrincipal UserDetails userDetails,
                                            @RequestBody @Valid CategoryRequest request) {
        return categoryService.createCategory(userDetails.getUsername(), request);
    }

    @PutMapping("/{id}")
    public CategoryResponse updateCategory(@AuthenticationPrincipal UserDetails userDetails,
                                            @PathVariable Long id,
                                            @RequestBody @Valid CategoryRequest request) {
        return categoryService.updateCategory(userDetails.getUsername(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@AuthenticationPrincipal UserDetails userDetails,
                                @PathVariable Long id) {
        categoryService.deleteCategory(userDetails.getUsername(), id);
    }
}