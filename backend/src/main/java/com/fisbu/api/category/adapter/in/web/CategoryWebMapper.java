package com.fisbu.api.category.adapter.in.web;

import java.util.List;

import org.mapstruct.Mapper;

import com.fisbu.api.category.domain.Category;

@Mapper(componentModel = "spring")
public interface CategoryWebMapper {

    CategoryResponse toResponse(Category category);

    List<CategoryResponse> toResponseList(List<Category> categories);
}
