package com.fisbu.api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fisbu.api.entity.Category;
import com.fisbu.api.entity.User;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByUser(User user);
}