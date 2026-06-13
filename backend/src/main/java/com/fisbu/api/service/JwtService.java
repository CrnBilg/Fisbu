package com.fisbu.api.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Service // JwtService, JWT token'larının oluşturulması ve doğrulanması işlemlerini yönetir
public class JwtService {

    @Value("${jwt.secret}") // application-secret.properties dosyasından jwt.secret değerini alır
    private String secret;

    // Token geçerlilik süresi: 24 saat (milisaniye cinsinden)
    private static final long EXPIRATION_TIME = 1000 * 60 * 60 * 24; // 24 saat

    private SecretKey getSigningKey() { // JWT imzalama anahtarını oluşturur
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    // Bir kullanıcı için yeni token üretir
    public String generateToken(String email) {
        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(getSigningKey())
                .compact();
    }

    // Token'dan email'i (subject) çıkarır
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Token geçerli mi kontrol eder
    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) { 
        return resolver.apply(extractAllClaims(token));
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}