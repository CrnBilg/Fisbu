package com.fisbu.api.receipt.adapter.out.persistence;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.fisbu.api.receipt.domain.Receipt;
import com.fisbu.api.receipt.domain.ReceiptItem;

@Mapper(componentModel = "spring")
public interface ReceiptPersistenceMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    Receipt toDomain(com.fisbu.api.entity.Receipt entity);

    ReceiptItem toDomain(com.fisbu.api.entity.ReceiptItem entity);
}
