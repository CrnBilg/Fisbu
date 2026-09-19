import 'package:flutter/material.dart';
import '../theme/app_colors.dart';

/// Bir ekran/bölüm veri çekerken gösterilen tutarlı yükleniyor göstergesi.
/// Referans: proje genelinde tekrarlanan `Center(child: CircularProgressIndicator(
/// color: AppColors.primary))` deseni.
class LoadingStateWidget extends StatelessWidget {
  const LoadingStateWidget({super.key});

  @override
  Widget build(BuildContext context) {
    return const Center(child: CircularProgressIndicator(color: AppColors.primary));
  }
}
