package com.fisbu.api.service;

import java.util.regex.Pattern;

/**
 * Fiş satırlarındaki ürün adlarını, aynı ürünü farklı fişlerde eşleştirebilmek için
 * deterministik olarak sadeleştirir. Fuzzy/embedding eşleştirme yapmaz — bkz. plan
 * dosyasındaki "Riskler" bölümü.
 */
public final class ProductNameNormalizer {

    private static final Pattern UNIT_TOKENS = Pattern.compile(
            "\\b\\d+([.,]\\d+)?\\s*(LT|L|ML|GR|G|KG|ADET|AD|PAKET|PKT|CL)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^A-ZÇĞİÖŞÜ0-9\\s]");
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");

    private ProductNameNormalizer() {
    }

    public static String normalize(String rawProductName) {
        if (rawProductName == null) {
            return "";
        }

        String upper = rawProductName.toUpperCase(java.util.Locale.forLanguageTag("tr-TR"));
        String withoutUnits = UNIT_TOKENS.matcher(upper).replaceAll(" ");
        String withoutSymbols = NON_ALPHANUMERIC.matcher(withoutUnits).replaceAll(" ");
        return MULTI_SPACE.matcher(withoutSymbols).replaceAll(" ").trim();
    }
}
