package com.fisbu.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.fisbu.api.budget.application.port.out.DeleteBudgetsByUserPort;
import com.fisbu.api.category.application.port.out.DeleteCategoriesByUserPort;
import com.fisbu.api.entity.User;
import com.fisbu.api.receipt.application.port.out.DeleteReceiptsByUserPort;
import com.fisbu.api.repository.UserRepository;
import com.fisbu.api.shared.CloudinaryPaths;
import com.fisbu.api.shared.application.port.out.DeleteSavingsGoalsByUserPort;

/**
 * ARCH-002/ADR-004: hesap silme orkestrasyonu — önceden AuthService'in içinde
 * (4 domain repository'sine doğrudan erişerek) yapılıyordu, artık her domain'in
 * kendi out-port'u üzerinden, tek transaction içinde sıralı olarak yapılıyor.
 * Sıralama FK zincirine bağımlı (Budget, Category'ye NOT NULL FK ile bağlı) —
 * bu nedenle domain event yerine doğrudan, sıralı port çağrısı kullanılıyor
 * (bkz. ADR-004: event-driven'ın sıralama garantisi vermemesi tercih dışı bırakıldı).
 */
@Service
public class AccountDeletionService {

    private static final Logger log = LoggerFactory.getLogger(AccountDeletionService.class);

    private final UserRepository userRepository;
    private final DeleteBudgetsByUserPort deleteBudgetsByUserPort;
    private final DeleteSavingsGoalsByUserPort deleteSavingsGoalsByUserPort;
    private final DeleteReceiptsByUserPort deleteReceiptsByUserPort;
    private final DeleteCategoriesByUserPort deleteCategoriesByUserPort;
    private final Cloudinary cloudinary;

    public AccountDeletionService(UserRepository userRepository,
                                   DeleteBudgetsByUserPort deleteBudgetsByUserPort,
                                   DeleteSavingsGoalsByUserPort deleteSavingsGoalsByUserPort,
                                   DeleteReceiptsByUserPort deleteReceiptsByUserPort,
                                   DeleteCategoriesByUserPort deleteCategoriesByUserPort,
                                   Cloudinary cloudinary) {
        this.userRepository = userRepository;
        this.deleteBudgetsByUserPort = deleteBudgetsByUserPort;
        this.deleteSavingsGoalsByUserPort = deleteSavingsGoalsByUserPort;
        this.deleteReceiptsByUserPort = deleteReceiptsByUserPort;
        this.deleteCategoriesByUserPort = deleteCategoriesByUserPort;
        this.cloudinary = cloudinary;
    }

    @Transactional
    public void deleteAccount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kullanıcı bulunamadı"));

        // Budget, category'ye NOT NULL FK ile bağlı — önce silinmeli, yoksa kategori/kullanıcı silme FK hatası verir
        deleteBudgetsByUserPort.deleteAllByUserId(user.getId());
        deleteSavingsGoalsByUserPort.deleteAllByUserId(user.getId());
        deleteReceiptsByUserPort.deleteAllByUserId(user.getId());
        deleteCategoriesByUserPort.deleteAllByUserId(user.getId());
        userRepository.delete(user);

        // ARCH-005: KVKK "verilerimi sil" beklentisi — kullanıcının yüklediği fiş görselleri
        // de silinmeli. Best-effort: Cloudinary API'si başarısız olursa (ağ hatası, kota vb.)
        // DB'deki silme İŞLEMİ ZATEN TAMAMLANDI (yukarıda), bu adım onu geri almaz/bloklamaz —
        // sadece loglanır. UploadController ile AYNI klasör adlandırması (CloudinaryPaths)
        // kullanılıyor, aksi halde hiçbir görsel eşleşmez.
        try {
            cloudinary.api().deleteResourcesByPrefix(CloudinaryPaths.userReceiptsFolder(email), ObjectUtils.emptyMap());
        } catch (Exception e) {
            log.warn("Hesap silindi ama Cloudinary görselleri temizlenemedi (email hash: {}): {}",
                    email.hashCode(), e.getMessage());
        }
    }
}
