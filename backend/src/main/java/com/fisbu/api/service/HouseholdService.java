package com.fisbu.api.service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fisbu.api.dto.CreateHouseholdRequest;
import com.fisbu.api.dto.HouseholdCategoryTotalResponse;
import com.fisbu.api.dto.HouseholdMemberResponse;
import com.fisbu.api.dto.HouseholdMemberTotalResponse;
import com.fisbu.api.dto.HouseholdResponse;
import com.fisbu.api.dto.HouseholdStatisticsResponse;
import com.fisbu.api.dto.JoinHouseholdRequest;
import com.fisbu.api.entity.Household;
import com.fisbu.api.entity.Receipt;
import com.fisbu.api.entity.User;
import com.fisbu.api.repository.HouseholdRepository;
import com.fisbu.api.repository.ReceiptRepository;
import com.fisbu.api.repository.UserRepository;

/**
 * Aile bütçe modu: kullanıcılar kendi fiş/kategori/bütçelerinin sahibi olmaya devam eder,
 * Household sadece davet koduyla katılınan bir grup olup toplu (salt-okunur) istatistik
 * görünümü sağlar. Mevcut ownership modeline dokunmaz.
 */
@Service
public class HouseholdService {

    private static final String INVITE_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // 0/O, 1/I çıkarıldı
    private static final int INVITE_CODE_LENGTH = 6;

    private final HouseholdRepository householdRepository;
    private final UserRepository userRepository;
    private final ReceiptRepository receiptRepository;
    private final SecureRandom random = new SecureRandom();

    public HouseholdService(HouseholdRepository householdRepository, UserRepository userRepository,
                             ReceiptRepository receiptRepository) {
        this.householdRepository = householdRepository;
        this.userRepository = userRepository;
        this.receiptRepository = receiptRepository;
    }

    public HouseholdResponse createHousehold(String email, CreateHouseholdRequest request) {
        User user = getUserByEmail(email);
        if (user.getHousehold() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Zaten bir aileye üyesin, önce ayrılmalısın");
        }

        Household household = new Household();
        household.setName(request.getName().trim());
        household.setInviteCode(generateUniqueInviteCode());
        household = householdRepository.save(household);

        user.setHousehold(household);
        userRepository.save(user);

        return toResponse(household);
    }

    public HouseholdResponse joinHousehold(String email, JoinHouseholdRequest request) {
        User user = getUserByEmail(email);
        if (user.getHousehold() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Zaten bir aileye üyesin, önce ayrılmalısın");
        }

        Household household = householdRepository.findByInviteCode(request.getInviteCode().trim().toUpperCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Geçersiz davet kodu"));

        user.setHousehold(household);
        userRepository.save(user);

        return toResponse(household);
    }

    public void leaveHousehold(String email) {
        User user = getUserByEmail(email);
        if (user.getHousehold() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bir aileye üye değilsin");
        }
        user.setHousehold(null);
        userRepository.save(user);
    }

    public HouseholdResponse getMyHousehold(String email) {
        User user = getUserByEmail(email);
        Household household = user.getHousehold();
        if (household == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Bir aileye üye değilsin");
        }
        return toResponse(household);
    }

    public HouseholdStatisticsResponse getStatistics(String email, Integer year, Integer month) {
        User user = getUserByEmail(email);
        Household household = user.getHousehold();
        if (household == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Bir aileye üye değilsin");
        }

        LocalDate today = LocalDate.now();
        int resolvedYear = year != null ? year : today.getYear();
        int resolvedMonth = month != null ? month : today.getMonthValue();
        LocalDate start = LocalDate.of(resolvedYear, resolvedMonth, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        List<User> members = userRepository.findByHousehold(household);
        List<Receipt> receipts = receiptRepository.findByUserInAndReceiptDateBetween(members, start, end);

        Map<Long, HouseholdMemberTotalResponse> byMember = new LinkedHashMap<>();
        for (User member : members) {
            HouseholdMemberTotalResponse entry = new HouseholdMemberTotalResponse();
            entry.setUserId(member.getId());
            entry.setName(member.getName());
            entry.setEmail(member.getEmail());
            entry.setTotalAmount(BigDecimal.ZERO);
            byMember.put(member.getId(), entry);
        }

        Map<String, BigDecimal> byCategory = new LinkedHashMap<>();
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (Receipt receipt : receipts) {
            BigDecimal amount = receipt.getTotalAmount() != null ? receipt.getTotalAmount() : BigDecimal.ZERO;
            grandTotal = grandTotal.add(amount);

            HouseholdMemberTotalResponse memberEntry = byMember.get(receipt.getUser().getId());
            if (memberEntry != null) {
                memberEntry.setTotalAmount(memberEntry.getTotalAmount().add(amount));
            }

            String categoryName = receipt.getCategory() != null ? receipt.getCategory().getName() : "Diğer";
            byCategory.merge(categoryName, amount, BigDecimal::add);
        }

        List<HouseholdMemberTotalResponse> memberTotals = new ArrayList<>(byMember.values());
        memberTotals.sort(Comparator.comparing(HouseholdMemberTotalResponse::getTotalAmount).reversed());

        List<HouseholdCategoryTotalResponse> categoryTotals = byCategory.entrySet().stream().map(e -> {
            HouseholdCategoryTotalResponse r = new HouseholdCategoryTotalResponse();
            r.setCategoryName(e.getKey());
            r.setTotalAmount(e.getValue());
            return r;
        }).sorted(Comparator.comparing(HouseholdCategoryTotalResponse::getTotalAmount).reversed())
          .collect(Collectors.toList());

        HouseholdStatisticsResponse response = new HouseholdStatisticsResponse();
        response.setYear(resolvedYear);
        response.setMonth(resolvedMonth);
        response.setTotalAmount(grandTotal);
        response.setByMember(memberTotals);
        response.setByCategory(categoryTotals);
        return response;
    }

    private HouseholdResponse toResponse(Household household) {
        List<HouseholdMemberResponse> members = userRepository.findByHousehold(household).stream()
                .map(u -> new HouseholdMemberResponse(u.getId(), u.getName(), u.getEmail()))
                .collect(Collectors.toList());
        return new HouseholdResponse(household.getId(), household.getName(), household.getInviteCode(), members);
    }

    private String generateUniqueInviteCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(INVITE_CODE_LENGTH);
            for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
                sb.append(INVITE_CODE_CHARS.charAt(random.nextInt(INVITE_CODE_CHARS.length())));
            }
            code = sb.toString();
        } while (householdRepository.existsByInviteCode(code));
        return code;
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kullanıcı bulunamadı"));
    }
}
