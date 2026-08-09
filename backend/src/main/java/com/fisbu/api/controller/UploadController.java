package com.fisbu.api.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

@RestController
@RequestMapping("/receipts")
public class UploadController {

    private static final Set<String> HEIC_BRANDS = Set.of(
            "heic", "heix", "hevc", "hevx", "mif1", "msf1", "heim", "heis", "hevm", "hevs"
    );

    private final Cloudinary cloudinary;

    public UploadController(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @PostMapping("/upload")
    public Map<String, String> uploadReceiptImage(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dosya boş olamaz");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dosya okunamadı");
        }

        // Mobil taraftaki bildirilen Content-Type'a güvenilmiyor: uzantı tabanlı
        // tahmin (ör. .txt uzantısı .jpg yapılıp yollanabiliyor) ya da tespit
        // edilemediğinde düşülen sabit varsayılan, bildirilen tipi kolayca
        // ALLOWED_CONTENT_TYPES'a uydurabiliyor. Bu yüzden gerçek format tek
        // doğruluk kaynağı olarak dosyanın ilk baytlarından (magic number)
        // belirleniyor; bildirilen tip ne olursa olsun bu kontrol atlanmıyor.
        if (detectImageContentType(bytes) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sadece resim dosyaları yüklenebilir");
        }

        try {
            Map uploadResult = cloudinary.uploader().upload(
                    bytes,
                    ObjectUtils.asMap(
                            "folder", "fisbu/receipts",
                            "resource_type", "image"
                    )
            );

            String imageUrl = (String) uploadResult.get("secure_url");
            return Map.of("imageUrl", imageUrl);

        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Resim yüklenemedi"
            );
        }
    }

    /** Dosya baytlarındaki magic number'dan gerçek resim tipini tespit eder, tanınmazsa null döner. */
    private String detectImageContentType(byte[] bytes) {
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') {
            return "image/png";
        }
        if (bytes.length >= 12
                && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "image/webp";
        }
        if (bytes.length >= 12 && bytes[4] == 'f' && bytes[5] == 't' && bytes[6] == 'y' && bytes[7] == 'p') {
            String brand = new String(bytes, 8, 4, StandardCharsets.US_ASCII);
            if (HEIC_BRANDS.contains(brand)) {
                return "image/heic";
            }
        }
        return null;
    }
}