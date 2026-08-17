package com.fisbu.api.budget.adapter.out.persistence;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.fisbu.api.budget.domain.Budget;

@Mapper(componentModel = "spring")
public interface BudgetPersistenceMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "categoryId", source = "category.id")
    Budget toDomain(com.fisbu.api.entity.Budget entity);
}
