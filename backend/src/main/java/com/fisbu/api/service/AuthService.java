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
import com.fisbu.api.dto.ProfileResponse;
import com.fisbu.api.dto.UpdateProfileRequest;
import com.fisbu.api.dto.LoginRequest;
import com.fisbu.api.dto.RegisterRequest;
import com.fisbu.api.dto.RegisterResponse;
import com.fisbu.api.dto.ResetPasswordRequest;
import com.fisbu.api.dto.VerifyEmailRequest;
import com.fisbu.api.entity.Category;
import com.fisbu.api.entity.User;
import com.fisbu.api.repository.BudgetRepository;
import com.fisbu.api.repository.CategoryRepository;
import com.fisbu.api.repository.ReceiptRepository;
import com.fisbu.api.repository.UserRepository;

@Service // AuthService, kullanıcı kayıt işlemlerini yönetir
public class AuthService {

    private static final int CODE_VALIDITY_MINUTES = 15;
    private static final int MAX_CODE_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

   private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CategoryRepository categoryRepository;
    private final ReceiptRepository receiptRepository;
    private final BudgetRepository budgetRepository;
    private final EmailService emailService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, CategoryRepository categoryRepository,
                       ReceiptRepository receiptRepository, BudgetRepository budgetRepository,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.categoryRepository = categoryRepository;
        this.receiptRepository = receiptRepository;
        this.budgetRepository = budgetRepository;
        this.emailService = emailService;
    }

   public RegisterResponse register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bu email zaten kayıtlı");
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());
        user.setEmailVerified(false);
        user.setVerificationCode(generateCode());
        user.setVerificationCodeExpiry(LocalDateTime.now().plusMinutes(CODE_VALIDITY_MINUTES));
        User savedUser = userRepository.save(user);

        createDefaultCategories(savedUser);
        emailService.sendVerificationCode(savedUser.getEmail(), savedUser.getVerificationCode());

        return new RegisterResponse(savedUser.getId(), savedUser.getEmail(), savedUser.getName(),
                savedUser.getEmailVerified());
    }

    public ProfileResponse getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kullanıcı bulunamadı"));
        return new ProfileResponse(
                user.getEmail(),
                user.getName(),
                user.getProfileImageUrl(),
                user.getCreatedAt().toString()
        );
    }

    public ProfileResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kullanıcı bulunamadı"));
        if (request.getName() != null) user.setName(request.getName());
        if (request.getProfileImageUrl() != null) user.setProfileImageUrl(request.getProfileImageUrl());
        userRepository.save(user);
        return new ProfileResponse(
                user.getEmail(),
                user.getName(),
                user.getProfileImageUrl(),
                user.getCreatedAt().toString()
        );
    }

    // Yeni kullanıcı için varsayılan kategorileri oluşturur
    private void createDefaultCategories(User user) {
        List<Category> defaults = List.of(
                buildCategory(user, "Market", "#4CAF50"),
                buildCategory(user, "Giyim", "#E91E63"),
                buildCategory(user, "Elektronik", "#2196F3"),
                buildCategory(user, "Restoran", "#FF9800"),
                buildCategory(user, "Ulaşım", "#9C27B0"),
                buildCategory(user, "Sağlık", "#F44336"),
                buildCategory(user, "Kafe", "#795548"),
                buildCategory(user, "Eğlence", "#FF4081"),
                buildCategory(user, "Spor", "#00BCD4"),
                buildCategory(user, "Faturalar", "#FFC107"),
                buildCategory(user, "Eğitim", "#3F51B5"),
                buildCategory(user, "Kozmetik", "#E91E63"),
                buildCategory(user, "Kişisel Bakım", "#9C27B0"),
                buildCategory(user, "Ev & Dekorasyon", "#FF5722"),
                buildCategory(user, "Çocuk", "#8BC34A"),
                buildCategory(user, "Hediye", "#F06292"),
                buildCategory(user, "Seyahat", "#03A9F4"),
                buildCategory(user, "Akaryakıt", "#FF6F00"),
                buildCategory(user, "Sigorta", "#546E7A"),
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
        User user = userRepository.findByEmail(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email veya şifre hatalı"));
        // Parola doğrulaması yapar, eşleşmezse 401 hatası
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {

            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email veya şifre hatalı");
        }

        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "E-postanı doğrulaman gerekiyor");
        }

        return jwtService.generateToken(user.getEmail(), currentTokenVersion(user));
    }

    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kullanıcı bulunamadı"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Mevcut şifre hatalı");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Yeni şifre mevcut şifrenizle aynı olamaz");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        // Şifre değişince eski cihazlardaki/oturumlardaki token'lar anında geçersiz kılınır
        user.setTokenVersion(currentTokenVersion(user) + 1);
        userRepository.save(user);
    }

    // Kullanıcı bulunamasa bile aynı (başarılı) yanıt döner — e-posta enumeration'ı önler,
    // bir saldırganın hangi e-postaların kayıtlı olduğunu bu uçla anlamasını engeller
    public void forgotPassword(String email) {
        userRepository.findByEmail(normalizeEmail(email)).ifPresent(user -> {
            String code = generateCode();
            user.setResetPasswordCode(code);
            user.setResetPasswordCodeExpiry(LocalDateTime.now().plusMinutes(CODE_VALIDITY_MINUTES));
            user.setResetPasswordAttempts(0);
            userRepository.save(user);

            emailService.sendPasswordResetCode(user.getEmail(), code);
        });
    }

    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kullanıcı bulunamadı"));

        // Deneme limiti aşıldıysa kod süresi dolmamış olsa bile geçersiz sayılır (brute force koruması)
        if (user.getResetPasswordAttempts() != null && user.getResetPasswordAttempts() >= MAX_CODE_ATTEMPTS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Çok fazla hatalı deneme yapıldı, lütfen yeni bir kod isteyin");
        }

        if (user.getResetPasswordCode() == null
                || !user.getResetPasswordCode().equals(request.getCode())
                || user.getResetPasswordCodeExpiry() == null
                || user.getResetPasswordCodeExpiry().isBefore(LocalDateTime.now())) {
            user.setResetPasswordAttempts(
                    (user.getResetPasswordAttempts() != null ? user.getResetPasswordAttempts() : 0) + 1);
            userRepository.save(user);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kod geçersiz veya süresi dolmuş");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Yeni şifre mevcut şifrenizle aynı olamaz");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetPasswordCode(null);
        user.setResetPasswordCodeExpiry(null);
        user.setResetPasswordAttempts(0);
        // Şifre resetlenince eski token'lar (olası hesap ele geçirme senaryosunda saldırganınki dahil) geçersiz kılınır
        user.setTokenVersion(currentTokenVersion(user) + 1);
        userRepository.save(user);
    }

    public void verifyEmail(VerifyEmailRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kullanıcı bulunamadı"));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            return;
        }

        // Deneme limiti aşıldıysa kod süresi dolmamış olsa bile geçersiz sayılır (brute force koruması)
        if (user.getVerificationAttempts() != null && user.getVerificationAttempts() >= MAX_CODE_ATTEMPTS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Çok fazla hatalı deneme yapıldı, lütfen yeni bir kod isteyin");
        }

        if (user.getVerificationCode() == null
                || !user.getVerificationCode().equals(request.getCode())
                || user.getVerificationCodeExpiry() == null
                || user.getVerificationCodeExpiry().isBefore(LocalDateTime.now())) {
            user.setVerificationAttempts(
                    (user.getVerificationAttempts() != null ? user.getVerificationAttempts() : 0) + 1);
            userRepository.save(user);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kod geçersiz veya süresi dolmuş");
        }

        user.setEmailVerified(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiry(null);
        user.setVerificationAttempts(0);
        userRepository.save(user);
    }

    // Kullanıcı bulunamasa ya da zaten doğrulanmış olsa bile sessizce döner (e-posta enumeration
    // önlemi) — aksi halde "zaten doğrulanmış" hatası, kayıtlı olmayan bir e-postadan ayırt
    // edilerek hangi e-postaların kayıtlı olduğunu anlamak için kullanılabilirdi
    public void resendVerificationCode(String email) {
        userRepository.findByEmail(normalizeEmail(email)).ifPresent(user -> {
            if (Boolean.TRUE.equals(user.getEmailVerified())) {
                return;
            }

            String code = generateCode();
            user.setVerificationCode(code);
            user.setVerificationCodeExpiry(LocalDateTime.now().plusMinutes(CODE_VALIDITY_MINUTES));
            user.setVerificationAttempts(0);
            userRepository.save(user);

            emailService.sendVerificationCode(user.getEmail(), code);
        });
    }

    @Transactional
    public void deleteAccount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kullanıcı bulunamadı"));

        // Budget, category'ye NOT NULL FK ile bağlı — önce silinmeli, yoksa kategori/kullanıcı silme FK hatası verir
        budgetRepository.deleteAll(budgetRepository.findByUser(user));
        receiptRepository.deleteAll(receiptRepository.findByUser(user));
        categoryRepository.deleteAll(categoryRepository.findByUser(user));
        userRepository.delete(user);
    }

    private static int currentTokenVersion(User user) {
        return user.getTokenVersion() != null ? user.getTokenVersion() : 0;
    }

    private static String generateCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    // E-postalar büyük/küçük harfe duyarsız olmalı — hem kayıt hem arama aynı normalize edilmiş haliyle yapılır
    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
