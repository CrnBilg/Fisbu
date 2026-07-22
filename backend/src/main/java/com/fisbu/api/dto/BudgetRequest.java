package com.fisbu.api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class BudgetRequest {

    @NotNull(message = "Kategori seçilmelidir")
    private Long categoryId;

    @NotNull(message = "Aylık limit boş olamaz")
    @DecimalMin(value = "0.01", message = "Aylık limit 0'dan büyük olmalıdır")
    private BigDecimal monthlyLimit;

    @NotNull(message = "Yıl boş olamaz")
    private Integer year;

    @NotNull(message = "Ay boş olamaz")
    @Min(value = 1, message = "Ay 1-12 arasında olmalıdır")
    @Max(value = 12, message = "Ay 1-12 arasında olmalıdır")
    private Integer month;

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public BigDecimal getMonthlyLimit() { return monthlyLimit; }
    public void setMonthlyLimit(BigDecimal monthlyLimit) { this.monthlyLimit = monthlyLimit; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public Integer getMonth() { return month; }
    public void setMonth(Integer month) { this.month = month; }
}
