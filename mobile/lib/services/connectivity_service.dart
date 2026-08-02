import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:connectivity_plus/connectivity_plus.dart';

/// Uygulama genelinde bağlantı durumunu izler. ThemeController ile aynı
/// ValueNotifier deseninde — ekranlar ValueListenableBuilder ile dinler.
class ConnectivityService {
  static final ValueNotifier<bool> isOnline = ValueNotifier<bool>(true);
  static StreamSubscription<List<ConnectivityResult>>? _subscription;
  static void Function()? _onReconnected;

  static Future<void> init({void Function()? onReconnected}) async {
    _onReconnected = onReconnected;

    final initial = await Connectivity().checkConnectivity();
    isOnline.value = _hasConnection(initial);

    _subscription?.cancel();
    _subscription = Connectivity().onConnectivityChanged.listen((results) {
      final wasOnline = isOnline.value;
      final nowOnline = _hasConnection(results);
      isOnline.value = nowOnline;
      if (!wasOnline && nowOnline) {
        _onReconnected?.call();
      }
    });
  }

  static bool _hasConnection(List<ConnectivityResult> results) {
    return results.any((r) => r != ConnectivityResult.none);
  }
}
