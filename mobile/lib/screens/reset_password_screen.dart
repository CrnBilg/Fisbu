import 'package:flutter/material.dart';
import '../services/auth_service.dart';
import '../core/theme/app_colors.dart';
import '../core/theme/app_typography.dart';
import '../core/widgets/code_input.dart';
import '../core/widgets/auth_text_field.dart';
import 'login_screen.dart';

class ResetPasswordScreen extends StatefulWidget {
  final String email;

  const ResetPasswordScreen({super.key, required this.email});

  @override
  State<ResetPasswordScreen> createState() => _ResetPasswordScreenState();
}

class _ResetPasswordScreenState extends State<ResetPasswordScreen> {
  final TextEditingController _newPasswordController = TextEditingController();
  final TextEditingController _confirmPasswordController =
      TextEditingController();
  String _code = '';
  bool _isLoading = false;
  bool _isResending = false;
  bool _obscurePassword = true;
  bool _obscureConfirmPassword = true;

  String? _newPasswordError;
  String? _confirmPasswordError;

  Future<void> _handleResend() async {
    setState(() => _isResending = true);
    final result = await AuthService.forgotPassword(widget.email);
    if (!mounted) return;
    setState(() => _isResending = false);
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(
          result.success
              ? 'Kod tekrar gönderildi'
              : (result.errorMessage ?? 'Kod gönderilemedi'),
        ),
      ),
    );
  }

  /// Şifre alanlarını tek seferde doğrular, hataları satır-içi gösterir.
  bool _validate() {
    final newPassword = _newPasswordController.text.trim();
    final confirmPassword = _confirmPasswordController.text.trim();

    String? newPasswordError;
    String? confirmPasswordError;

    if (newPassword.isEmpty) {
      newPasswordError = 'Yeni şifreni gir';
    } else if (newPassword.length < 8) {
      newPasswordError = 'Şifre en az 8 karakter olmalı';
    } else if (!newPassword.contains(RegExp(r'\d'))) {
      newPasswordError = 'Şifre en az bir rakam içermeli';
    } else if (!newPassword.contains(RegExp(r'[^a-zA-Z0-9]'))) {
      newPasswordError = 'Şifre en az bir özel karakter içermeli';
    }

    if (confirmPassword.isEmpty) {
      confirmPasswordError = 'Şifreyi tekrar gir';
    } else if (newPassword != confirmPassword) {
      confirmPasswordError = 'Şifreler aynı değil';
    }

    setState(() {
      _newPasswordError = newPasswordError;
      _confirmPasswordError = confirmPasswordError;
    });

    return newPasswordError == null && confirmPasswordError == null;
  }

  Future<void> _handleReset() async {
    final newPassword = _newPasswordController.text.trim();

    if (_code.length != 6) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Lütfen 6 haneli kodu tam gir')),
      );
      return;
    }

    if (!_validate()) return;

    setState(() => _isLoading = true);
    final result = await AuthService.resetPassword(
      widget.email,
      _code,
      newPassword,
    );
    setState(() => _isLoading = false);

    if (!mounted) return;

    if (result.success) {
      Navigator.pushAndRemoveUntil(
        context,
        MaterialPageRoute(builder: (context) => const LoginScreen()),
        (route) => false,
      );
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Şifren güncellendi, giriş yapabilirsin')),
      );
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(result.errorMessage ?? 'Şifre sıfırlanamadı')),
      );
    }
  }

  @override
  void dispose() {
    _newPasswordController.dispose();
    _confirmPasswordController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    // Bazı fiziksel cihazlarda klavyenin odak kaybında düzgün kapanmadığı
    // gözlemlendi (issue #48, tekrar üretilemedi) — dışarı dokununca klavyeyi
    // kapatmak ucuz, zararsız bir sağlamlaştırma
    return GestureDetector(
      onTap: () => FocusScope.of(context).unfocus(),
      behavior: HitTestBehavior.opaque,
      child: Scaffold(
      body: Container(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: Theme.of(context).brightness == Brightness.dark
                ? [AppColors.backgroundDark, AppColors.surfaceDark]
                : [AppColors.background, AppColors.surface],
          ),
        ),
        child: SafeArea(
          child: SingleChildScrollView(
            padding: const EdgeInsets.symmetric(horizontal: 28),
            child: ConstrainedBox(
              constraints: BoxConstraints(
                minHeight:
                    MediaQuery.of(context).size.height -
                    MediaQuery.of(context).padding.top -
                    MediaQuery.of(context).padding.bottom,
              ),
              child: IntrinsicHeight(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const SizedBox(height: 48),

                    GestureDetector(
                      onTap: () => Navigator.pop(context),
                      child: Container(
                        padding: const EdgeInsets.all(10),
                        decoration: BoxDecoration(
                          color: AppColors.txt(context).withValues(alpha: 0.08),
                          borderRadius: BorderRadius.circular(12),
                          border: Border.all(
                            color: AppColors.txt(context).withValues(alpha: 0.1),
                          ),
                        ),
                        child: Icon(
                          Icons.arrow_back_rounded,
                          color: AppColors.txt(context),
                          size: 20,
                        ),
                      ),
                    ),

                    const SizedBox(height: 36),

                    Text(
                      'Şifreni\nSıfırla',
                      style: AppTypography.display.copyWith(color: AppColors.txt(context)),
                    ),
                    const SizedBox(height: 10),
                    Text(
                      '${widget.email} adresine gönderilen 6 haneli kodu ve yeni şifreni gir',
                      style: TextStyle(
                        color: AppColors.txtSecondary(context),
                        fontSize: 15,
                        fontWeight: FontWeight.w400,
                      ),
                    ),

                    const SizedBox(height: 32),

                    CodeInput(onChanged: (code) => _code = code),

                    const SizedBox(height: 10),

                    Align(
                      alignment: Alignment.centerRight,
                      child: TextButton(
                        onPressed: _isResending ? null : _handleResend,
                        child: Text(
                          _isResending
                              ? 'Gönderiliyor...'
                              : 'Kodu tekrar gönder',
                          style: const TextStyle(
                            color: AppColors.primaryLight,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                    ),

                    const SizedBox(height: 10),

                    AuthTextField(
                      controller: _newPasswordController,
                      label: 'Yeni Şifre',
                      icon: Icons.lock_outline,
                      obscureText: _obscurePassword,
                      errorText: _newPasswordError,
                      suffixIcon: IconButton(
                        icon: Icon(
                          _obscurePassword
                              ? Icons.visibility_outlined
                              : Icons.visibility_off_outlined,
                          color: AppColors.txtSecondary(context),
                          size: 20,
                        ),
                        onPressed: () => setState(
                          () => _obscurePassword = !_obscurePassword,
                        ),
                      ),
                    ),
                    const SizedBox(height: 14),

                    AuthTextField(
                      controller: _confirmPasswordController,
                      label: 'Yeni Şifre Tekrar',
                      icon: Icons.lock_outline,
                      obscureText: _obscureConfirmPassword,
                      errorText: _confirmPasswordError,
                      suffixIcon: IconButton(
                        icon: Icon(
                          _obscureConfirmPassword
                              ? Icons.visibility_outlined
                              : Icons.visibility_off_outlined,
                          color: AppColors.txtSecondary(context),
                          size: 20,
                        ),
                        onPressed: () => setState(
                          () => _obscureConfirmPassword =
                              !_obscureConfirmPassword,
                        ),
                      ),
                    ),

                    const SizedBox(height: 32),

                    GestureDetector(
                      onTap: _isLoading ? null : _handleReset,
                      child: Container(
                        width: double.infinity,
                        padding: const EdgeInsets.symmetric(vertical: 18),
                        decoration: BoxDecoration(
                          gradient: const LinearGradient(
                            begin: Alignment.centerLeft,
                            end: Alignment.centerRight,
                            colors: [AppColors.primary, AppColors.primaryLight],
                          ),
                          borderRadius: BorderRadius.circular(16),
                          boxShadow: [
                            BoxShadow(
                              color: AppColors.primary.withValues(alpha: 0.5),
                              blurRadius: 24,
                              offset: const Offset(0, 8),
                            ),
                          ],
                        ),
                        child: Center(
                          child: _isLoading
                              ? const SizedBox(
                                  height: 22,
                                  width: 22,
                                  child: CircularProgressIndicator(
                                    strokeWidth: 2.5,
                                    color: Colors.white,
                                  ),
                                )
                              : const Text(
                                  'Şifreyi Sıfırla',
                                  style: TextStyle(
                                    color: Colors.white,
                                    fontSize: 16,
                                    fontWeight: FontWeight.w700,
                                    letterSpacing: 0.2,
                                  ),
                                ),
                        ),
                      ),
                    ),

                    const SizedBox(height: 24),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
      ),
    );
  }
}
