package com.fisbu.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * SEC-006: EmailService.maskEmail'in log'a yazılan e-posta maskeleme davranışını kilitler
 * (PII sızıntısı düzeltmesi).
 */
class EmailServiceTest {

    @Test
    void maskEmail_normalAdres_ilkKarakterVeDomainKorunur() {
        assertThat(EmailService.maskEmail("jane.doe@fisbu.com")).isEqualTo("j***@fisbu.com");
    }

    @Test
    void maskEmail_tekKarakterliYerelKisim_dogruMaskelenir() {
        assertThat(EmailService.maskEmail("a@fisbu.com")).isEqualTo("a***@fisbu.com");
    }

    @Test
    void maskEmail_atIsaretiYoksa_tamamenMaskelenir() {
        assertThat(EmailService.maskEmail("gecersiz-adres")).isEqualTo("***");
    }

    @Test
    void maskEmail_nullIse_bilinmiyorDoner() {
        assertThat(EmailService.maskEmail(null)).isEqualTo("(bilinmiyor)");
    }

    @Test
    void maskEmail_bosStringIse_bilinmiyorDoner() {
        assertThat(EmailService.maskEmail("")).isEqualTo("(bilinmiyor)");
    }
}
