package com.fisbu.api.service;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fisbu.api.dto.UserDataExportResponse;
import com.fisbu.api.entity.User;
import com.fisbu.api.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final AuthService authService;
    private final CategoryService categoryService;
    private final BudgetService budgetService;
    private final ReceiptService receiptService;

    public UserService(UserRepository userRepository, AuthService authService,
                        CategoryService categoryService, BudgetService budgetService,
                        ReceiptService receiptService) {
        this.userRepository = userRepository;
        this.authService = authService;
        this.categoryService = categoryService;
        this.budgetService = budgetService;
        this.receiptService = receiptService;
    }

    /** Kullanıcının FCM cihaz token'ını kaydeder/günceller. */
    public void updateFcmToken(String email, String fcmToken) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kullanıcı bulunamadı"));
        user.setFcmToken(fcmToken);
        userRepository.save(user);
    }

    // KVKK m. 11 veri taşınabilirliği hakkı — kullanıcının tüm verilerini tek JSON'da toplar
    public UserDataExportResponse exportMyData(String email) {
        return new UserDataExportResponse(
                LocalDateTime.now(),
                authService.getProfile(email),
                categoryService.getCategories(email),
                budgetService.getAllBudgets(email),
                receiptService.getReceipts(email));
    }
}
