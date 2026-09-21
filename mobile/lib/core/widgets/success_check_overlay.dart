import 'package:flutter/material.dart';
import '../theme/app_colors.dart';

/// Bir işlem (fiş kaydetme gibi) başarıyla tamamlandığında kısa süreli
/// gösterilen, ölçeklenerek beliren bir onay ikonu. Herhangi bir dış paket
/// (Lottie vb.) gerektirmez — `TweenAnimationBuilder` ile elastik bir
/// scale-in animasyonu üretir, ~650ms sonra kendiliğinden kapanır.
class SuccessCheckOverlay {
  /// Overlay'i gösterir ve otomatik kapanana kadar bekleyen bir Future döner.
  /// Çağıran, bu Future tamamlandıktan sonra (ör.) `Navigator.pop` çağırabilir.
  static Future<void> show(BuildContext context, {String? message}) async {
    final navigator = Navigator.of(context);
    await navigator.push(
      PageRouteBuilder(
        opaque: false,
        barrierColor: Colors.black.withValues(alpha: 0.25),
        transitionDuration: const Duration(milliseconds: 200),
        reverseTransitionDuration: const Duration(milliseconds: 150),
        pageBuilder: (context, animation, secondaryAnimation) {
          Future.delayed(const Duration(milliseconds: 650), () {
            if (navigator.canPop()) navigator.pop();
          });
          return FadeTransition(
            opacity: animation,
            child: _SuccessCheckContent(message: message),
          );
        },
      ),
    );
  }
}

class _SuccessCheckContent extends StatelessWidget {
  final String? message;

  const _SuccessCheckContent({this.message});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.transparent,
      body: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TweenAnimationBuilder<double>(
              tween: Tween(begin: 0, end: 1),
              duration: const Duration(milliseconds: 400),
              curve: Curves.elasticOut,
              builder: (context, scale, child) => Transform.scale(
                scale: scale,
                child: child,
              ),
              child: Container(
                width: 88,
                height: 88,
                decoration: const BoxDecoration(
                  color: AppColors.success,
                  shape: BoxShape.circle,
                ),
                child: const Icon(Icons.check_rounded, color: Colors.white, size: 48),
              ),
            ),
            if (message != null) ...[
              const SizedBox(height: 16),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
                decoration: BoxDecoration(
                  color: AppColors.surf(context),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Text(
                  message!,
                  style: TextStyle(fontWeight: FontWeight.w600, color: AppColors.txt(context)),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
