import 'package:flutter/material.dart';

/// Uygulama genelinde tutarlı, hafif bir sayfa geçiş animasyonu — platform
/// varsayılanı (Android'de "sağdan kayarak gelme", iOS'ta zaten kendi doğal
/// geçişi) yerine, her iki platformda da aynı, yumuşak fade+slide hissi
/// verir. Tek bir yerden (AppTheme.pageTransitionsTheme) tüm
/// `Navigator.push(MaterialPageRoute(...))` çağrılarına otomatik uygulanır —
/// tek tek her çağrı noktasını değiştirmeye gerek yok.
class AppFadeSlidePageTransitionsBuilder extends PageTransitionsBuilder {
  const AppFadeSlidePageTransitionsBuilder();

  @override
  Widget buildTransitions<T>(
    PageRoute<T> route,
    BuildContext context,
    Animation<double> animation,
    Animation<double> secondaryAnimation,
    Widget child,
  ) {
    final curved = CurvedAnimation(parent: animation, curve: Curves.easeOutCubic);
    return FadeTransition(
      opacity: curved,
      child: SlideTransition(
        position: Tween<Offset>(
          begin: const Offset(0, 0.04),
          end: Offset.zero,
        ).animate(curved),
        child: child,
      ),
    );
  }
}
