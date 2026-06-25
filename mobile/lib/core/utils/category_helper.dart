import 'package:flutter/material.dart';
import '../theme/app_colors.dart';

class CategoryHelper {
  CategoryHelper._();

  static Color getColor(String? categoryName) {
    if (categoryName == null) return AppColors.categoryDiger;
    switch (categoryName.toLowerCase()) {
      case 'market':
        return AppColors.categoryMarket;
      case 'giyim':
        return AppColors.categoryGiyim;
      case 'elektronik':
        return AppColors.categoryElektronik;
      case 'restoran':
        return AppColors.categoryRestoran;
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