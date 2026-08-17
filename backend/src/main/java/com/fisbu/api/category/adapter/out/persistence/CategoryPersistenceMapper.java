package com.fisbu.api.category.adapter.out.persistence;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.fisbu.api.category.domain.Category;

@Mapper(componentModel = "spring")
public interface CategoryPersistenceMapper {

    @Mapping(target = "userId", source = "user.id")
    Category toDomain(com.fisbu.api.entity.Category entity);
}
