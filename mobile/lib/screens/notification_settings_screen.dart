import 'package:flutter/material.dart';
import '../services/notification_prefs_service.dart';
import '../core/theme/app_colors.dart';

class NotificationSettingsScreen extends StatefulWidget {
  const NotificationSettingsScreen({super.key});

  @override
  State<NotificationSettingsScreen> createState() => _NotificationSettingsScreenState();
}

class _NotificationSettingsScreenState extends State<NotificationSettingsScreen> {
  bool _budgetWarningEnabled = true;
  bool _budgetOverspendEnabled = true;
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadPrefs();
  }

  Future<void> _loadPrefs() async {
    final warning = await NotificationPrefsService.isBudgetWarningEnabled();
    final overspend = await NotificationPrefsService.isBudgetOverspendEnabled();
    if (!mounted) return;
    setState(() {
      _budgetWarningEnabled = warning;
      _budgetOverspendEnabled = overspend;
      _isLoading = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Bildirimler')),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator(color: AppColors.primary))
          : Padding(
              padding: const EdgeInsets.all(20),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Bütçe Bildirimleri',
                    style: TextStyle(
                      fontSize: 13,
                      fontWeight: FontWeight.w600,
                      color: AppColors.textSecondary,
                    ),
                  ),
                  const SizedBox(height: 10),
                  Container(
                    decoration: BoxDecoration(
                      color: AppColors.surf(context),
                      borderRadius: BorderRadius.circular(20),
                      border: Border.all(color: AppColors.brd(context)),
                    ),
                    child: Column(
                      children: [
                        _buildToggleRow(
                          icon: Icons.warning_amber_rounded,
                          iconColor: AppColors.warning,
                          label: 'Bütçe %80 Uyarısı',
                          value: _budgetWarningEnabled,
                          isFirst: true,
                          onChanged: (value) async {
                            await NotificationPrefsService.setBudgetWarningEnabled(value);
                            setState(() => _budgetWarningEnabled = value);
                          },
                        ),
                        _buildToggleRow(
                          icon: Icons.error_outline,
                          iconColor: AppColors.error,
                          label: 'Bütçe Aşım Uyarısı',
                          value: _budgetOverspendEnabled,
                          isLast: true,
                          onChanged: (value) async {
                            await NotificationPrefsService.setBudgetOverspendEnabled(value);
                            setState(() => _budgetOverspendEnabled = value);
                          },
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 12),
                  Text(
                    'Bu bildirimler bütçe özelliğiyle ilgilidir. Uygulama içi uyarı kartları her zaman gösterilir; bu tercihler push bildirimleri için kullanılır.',
                    style: TextStyle(
                      fontSize: 12,
                      color: AppColors.textSecondary,
                      height: 1.4,
                    ),
                  ),
                ],
              ),
            ),
    );
  }

  Widget _buildToggleRow({
    required IconData icon,
    required Color iconColor,
    required String label,
    required bool value,
    required ValueChanged<bool> onChanged,
    bool isFirst = false,
    bool isLast = false,
  }) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 8),
      decoration: BoxDecoration(
        border: isLast ? null : Border(bottom: BorderSide(color: AppColors.brd(context))),
      ),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(
              color: iconColor.withOpacity(0.12),
              borderRadius: BorderRadius.circular(10),
            ),
            child: Icon(icon, color: iconColor, size: 18),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Text(
              label,
              style: TextStyle(
                fontSize: 15,
                fontWeight: FontWeight.w600,
                color: AppColors.txt(context),
              ),
            ),
          ),
          Switch(value: value, onChanged: onChanged),
        ],
      ),
    );
  }
}
