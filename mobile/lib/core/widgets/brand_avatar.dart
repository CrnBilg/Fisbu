import 'package:flutter/material.dart';
import '../utils/brand_helper.dart';
import '../utils/category_helper.dart';

/// Fiş kartlarındaki avatarı çizer: BrandHelper'da eşleşen bir marka logosu
/// varsa VE dosya gerçekten assets/logos/ altında mevcutsa onu gösterir;
/// aksi halde (eşleşme yok ya da logo dosyası henüz eklenmemiş) mevcut
/// CategoryHelper ikonuna sorunsuz düşer — hiçbir zaman kırık görsel göstermez.
class BrandAvatar extends StatelessWidget {
  final String storeName;
  final String? categoryName;
  final Color backgroundColor;
  final Color iconColor;
  final double padding;
  final double iconSize;
  final double borderRadius;

  const BrandAvatar({
    super.key,
    required this.storeName,
    required this.categoryName,
    required this.backgroundColor,
    required this.iconColor,
    this.padding = 12,
    this.iconSize = 22,
    this.borderRadius = 12,
  });

  @override
  Widget build(BuildContext context) {
    final logoAsset = BrandHelper.getLogoAsset(storeName);

    return Container(
      padding: EdgeInsets.all(padding),
      decoration: BoxDecoration(
        color: backgroundColor,
        borderRadius: BorderRadius.circular(borderRadius),
      ),
      child: logoAsset != null
          ? Image.asset(
              logoAsset,
              width: iconSize,
              height: iconSize,
              fit: BoxFit.contain,
              errorBuilder: (context, error, stackTrace) => Icon(
                CategoryHelper.getIcon(categoryName),
                color: iconColor,
                size: iconSize,
              ),
            )
          : Icon(
              CategoryHelper.getIcon(categoryName),
              color: iconColor,
              size: iconSize,
            ),
    );
  }
}
