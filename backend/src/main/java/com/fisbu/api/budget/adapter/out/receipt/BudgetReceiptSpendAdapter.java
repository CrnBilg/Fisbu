package com.fisbu.api.budget.adapter.out.receipt;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.fisbu.api.budget.application.port.out.SumReceiptSpendPort;
import com.fisbu.api.entity.Category;
import com.fisbu.api.entity.User;
import com.fisbu.api.repository.CategoryRepository;
import com.fisbu.api.repository.ReceiptRepository;
import com.fisbu.api.repository.UserRepository;

// Receipt modülü henüz hexagonal'a taşınmadığı için geçici köprü adaptörü.
// Receipt migrate olduğunda bu adaptör kaldırılıp Receipt modülünün kendi out-port'u kullanılacak.
@Component
public class BudgetReceiptSpendAdapter implements SumReceiptSpendPort {

    private final ReceiptRepository receiptRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    public BudgetReceiptSpendAdapter(ReceiptRepository receiptRepository, UserRepository userRepository,
                                      CategoryRepository categoryRepository) {
        this.receiptRepository = receiptRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public BigDecimal sumSpend(Long userId, Long categoryId, int year, int month) {
        User user = userRepository.findById(userId).orElseThrow();
        Category category = categoryRepository.findById(categoryId).orElseThrow();

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        return receiptRepository.sumTotalAmountByUserAndCategoryAndReceiptDateBetween(user, category, start, end);
    }
}
