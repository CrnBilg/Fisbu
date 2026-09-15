package com.fisbu.api.shared;

/**
 * ARCH-005: kullanıcı başına Cloudinary klasör yapısı — hem upload sırasında (UploadController)
 * hem hesap silme sırasında (AccountDeletionService, deleteResourcesByPrefix ile toplu temizlik)
 * AYNI mantıkla üretilmesi gerektiği için tek bir yerde tanımlandı.
 */
public final class CloudinaryPaths {

    private static final String RECEIPTS_ROOT = "fisbu/receipts";

    private CloudinaryPaths() {
    }

    /** E-postadaki Cloudinary klasör adları için güvenli olmayan karakterleri (@, .) alt çizgiye çevirir. */
    public static String userReceiptsFolder(String email) {
        String sanitized = email.trim().toLowerCase().replaceAll("[^a-z0-9]", "_");
        return RECEIPTS_ROOT + "/" + sanitized;
    }
}
