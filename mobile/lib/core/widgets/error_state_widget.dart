import 'package:flutter/material.dart';
import '../theme/app_colors.dart';
import '../utils/network_error.dart';

/// Bir yükleme/işlem hatasını tutarlı bir stilde gösterir: ikon + Türkçe hata
/// mesajı + "Tekrar Dene" butonu. Referans: budget_screen.dart/spending_calendar_screen.dart'taki
/// (tekrarlanan) hata durumu deseni.
///
/// KRİTİK: `error` parametresi HAM exception nesnesini alır, zaten çevrilmiş bir
/// String DEĞİL — çeviri (`NetworkError.friendlyMessage`) widget'ın İÇİNDE yapılır.
/// Bu bilinçli bir API kararı: proje genelinde tekrarlayan bir hata (MOB-006/
/// UI-UX denetimi) `catch (e) { Text('Hata: $e') }` gibi ham exception'ın doğrudan
/// kullanıcıya gösterilmesiydi. Bu widget'ın imzası, bir çağıranın yanlışlıkla
/// çevrilmemiş bir mesaj geçirmesini YAPISAL olarak engelliyor — string kabul eden
/// bir "message" parametresi bilerek YOK.
class ErrorStateWidget extends StatelessWidget {
  final Object error;
  final VoidCallback onRetry;
  final String fallback;
  final IconData icon;

  const ErrorStateWidget({
    super.key,
    required this.error,
    required this.onRetry,
    this.fallback = 'Veri alınamadı, lütfen tekrar deneyin.',
    this.icon = Icons.cloud_off_outlined,
  });

  @override
  Widget build(BuildContext context) {
    final message = NetworkError.friendlyMessage(error, fallback: fallback);
    return Center(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 32),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(icon, size: 48, color: AppColors.error),
            const SizedBox(height: 16),
            Text(
              message,
              textAlign: TextAlign.center,
              style: TextStyle(fontSize: 14, color: AppColors.txtSecondary(context)),
            ),
            const SizedBox(height: 16),
            ElevatedButton(onPressed: onRetry, child: const Text('Tekrar Dene')),
          ],
        ),
      ),
    );
  }
}
