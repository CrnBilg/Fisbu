import 'package:flutter/material.dart';
import '../services/auth_service.dart';
import '../core/theme/app_colors.dart';
import '../core/widgets/auth_text_field.dart';
import 'verify_email_screen.dart';

class RegisterScreen extends StatefulWidget {
  const RegisterScreen({super.key});

  @override
  State<RegisterScreen> createState() => _RegisterScreenState();
}

class _RegisterScreenState extends State<RegisterScreen> {
  static final RegExp _emailRegex = RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$');

  final TextEditingController _nameController = TextEditingController();
  final TextEditingController _emailController = TextEditingController();
  final TextEditingController _passwordController = TextEditingController();
  final TextEditingController _passwordAgainController =
      TextEditingController();
  bool _isLoading = false;
  bool _obscurePassword = true;
  bool _obscurePasswordAgain = true;
  bool _kvkkAccepted = false;

  String? _nameError;
  String? _emailError;
  String? _passwordError;
  String? _passwordAgainError;

  void _showKvkkSheet() {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: AppColors.surf(context),
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (sheetContext) => Padding(
        padding: EdgeInsets.fromLTRB(
          24,
          20,
          24,
          MediaQuery.of(sheetContext).viewInsets.bottom + 24,
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Center(
              child: Container(
                width: 40,
                height: 4,
                decoration: BoxDecoration(
                  color: AppColors.txt(sheetContext).withValues(alpha: 0.2),
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
            ),
            const SizedBox(height: 20),
            Text(
              'KVKK Aydınlatma Metni',
              style: TextStyle(
                color: AppColors.txt(sheetContext),
                fontSize: 18,
                fontWeight: FontWeight.w700,
              ),
            ),
            const SizedBox(height: 14),
            ConstrainedBox(
              constraints: BoxConstraints(
                maxHeight: MediaQuery.of(sheetContext).size.height * 0.5,
              ),
              child: SingleChildScrollView(
                child: Text(
                  '6698 sayılı Kişisel Verilerin Korunması Kanunu (KVKK) kapsamında, '
                  'FişBu uygulamasına kaydolurken e-posta adresin ve şifren şifreli olarak saklanır. '
                  'Uygulamaya yüklediğin fiş görselleri ve bu görsellerden çıkarılan harcama verileri '
                  '(tutar, tarih, kategori) yalnızca senin harcama takibini yapabilmen amacıyla işlenir '
                  've üçüncü taraflarla paylaşılmaz. Verilerinin silinmesini istediğinde profil ekranından '
                  'hesabını ve tüm verilerini kalıcı olarak silebilirsin. Kaydolarak bu şartları kabul etmiş olursun.',
                  style: TextStyle(
                    color: AppColors.txtSecondary(sheetContext),
                    fontSize: 14,
                    height: 1.5,
                  ),
                ),
              ),
            ),
            const SizedBox(height: 20),
            SizedBox(
              width: double.infinity,
              child: TextButton(
                onPressed: () => Navigator.pop(sheetContext),
                style: TextButton.styleFrom(
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  backgroundColor: AppColors.txt(sheetContext).withValues(alpha: 0.08),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(14),
                  ),
                ),
                child: Text(
                  'Kapat',
                  style: TextStyle(
                    color: AppColors.txt(sheetContext),
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// Tüm alanları tek seferde doğrular, hataları satır-içi gösterir.
  /// Sıralı SnackBar'lar yerine kullanıcı tüm sorunları aynı anda görür.
  bool _validate() {
    final name = _nameController.text.trim();
    final email = _emailController.text.trim();
    final password = _passwordController.text.trim();
    final passwordAgain = _passwordAgainController.text.trim();

    String? nameError;
    String? emailError;
    String? passwordError;
    String? passwordAgainError;

    if (name.isEmpty) {
      nameError = 'Ad soyad gerekli';
    }

    if (email.isEmpty) {
      emailError = 'E-posta gerekli';
    } else if (!_emailRegex.hasMatch(email)) {
      emailError = 'Geçerli bir e-posta adresi gir';
    }

    if (password.isEmpty) {
      passwordError = 'Şifre gerekli';
    } else if (password.length < 8) {
      passwordError = 'Şifre en az 8 karakter olmalı';
    } else if (!password.contains(RegExp(r'\d'))) {
      passwordError = 'Şifre en az bir rakam içermeli';
    } else if (!password.contains(RegExp(r'[^a-zA-Z0-9]'))) {
      passwordError = 'Şifre en az bir özel karakter içermeli';
    }

    if (passwordAgain.isEmpty) {
      passwordAgainError = 'Şifreyi tekrar gir';
    } else if (password != passwordAgain) {
      passwordAgainError = 'Şifreler aynı değil';
    }

    setState(() {
      _nameError = nameError;
      _emailError = emailError;
      _passwordError = passwordError;
      _passwordAgainError = passwordAgainError;
    });

    return nameError == null &&
        emailError == null &&
        passwordError == null &&
        passwordAgainError == null;
  }

  Future<void> _handleRegister() async {
    final email = _emailController.text.trim();
    final password = _passwordController.text.trim();
    final name = _nameController.text.trim();

    if (!_validate()) return;

    if (!_kvkkAccepted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text(
            'Devam etmek için KVKK Aydınlatma Metni\'ni onaylamalısın',
          ),
        ),
      );
      return;
    }

    setState(() => _isLoading = true);
    final result = await AuthService.register(email, password, name: name);
    setState(() => _isLoading = false);

    if (!mounted) return;

    if (result.success) {
      Navigator.pushReplacement(
        context,
        MaterialPageRoute(
          builder: (context) => VerifyEmailScreen(email: email),
        ),
      );
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(result.errorMessage ?? 'Kayıt başarısız')),
      );
    }
  }

  @override
  void dispose() {
    _nameController.dispose();
    _emailController.dispose();
    _passwordController.dispose();
    _passwordAgainController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
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

                    // Geri butonu
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

                    // Başlık
                    Text(
                      'Hesap\nOluştur',
                      style: TextStyle(
                        color: AppColors.txt(context),
                        fontSize: 40,
                        fontWeight: FontWeight.w800,
                        height: 1.1,
                        letterSpacing: -1.0,
                      ),
                    ),
                    const SizedBox(height: 10),
                    Text(
                      'Fişlerini takip etmeye hemen başla ✨',
                      style: TextStyle(
                        color: AppColors.txtSecondary(context),
                        fontSize: 15,
                        fontWeight: FontWeight.w400,
                      ),
                    ),

                    const SizedBox(height: 40),

                    // Ad Soyad
                    AuthTextField(
                      controller: _nameController,
                      label: 'Ad Soyad',
                      icon: Icons.person_outline,
                      errorText: _nameError,
                    ),
                    const SizedBox(height: 14),

                    // Email
                    AuthTextField(
                      controller: _emailController,
                      label: 'E-posta',
                      icon: Icons.email_outlined,
                      keyboardType: TextInputType.emailAddress,
                      errorText: _emailError,
                    ),
                    const SizedBox(height: 14),

                    // Şifre
                    AuthTextField(
                      controller: _passwordController,
                      label: 'Şifre',
                      icon: Icons.lock_outline,
                      obscureText: _obscurePassword,
                      errorText: _passwordError,
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

                    // Şifre tekrar
                    AuthTextField(
                      controller: _passwordAgainController,
                      label: 'Şifre Tekrar',
                      icon: Icons.lock_outline,
                      obscureText: _obscurePasswordAgain,
                      errorText: _passwordAgainError,
                      suffixIcon: IconButton(
                        icon: Icon(
                          _obscurePasswordAgain
                              ? Icons.visibility_outlined
                              : Icons.visibility_off_outlined,
                          color: AppColors.txtSecondary(context),
                          size: 20,
                        ),
                        onPressed: () => setState(
                          () => _obscurePasswordAgain = !_obscurePasswordAgain,
                        ),
                      ),
                    ),

                    const SizedBox(height: 20),

                    // KVKK checkbox
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.center,
                      children: [
                        SizedBox(
                          width: 24,
                          height: 24,
                          child: Checkbox(
                            value: _kvkkAccepted,
                            onChanged: (value) =>
                                setState(() => _kvkkAccepted = value ?? false),
                            checkColor: Colors.white,
                            activeColor: AppColors.primary,
                            side: BorderSide(
                              color: AppColors.txt(context).withValues(alpha: 0.3),
                            ),
                          ),
                        ),
                        const SizedBox(width: 10),
                        Expanded(
                          child: GestureDetector(
                            onTap: _showKvkkSheet,
                            child: RichText(
                              text: TextSpan(
                                style: TextStyle(
                                  color: AppColors.txtSecondary(context),
                                  fontSize: 13,
                                  height: 1.3,
                                ),
                                children: const [
                                  TextSpan(
                                    text: 'KVKK Aydınlatma Metni\'ni okudum, ',
                                  ),
                                  TextSpan(
                                    text: 'kabul ediyorum',
                                    style: TextStyle(
                                      color: AppColors.primaryLight,
                                      fontWeight: FontWeight.w700,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ),
                        ),
                      ],
                    ),

                    const SizedBox(height: 20),

                    // Kayıt ol butonu
                    GestureDetector(
                      onTap: (_isLoading || !_kvkkAccepted)
                          ? null
                          : _handleRegister,
                      child: Opacity(
                        opacity: _kvkkAccepted ? 1.0 : 0.4,
                        child: Container(
                          width: double.infinity,
                          padding: const EdgeInsets.symmetric(vertical: 18),
                          decoration: BoxDecoration(
                            gradient: const LinearGradient(
                              begin: Alignment.centerLeft,
                              end: Alignment.centerRight,
                              colors: [AppColors.primary, Color(0xFF818CF8)],
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
                                    'Kayıt Ol',
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
                    ),

                    const SizedBox(height: 24),

                    // Giriş yap
                    Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Text(
                          'Zaten hesabın var mı?',
                          style: TextStyle(
                            color: AppColors.txtSecondary(context),
                            fontSize: 14,
                          ),
                        ),
                        TextButton(
                          onPressed: () => Navigator.pop(context),
                          child: const Text(
                            'Giriş yap',
                            style: TextStyle(
                              color: AppColors.primaryLight,
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                        ),
                      ],
                    ),

                    const Spacer(),

                    Center(
                      child: Text(
                        'FişBu © 2026',
                        style: TextStyle(
                          fontSize: 12,
                          color: AppColors.txtSecondary(context).withValues(alpha: 0.6),
                        ),
                      ),
                    ),
                    const SizedBox(height: 16),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
