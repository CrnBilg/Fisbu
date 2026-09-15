package com.fisbu.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.fisbu.api.dto.ChangePasswordRequest;
import com.fisbu.api.dto.LoginRequest;
import com.fisbu.api.dto.RegisterRequest;
import com.fisbu.api.dto.ResetPasswordRequest;
import com.fisbu.api.dto.UpdateProfileRequest;
import com.fisbu.api.dto.VerifyEmailRequest;
import com.fisbu.api.entity.Category;
import com.fisbu.api.entity.User;
import com.fisbu.api.repository.CategoryRepository;
import com.fisbu.api.repository.UserRepository;

/**
 * SEC-003 — AuthService'in mevcut davranışını kilitleyen regresyon testleri.
 * Bu dosya AuthService.java'nın kodunu DEĞİŞTİRMEZ, sadece mevcut davranışı test eder.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String EMAIL = "test@fisbu.com";
    private static final String RAW_PASSWORD = "CurrentPass1!";
    private static final String ENCODED_PASSWORD = "encoded:" + RAW_PASSWORD;

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private EmailService emailService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService, categoryRepository,
                emailService);
    }

    private User verifiedUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail(EMAIL);
        user.setPassword(ENCODED_PASSWORD);
        user.setEmailVerified(true);
        user.setTokenVersion(0);
        return user;
    }

    // ---------- login ----------

    @Test
    void login_gecerliKimlikBilgileriyle_tokenDoner() {
        User user = verifiedUser();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(jwtService.generateToken(EMAIL, 0)).thenReturn("jwt-token");

        LoginRequest request = new LoginRequest();
        request.setEmail(EMAIL);
        request.setPassword(RAW_PASSWORD);

        String token = authService.login(request);

        assertThat(token).isEqualTo("jwt-token");
    }

    @Test
    void login_emailBuyukKucukHarfDuyarsizNormalizeEdilir() {
        User user = verifiedUser();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(jwtService.generateToken(EMAIL, 0)).thenReturn("jwt-token");

        LoginRequest request = new LoginRequest();
        request.setEmail("  Test@FisBu.com  ");
        request.setPassword(RAW_PASSWORD);

        authService.login(request);

        verify(userRepository).findByEmail(EMAIL);
    }

    @Test
    void login_varOlmayanKullanici_401VeJenerikMesajDoner() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest();
        request.setEmail(EMAIL);
        request.setPassword(RAW_PASSWORD);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401")
                .hasMessageContaining("Email veya şifre hatalı");
    }

    @Test
    void login_yanlisSifre_401VeVarOlmayanKullaniciyla_AYNI_JenerikMesajDoner() {
        User user = verifiedUser();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", ENCODED_PASSWORD)).thenReturn(false);

        LoginRequest request = new LoginRequest();
        request.setEmail(EMAIL);
        request.setPassword("wrong");

        // Enumeration koruması: "kullanıcı yok" ile "şifre yanlış" AYNI mesajı dönmeli —
        // bu iki test bilerek aynı mesajı doğruluyor, saldırgan ayırt edemesin.
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401")
                .hasMessageContaining("Email veya şifre hatalı");
    }

    @Test
    void login_dogrulanmamisEmail_403Doner() {
        User user = verifiedUser();
        user.setEmailVerified(false);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);

        LoginRequest request = new LoginRequest();
        request.setEmail(EMAIL);
        request.setPassword(RAW_PASSWORD);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    // ---------- register ----------

    @Test
    void register_varOlanEmail_409Doner() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(verifiedUser()));

        RegisterRequest request = new RegisterRequest();
        request.setEmail(EMAIL);
        request.setPassword(RAW_PASSWORD);
        request.setName("Test User");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_basarili_20VarsayilanKategoriOlusturur_veDogrulamaKoduGonderir() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        User saved = verifiedUser();
        saved.setEmailVerified(false);
        when(userRepository.save(any(User.class))).thenReturn(saved);

        RegisterRequest request = new RegisterRequest();
        request.setEmail(EMAIL);
        request.setPassword(RAW_PASSWORD);
        request.setName("Test User");

        authService.register(request);

        ArgumentCaptor<List<Category>> categoriesCaptor = ArgumentCaptor.forClass(List.class);
        verify(categoryRepository).saveAll(categoriesCaptor.capture());
        assertThat(categoriesCaptor.getValue()).hasSize(20);

        verify(emailService).sendVerificationCode(eq(EMAIL), any());
    }

    // ---------- changePassword ----------

    @Test
    void changePassword_yanlisMevcutSifre_401Doner() {
        User user = verifiedUser();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-current", ENCODED_PASSWORD)).thenReturn(false);

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrong-current");
        request.setNewPassword("NewPass1!");

        assertThatThrownBy(() -> authService.changePassword(EMAIL, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401");

        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_yeniSifreMevcutSifreyleAyni_400Doner() {
        User user = verifiedUser();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword(RAW_PASSWORD);
        request.setNewPassword(RAW_PASSWORD);

        assertThatThrownBy(() -> authService.changePassword(EMAIL, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");
    }

    @Test
    void changePassword_basarili_tokenVersionArtar() {
        User user = verifiedUser();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(passwordEncoder.matches("NewPass1!", ENCODED_PASSWORD)).thenReturn(false);
        when(passwordEncoder.encode("NewPass1!")).thenReturn("encoded:NewPass1!");

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword(RAW_PASSWORD);
        request.setNewPassword("NewPass1!");

        authService.changePassword(EMAIL, request);

        assertThat(user.getTokenVersion()).isEqualTo(1);
        assertThat(user.getPassword()).isEqualTo("encoded:NewPass1!");
        verify(userRepository).save(user);
    }

    // ---------- forgotPassword (enumeration koruması) ----------

    @Test
    void forgotPassword_varOlmayanKullanici_HataFirlatmazVeEmailGondermez() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        // Kritik: kullanıcı yoksa da metot sessizce (hatasız) döner — enumeration koruması
        authService.forgotPassword(EMAIL);

        verify(emailService, never()).sendPasswordResetCode(any(), any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void forgotPassword_varOlanKullanici_KodUretirVeEmailGonderir() {
        User user = verifiedUser();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        authService.forgotPassword(EMAIL);

        assertThat(user.getResetPasswordCode()).isNotNull().hasSize(6);
        verify(emailService).sendPasswordResetCode(eq(EMAIL), any());
    }

    // ---------- resetPassword ----------

    @Test
    void resetPassword_denemeLimitiAsilmis_kodSuresiDolmamisOlsaBileReddedilir() {
        User user = verifiedUser();
        user.setResetPasswordAttempts(5);
        user.setResetPasswordCode("123456");
        user.setResetPasswordCodeExpiry(LocalDateTime.now().plusMinutes(10));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail(EMAIL);
        request.setCode("123456");
        request.setNewPassword("NewPass1!");

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");
    }

    @Test
    void resetPassword_gecersizKod_denemeSayaciniArtirirVeReddeder() {
        User user = verifiedUser();
        user.setResetPasswordAttempts(0);
        user.setResetPasswordCode("123456");
        user.setResetPasswordCodeExpiry(LocalDateTime.now().plusMinutes(10));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail(EMAIL);
        request.setCode("999999");
        request.setNewPassword("NewPass1!");

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(ResponseStatusException.class);

        assertThat(user.getResetPasswordAttempts()).isEqualTo(1);
        verify(userRepository).save(user);
    }

    @Test
    void resetPassword_suresiDolmusKod_reddedilir() {
        User user = verifiedUser();
        user.setResetPasswordAttempts(0);
        user.setResetPasswordCode("123456");
        user.setResetPasswordCodeExpiry(LocalDateTime.now().minusMinutes(1));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail(EMAIL);
        request.setCode("123456");
        request.setNewPassword("NewPass1!");

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void resetPassword_basarili_tokenVersionArtarVeKodTemizlenir() {
        User user = verifiedUser();
        user.setResetPasswordAttempts(0);
        user.setResetPasswordCode("123456");
        user.setResetPasswordCodeExpiry(LocalDateTime.now().plusMinutes(10));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("NewPass1!", ENCODED_PASSWORD)).thenReturn(false);
        when(passwordEncoder.encode("NewPass1!")).thenReturn("encoded:NewPass1!");

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail(EMAIL);
        request.setCode("123456");
        request.setNewPassword("NewPass1!");

        authService.resetPassword(request);

        assertThat(user.getTokenVersion()).isEqualTo(1);
        assertThat(user.getResetPasswordCode()).isNull();
        assertThat(user.getResetPasswordAttempts()).isZero();
    }

    // ---------- verifyEmail ----------

    @Test
    void verifyEmail_zatenDogrulanmis_sessizceDoner() {
        User user = verifiedUser();
        user.setEmailVerified(true);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setEmail(EMAIL);
        request.setCode("000000");

        authService.verifyEmail(request);

        verify(userRepository, never()).save(any());
    }

    @Test
    void verifyEmail_denemeLimitiAsilmis_reddedilir() {
        User user = verifiedUser();
        user.setEmailVerified(false);
        user.setVerificationAttempts(5);
        user.setVerificationCode("123456");
        user.setVerificationCodeExpiry(LocalDateTime.now().plusMinutes(10));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setEmail(EMAIL);
        request.setCode("123456");

        assertThatThrownBy(() -> authService.verifyEmail(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");
    }

    @Test
    void verifyEmail_basarili_dogrulanmisOlarakIsaretlerVeKoduTemizler() {
        User user = verifiedUser();
        user.setEmailVerified(false);
        user.setVerificationAttempts(0);
        user.setVerificationCode("123456");
        user.setVerificationCodeExpiry(LocalDateTime.now().plusMinutes(10));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setEmail(EMAIL);
        request.setCode("123456");

        authService.verifyEmail(request);

        assertThat(user.getEmailVerified()).isTrue();
        assertThat(user.getVerificationCode()).isNull();
        assertThat(user.getVerificationAttempts()).isZero();
    }

    // ---------- resendVerificationCode (enumeration koruması) ----------

    @Test
    void resendVerificationCode_varOlmayanKullanici_sessizceDoner() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        authService.resendVerificationCode(EMAIL);

        verify(emailService, never()).sendVerificationCode(any(), any());
    }

    @Test
    void resendVerificationCode_zatenDogrulanmis_emailGondermez() {
        User user = verifiedUser();
        user.setEmailVerified(true);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        authService.resendVerificationCode(EMAIL);

        verify(emailService, never()).sendVerificationCode(any(), any());
    }

    // deleteAccount testleri ARCH-002/ADR-004 ile AccountDeletionServiceTest'e taşındı —
    // AuthService artık deleteAccount() içermiyor (hesap silme orkestrasyonu ayrı bir
    // servise, domain port'ları üzerinden taşındı; AuthService sadece kimlik doğrulama
    // sorumluluğunu taşıyor).

    // ---------- getProfile / updateProfile ----------

    @Test
    void getProfile_varOlmayanKullanici_404Doner() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getProfile(EMAIL))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void updateProfile_sadeceGonderilenAlanlariGuncellerVeDigerlerineDokunmaz() {
        User user = verifiedUser();
        user.setName("Eski Ad");
        user.setProfileImageUrl("https://old.example/img.png");
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setName("Yeni Ad");
        // profileImageUrl gönderilmiyor (null) — mevcut değer korunmalı

        authService.updateProfile(EMAIL, request);

        assertThat(user.getName()).isEqualTo("Yeni Ad");
        assertThat(user.getProfileImageUrl()).isEqualTo("https://old.example/img.png");
    }
}
