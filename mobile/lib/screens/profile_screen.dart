import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart' show Clipboard, ClipboardData;
import 'package:image_picker/image_picker.dart';
import 'package:path_provider/path_provider.dart';
import 'package:share_plus/share_plus.dart';
import '../services/receipt_service.dart';
import '../services/auth_service.dart';
import '../services/biometric_service.dart';
import '../services/pin_service.dart';
import 'pin_entry_screen.dart';
import 'auth_wrapper.dart';
import 'categories_screen.dart';
import 'household_screen.dart';
import 'savings_goals_screen.dart';
import 'spending_calendar_screen.dart';
import 'notification_settings_screen.dart';
import '../core/theme/app_colors.dart';
import '../main.dart';

class ProfileScreen extends StatefulWidget {
  const ProfileScreen({super.key});

  @override
  State<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends State<ProfileScreen> {
  String? _email;
  String? _name;
  String? _profileImageUrl;
  String? _createdAt;
  bool _isUpdatingPhoto = false;
  final _currentPasswordController = TextEditingController();
  final _newPasswordController = TextEditingController();
  final _confirmPasswordController = TextEditingController();
  bool _currentPasswordVisible = false;
  bool _newPasswordVisible = false;
  bool _confirmPasswordVisible = false;
  bool _isChangingPassword = false;
  bool _isDeletingAccount = false;
  bool _isExportingData = false;
  bool _biometricSupported = false;
  bool _biometricEnabled = false;
  bool _pinSet = false;

  @override
  void initState() {
    super.initState();
    _loadEmail();
    _loadBiometricState();
  }

  Future<void> _loadBiometricState() async {
    final supported = await BiometricService.isDeviceSupported();
    final enabled = await BiometricService.isEnabled();
    final pinSet = await PinService.isPinSet();
    if (!mounted) return;
    setState(() {
      _biometricSupported = supported;
      _biometricEnabled = enabled;
      _pinSet = pinSet;
    });
  }

  Future<void> _managePin() async {
    if (!_pinSet) {
      final result = await Navigator.push<bool>(
        context,
        MaterialPageRoute(builder: (context) => const PinEntryScreen(mode: PinEntryMode.set)),
      );
      if (result == true && mounted) setState(() => _pinSet = true);
      return;
    }

    final action = await showModalBottomSheet<String>(
      context: context,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
      builder: (ctx) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            ListTile(
              leading: const Icon(Icons.edit_outlined),
              title: const Text('PIN\'i Değiştir'),
              onTap: () => Navigator.pop(ctx, 'change'),
            ),
            ListTile(
              leading: Icon(Icons.delete_outline, color: AppColors.error),
              title: Text('PIN\'i Kaldır', style: TextStyle(color: AppColors.error)),
              onTap: () => Navigator.pop(ctx, 'remove'),
            ),
          ],
        ),
      ),
    );

    if (!mounted) return;

    if (action == 'change') {
      final result = await Navigator.push<bool>(
        context,
        MaterialPageRoute(builder: (context) => const PinEntryScreen(mode: PinEntryMode.set)),
      );
      if (result == true && mounted) setState(() => _pinSet = true);
    } else if (action == 'remove') {
      await PinService.clearPin();
      if (mounted) setState(() => _pinSet = false);
    }
  }

  Future<void> _toggleBiometric(bool value) async {
    if (value) {
      final success = await BiometricService.authenticate();
      if (!success) {
        if (!mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Kimlik doğrulama başarısız')),
        );
        return;
      }
    }
    await BiometricService.setEnabled(value);
    if (!mounted) return;
    setState(() => _biometricEnabled = value);
  }

  @override
  void dispose() {
    _currentPasswordController.dispose();
    _newPasswordController.dispose();
    _confirmPasswordController.dispose();
    super.dispose();
  }

  Future<void> _loadEmail() async {
    final profile = await AuthService.getProfile();
    if (profile != null) {
      setState(() {
        _email = profile['email'];
        _name = profile['name'];
        _profileImageUrl = profile['profileImageUrl'];
        _createdAt = profile['createdAt'];
      });
    } else {
      final email = await AuthService.getEmail();
      setState(() => _email = email);
    }
  }

  Future<void> _updateProfilePhoto() async {
    final source = await showModalBottomSheet<ImageSource>(
      context: context,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
      builder: (ctx) => Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            ListTile(
              leading: const Icon(Icons.camera_alt_outlined),
              title: const Text('Kamera'),
              onTap: () => Navigator.pop(ctx, ImageSource.camera),
            ),
            ListTile(
              leading: const Icon(Icons.photo_library_outlined),
              title: const Text('Galeriden Seç'),
              onTap: () => Navigator.pop(ctx, ImageSource.gallery),
            ),
          ],
        ),
      ),
    );
    if (source == null) return;
    final picker = ImagePicker();
    final image = await picker.pickImage(source: source, imageQuality: 60);
    if (image == null) return;
    setState(() => _isUpdatingPhoto = true);
    try {
      final imageUrl = await ReceiptService.uploadImage(image);
      await AuthService.updateProfile(profileImageUrl: imageUrl);
      setState(() {
        _profileImageUrl = imageUrl;
        _isUpdatingPhoto = false;
      });
    } catch (e) {
      setState(() => _isUpdatingPhoto = false);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Fotoğraf yüklenemedi: \$e')),
        );
      }
    }
  }

  void _showEditNameSheet() {
    final controller = TextEditingController(text: _name ?? '');
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(24))),
      builder: (ctx) => Padding(
        padding: EdgeInsets.fromLTRB(20, 20, 20, MediaQuery.of(ctx).viewInsets.bottom + 24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Center(child: Container(width: 40, height: 4, decoration: BoxDecoration(color: AppColors.brd(ctx), borderRadius: BorderRadius.circular(2)))),
            const SizedBox(height: 20),
            Text('Adı Düzenle', style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700, color: AppColors.txt(ctx))),
            const SizedBox(height: 16),
            TextField(
              controller: controller,
              autofocus: true,
              decoration: InputDecoration(
                labelText: 'Ad Soyad',
                prefixIcon: const Icon(Icons.person_outline),
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
              ),
            ),
            const SizedBox(height: 20),
            ElevatedButton(
              onPressed: () async {
                final name = controller.text.trim();
                if (name.isEmpty) return;
                await AuthService.updateProfile(name: name);
                setState(() => _name = name);
                if (ctx.mounted) Navigator.pop(ctx);
              },
              style: ElevatedButton.styleFrom(padding: const EdgeInsets.symmetric(vertical: 16), shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12))),
              child: const Text('Kaydet', style: TextStyle(fontSize: 15, fontWeight: FontWeight.w600)),
            ),
          ],
        ),
      ),
    );
  }

  void _showAboutSheet() {
    const supportEmail = 'baris.hansu57@outlook.com';
    const privacyText =
        '6698 sayılı Kişisel Verilerin Korunması Kanunu (KVKK) kapsamında, '
        'FişBu uygulamasına kaydolurken e-posta adresin ve şifren şifreli olarak saklanır. '
        'Uygulamaya yüklediğin fiş görselleri ve bu görsellerden çıkarılan harcama verileri '
        '(tutar, tarih, kategori) yalnızca senin harcama takibini yapabilmen amacıyla işlenir '
        've üçüncü taraflarla paylaşılmaz. Verilerinin silinmesini istediğinde profil ekranından '
        'hesabını ve tüm verilerini kalıcı olarak silebilirsin. Kaydolarak bu şartları kabul etmiş olursun.';

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(24))),
      builder: (ctx) => Padding(
        padding: EdgeInsets.fromLTRB(20, 20, 20, MediaQuery.of(ctx).viewInsets.bottom + 24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Center(child: Container(width: 40, height: 4, decoration: BoxDecoration(color: AppColors.brd(ctx), borderRadius: BorderRadius.circular(2)))),
            const SizedBox(height: 20),
            Text('Hakkında', style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700, color: AppColors.txt(ctx))),
            const SizedBox(height: 16),
            ListTile(
              contentPadding: EdgeInsets.zero,
              leading: Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(color: AppColors.primDim(ctx), borderRadius: BorderRadius.circular(10)),
                child: const Icon(Icons.info_outline, color: AppColors.primary),
              ),
              title: Text('Sürüm', style: TextStyle(color: AppColors.txt(ctx), fontWeight: FontWeight.w600)),
              subtitle: const Text('1.0.0'),
            ),
            ListTile(
              contentPadding: EdgeInsets.zero,
              leading: Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(color: AppColors.primDim(ctx), borderRadius: BorderRadius.circular(10)),
                child: const Icon(Icons.mail_outline, color: AppColors.primary),
              ),
              title: Text('İletişim', style: TextStyle(color: AppColors.txt(ctx), fontWeight: FontWeight.w600)),
              subtitle: const Text(supportEmail),
              trailing: const Icon(Icons.copy, size: 18),
              onTap: () async {
                await Clipboard.setData(const ClipboardData(text: supportEmail));
                if (ctx.mounted) {
                  ScaffoldMessenger.of(ctx).showSnackBar(
                    const SnackBar(content: Text('E-posta adresi kopyalandı')),
                  );
                }
              },
            ),
            ListTile(
              contentPadding: EdgeInsets.zero,
              leading: Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(color: AppColors.primDim(ctx), borderRadius: BorderRadius.circular(10)),
                child: const Icon(Icons.privacy_tip_outlined, color: AppColors.primary),
              ),
              title: Text('Gizlilik Politikası (KVKK)', style: TextStyle(color: AppColors.txt(ctx), fontWeight: FontWeight.w600)),
              trailing: const Icon(Icons.chevron_right, size: 20),
              onTap: () {
                showDialog(
                  context: ctx,
                  builder: (dialogContext) => AlertDialog(
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
                    title: const Text('Gizlilik Politikası (KVKK)', style: TextStyle(fontWeight: FontWeight.w700)),
                    content: SingleChildScrollView(
                      child: Text(privacyText, style: TextStyle(color: AppColors.textSecondary, height: 1.5)),
                    ),
                    actions: [
                      TextButton(
                        onPressed: () => Navigator.pop(dialogContext),
                        child: const Text('Kapat'),
                      ),
                    ],
                  ),
                );
              },
            ),
          ],
        ),
      ),
    );
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
              20,
              20,
              20,
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
                        () =>
                            _currentPasswordVisible = !_currentPasswordVisible,
                      ),
                    ),
                    filled: true,
                    fillColor: isDark
                        ? AppColors.surfaceDark
                        : AppColors.surface,
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(12),
                    ),
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
                        () => _newPasswordVisible = !_newPasswordVisible,
                      ),
                    ),
                    filled: true,
                    fillColor: isDark
                        ? AppColors.surfaceDark
                        : AppColors.surface,
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(12),
                    ),
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
                        () =>
                            _confirmPasswordVisible = !_confirmPasswordVisible,
                      ),
                    ),
                    filled: true,
                    fillColor: isDark
                        ? AppColors.surfaceDark
                        : AppColors.surface,
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(12),
                    ),
                  ),
                ),
                const SizedBox(height: 24),
                ElevatedButton(
                  onPressed: _isChangingPassword
                      ? null
                      : () =>
                            _handleChangePassword(sheetContext, setSheetState),
                  style: ElevatedButton.styleFrom(
                    padding: const EdgeInsets.symmetric(vertical: 16),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(12),
                    ),
                  ),
                  child: _isChangingPassword
                      ? const SizedBox(
                          height: 20,
                          width: 20,
                          child: CircularProgressIndicator(
                            strokeWidth: 2,
                            color: Colors.white,
                          ),
                        )
                      : const Text(
                          'Kaydet',
                          style: TextStyle(
                            fontSize: 15,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                ),
              ],
            ),
          );
        },
      ),
    );
  }

  Future<void> _handleChangePassword(
    BuildContext sheetContext,
    StateSetter setSheetState,
  ) async {
    final current = _currentPasswordController.text.trim();
    final newPass = _newPasswordController.text.trim();
    final confirm = _confirmPasswordController.text.trim();

    if (current.isEmpty || newPass.isEmpty || confirm.isEmpty) {
      ScaffoldMessenger.of(
        sheetContext,
      ).showSnackBar(const SnackBar(content: Text('Tüm alanları doldur')));
      return;
    }
    if (newPass != confirm) {
      ScaffoldMessenger.of(
        sheetContext,
      ).showSnackBar(const SnackBar(content: Text('Yeni şifreler eşleşmiyor')));
      return;
    }
    if (newPass.length < 8) {
      ScaffoldMessenger.of(sheetContext).showSnackBar(
        const SnackBar(content: Text('Yeni şifre en az 8 karakter olmalı')),
      );
      return;
    }
    if (!newPass.contains(RegExp(r'\d'))) {
      ScaffoldMessenger.of(sheetContext).showSnackBar(
        const SnackBar(content: Text('Yeni şifre en az bir rakam içermeli')),
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

  Future<void> _handleDeleteAccount() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: const Text(
          'Hesabımı Sil',
          style: TextStyle(fontWeight: FontWeight.w700),
        ),
        content: const Text(
          'Hesabını silmek istediğine emin misin? Bu işlem geri alınamaz, '
          'tüm fişlerin ve kategorilerin kalıcı olarak silinecek.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Vazgeç'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            style: TextButton.styleFrom(foregroundColor: AppColors.error),
            child: const Text('Hesabımı Sil'),
          ),
        ],
      ),
    );
    if (confirmed != true) return;

    setState(() => _isDeletingAccount = true);
    final result = await AuthService.deleteAccount();
    if (!mounted) return;
    setState(() => _isDeletingAccount = false);

    if (result.success) {
      Navigator.pushAndRemoveUntil(
        context,
        MaterialPageRoute(builder: (context) => const AuthWrapper()),
        (route) => false,
      );
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(result.errorMessage ?? 'Hesap silinemedi')),
      );
    }
  }

  Future<void> _handleDownloadData() async {
    if (_isExportingData) return;
    setState(() => _isExportingData = true);
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('Verileriniz hazırlanıyor...'), duration: Duration(seconds: 2)),
    );
    try {
      final json = await AuthService.downloadMyData();
      final dir = await getTemporaryDirectory();
      final filename =
          'fisbu_verilerim_${DateTime.now().millisecondsSinceEpoch}.json';
      final file = File('${dir.path}/$filename');
      await file.writeAsString(json);

      if (!mounted) return;
      await SharePlus.instance.share(
        ShareParams(files: [XFile(file.path)], text: 'FişBu - Verilerim (KVKK)'),
      );
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Verileriniz indirilemedi: $e')),
        );
      }
    } finally {
      if (mounted) setState(() => _isExportingData = false);
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
              style: TextStyle(
                color: Colors.white,
                fontWeight: FontWeight.w700,
              ),
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
                      GestureDetector(
                        onTap: _updateProfilePhoto,
                        child: Stack(
                          children: [
                            Container(
                              padding: const EdgeInsets.all(3),
                              decoration: BoxDecoration(
                                shape: BoxShape.circle,
                                border: Border.all(color: Colors.white, width: 3),
                              ),
                              child: _isUpdatingPhoto
                                  ? const CircleAvatar(radius: 40, backgroundColor: Colors.white24, child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2))
                                  : CircleAvatar(
                                      radius: 40,
                                      backgroundColor: Colors.white24,
                                      backgroundImage: _profileImageUrl != null ? NetworkImage(_profileImageUrl!) : null,
                                      child: _profileImageUrl == null ? const Icon(Icons.person, size: 40, color: Colors.white) : null,
                                    ),
                            ),
                            Positioned(
                              bottom: 0, right: 0,
                              child: Container(
                                padding: const EdgeInsets.all(4),
                                decoration: const BoxDecoration(color: Colors.white, shape: BoxShape.circle),
                                child: const Icon(Icons.camera_alt, size: 16, color: AppColors.primary),
                              ),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(height: 12),
                      GestureDetector(
                        onTap: _showEditNameSheet,
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Text(
                              _name ?? 'Ad Soyad Ekle',
                              style: const TextStyle(color: Colors.white, fontSize: 17, fontWeight: FontWeight.w700),
                            ),
                            const SizedBox(width: 6),
                            const Icon(Icons.edit, size: 14, color: Colors.white70),
                          ],
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        _email ?? 'Yükleniyor...',
                        style: const TextStyle(color: Colors.white70, fontSize: 13, fontWeight: FontWeight.w500),
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
                          icon: Icons.family_restroom,
                          label: 'Aile Bütçesi',
                          onTap: () {
                            Navigator.push(
                              context,
                              MaterialPageRoute(
                                builder: (context) => const HouseholdScreen(),
                              ),
                            );
                          },
                        ),
                        _buildMenuItem(
                          icon: Icons.calendar_month_outlined,
                          label: 'Harcama Takvimi',
                          onTap: () {
                            Navigator.push(
                              context,
                              MaterialPageRoute(
                                builder: (context) => const SpendingCalendarScreen(),
                              ),
                            );
                          },
                        ),
                        _buildMenuItem(
                          icon: Icons.savings_outlined,
                          label: 'Tasarruf Hedeflerim',
                          onTap: () {
                            Navigator.push(
                              context,
                              MaterialPageRoute(
                                builder: (context) => const SavingsGoalsScreen(),
                              ),
                            );
                          },
                        ),
                        _buildMenuItem(
                          icon: Icons.notifications_outlined,
                          label: 'Bildirimler',
                          onTap: () {
                            Navigator.push(
                              context,
                              MaterialPageRoute(
                                builder: (context) => const NotificationSettingsScreen(),
                              ),
                            );
                          },
                        ),
                        _buildMenuItem(
                          icon: Icons.lock_outline,
                          label: 'Şifremi Değiştir',
                          onTap: _showChangePasswordSheet,
                        ),
                        if (_biometricSupported)
                          _buildMenuItemWithTrailing(
                            icon: Icons.fingerprint,
                            label: 'Face ID ile Giriş',
                            trailing: Switch(
                              value: _biometricEnabled,
                              onChanged: _toggleBiometric,
                            ),
                          ),
                        if (_biometricSupported && _biometricEnabled)
                          _buildMenuItem(
                            icon: Icons.pin_outlined,
                            label: _pinSet ? 'Yedek PIN Kodu (Belirlendi)' : 'Yedek PIN Kodu Belirle',
                            onTap: _managePin,
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
                          icon: Icons.download_outlined,
                          label: 'Verilerimi İndir (KVKK)',
                          onTap: _handleDownloadData,
                        ),
                        _buildMenuItem(
                          icon: Icons.info_outline,
                          label: 'Hakkında',
                          isLast: true,
                          onTap: _showAboutSheet,
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
                          color: AppColors.error.withOpacity(0.2),
                        ),
                      ),
                      child: const Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(
                            Icons.logout_rounded,
                            color: AppColors.error,
                            size: 20,
                          ),
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

                  const SizedBox(height: 12),

                  GestureDetector(
                    onTap: _isDeletingAccount ? null : _handleDeleteAccount,
                    child: Container(
                      width: double.infinity,
                      padding: const EdgeInsets.symmetric(vertical: 16),
                      decoration: BoxDecoration(
                        color: AppColors.errDim(context),
                        borderRadius: BorderRadius.circular(16),
                        border: Border.all(
                          color: AppColors.error.withOpacity(0.2),
                        ),
                      ),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          if (_isDeletingAccount)
                            const SizedBox(
                              height: 18,
                              width: 18,
                              child: CircularProgressIndicator(
                                strokeWidth: 2,
                                color: AppColors.error,
                              ),
                            )
                          else ...[
                            const Icon(
                              Icons.delete_forever_outlined,
                              color: AppColors.error,
                              size: 20,
                            ),
                            const SizedBox(width: 10),
                            const Text(
                              'Hesabımı Sil',
                              style: TextStyle(
                                color: AppColors.error,
                                fontSize: 15,
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                          ],
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
            const Icon(
              Icons.chevron_right,
              color: AppColors.textSecondary,
              size: 20,
            ),
          ],
        ),
      ),
    );
  }
}
