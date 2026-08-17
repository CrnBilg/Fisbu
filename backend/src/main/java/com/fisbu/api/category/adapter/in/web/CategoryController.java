package com.fisbu.api.category.adapter.in.web;

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

import com.fisbu.api.category.application.port.in.CreateCategoryUseCase;
import com.fisbu.api.category.application.port.in.CreateCategoryUseCase.CreateCategoryCommand;
import com.fisbu.api.category.application.port.in.DeleteCategoryUseCase;
import com.fisbu.api.category.application.port.in.DeleteCategoryUseCase.DeleteCategoryCommand;
import com.fisbu.api.category.application.port.in.GetCategoriesUseCase;
import com.fisbu.api.category.application.port.in.UpdateCategoryUseCase;
import com.fisbu.api.category.application.port.in.UpdateCategoryUseCase.UpdateCategoryCommand;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/categories")
public class CategoryController {

    private final GetCategoriesUseCase getCategoriesUseCase;
    private final CreateCategoryUseCase createCategoryUseCase;
    private final UpdateCategoryUseCase updateCategoryUseCase;
    private final DeleteCategoryUseCase deleteCategoryUseCase;
    private final CategoryWebMapper mapper;

    public CategoryController(GetCategoriesUseCase getCategoriesUseCase, CreateCategoryUseCase createCategoryUseCase,
                               UpdateCategoryUseCase updateCategoryUseCase, DeleteCategoryUseCase deleteCategoryUseCase,
                               CategoryWebMapper mapper) {
        this.getCategoriesUseCase = getCategoriesUseCase;
        this.createCategoryUseCase = createCategoryUseCase;
        this.updateCategoryUseCase = updateCategoryUseCase;
        this.deleteCategoryUseCase = deleteCategoryUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    public List<CategoryResponse> getCategories(@AuthenticationPrincipal UserDetails userDetails) {
        return mapper.toResponseList(getCategoriesUseCase.getCategories(userDetails.getUsername()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse createCategory(@AuthenticationPrincipal UserDetails userDetails,
                                            @RequestBody @Valid CategoryRequest request) {
        var command = new CreateCategoryCommand(userDetails.getUsername(), request.getName(), request.getColor());
        return mapper.toResponse(createCategoryUseCase.createCategory(command));
    }

    @PutMapping("/{id}")
    public CategoryResponse updateCategory(@AuthenticationPrincipal UserDetails userDetails,
                                            @PathVariable Long id,
                                            @RequestBody @Valid CategoryRequest request) {
        var command = new UpdateCategoryCommand(userDetails.getUsername(), id, request.getName(), request.getColor());
        return mapper.toResponse(updateCategoryUseCase.updateCategory(command));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@AuthenticationPrincipal UserDetails userDetails,
                                @PathVariable Long id) {
        deleteCategoryUseCase.deleteCategory(new DeleteCategoryCommand(userDetails.getUsername(), id));
    }
}
