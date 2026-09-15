package com.fisbu.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.jupiter.api.Test;

/**
 * ARCH-004 — StatementImportService/UploadController'ın MultipartFile'ı tamamen belleğe
 * okumasının gerçek riski değerlendirildi: `spring.servlet.multipart.max-file-size` zaten
 * her isteği 10MB'a sınırlıyor (Spring bu limiti servlet/filter katmanında, controller'a
 * ULAŞMADAN önce uyguluyor) — 10MB bir PDF/CSV'yi belleğe almak modern bir JVM için önemsiz,
 * bu yüzden streaming'e geçmenin gerçek bir fayda sağlamadığına karar verildi (bkz.
 * .sdlc/stories/ARCH-004.md). Bu test, o kararın dayandığı varsayımı (limit gerçekten var VE
 * makul bir üst sınırda) kilitliyor — biri bu satırı silip/çok büyütürse test kırılır.
 */
class MultipartFileSizeLimitTest {

    @Test
    void applicationProperties_multipartDosyaBoyutuLimitiTanimliVeMakulUstSinirda() throws IOException {
        Properties props = new Properties();
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("application.properties")) {
            assertThat(in).as("application.properties bulunamadı").isNotNull();
            props.load(in);
        }

        String maxFileSize = props.getProperty("spring.servlet.multipart.max-file-size");
        String maxRequestSize = props.getProperty("spring.servlet.multipart.max-request-size");

        assertThat(maxFileSize).as("max-file-size tanımsız olamaz").isNotBlank();
        assertThat(maxRequestSize).as("max-request-size tanımsız olamaz").isNotBlank();

        // "10MB" gibi bir değeri MB'a çevirip mantıklı bir üst sınırda (<= 25MB) kaldığını doğrular
        assertThat(parseMegabytes(maxFileSize)).isLessThanOrEqualTo(25);
        assertThat(parseMegabytes(maxRequestSize)).isLessThanOrEqualTo(25);
    }

    private static int parseMegabytes(String value) {
        String normalized = value.trim().toUpperCase();
        if (normalized.endsWith("MB")) {
            return Integer.parseInt(normalized.substring(0, normalized.length() - 2));
        }
        throw new IllegalArgumentException("Beklenmeyen boyut formatı: " + value);
    }
}
