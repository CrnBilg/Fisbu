# FişBu 🧾

> Kağıt fişlerinizi fotoğraflayın, OCR ile otomatik okutun, harcamalarınızı takip edin.

[![Flutter](https://img.shields.io/badge/Flutter-3.x-02569B?logo=flutter)](https://flutter.dev)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-336791?logo=postgresql)](https://www.postgresql.org)
[![Railway](https://img.shields.io/badge/Deploy-Railway-0B0D0E?logo=railway)](https://railway.app)

---

## 📱 Ekran Görüntüleri

> *(Ekran görüntüleri eklenecek)*

---

## 🎯 Problem

Cüzdanınızdaki onlarca kağıt fiş kaybolur, yıpranır ya da okunaksız hale gelir. Hangi ayda ne kadar harcadığınızı, hangi kategoriye ne kadar para gittiğini bilmiyorsunuz. **FişBu**, fişlerinizi telefon kamerasıyla tarayıp dijital ortamda saklamanızı ve harcamalarınızı analiz etmenizi sağlar.

---

## ✨ Özellikler

- 🔐 **Kayıt & Giriş** — E-posta/şifre ile güvenli kimlik doğrulama (JWT)
- 📷 **Fiş Tarama** — Kamera veya galeriden fotoğraf seç, OCR ile mağaza adı, tutar ve tarih otomatik okunur
- 📋 **Fiş Yönetimi** — Fişleri listele, detayına bak, sil
- 🗂️ **Kategori Yönetimi** — Özel kategoriler oluştur, düzenle, sil (Market, Giyim, Elektronik vb.)
- 📊 **İstatistikler** — Aylık harcama özeti, kategori bazlı bar grafikler
- 🌙 **Dark Mode** — Sistem temasıyla uyumlu, tercih kaydedilir
- 👤 **Profil** — Şifre değiştirme, kategori yönetimi, çıkış

---

## 🛠️ Teknoloji Stack

| Katman | Teknoloji |
|---|---|
| Mobil | Flutter (Dart) |
| Backend | Spring Boot (Java) |
| Veritabanı | PostgreSQL |
| Kimlik Doğrulama | JWT (JSON Web Token) |
| OCR | Apple Vision Framework (iOS) |
| Fotoğraf Depolama | Cloudinary |
| Sunucu | Railway |

---

## 🏗️ Mimari

```
┌─────────────────────┐
│   Flutter Mobil App │
│  (iOS / Android)    │
└────────┬────────────┘
         │ HTTP/REST + JWT
         ▼
┌─────────────────────┐       ┌──────────────┐
│  Spring Boot API    │──────▶│  PostgreSQL  │
│  (Railway)          │       │  (Veritabanı)│
└────────┬────────────┘       └──────────────┘
         │
         ▼
┌─────────────────────┐
│     Cloudinary      │
│  (Fotoğraf Depo)    │
└─────────────────────┘

Telefonda (Offline):
┌─────────────────────┐
│  Apple Vision OCR   │
│  (Metin Tanıma)     │
└─────────────────────┘
```

---

## 🗄️ Veritabanı Şeması

```
users
├── id (PK)
├── email (unique)
├── password (BCrypt)
└── created_at

receipts
├── id (PK)
├── user_id (FK → users)
├── category_id (FK → categories)
├── store_name
├── total_amount
├── receipt_date
├── image_url
├── raw_ocr_text
└── created_at

categories
├── id (PK)
├── user_id (FK → users)
├── name
└── color
```

---

## 📡 API Endpoint'leri

| Method | Endpoint | Açıklama |
|---|---|---|
| POST | `/auth/register` | Kullanıcı kaydı |
| POST | `/auth/login` | Giriş, JWT döner |
| POST | `/auth/change-password` | Şifre değiştirme |
| GET | `/receipts` | Fişleri listele |
| POST | `/receipts` | Yeni fiş ekle |
| GET | `/receipts/{id}` | Fiş detayı |
| DELETE | `/receipts/{id}` | Fiş sil |
| POST | `/receipts/upload` | Fotoğraf yükle |
| GET | `/categories` | Kategorileri listele |
| POST | `/categories` | Kategori ekle |
| PUT | `/categories/{id}` | Kategori düzenle |
| DELETE | `/categories/{id}` | Kategori sil |
| GET | `/statistics/monthly` | Aylık harcama özeti |

> Tüm `/receipts` ve `/categories` endpoint'leri `Authorization: Bearer <token>` gerektirir.

---

## 🚀 Kurulum

### Backend

```bash
# Repoyu klonla
git clone https://github.com/CrnBilg/Fisbu.git
cd Fisbu/backend

# application.properties dosyasını düzenle
# DB bağlantısı ve JWT secret'ını gir

# Çalıştır
./mvnw spring-boot:run
```

### Mobil (Flutter)

```bash
cd Fisbu/mobile

# Bağımlılıkları yükle
flutter pub get

# Çalıştır (bağlı cihaz veya emülatör gerekli)
flutter run

# APK derle
flutter build apk --release
```

> **Not:** iOS için Xcode ve Apple Developer hesabı gereklidir.

---

## 🔗 Canlı Backend

```
https://fisbu-production-613c.up.railway.app
```

---

## 👥 Ekip

| Rol | Kişi |
|---|---|
| Backend (Spring Boot, PostgreSQL, Railway) | [barishansu45](https://github.com/barishansu45) |
| Frontend (Flutter, OCR, UI/UX) | [CrnBilg](https://github.com/CrnBilg) |

---

## 📄 Lisans

MIT
