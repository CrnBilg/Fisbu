package com.fisbu.api.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fisbu.api.dto.RegisterRequest;
import com.fisbu.api.entity.User;
import com.fisbu.api.repository.UserRepository;

@Service // AuthService, kullanıcı kayıt işlemlerini yönetir
public class AuthService {

    private final UserRepository userRepository; // Kullanıcı veritabanı işlemleri için UserRepository'yi kullanır
    private final PasswordEncoder passwordEncoder; // Parola güvenliği için PasswordEncoder'ı kullanır

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) { // Veritabanında aynı email adresine sahip bir kullanıcı var mı kontrol eder
throw new ResponseStatusException(HttpStatus.CONFLICT, "Bu email zaten kayıtlı");        }

        User user = new User(); // Yeni bir User nesnesi oluşturur
        user.setEmail(request.getEmail()); // Kullanıcının email adresini ayarlar
        user.setPassword(passwordEncoder.encode(request.getPassword())); // Kullanıcının parolasını güvenli bir şekilde şifreler ve ayarlar

        return userRepository.save(user);// Yeni kullanıcıyı veritabanına kaydeder ve kaydedilen kullanıcıyı döner
    }
}