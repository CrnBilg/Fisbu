import 'package:flutter/material.dart';
import '../services/auth_service.dart';
import '../core/theme/app_colors.dart';

class RegisterScreen extends StatefulWidget {
  const RegisterScreen({super.key});

  @override
  State<RegisterScreen> createState() => _RegisterScreenState();
}

class _RegisterScreenState extends State<RegisterScreen> {
  final TextEditingController _emailController = TextEditingController();
  final TextEditingController _passwordController = TextEditingController();
  final TextEditingController _passwordAgainController =
      TextEditingController();
  bool _isLoading = false;
  bool _obscurePassword = true;
  bool _obscurePasswordAgain = true;

  Future<void> _handleRegister() async {
    final email = _emailController.text.trim();
    final password = _passwordController.text.trim();
    final passwordAgain = _passwordAgainController.text.trim();

    if (email.isEmpty || password.isEmpty || passwordAgain.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Lütfen tüm alanları doldur')),
      );
      return;
    }

    if (password != passwordAgain) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Şifreler aynı değil')),
      );
      return;
    }

    setState(() => _isLoading = true);
    final result = await AuthService.register(email, password);
    setState(() => _isLoading = false);

    if (!mounted) return;

    if (result.success) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
            content: Text('Kayıt başarılı! Giriş yapabilirsin.')),
      );
      Navigator.pop(context);
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
            content: Text(result.errorMessage ?? 'Kayıt başarısız')),
      );
    }
  }

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    _passwordAgainController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Scaffold(
      body: SafeArea(
        child: SingleChildScrollView(
          child: ConstrainedBox(
            constraints: BoxConstraints(
              minHeight: MediaQuery.of(context).size.height -
                  MediaQuery.of(context).padding.top -
                  MediaQuery.of(context).padding.bottom,
            ),
            child: IntrinsicHeight(
              child: Column(
                children: [
                  // Üst gradient alan
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.fromLTRB(28, 48, 28, 40),
                    decoration: BoxDecoration(
                      gradient: LinearGradient(
                        begin: Alignment.topLeft,
                        end: Alignment.bottomRight,
                        colors: isDark
                            ? [
                                AppColors.primaryDimDark,
                                AppColors.surfaceDark
                              ]
                            : [AppColors.primary, AppColors.primaryDark],
                      ),
                      borderRadius: const BorderRadius.only(
                        bottomLeft: Radius.circular(36),
                        bottomRight: Radius.circular(36),
                      ),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        // Geri butonu
                        GestureDetector(
                          onTap: () => Navigator.pop(context),
                          child: Container(
                            padding: const EdgeInsets.all(10),
                            decoration: BoxDecoration(
                              color: Colors.white.withOpacity(0.2),
                              borderRadius: BorderRadius.circular(12),
                            ),
                            child: const Icon(
                              Icons.arrow_back_rounded,
                              color: Colors.white,
                              size: 20,
                            ),
                          ),
                        ),
                        const SizedBox(height: 24),
                        const Text(
                          'Hesap\nOluştur ✨',
                          style: TextStyle(
                            color: Colors.white,
                            fontSize: 34,
                            fontWeight: FontWeight.w800,
                            height: 1.15,
                            letterSpacing: -0.8,
                          ),
                        ),
                        const SizedBox(height: 10),
                        Text(
                          'Fişlerini takip etmeye hemen başla',
                          style: TextStyle(
                            color: Colors.white.withOpacity(0.75),
                            fontSize: 15,
                            fontWeight: FontWeight.w400,
                          ),
                        ),
                      ],
                    ),
                  ),

                  // Form alanı
                  Expanded(
                    child: Padding(
                      padding: const EdgeInsets.fromLTRB(24, 32, 24, 24),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          // Email
                          TextField(
                            controller: _emailController,
                            keyboardType: TextInputType.emailAddress,
                            decoration: const InputDecoration(
                              labelText: 'E-posta',
                              prefixIcon: Icon(Icons.email_outlined),
                            ),
                          ),
                          const SizedBox(height: 16),

                          // Şifre
                          TextField(
                            controller: _passwordController,
                            obscureText: _obscurePassword,
                            decoration: InputDecoration(
                              labelText: 'Şifre',
                              prefixIcon:
                                  const Icon(Icons.lock_outline),
                              suffixIcon: IconButton(
                                icon: Icon(
                                  _obscurePassword
                                      ? Icons.visibility_outlined
                                      : Icons.visibility_off_outlined,
                                ),
                                onPressed: () => setState(() =>
                                    _obscurePassword = !_obscurePassword),
                              ),
                            ),
                          ),
                          const SizedBox(height: 16),

                          // Şifre tekrar
                          TextField(
                            controller: _passwordAgainController,
                            obscureText: _obscurePasswordAgain,
                            decoration: InputDecoration(
                              labelText: 'Şifre Tekrar',
                              prefixIcon:
                                  const Icon(Icons.lock_outline),
                              suffixIcon: IconButton(
                                icon: Icon(
                                  _obscurePasswordAgain
                                      ? Icons.visibility_outlined
                                      : Icons.visibility_off_outlined,
                                ),
                                onPressed: () => setState(() =>
                                    _obscurePasswordAgain =
                                        !_obscurePasswordAgain),
                              ),
                            ),
                          ),
                          const SizedBox(height: 28),

                          // Kayıt ol butonu
                          ElevatedButton(
                            onPressed: _isLoading ? null : _handleRegister,
                            child: _isLoading
                                ? const SizedBox(
                                    height: 20,
                                    width: 20,
                                    child: CircularProgressIndicator(
                                      strokeWidth: 2,
                                      color: Colors.white,
                                    ),
                                  )
                                : const Text('Kayıt Ol'),
                          ),
                          const SizedBox(height: 16),

                          // Giriş yap
                          Row(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              Text(
                                'Zaten hesabın var mı?',
                                style: TextStyle(
                                  color: isDark
                                      ? AppColors.textSecondaryDark
                                      : AppColors.textSecondary,
                                ),
                              ),
                              TextButton(
                                onPressed: () => Navigator.pop(context),
                                child: const Text('Giriş yap'),
                              ),
                            ],
                          ),

                          const Spacer(),

                          Center(
                            child: Text(
                              'FişBu © 2026',
                              style: TextStyle(
                                fontSize: 12,
                                color: isDark
                                    ? AppColors.textTertiaryDark
                                    : AppColors.textTertiary,
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}