package com.fisbu.api.service;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fisbu.api.budget.application.port.in.GetAllBudgetsUseCase;
import com.fisbu.api.category.adapter.in.web.CategoryWebMapper;
import com.fisbu.api.category.application.port.in.GetCategoriesUseCase;
import com.fisbu.api.dto.NotificationPrefsRequest;
import com.fisbu.api.dto.NotificationPrefsResponse;
import com.fisbu.api.dto.UserDataExportResponse;
import com.fisbu.api.entity.User;
import com.fisbu.api.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final AuthService authService;
    private final GetCategoriesUseCase getCategoriesUseCase;
    private final CategoryWebMapper categoryWebMapper;
    private final GetAllBudgetsUseCase getAllBudgetsUseCase;
    private final ReceiptService receiptService;

    public UserService(UserRepository userRepository, AuthService authService,
                        GetCategoriesUseCase getCategoriesUseCase, CategoryWebMapper categoryWebMapper,
                        GetAllBudgetsUseCase getAllBudgetsUseCase, ReceiptService receiptService) {
        this.userRepository = userRepository;
        this.authService = authService;
        this.getCategoriesUseCase = getCategoriesUseCase;
        this.categoryWebMapper = categoryWebMapper;
        this.getAllBudgetsUseCase = getAllBudgetsUseCase;
        this.receiptService = receiptService;
    }

    /** Kullanıcının FCM cihaz token'ını kaydeder/günceller. */
    public void updateFcmToken(String email, String fcmToken) {
        User user = getUserByEmail(email);
        user.setFcmToken(fcmToken);
        userRepository.save(user);
    }

    public NotificationPrefsResponse getNotificationPrefs(String email) {
        User user = getUserByEmail(email);
        return new NotificationPrefsResponse(
                Boolean.TRUE.equals(user.getNotifyBudgetWarning()),
                Boolean.TRUE.equals(user.getNotifyBudgetOverspend()));
    }

    public NotificationPrefsResponse updateNotificationPrefs(String email, NotificationPrefsRequest request) {
        User user = getUserByEmail(email);
        user.setNotifyBudgetWarning(request.getBudgetWarningEnabled());
        user.setNotifyBudgetOverspend(request.getBudgetOverspendEnabled());
        userRepository.save(user);
        return new NotificationPrefsResponse(user.getNotifyBudgetWarning(), user.getNotifyBudgetOverspend());
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kullanıcı bulunamadı"));
    }

    // KVKK m. 11 veri taşınabilirliği hakkı — kullanıcının tüm verilerini tek JSON'da toplar
    public UserDataExportResponse exportMyData(String email) {
        return new UserDataExportResponse(
                LocalDateTime.now(),
                authService.getProfile(email),
                categoryWebMapper.toResponseList(getCategoriesUseCase.getCategories(email)),
                getAllBudgetsUseCase.getAllBudgets(email),
                receiptService.getReceipts(email));
    }
}
