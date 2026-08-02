import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
    // Firebase Cloud Messaging (bütçe uyarı/aşım push bildirimleri)
    id("com.google.gms.google-services")
    // Firebase Crashlytics (Gün 15) — çökme raporları
    id("com.google.firebase.crashlytics")
}

// Yayın imzalama: gerçek keystore bilgileri android/key.properties dosyasından okunur
// (git'e girmez, bkz. .gitignore). Dosya yoksa (henüz keystore üretilmediyse) release
// build eskisi gibi debug key'iyle imzalanmaya devam eder — `flutter run --release`
// yerel geliştirmede kırılmaz, ama STORE'A BÖYLE YÜKLENEMEZ.
val keystorePropertiesFile = rootProject.file("key.properties")
val hasReleaseSigning = keystorePropertiesFile.exists()
val keystoreProperties = Properties()
if (hasReleaseSigning) {
    FileInputStream(keystorePropertiesFile).use { keystoreProperties.load(it) }
}

android {
    namespace = "com.fisbu.app"
    // flutter.compileSdkVersion (34) yetersiz — file_picker'ın transitive bağımlılığı
    // flutter_plugin_android_lifecycle, compileSdk 36+ istiyor (release build'de AAR
    // metadata kontrolüyle zorlanıyor, debug build'de fark edilmiyordu).
    compileSdk = 36
    ndkVersion = flutter.ndkVersion

    compileOptions {
        // flutter_local_notifications için gerekli (eski Android sürümlerinde java.time desteği)
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        applicationId = "com.fisbu.app"
        // You can update the following values to match your application needs.
        // For more information, see: https://flutter.dev/to/review-gradle-config.
        // local_auth (biyometrik giriş) minSdk 23 gerektiriyor
        minSdk = maxOf(flutter.minSdkVersion, 23)
        targetSdk = flutter.targetSdkVersion
        versionCode = flutter.versionCode
        versionName = flutter.versionName
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
            }
        }
    }

    buildTypes {
        release {
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                // key.properties henüz oluşturulmadı — bkz. android/key.properties.example
                signingConfigs.getByName("debug")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    implementation("com.google.mlkit:text-recognition:16.0.1")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")
}

flutter {
    source = "../.."
}
