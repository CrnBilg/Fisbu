package com.fisbu.api.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fisbu.api.dto.ChangePasswordRequest;
import com.fisbu.api.dto.LoginRequest;
import com.fisbu.api.dto.RegisterRequest;
import com.fisbu.api.dto.ResetPasswordRequest;
import com.fisbu.api.dto.VerifyEmailRequest;
import com.fisbu.api.entity.Category;
import com.fisbu.api.entity.User;
import com.fisbu.api.repository.CategoryRepository;
import com.fisbu.api.repository.ReceiptRepository;
import com.fisbu.api.repository.UserRepository;

@Service // AuthService, kullanıcı kayıt işlemlerini yönetir
public class AuthService {

    private static final int CODE_VALIDITY_MINUTES = 15;
    private static final SecureRandom RANDOM = new SecureRandom();

   private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CategoryRepository categoryRepository;
    private final ReceiptRepository receiptRepository;
    private final EmailService emailService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, CategoryRepository categoryRepository,
                       ReceiptRepository receiptRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.categoryRepository = categoryRepository;
        this.receiptRepository = receiptRepository;
        this.emailService = emailService;
    }

   public User register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bu email zaten kayıtlı");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmailVerified(false);
        user.setVerificationCode(generateCode());
        user.setVerificationCodeExpiry(LocalDateTime.now().plusMinutes(CODE_VALIDITY_MINUTES));
        User savedUser = userRepository.save(user);

        createDefaultCategories(savedUser);
        emailService.sendVerificationCode(savedUser.getEmail(), savedUser.getVerificationCode());

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

        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "E-postanı doğrulaman gerekiyor");
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

    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kullanıcı bulunamadı"));

        String code = generateCode();
        user.setResetPasswordCode(code);
        user.setResetPasswordCodeExpiry(LocalDateTime.now().plusMinutes(CODE_VALIDITY_MINUTES));
        userRepository.save(user);

        emailService.sendPasswordResetCode(user.getEmail(), code);
    }

    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kullanıcı bulunamadı"));

        if (user.getResetPasswordCode() == null
                || !user.getResetPasswordCode().equals(request.getCode())
                || user.getResetPasswordCodeExpiry() == null
                || user.getResetPasswordCodeExpiry().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kod geçersiz veya süresi dolmuş");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetPasswordCode(null);
        user.setResetPasswordCodeExpiry(null);
        userRepository.save(user);
    }

    public void verifyEmail(VerifyEmailRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kullanıcı bulunamadı"));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            return;
        }

        if (user.getVerificationCode() == null
                || !user.getVerificationCode().equals(request.getCode())
                || user.getVerificationCodeExpiry() == null
                || user.getVerificationCodeExpiry().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kod geçersiz veya süresi dolmuş");
        }

        user.setEmailVerified(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiry(null);
        userRepository.save(user);
    }

    public void resendVerificationCode(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kullanıcı bulunamadı"));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-posta zaten doğrulanmış");
        }

        String code = generateCode();
        user.setVerificationCode(code);
        user.setVerificationCodeExpiry(LocalDateTime.now().plusMinutes(CODE_VALIDITY_MINUTES));
        userRepository.save(user);

        emailService.sendVerificationCode(user.getEmail(), code);
    }

    @Transactional
    public void deleteAccount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kullanıcı bulunamadı"));

        receiptRepository.deleteAll(receiptRepository.findByUser(user));
        categoryRepository.deleteAll(categoryRepository.findByUser(user));
        userRepository.delete(user);
    }

    private static String generateCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }
}
