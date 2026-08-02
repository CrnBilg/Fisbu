package com.fisbu.api.config;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * IP başına, brute-force/spam'e açık /auth/** uçlarını dakikada sabit istekle sınırlar.
 * Dış kütüphane gerektirmeyen basit sabit pencereli sayaç — tek Railway instance'ı için yeterli.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_WINDOW = 10;
    private static final long WINDOW_MILLIS = 60_000;

    private static final Set<String> LIMITED_PATHS = Set.of(
            "/auth/login",
            "/auth/register",
            "/auth/forgot-password",
            "/auth/reset-password",
            "/auth/verify-email",
            "/auth/resend-verification"
    );

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    private static final class Window {
        volatile long start = System.currentTimeMillis();
        final AtomicInteger count = new AtomicInteger(0);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!LIMITED_PATHS.contains(path)) {
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
            allowed = window.count.incrementAndGet() <= MAX_REQUESTS_PER_WINDOW;
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
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
