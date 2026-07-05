package com.fisbu.api.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fisbu.api.dto.ChangePasswordRequest;
import com.fisbu.api.dto.LoginRequest;
import com.fisbu.api.dto.RegisterRequest;
import com.fisbu.api.entity.Category;
import com.fisbu.api.entity.User;
import com.fisbu.api.repository.CategoryRepository;
import com.fisbu.api.repository.UserRepository;

@Service // AuthService, kullanıcı kayıt işlemlerini yönetir
public class AuthService {

   private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CategoryRepository categoryRepository;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, CategoryRepository categoryRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.categoryRepository = categoryRepository;
    }

   public User register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bu email zaten kayıtlı");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        User savedUser = userRepository.save(user);

        createDefaultCategories(savedUser);

        return savedUser;
    }

    // Yeni kullanıcı için varsayılan kategorileri oluşturur
    private void createDefaultCategories(User user) {
        List<Category> defaults = List.of(
                buildCategory(user, "Market", "#4CAF50"),
                buildCategory(user, "Giyim", "#E91E63"),
                buildCategory(user, "Elektronik", "#2196F3"),
                buildCategory(user, "Restoran", "#FF9800"),
                buildCategory(user, "Ulaşım", "#9C27B0"),
                buildCategory(user, "Diğer", "#607D8B")
        );
        categoryRepository.saveAll(defaults);
    }

    private Category buildCategory(User user, String name, String color) {
        Category category = new Category();
        category.setUser(user);
        category.setName(name);
        category.setColor(color);
        return category;
    }

    public String login(LoginRequest request) {
        //findByEmail() -> Email ile kullanıcıyı arar
        //Bulamaz ise 401 hatası
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email veya şifre hatalı"));
        // Parola doğrulaması yapar, eşleşmezse 401 hatası
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email veya şifre hatalı");
        }

        return jwtService.generateToken(user.getEmail());
    }

    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kullanıcı bulunamadı"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Mevcut şifre hatalı");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}