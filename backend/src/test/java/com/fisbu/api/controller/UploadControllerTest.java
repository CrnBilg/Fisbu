package com.fisbu.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.fisbu.api.config.SecurityConfig;
import com.fisbu.api.repository.UserRepository;
import com.fisbu.api.service.JwtService;

/**
 * UploadController için dar (@WebMvcTest) kapsamlı testler — TEST-003/TEST-004 deseni izlenir.
 *
 * Not (Sprint 2 denetimi): UploadController, @AuthenticationPrincipal userDetails parametresini
 * alıyor ama HİÇ KULLANMIYOR (kim yüklediğine dair bir kayıt tutulmuyor) — bu bilinen bir bulgu,
 * bu story'nin kapsamı dışında (production kodu değiştirilmiyor). Testler mevcut (kullanılmayan)
 * davranışı olduğu gibi doğrular.
 */
@WebMvcTest(UploadController.class)
@Import(SecurityConfig.class)
class UploadControllerTest {

    private static final byte[] JPEG_MAGIC_BYTES = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x01, 0x02};

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private Cloudinary cloudinary;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void upload_authHeaderOlmadan403Doner() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "receipt.jpg", "image/jpeg", JPEG_MAGIC_BYTES);

        mockMvc.perform(multipart("/receipts/upload").file(file))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "test@fisbu.com")
    void upload_gecerliJpegDosyasi_imageUrlDoner() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "receipt.jpg", "image/jpeg", JPEG_MAGIC_BYTES);

        Uploader uploader = org.mockito.Mockito.mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), any(Map.class)))
                .thenReturn(Map.of("secure_url", "https://cloudinary.example/receipt.jpg"));

        mockMvc.perform(multipart("/receipts/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").value("https://cloudinary.example/receipt.jpg"));
    }

    @Test
    @WithMockUser(username = "test@fisbu.com")
    void upload_bosDosya_400Doner() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);

        mockMvc.perform(multipart("/receipts/upload").file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "test@fisbu.com")
    void upload_gecersizMagicByte_resimOlmayanDosya_400Doner() throws Exception {
        // .jpg uzantılı ama gerçek magic byte'ı olmayan bir dosya — controller'ın
        // bildirilen content-type'a değil gerçek baytlara güvendiğini doğrular
        MockMultipartFile file = new MockMultipartFile("file", "fake.jpg", "image/jpeg",
                "bu bir resim değil, düz metin".getBytes());

        mockMvc.perform(multipart("/receipts/upload").file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "test@fisbu.com")
    void upload_cloudinaryHataVerirse_500Doner() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "receipt.png",
                "image/png", pngMagicBytes());

        Uploader uploader = org.mockito.Mockito.mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), any(Map.class))).thenThrow(new java.io.IOException("network"));

        mockMvc.perform(multipart("/receipts/upload").file(file))
                .andExpect(status().isInternalServerError());
    }

    private byte[] pngMagicBytes() {
        return new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
    }
}
