import 'package:flutter/material.dart';
import '../theme/app_colors.dart';

/// Bir liste/veri boş olduğunda tutarlı bir stilde ikon + başlık + (opsiyonel)
/// açıklama + (opsiyonel) CTA butonu gösterir. Referans: categories_screen.dart'taki
/// boş durum deseni (dairesel ikon arka planı + başlık + yönlendirici metin).
class EmptyStateWidget extends StatelessWidget {
  final IconData icon;
  final String title;
  final String? subtitle;
  final String? ctaLabel;
  final VoidCallback? onCtaPressed;

  const EmptyStateWidget({
    super.key,
    required this.icon,
    required this.title,
    this.subtitle,
    this.ctaLabel,
    this.onCtaPressed,
  });

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 32),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              padding: const EdgeInsets.all(24),
              decoration: BoxDecoration(
                color: AppColors.primDim(context),
                shape: BoxShape.circle,
              ),
              child: Icon(icon, size: 48, color: AppColors.primary),
            ),
            const SizedBox(height: 16),
            Text(
              title,
              textAlign: TextAlign.center,
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.w700,
                color: AppColors.txt(context),
              ),
            ),
            if (subtitle != null) ...[
              const SizedBox(height: 6),
              Text(
                subtitle!,
                textAlign: TextAlign.center,
                style: TextStyle(fontSize: 14, color: AppColors.txtSecondary(context)),
              ),
            ],
            if (ctaLabel != null && onCtaPressed != null) ...[
              const SizedBox(height: 16),
              ElevatedButton(onPressed: onCtaPressed, child: Text(ctaLabel!)),
            ],
          ],
        ),
      ),
    );
  }
}
