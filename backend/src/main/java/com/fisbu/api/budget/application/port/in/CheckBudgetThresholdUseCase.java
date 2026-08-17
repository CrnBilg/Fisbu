package com.fisbu.api.budget.application.port.in;

/**
 * Bir fiş eklendikten sonra çağrılır: ilgili kategori/ay bütçesinin harcama oranını
 * hesaplar ve %80 (uyarı) / %100 (aşım) eşiklerini yeni geçtiyse push bildirimi gönderir.
 * Bütçe tanımlı değilse veya Firebase yapılandırılmamışsa sessizce çıkar.
 */
public interface CheckBudgetThresholdUseCase {

    void checkAndNotify(Long userId, Long categoryId, int year, int month);
}
