import 'package:flutter/material.dart';
import '../theme/app_colors.dart';

/// Auth akışının (login/register/forgot_password/reset_password) ortak
/// giriş alanı stili. Önceden her ekranda ayrı ayrı tanımlanan birebir
/// aynı `_buildInput` fonksiyonunun (bkz. UI/UX denetimi bulgu O4)
/// tek bir yerden yönetilen karşılığı.
class AuthTextField extends StatelessWidget {
  final TextEditingController controller;
  final String label;
  final IconData icon;
  final TextInputType? keyboardType;
  final bool obscureText;
  final Widget? suffixIcon;
  final String? errorText;

  const AuthTextField({
    super.key,
    required this.controller,
    required this.label,
    required this.icon,
    this.keyboardType,
    this.obscureText = false,
    this.suffixIcon,
    this.errorText,
  });

  @override
  Widget build(BuildContext context) {
    final hasError = errorText != null;
    return Container(
      decoration: BoxDecoration(
        color: AppColors.txt(context).withValues(alpha: 0.06),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(
          color: hasError
              ? AppColors.error.withValues(alpha: 0.6)
              : AppColors.txt(context).withValues(alpha: 0.1),
          width: hasError ? 1.5 : 1,
        ),
      ),
      child: TextField(
        controller: controller,
        keyboardType: keyboardType,
        obscureText: obscureText,
        style: TextStyle(
          color: AppColors.txt(context),
          fontSize: 15,
          fontWeight: FontWeight.w500,
        ),
        decoration: InputDecoration(
          labelText: label,
          labelStyle: TextStyle(
            color: AppColors.txtSecondary(context),
            fontSize: 14,
          ),
          prefixIcon: Icon(icon, color: AppColors.txtSecondary(context), size: 20),
          suffixIcon: suffixIcon,
          errorText: errorText,
          errorMaxLines: 2,
          border: InputBorder.none,
          enabledBorder: InputBorder.none,
          focusedBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(14),
            borderSide: BorderSide(
              color: AppColors.primary.withValues(alpha: 0.6),
              width: 1.5,
            ),
          ),
          contentPadding: const EdgeInsets.symmetric(
            horizontal: 16,
            vertical: 16,
          ),
          filled: false,
        ),
      ),
    );
  }
}
