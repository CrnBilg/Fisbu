package com.fisbu.api.dto;

import java.time.LocalDateTime;
import java.util.List;

// KVKK m. 11 (veri taşınabilirliği) kapsamında kullanıcının kendi verilerini
// makine okunabilir formatta indirmesi için — profil + kategori + bütçe + fiş verilerinin tamamı
public class UserDataExportResponse {
    private LocalDateTime exportedAt;
    private ProfileResponse profile;
    private List<CategoryResponse> categories;
    private List<BudgetResponse> budgets;
    private List<ReceiptResponse> receipts;

    public UserDataExportResponse(LocalDateTime exportedAt, ProfileResponse profile,
                                   List<CategoryResponse> categories, List<BudgetResponse> budgets,
                                   List<ReceiptResponse> receipts) {
        this.exportedAt = exportedAt;
        this.profile = profile;
        this.categories = categories;
        this.budgets = budgets;
        this.receipts = receipts;
    }

    public LocalDateTime getExportedAt() { return exportedAt; }
    public void setExportedAt(LocalDateTime exportedAt) { this.exportedAt = exportedAt; }

    public ProfileResponse getProfile() { return profile; }
    public void setProfile(ProfileResponse profile) { this.profile = profile; }

    public List<CategoryResponse> getCategories() { return categories; }
    public void setCategories(List<CategoryResponse> categories) { this.categories = categories; }

    public List<BudgetResponse> getBudgets() { return budgets; }
    public void setBudgets(List<BudgetResponse> budgets) { this.budgets = budgets; }

    public List<ReceiptResponse> getReceipts() { return receipts; }
    public void setReceipts(List<ReceiptResponse> receipts) { this.receipts = receipts; }
}
