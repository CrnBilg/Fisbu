**Durum**: Done (araştırma — düzeltme YAPILMADI, kullanıcı talebiyle sadece rapor)
**Tahmin**: -

## Kaynak
E2E-001 sırasında (mobil `integration_test` E2E'sini yazıp gerçek bir Android emulator'de çalıştırırken), kullanıcının bildirdiği "debug modda kasma/donma" şikayetini araştırma talebi.

## Bulgu — kanıtlanmış kök neden

**`mobile/lib/main.dart`**, `runApp()`'ı çağırmadan ÖNCE şu zinciri senkron olarak `await` ediyor:
```dart
await Firebase.initializeApp();
await PushNotificationService.init();   // <-- burada bloklanıyor
```

`PushNotificationService.init()` (`mobile/lib/services/push_notification_service.dart:41,65-77`) şunu çağırıyor:
```dart
static Future<void> _requestPermission() async {
    await FirebaseMessaging.instance.requestPermission(alert: true, badge: true, sound: true);
    await _localNotifications...?.requestNotificationsPermission();  // Android 13+ runtime izni
}
```

**Sonuç**: Uygulamanın Dart `main()` isolate'i, native Android "Bildirim izni" diyaloğu (`GrantPermissionsActivity`) kullanıcı tarafından kapatılana kadar bloklanıyor — ve bu diyalog kapatılmadan **`runApp()` hiç çağrılmıyor, yani hiçbir Flutter widget'ı (splash, login ekranı, hiçbir şey) render edilmiyor**. Kullanıcı bu süre boyunca native launcher splash'i (veya boş/donmuş bir ekran) görür.

### Kanıt (bu E2E denemesinde birebir yakalandı)
Otomatik testte izin ÖNCEDEN verilmediğinde, `app.main()` çağrıldıktan sonra **33+ saniye** boyunca ekranda hiçbir Flutter widget'ı görünmedi (sadece `integration_test` paketinin "Test starting..." placeholder'ı). `adb logcat` ile yakalanan gerçek neden:
```
topActivity=ComponentInfo{com.google.android.permissioncontroller/
  com.android.permissioncontroller.permission.ui.GrantPermissionsActivity}
```
— yani ön plandaki aktivite bizim uygulamamız değil, native izin diyaloğuydu. Bu diyalog Flutter widget ağacının PARÇASI OLMADIĞI için:
1. Gerçek bir kullanıcı bunu görüp Allow/Deny'a dokunabilir (donma kalıcı değil, ama runApp()'a kadar HİÇBİR UI göstermez — kullanıcı bu süre boyunca "uygulama açılmıyor/donmuş" hissi yaşayabilir, özellikle diyalog render'ı geciken cihazlarda).
2. Otomasyon (bizim E2E testimiz gibi) hiçbir şey render edilmediği için `pump`/`pumpAndSettle` ile bunu ASLA geçemez — izin `adb shell pm grant` ile önceden verilene kadar test süresiz "donmuş" kaldı.

İzin önceden verildiğinde (`adb shell pm grant com.fisbu.app android.permission.POST_NOTIFICATIONS`), akış normal ilerledi — login ekranı hemen göründü, ama login adımının kendisi de **24.36 saniye** sürdü (bkz. Ek Bulgu).

## Kök neden değerlendirmesi: backend gecikmesi mi, mobil rebuild sorunu mu?
**Ne ikisi de değil — bu üçüncü bir kategori: senkron/bloklayıcı bir native izin isteği, uygulamanın soğuk başlangıç (`main()`) zincirinde.** Backend'in kendisi hızlı yanıt veriyordu (bkz. backend E2E: tüm adımlar <300ms). Bu, "mobil rebuild"/hot-reload ile de ilgisiz — release/debug fark etmeden HER soğuk başlangıçta (özellikle bildirim izni ilk kez isteniyorsa) tetiklenen bir mimari desen.

## Ek bulgu (aynı araştırmada, ayrı not — düzeltilmedi)
Login adımı (izin verildikten sonra) **24.36 saniye** sürdü — bu da beklenenden çok yüksek. Bu, ayrı bir araştırma gerektiriyor (muhtemelen debug modda ilk soğuk başlangıçta JIT derleme + Firebase/Crashlytics'in arka planda hâlâ ağ denemesi yapması — logcat'te `Unable to resolve host "firebase-settings.crashlytics.com"` hataları görüldü, bu emulator'ün DNS/network kısıtından kaynaklanıyor olabilir, gerçek cihazda farklı davranabilir). Bu rapor kapsamında kök nedeni netleştirilmedi, ayrı bir araştırma önerilir.

## Önerilen sonraki adım (uygulanmadı — kullanıcı kararına bırakıldı)
`PushNotificationService.init()` çağrısını (özellikle `_requestPermission()`'ı) `main()`'in bloklayan zincirinden çıkarıp, `runApp()`'dan SONRA (örn. Dashboard ilk render olduktan sonra, `WidgetsBinding.instance.addPostFrameCallback` ile) tetiklemek — böylece ilk Flutter frame'i izin diyaloğunun sonucunu beklemeden hemen render edilir.

## Yazılan/değiştirilen dosyalar
- **Hiçbir production kodu değiştirilmedi** (kullanıcı talebi: "henüz düzeltme, önce raporla").
- `mobile/integration_test/receipt_to_budget_flow_test.dart`: bu bulguyu tetikleyen ve teşhis eden test (bkz. E2E-001).

## Öğrenilen Dersler
- Bir mobil E2E testinin "neden geçmiyor" sorusu, bazen testin kendi hatası değil, gerçek bir production davranışının doğru şekilde yakalanmasıdır — burada tam olarak bu oldu.
- `adb logcat` + `pm grant`, Flutter widget testleri native Android UI (izin diyaloğu gibi) ile karşılaştığında görünürlük kazandırmanın standart yolu — `pump`/`pumpAndSettle` sadece Flutter frame'lerini görür, native activity geçişlerini göremez.
- Bu bulgu, "debug modda kasma" şikayetinin en azından bir kısmı için somut, kod-satırı seviyesinde bir açıklama sağladı — kullanıcının orijinal şikayetinin TAMAMEN bu mu olduğu kesin değil (ek bulgu — 24s login gecikmesi — ayrı ve henüz açıklanmamış), ama bu gerçek ve düzeltilebilir bir mimari sorun.
