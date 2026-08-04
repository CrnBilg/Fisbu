package com.fisbu.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

// Abonelik olabilecek tekrarlayan ödeme (Netflix/Spotify vb.) — aynı mağaza+yakın tutarın
// aylık aralıklarla tekrarı tespit edildiğinde döner
@Getter
@Setter
public class SubscriptionCandidateResponse {
    private String storeName;
    private BigDecimal averageAmount;
    private int occurrenceCount;
    private LocalDate firstDate;
    private LocalDate lastDate;
    private int averageIntervalDays;
    private LocalDate estimatedNextDate;
}
