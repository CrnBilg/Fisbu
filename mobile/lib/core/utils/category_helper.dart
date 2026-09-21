import 'package:flutter/material.dart';
import '../theme/app_colors.dart';

class CategoryHelper {
  CategoryHelper._();

  static Color getColor(String? categoryName) {
    if (categoryName == null) return AppColors.categoryDiger;
    switch (categoryName.toLowerCase()) {
      case 'market':
        return AppColors.success;
      case 'giyim':
        return AppColors.categoryGiyim;
      case 'elektronik':
        return AppColors.primary;
      case 'restoran':
        return AppColors.warning;
      case 'ulaşım':
      case 'ulasim':
        return AppColors.categoryUlasim;
      default:
        return AppColors.categoryDiger;
    }
  }

  static IconData getIcon(String? categoryName) {
    if (categoryName == null) return Icons.label_outline;
    switch (categoryName.toLowerCase()) {
      case 'market':
        return Icons.shopping_cart_outlined;
      case 'giyim':
        return Icons.checkroom_outlined;
      case 'elektronik':
        return Icons.devices_outlined;
      case 'restoran':
        return Icons.restaurant_outlined;
      case 'ulaşım':
      case 'ulasim':
        return Icons.directions_bus_outlined;
      default:
        return Icons.label_outline;
    }
  }
}