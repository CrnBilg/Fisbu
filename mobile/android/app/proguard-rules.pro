# Flutter
-keep class io.flutter.app.** { *; }
-keep class io.flutter.plugin.** { *; }
-keep class io.flutter.util.** { *; }
-keep class io.flutter.view.** { *; }
-keep class io.flutter.** { *; }
-keep class io.flutter.plugins.** { *; }
-dontwarn io.flutter.embedding.**

# Flutter'ın deferred-components desteği referans veriyor, kullanılmasa da R8 uyarı vermesin
-dontwarn com.google.android.play.core.**

# Google ML Kit text recognition — MainActivity.kt'teki native OCR köprüsü (com.fisbu/ocr) kullanıyor
-keep class com.google.mlkit.vision.text.** { *; }
-keep class com.google.mlkit.vision.common.** { *; }

# Firebase Messaging / Crashlytics
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
