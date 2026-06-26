import 'package:flutter/material.dart';

class AppColors {
  AppColors._();

  // Primary — Derin Violet (2025 trend)
  static const Color primary = Color(0xFF6366F1);        // Indigo-500
  static const Color primaryLight = Color(0xFF818CF8);   // Indigo-400
  static const Color primaryDark = Color(0xFF4F46E5);    // Indigo-600
  static const Color primaryDim = Color(0xFFEEF2FF);     // Indigo-50
  static const Color primaryDimDark = Color(0xFF1E1B4B); // Indigo-950

  // Secondary — Vibrant Cyan/Teal
  static const Color secondary = Color(0xFF06B6D4);      // Cyan-500
  static const Color secondaryDim = Color(0xFFECFEFF);   // Cyan-50
  static const Color secondaryDark = Color(0xFF0891B2);  // Cyan-600

  // Accent — Rose (trending 2025)
  static const Color accent = Color(0xFFF43F5E);         // Rose-500
  static const Color accentDim = Color(0xFFFFF1F2);      // Rose-50

  // Light Mode
  static const Color background = Color(0xFFF8FAFC);     // Slate-50
  static const Color surface = Color(0xFFFFFFFF);
  static const Color surface2 = Color(0xFFF1F5F9);       // Slate-100
  static const Color border = Color(0xFFE2E8F0);         // Slate-200
  static const Color borderLight = Color(0xFFF1F5F9);    // Slate-100

  // Dark Mode
  static const Color backgroundDark = Color(0xFF0F172A);  // Slate-900
  static const Color surfaceDark = Color(0xFF1E293B);     // Slate-800
  static const Color surface2Dark = Color(0xFF334155);    // Slate-700
  static const Color borderDark = Color(0xFF334155);      // Slate-700
  static const Color borderLightDark = Color(0xFF1E293B); // Slate-800

  // Text Light
  static const Color textPrimary = Color(0xFF0F172A);     // Slate-900
  static const Color textSecondary = Color(0xFF64748B);   // Slate-500
  static const Color textTertiary = Color(0xFF94A3B8);    // Slate-400

  // Text Dark
  static const Color textPrimaryDark = Color(0xFFF8FAFC);  // Slate-50
  static const Color textSecondaryDark = Color(0xFF94A3B8); // Slate-400
  static const Color textTertiaryDark = Color(0xFF64748B);  // Slate-500

  // Semantic
  static const Color error = Color(0xFFEF4444);           // Red-500
  static const Color errorDim = Color(0xFFFEF2F2);        // Red-50
  static const Color errorDimDark = Color(0xFF450A0A);
  static const Color success = Color(0xFF10B981);         // Emerald-500
  static const Color successDim = Color(0xFFECFDF5);      // Emerald-50
  static const Color warning = Color(0xFFF59E0B);         // Amber-500
  static const Color warningDim = Color(0xFFFFFBEB);      // Amber-50

  // Kategori renkleri (vibrant & modern)
  static const Color categoryMarket = Color(0xFF10B981);   // Emerald
  static const Color categoryGiyim = Color(0xFFF43F5E);    // Rose
  static const Color categoryElektronik = Color(0xFF6366F1); // Indigo
  static const Color categoryRestoran = Color(0xFFF59E0B);  // Amber
  static const Color categoryUlasim = Color(0xFF8B5CF6);    // Violet
  static const Color categoryDiger = Color(0xFF64748B); 
  // Context'ten tema rengini al
  static Color bg(BuildContext context) =>
      Theme.of(context).scaffoldBackgroundColor;
  
  static Color surf(BuildContext context) =>
      Theme.of(context).colorScheme.surface;
      
  static Color txt(BuildContext context) =>
      Theme.of(context).colorScheme.onSurface;
      
  static Color brd(BuildContext context) =>
      Theme.of(context).brightness == Brightness.dark
          ? AppColors.borderDark
          : AppColors.border;    
}