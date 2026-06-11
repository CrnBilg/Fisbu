package com.fisbu.api.repository;

import com.fisbu.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

// Spring ile interface otomatik dolduruyoruz.

public interface UserRepository extends JpaRepository<User, Long> {


 // Spring save() , findById() , findAll() , delete() gibi JpaRepository ile otomatik oluşturur.

    Optional<User> findByEmail(String email);// email'e göre kullanıcı bul.
}