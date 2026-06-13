package com.fisbu.api.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.fisbu.api.dto.LoginRequest;
import com.fisbu.api.dto.RegisterRequest;
import com.fisbu.api.entity.User;
import com.fisbu.api.repository.UserRepository;

@Service // AuthService, kullanıcı kayıt işlemlerini yönetir
public class AuthService {

   private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public User register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) { // Veritabanında aynı email adresine sahip bir kullanıcı var mı kontrol eder
throw new ResponseStatusException(HttpStatus.CONFLICT, "Bu email zaten kayıtlı");        }

        User user = new User(); // Yeni bir User nesnesi oluşturur
        user.setEmail(request.getEmail()); // Kullanıcının email adresini ayarlar
        user.setPassword(passwordEncoder.encode(request.getPassword())); // Kullanıcının parolasını güvenli bir şekilde şifreler ve ayarlar

        return userRepository.save(user);// Yeni kullanıcıyı veritabanına kaydeder ve kaydedilen kullanıcıyı döner
    }

    public String login(LoginRequest request) {
        //findByEmail() -> Email ile kullanıcıyı arar
        //Bulamaz ise 401 hatası
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email veya şifre hatalı"));
        // Parola doğrulaması yapar, eşleşmezse 401 hatası
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email veya şifre hatalı");
        }

        return jwtService.generateToken(user.getEmail());
    }
}