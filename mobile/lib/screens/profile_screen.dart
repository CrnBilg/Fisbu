import 'package:flutter/material.dart';
import '../services/auth_service.dart';
import 'auth_wrapper.dart';
import 'categories_screen.dart';
import '../core/theme/app_colors.dart';
import '../main.dart';

class ProfileScreen extends StatefulWidget {
  const ProfileScreen({super.key});

  @override
  State<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends State<ProfileScreen> {
  String? _email;
  final _currentPasswordController = TextEditingController();
  final _newPasswordController = TextEditingController();
  final _confirmPasswordController = TextEditingController();
  bool _currentPasswordVisible = false;
  bool _newPasswordVisible = false;
  bool _confirmPasswordVisible = false;
  bool _isChangingPassword = false;

  @override
  void initState() {
    super.initState();
    _loadEmail();
  }

  @override
  void dispose() {
    _currentPasswordController.dispose();
    _newPasswordController.dispose();
    _confirmPasswordController.dispose();
    super.dispose();
  }

  Future<void> _loadEmail() async {
    final email = await AuthService.getEmail();
    setState(() => _email = email);
  }

  void _showChangePasswordSheet() {
    _currentPasswordController.clear();
    _newPasswordController.clear();
    _confirmPasswordController.clear();
    _currentPasswordVisible = false;
    _newPasswordVisible = false;
    _confirmPasswordVisible = false;

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (sheetContext) => StatefulBuilder(
        builder: (sheetContext, setSheetState) {
          final isDark = Theme.of(sheetContext).brightness == Brightness.dark;
          return Padding(
            padding: EdgeInsets.fromLTRB(
              20, 20, 20,
              MediaQuery.of(sheetContext).viewInsets.bottom + 24,
            ),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Center(
                  child: Container(
                    width: 40,
                    height: 4,
                    decoration: BoxDecoration(
                      color: AppColors.brd(sheetContext),
                      borderRadius: BorderRadius.circular(2),
                    ),
                  ),
                ),
                const SizedBox(height: 20),
                Text(
                  'Şifre Değiştir',
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.w700,
                    color: AppColors.txt(sheetContext),
                  ),
                ),
                const SizedBox(height: 20),
                TextField(
                  controller: _currentPasswordController,
                  obscureText: !_currentPasswordVisible,
                  decoration: InputDecoration(
                    labelText: 'Mevcut Şifre',
                    prefixIcon: const Icon(Icons.lock_outline),
                    suffixIcon: IconButton(
                      icon: Icon(
                        _currentPasswordVisible
                            ? Icons.visibility_off_outlined
                            : Icons.visibility_outlined,
                      ),
                      onPressed: () => setSheetState(
                          () => _currentPasswordVisible = !_currentPasswordVisible),
                    ),
                    filled: true,
                    fillColor: isDark ? AppColors.surfaceDark : AppColors.surface,
                    border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(12)),
                  ),
                ),
                const SizedBox(height: 14),
                TextField(
                  controller: _newPasswordController,
                  obscureText: !_newPasswordVisible,
                  decoration: InputDecoration(
                    labelText: 'Yeni Şifre',
                    prefixIcon: const Icon(Icons.lock_open_outlined),
                    suffixIcon: IconButton(
                      icon: Icon(
                        _newPasswordVisible
                            ? Icons.visibility_off_outlined
                            : Icons.visibility_outlined,
                      ),
                      onPressed: () => setSheetState(
                          () => _newPasswordVisible = !_newPasswordVisible),
                    ),
                    filled: true,
                    fillColor: isDark ? AppColors.surfaceDark : AppColors.surface,
                    border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(12)),
                  ),
                ),
                const SizedBox(height: 14),
                TextField(
                  controller: _confirmPasswordController,
                  obscureText: !_confirmPasswordVisible,
                  decoration: InputDecoration(
                    labelText: 'Yeni Şifre Tekrar',
                    prefixIcon: const Icon(Icons.lock_open_outlined),
                    suffixIcon: IconButton(
                      icon: Icon(
                        _confirmPasswordVisible
                            ? Icons.visibility_off_outlined
                            : Icons.visibility_outlined,
                      ),
                      onPressed: () => setSheetState(
                          () => _confirmPasswordVisible = !_confirmPasswordVisible),
                    ),
                    filled: true,
                    fillColor: isDark ? AppColors.surfaceDark : AppColors.surface,
                    border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(12)),
                  ),
                ),
                const SizedBox(height: 24),
                ElevatedButton(
                  onPressed: _isChangingPassword
                      ? null
                      : () => _handleChangePassword(sheetContext, setSheetState),
                  style: ElevatedButton.styleFrom(
                    padding: const EdgeInsets.symmetric(vertical: 16),
                    shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12)),
                  ),
                  child: _isChangingPassword
                      ? const SizedBox(
                          height: 20,
                          width: 20,
                          child: CircularProgressIndicator(
                              strokeWidth: 2, color: Colors.white),
                        )
                      : const Text('Kaydet',
                          style: TextStyle(
                              fontSize: 15, fontWeight: FontWeight.w600)),
                ),
              ],
            ),
          );
        },
      ),
    );
  }

  Future<void> _handleChangePassword(
      BuildContext sheetContext, StateSetter setSheetState) async {
    final current = _currentPasswordController.text.trim();
    final newPass = _newPasswordController.text.trim();
    final confirm = _confirmPasswordController.text.trim();

    if (current.isEmpty || newPass.isEmpty || confirm.isEmpty) {
      ScaffoldMessenger.of(sheetContext).showSnackBar(
        const SnackBar(content: Text('Tüm alanları doldur')),
      );
      return;
    }
    if (newPass != confirm) {
      ScaffoldMessenger.of(sheetContext).showSnackBar(
        const SnackBar(content: Text('Yeni şifreler eşleşmiyor')),
      );
      return;
    }
    if (newPass.length < 6) {
      ScaffoldMessenger.of(sheetContext).showSnackBar(
        const SnackBar(content: Text('Yeni şifre en az 6 karakter olmalı')),
      );
      return;
    }

    setSheetState(() => _isChangingPassword = true);
    final result = await AuthService.changePassword(current, newPass);
    setSheetState(() => _isChangingPassword = false);

    if (!sheetContext.mounted) return;

    if (result.success) {
      Navigator.pop(sheetContext);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Şifre başarıyla değiştirildi')),
        );
      }
    } else {
      ScaffoldMessenger.of(sheetContext).showSnackBar(
        SnackBar(content: Text(result.errorMessage ?? 'Bir hata oluştu')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(

      body: CustomScrollView(
        slivers: [
          SliverAppBar(
            expandedHeight: 200,
            floating: false,
            pinned: true,
            backgroundColor: AppColors.primary,
            foregroundColor: Colors.white,
            centerTitle: false,
            title: Text(
              'Profil',
              style: TextStyle(color: Colors.white, fontWeight: FontWeight.w700),
            ),
            flexibleSpace: FlexibleSpaceBar(
              background: Container(
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                    colors: Theme.of(context).brightness == Brightness.dark
                        ? [AppColors.primaryDimDark, AppColors.surfaceDark]
                        : [AppColors.primary, AppColors.primaryDark],
                  ),
                ),
                child: SafeArea(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      const SizedBox(height: 40),
                      Container(
                        padding: const EdgeInsets.all(3),
                        decoration: BoxDecoration(
                          shape: BoxShape.circle,
                          border: Border.all(color: Colors.white, width: 3),
                        ),
                        child: const CircleAvatar(
                          radius: 40,
                          backgroundColor: Colors.white24,
                          child: Icon(Icons.person, size: 40, color: Colors.white),
                        ),
                      ),
                      const SizedBox(height: 12),
                      Text(
                        _email ?? 'Yükleniyor...',
                        style: TextStyle(
                          color: Colors.white,
                          fontSize: 15,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),

          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.all(20),
              child: Column(
                children: [
                  Container(
                    decoration: BoxDecoration(
                      color: AppColors.surf(context),
                      borderRadius: BorderRadius.circular(20),
                      border: Border.all(color: AppColors.brd(context)),
                    ),
                    child: Column(
                      children: [
                        _buildMenuItem(
                          icon: Icons.category_outlined,
                          label: 'Kategorilerim',
                          isFirst: true,
                          onTap: () {
                            Navigator.push(
                              context,
                              MaterialPageRoute(
                                builder: (context) => const CategoriesScreen(),
                              ),
                            );
                          },
                        ),
                        _buildMenuItem(
                          icon: Icons.notifications_outlined,
                          label: 'Bildirimler',
                          onTap: () {},
                        ),
                        _buildMenuItem(
                          icon: Icons.lock_outline,
                          label: 'Şifremi Değiştir',
                          onTap: _showChangePasswordSheet,
                        ),
                       _buildMenuItemWithTrailing(
                          icon: Icons.dark_mode_outlined,
                          label: 'Karanlık Mod',
                          trailing: Switch(
                            value: MyApp.of(context)?.isDarkMode ?? false,
                            onChanged: (_) async {
                              await MyApp.of(context)?.toggleTheme();
                              setState(() {});
                            },
                          ),
                        ),
                        _buildMenuItem(
                          icon: Icons.info_outline,
                          label: 'Hakkında',
                          isLast: true,
                          onTap: () {},
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 32),

                  GestureDetector(
                    onTap: () async {
                      await AuthService.logout();
                      if (!context.mounted) return;
                      Navigator.pushAndRemoveUntil(
                        context,
                        MaterialPageRoute(
                          builder: (context) => const AuthWrapper(),
                        ),
                        (route) => false,
                      );
                    },
                    child: Container(
                      width: double.infinity,
                      padding: const EdgeInsets.symmetric(vertical: 16),
                      decoration: BoxDecoration(
                        color: AppColors.errDim(context),
                        borderRadius: BorderRadius.circular(16),
                        border: Border.all(
                            color: AppColors.error.withOpacity(0.2)),
                      ),
                      child: const Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(Icons.logout_rounded,
                              color: AppColors.error, size: 20),
                          SizedBox(width: 10),
                          Text(
                            'Çıkış Yap',
                            style: TextStyle(
                              color: AppColors.error,
                              fontSize: 15,
                              fontWeight: FontWeight.w600,
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
        ],
      ),
    );
  }

  Widget _buildMenuItemWithTrailing({
    required IconData icon,
    required String label,
    required Widget trailing,
  }) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 8),
      decoration: BoxDecoration(
        border: Border(bottom: BorderSide(color: AppColors.brd(context))),
      ),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(
              color: AppColors.primDim(context),
              borderRadius: BorderRadius.circular(10),
            ),
            child: Icon(icon, color: AppColors.primary, size: 18),
          ),
          const SizedBox(width: 14),
          Text(
            label,
            style: TextStyle(
              fontSize: 15,
              fontWeight: FontWeight.w600,
              color: AppColors.txt(context),
            ),
          ),
          const Spacer(),
          trailing,
        ],
      ),
    );
  }

  Widget _buildMenuItem({
    required IconData icon,
    required String label,
    required VoidCallback onTap,
    bool isFirst = false,
    bool isLast = false,
  }) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.vertical(
        top: isFirst ? const Radius.circular(20) : Radius.zero,
        bottom: isLast ? const Radius.circular(20) : Radius.zero,
      ),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
        decoration: BoxDecoration(
          border: isLast
              ? null
              : Border(bottom: BorderSide(color: AppColors.brd(context))),
        ),
        child: Row(
          children: [
            Container(
              padding: const EdgeInsets.all(8),
              decoration: BoxDecoration(
                color: AppColors.primDim(context),
                borderRadius: BorderRadius.circular(10),
              ),
              child: Icon(icon, color: AppColors.primary, size: 18),
            ),
            const SizedBox(width: 14),
            Text(
              label,
              style: TextStyle(
                fontSize: 15,
                fontWeight: FontWeight.w600,
                color: AppColors.txt(context),
              ),
            ),
            const Spacer(),
            const Icon(Icons.chevron_right,
                color: AppColors.textSecondary, size: 20),
          ],
        ),
      ),
    );
  }
}