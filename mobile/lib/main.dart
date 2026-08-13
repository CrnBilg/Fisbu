import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_crashlytics/firebase_crashlytics.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'core/theme/app_theme.dart';
import 'screens/auth_wrapper.dart';
import 'services/push_notification_service.dart';
import 'services/local_cache_service.dart';
import 'services/connectivity_service.dart';
import 'services/pending_receipt_queue.dart';

final RegExp _emailPattern = RegExp(r'[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}');
final RegExp _jwtPattern = RegExp(r'eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+');

/// Crashlytics'e gitmeden önce hata mesajındaki e-posta/JWT gibi olası hassas
/// parçaları maskeler — yakalanan exception'lar sunucu yanıt gövdesi içerebilir
/// (bkz. auth_service.dart'taki 'Bağlantı hatası: $e' deseni), crash raporları
/// bunları düz metin taşımasın diye
Object _sanitizeForCrashlytics(Object error) {
  final message = error.toString();
  final sanitized = message
      .replaceAll(_emailPattern, '[email]')
      .replaceAll(_jwtPattern, '[jwt]');
  return sanitized == message ? error : Exception(sanitized);
}

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // table_calendar gibi intl DateFormat kullanan paketler locale verisi
  // başlatılmadan çağrılırsa LocaleDataException fırlatır
  await initializeDateFormatting('tr_TR', null);

  // Firebase + push bildirim altyapısı (native config dosyalarından okur)
  try {
    await Firebase.initializeApp();
    await PushNotificationService.init();

    // Crashlytics: debug modda toplama kapalı, sadece release'de gerçek kullanıcı çökmeleri raporlanır
    await FirebaseCrashlytics.instance.setCrashlyticsCollectionEnabled(!kDebugMode);
    FlutterError.onError = (FlutterErrorDetails details) {
      FirebaseCrashlytics.instance.recordFlutterFatalError(FlutterErrorDetails(
        exception: _sanitizeForCrashlytics(details.exception),
        stack: details.stack,
        library: details.library,
        context: details.context,
      ));
    };
    PlatformDispatcher.instance.onError = (error, stack) {
      FirebaseCrashlytics.instance.recordError(_sanitizeForCrashlytics(error), stack, fatal: true);
      return true;
    };
  } catch (_) {
    // Firebase başlatılamazsa (ör. config eksik) uygulama yine de açılmalı
  }

  // Çevrimdışı cache + bekleyen fiş kuyruğu altyapısı — biri başarısız olsa da uygulama açılmalı
  try {
    await LocalCacheService.init();
    await ConnectivityService.init(onReconnected: () => PendingReceiptQueue.flush());
    // Uygulama açılışında zaten çevrimiçiysek bekleyen fişleri hemen göndermeyi dene
    if (ConnectivityService.isOnline.value) {
      PendingReceiptQueue.flush();
    }
  } catch (_) {
    // Offline altyapısı başlatılamazsa uygulama yine de (online modda) çalışmaya devam etmeli
  }

  final prefs = await SharedPreferences.getInstance();
  final isDark = prefs.getBool('isDarkMode') ?? false;
  SystemChrome.setSystemUIOverlayStyle(
    const SystemUiOverlayStyle(
      statusBarColor: Colors.transparent,
    ),
  );
  runApp(MyApp(initialDarkMode: isDark));
}

class ThemeController extends ValueNotifier<ThemeMode> {
  ThemeController(bool initialDarkMode)
      : super(initialDarkMode ? ThemeMode.dark : ThemeMode.light);

  bool get isDarkMode => value == ThemeMode.dark;

  Future<void> toggleTheme() async {
    value = isDarkMode ? ThemeMode.light : ThemeMode.dark;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('isDarkMode', isDarkMode);
  }
}

class _ThemeControllerScope extends InheritedNotifier<ThemeController> {
  const _ThemeControllerScope({
    required ThemeController controller,
    required super.child,
  }) : super(notifier: controller);
}

class MyApp extends StatefulWidget {
  final bool initialDarkMode;
  const MyApp({super.key, required this.initialDarkMode});

  static ThemeController? of(BuildContext context) =>
      context
          .dependOnInheritedWidgetOfExactType<_ThemeControllerScope>()
          ?.notifier;

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  late final ThemeController _themeController;

  @override
  void initState() {
    super.initState();
    _themeController = ThemeController(widget.initialDarkMode);
  }

  @override
  void dispose() {
    _themeController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return _ThemeControllerScope(
      controller: _themeController,
      child: ValueListenableBuilder<ThemeMode>(
        valueListenable: _themeController,
        builder: (context, themeMode, child) {
          return MaterialApp(
            title: 'FişBu',
            debugShowCheckedModeBanner: false,
            theme: AppTheme.light,
            darkTheme: AppTheme.dark.copyWith(
              brightness: Brightness.dark,
              scaffoldBackgroundColor: const Color(0xFF1A1A2E),
            ),
            themeMode: themeMode,
            home: const AuthWrapper(),
          );
        },
      ),
    );
  }
}
