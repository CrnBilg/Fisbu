package com.fisbu.api.config;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * IP başına, brute-force/spam'e ve maliyetli işlemlere açık uçları dakikada sabit
 * istekle sınırlar. Dış kütüphane gerektirmeyen basit sabit pencereli sayaç — tek
 * Railway instance'ı için yeterli.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MILLIS = 60_000;

    // Tam yol eşleşmesi — brute-force'a açık auth uçları
    private static final Map<String, Integer> EXACT_LIMITS = Map.of(
            "/auth/login", 10,
            "/auth/register", 10,
            "/auth/forgot-password", 10,
            "/auth/reset-password", 10,
            "/auth/verify-email", 10,
            "/auth/resend-verification", 10,
            // Davet kodu 6 karakter/32 alfabe (~1 milyar kombinasyon) — pratik açıdan brute-force
            // zor olsa da bu uç hiç sınırlanmıyordu, defense-in-depth için ekleniyor
            "/households/join", 10
    );

    // Önek eşleşmesi — Groq AI çağrısı yapan veya CPU-ağır PDF/CSV parse eden uçlar,
    // maliyet/DoS istismarına karşı daha sıkı sınırlanır
    private static final Map<String, Integer> PREFIX_LIMITS = Map.of(
            "/ai/", 5,
            "/receipts/import/", 5
    );

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    private static final class Window {
        volatile long start = System.currentTimeMillis();
        final AtomicInteger count = new AtomicInteger(0);
    }

    private Integer resolveLimit(String path) {
        Integer exact = EXACT_LIMITS.get(path);
        if (exact != null) {
            return exact;
        }
        for (Map.Entry<String, Integer> entry : PREFIX_LIMITS.entrySet()) {
            if (path.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        Integer maxRequests = resolveLimit(path);
        if (maxRequests == null) {
            filterChain.doFilter(request, response);
            return;
        }

        Window window = windows.computeIfAbsent(clientIp(request) + ":" + path, k -> new Window());
        boolean allowed;
        synchronized (window) {
            long now = System.currentTimeMillis();
            if (now - window.start > WINDOW_MILLIS) {
                window.start = now;
                window.count.set(0);
            }
            allowed = window.count.incrementAndGet() <= maxRequests;
        }

        if (!allowed) {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"Çok fazla istek gönderildi, lütfen bir dakika sonra tekrar deneyin\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String clientIp(HttpServletRequest request) {
        // Railway tek reverse-proxy hop'u olarak konumlandığından zincirdeki SON değer
        // Railway'in eklediği gerçek istemci IP'sidir — İLK değer istemci tarafından
        // serbestçe ayarlanabildiği için güvenilmez (rate limit atlatma riski)
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String[] parts = forwarded.split(",");
            return parts[parts.length - 1].trim();
        }
        return request.getRemoteAddr();
    }
}
