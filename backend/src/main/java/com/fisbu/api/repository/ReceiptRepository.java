package com.fisbu.api.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.fisbu.api.entity.Category;
import com.fisbu.api.entity.Receipt;
import com.fisbu.api.entity.User;

public interface ReceiptRepository extends JpaRepository<Receipt, Long>, JpaSpecificationExecutor<Receipt> {

    // category/user ManyToOne LAZY — bu metodların hepsi sonuçları döngüyle işleyip
    // getCategory()/getUser() çağırıyor, EntityGraph olmadan her satır için ayrı sorgu tetiklenir (N+1)
    @EntityGraph(attributePaths = {"category", "user"})
    List<Receipt> findByUser(User user);

    // GET /receipts sınırsız büyümeye karşı üst sınırlı — response şekli (List) korunuyor,
    // gerçek sayfalama mobil tarafında (Dashboard/Statistics) benimsendiğinde kaldırılabilir
    @EntityGraph(attributePaths = {"category", "user"})
    List<Receipt> findByUser(User user, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "user"})
    List<Receipt> findByCategory(Category category);

    @EntityGraph(attributePaths = {"category", "user"})
    List<Receipt> findByUserAndReceiptDateBetween(User user, LocalDate start, LocalDate end);

    @EntityGraph(attributePaths = {"category", "user"})
    List<Receipt> findByUserAndCategoryAndReceiptDateBetween(User user, Category category, LocalDate start, LocalDate end);

    List<Receipt> findByUserAndStoreNameIgnoreCaseAndTotalAmountAndReceiptDate(
            User user, String storeName, BigDecimal totalAmount, LocalDate receiptDate);

    // Aile bütçe modu — household üyelerinin fişlerini tek sorguda toplamak için
    @EntityGraph(attributePaths = {"category", "user"})
    List<Receipt> findByUserInAndReceiptDateBetween(List<User> users, LocalDate start, LocalDate end);

    // Garanti/iade hatırlatıcı — WarrantyReminderScheduler'ın günlük taraması için, receipt.getUser() erişiyor
    @EntityGraph(attributePaths = {"user"})
    List<Receipt> findByReturnDeadlineBetweenAndReturnReminderSentFalse(LocalDate start, LocalDate end);

    @EntityGraph(attributePaths = {"user"})
    List<Receipt> findByWarrantyExpiryDateBetweenAndWarrantyReminderSentFalse(LocalDate start, LocalDate end);

    // Bütçe harcama hesaplaması — tüm fişleri belleğe yükleyip Java'da toplamak yerine DB'de SUM
    @Query("SELECT COALESCE(SUM(r.totalAmount), 0) FROM Receipt r " +
            "WHERE r.user = :user AND r.category = :category AND r.receiptDate BETWEEN :start AND :end")
    BigDecimal sumTotalAmountByUserAndCategoryAndReceiptDateBetween(
            @Param("user") User user, @Param("category") Category category,
            @Param("start") LocalDate start, @Param("end") LocalDate end);

    // Tutar anomali tespiti — ortalama/adet DB'de hesaplanır, kategori geçmişi belleğe yüklenmez
    long countByCategoryAndTotalAmountIsNotNull(Category category);

    @Query("SELECT COALESCE(AVG(r.totalAmount), 0) FROM Receipt r " +
            "WHERE r.category = :category AND r.totalAmount IS NOT NULL")
    BigDecimal avgTotalAmountByCategory(@Param("category") Category category);
}