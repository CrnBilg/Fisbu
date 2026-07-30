import 'dart:convert';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:http/http.dart' as http;
import 'auth_service.dart';

/// Uygulama arka plandayken gelen mesajları işleyen üst düzey fonksiyon.
/// FCM "notification" payload'ı taşıdığında sistem bildirimini kendisi gösterir,
/// bu yüzden burada ekstra bir şey yapmaya gerek yok; yine de kayıtlı olması gerekir.
@pragma('vm:entry-point')
Future<void> _firebaseBackgroundHandler(RemoteMessage message) async {
  // Arka planda ek işlem gerekmiyor (bildirim sistem tarafından gösteriliyor).
}

/// Bütçe uyarı/aşım push bildirimlerini yöneten servis.
/// - FCM token alır ve backend'e kaydeder
/// - Bildirim izni ister (iOS + Android 13+)
/// - Ön planda gelen bildirimleri yerel bildirim olarak gösterir
class PushNotificationService {
  static const String _baseUrl = 'https://fisbu-production-613c.up.railway.app';

  static final FlutterLocalNotificationsPlugin _localNotifications =
      FlutterLocalNotificationsPlugin();

  static const AndroidNotificationChannel _budgetChannel =
      AndroidNotificationChannel(
    'budget_alerts',
    'Bütçe Bildirimleri',
    description: 'Bütçe uyarı ve aşım bildirimleri',
    importance: Importance.high,
  );

  static bool _initialized = false;

  /// Uygulama açılışında bir kez çağrılır. Firebase.initializeApp() bundan
  /// önce main() içinde çağrılmış olmalıdır.
  static Future<void> init() async {
    if (_initialized) return;
    _initialized = true;

    FirebaseMessaging.onBackgroundMessage(_firebaseBackgroundHandler);

    await _setupLocalNotifications();
    await _requestPermission();

    // Ön planda gelen mesajları yerel bildirim olarak göster
    FirebaseMessaging.onMessage.listen(_showForegroundNotification);

    // Token yenilenince backend'e tekrar kaydet
    FirebaseMessaging.instance.onTokenRefresh.listen(_sendTokenToBackend);
  }

  static Future<void> _setupLocalNotifications() async {
    const androidInit =
        AndroidInitializationSettings('@mipmap/ic_launcher');
    const iosInit = DarwinInitializationSettings();
    await _localNotifications.initialize(
      settings:
          const InitializationSettings(android: androidInit, iOS: iosInit),
    );

    await _localNotifications
        .resolvePlatformSpecificImplementation<
            AndroidFlutterLocalNotificationsPlugin>()
        ?.createNotificationChannel(_budgetChannel);
  }

  static Future<void> _requestPermission() async {
    // iOS izin diyaloğu
    await FirebaseMessaging.instance.requestPermission(
      alert: true,
      badge: true,
      sound: true,
    );

    // Android 13+ runtime bildirim izni
    await _localNotifications
        .resolvePlatformSpecificImplementation<
            AndroidFlutterLocalNotificationsPlugin>()
        ?.requestNotificationsPermission();
  }

  static void _showForegroundNotification(RemoteMessage message) {
    final notification = message.notification;
    if (notification == null) return;

    _localNotifications.show(
      id: notification.hashCode,
      title: notification.title,
      body: notification.body,
      notificationDetails: NotificationDetails(
        android: AndroidNotificationDetails(
          _budgetChannel.id,
          _budgetChannel.name,
          channelDescription: _budgetChannel.description,
          importance: Importance.high,
          priority: Priority.high,
          icon: '@mipmap/ic_launcher',
        ),
        iOS: const DarwinNotificationDetails(),
      ),
    );
  }

  /// Giriş yaptıktan sonra çağrılır: mevcut FCM token'ı backend'e kaydeder.
  static Future<void> registerToken() async {
    final token = await FirebaseMessaging.instance.getToken();
    if (token != null) {
      await _sendTokenToBackend(token);
    }
  }

  static Future<void> _sendTokenToBackend(String fcmToken) async {
    final jwt = await AuthService.getToken();
    if (jwt == null) return; // Giriş yapılmamışsa kaydetme

    try {
      await http.post(
        Uri.parse('$_baseUrl/users/fcm-token'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $jwt',
        },
        body: jsonEncode({'fcmToken': fcmToken}),
      );
    } catch (_) {
      // Token kaydı başarısız olsa da uygulama akışı bozulmamalı
    }
  }
}
