import 'package:flutter/material.dart';
import 'package:hive_flutter/hive_flutter.dart';
import '../../services/connectivity_service.dart';
import '../../services/local_cache_service.dart';
import '../../services/pending_receipt_queue.dart';
import '../theme/app_colors.dart';

/// ConnectivityService.isOnline'ı dinleyip çevrimdışıyken ekranın üstünde
/// gösterilen basit bir uyarı şeridi. Parametresiz — her ekran kendi build'ine
/// tek satırla ekler (projenin "ortak shell yok, her ekran kendi kompoze eder" tarzıyla uyumlu).
class OfflineBanner extends StatelessWidget {
  const OfflineBanner({super.key});

  @override
  Widget build(BuildContext context) {
    return ValueListenableBuilder<bool>(
      valueListenable: ConnectivityService.isOnline,
      builder: (context, isOnline, _) {
        if (isOnline) return const SizedBox.shrink();
        return ValueListenableBuilder<Box>(
          valueListenable:
              Hive.box(LocalCacheService.pendingReceiptsBox).listenable(),
          builder: (context, box, _) {
            final pendingCount = PendingReceiptQueue.pendingCount;
            final message = pendingCount > 0
                ? 'Çevrimdışı mod — $pendingCount fiş bağlantı gelince gönderilecek.'
                : 'Çevrimdışı mod — son bilinen veriler gösteriliyor.';
            return Container(
              width: double.infinity,
              margin: const EdgeInsets.fromLTRB(20, 12, 20, 0),
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
              decoration: BoxDecoration(
                color: AppColors.warningDim,
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: AppColors.warning.withOpacity(0.3)),
              ),
              child: Row(
                children: [
                  const Icon(Icons.cloud_off_outlined, color: AppColors.warning, size: 18),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      message,
                      style: TextStyle(
                        fontSize: 12,
                        fontWeight: FontWeight.w600,
                        color: AppColors.warning,
                      ),
                    ),
                  ),
                ],
              ),
            );
          },
        );
      },
    );
  }
}
