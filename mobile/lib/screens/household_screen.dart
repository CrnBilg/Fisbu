import 'package:flutter/material.dart';
import 'package:flutter/services.dart' show Clipboard, ClipboardData;
import 'package:share_plus/share_plus.dart';
import '../models/household.dart';
import '../models/household_statistics.dart';
import '../services/household_service.dart';
import '../core/theme/app_colors.dart';
import '../core/utils/network_error.dart';
import '../core/widgets/offline_banner.dart';

class HouseholdScreen extends StatefulWidget {
  const HouseholdScreen({super.key});

  @override
  State<HouseholdScreen> createState() => _HouseholdScreenState();
}

class _HouseholdScreenState extends State<HouseholdScreen> {
  bool _isLoading = true;
  Household? _household;
  String? _errorMessage;

  final _createNameController = TextEditingController();
  final _joinCodeController = TextEditingController();
  bool _isSubmitting = false;

  bool _isLoadingStats = false;
  HouseholdStatistics? _statistics;
  String? _statsError;
  late int _selectedYear;
  late int _selectedMonth;

  @override
  void initState() {
    super.initState();
    final now = DateTime.now();
    _selectedYear = now.year;
    _selectedMonth = now.month;
    _loadHousehold();
  }

  @override
  void dispose() {
    _createNameController.dispose();
    _joinCodeController.dispose();
    super.dispose();
  }

  Future<void> _loadHousehold() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });
    try {
      final household = await HouseholdService.getMyHousehold();
      setState(() {
        _household = household;
        _isLoading = false;
      });
      if (household != null) _loadStatistics();
    } catch (e) {
      setState(() {
        _errorMessage = NetworkError.friendlyMessage(e, fallback: 'Aile bilgisi alınamadı, lütfen tekrar deneyin.');
        _isLoading = false;
      });
    }
  }

  Future<void> _loadStatistics() async {
    setState(() {
      _isLoadingStats = true;
      _statsError = null;
    });
    try {
      final stats = await HouseholdService.getStatistics(year: _selectedYear, month: _selectedMonth);
      setState(() => _statistics = stats);
    } catch (e) {
      setState(() => _statsError = NetworkError.friendlyMessage(e, fallback: 'İstatistik alınamadı, lütfen tekrar deneyin.'));
    } finally {
      if (mounted) setState(() => _isLoadingStats = false);
    }
  }

  void _changeMonth(int delta) {
    setState(() {
      final newMonth = _selectedMonth + delta;
      if (newMonth < 1) {
        _selectedMonth = 12;
        _selectedYear -= 1;
      } else if (newMonth > 12) {
        _selectedMonth = 1;
        _selectedYear += 1;
      } else {
        _selectedMonth = newMonth;
      }
    });
    _loadStatistics();
  }

  Future<void> _handleCreate() async {
    final name = _createNameController.text.trim();
    if (name.isEmpty || _isSubmitting) return;
    setState(() => _isSubmitting = true);
    try {
      final household = await HouseholdService.createHousehold(name);
      _createNameController.clear();
      setState(() {
        _household = household;
        _isSubmitting = false;
      });
      _loadStatistics();
    } catch (e) {
      setState(() => _isSubmitting = false);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
      }
    }
  }

  Future<void> _handleJoin() async {
    final code = _joinCodeController.text.trim();
    if (code.isEmpty || _isSubmitting) return;
    setState(() => _isSubmitting = true);
    try {
      final household = await HouseholdService.joinHousehold(code);
      _joinCodeController.clear();
      setState(() {
        _household = household;
        _isSubmitting = false;
      });
      _loadStatistics();
    } catch (e) {
      setState(() => _isSubmitting = false);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
      }
    }
  }

  Future<void> _handleLeave() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: const Text('Aileden Ayrıl', style: TextStyle(fontWeight: FontWeight.w700)),
        content: const Text(
          'Aileden ayrılmak istediğine emin misin? Kendi fiş/kategori/bütçelerin etkilenmez, '
          'sadece bu ailenin toplu görünümünden çıkarsın.',
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Vazgeç')),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            style: TextButton.styleFrom(foregroundColor: AppColors.error),
            child: const Text('Ayrıl'),
          ),
        ],
      ),
    );
    if (confirmed != true) return;

    try {
      await HouseholdService.leaveHousehold();
      setState(() {
        _household = null;
        _statistics = null;
      });
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
      }
    }
  }

  Future<void> _shareInviteCode() async {
    final household = _household;
    if (household == null) return;
    await SharePlus.instance.share(ShareParams(
      text: 'FişBu\'da "${household.name}" ailesine katıl! Davet kodu: ${household.inviteCode}',
    ));
  }

  String _monthLabel(int month) {
    const months = [
      'Ocak', 'Şubat', 'Mart', 'Nisan', 'Mayıs', 'Haziran',
      'Temmuz', 'Ağustos', 'Eylül', 'Ekim', 'Kasım', 'Aralık'
    ];
    return months[month - 1];
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Aile Bütçesi')),
      body: Column(
        children: [
          const OfflineBanner(),
          Expanded(
            child: _isLoading
                ? const Center(child: CircularProgressIndicator(color: AppColors.primary))
                : _errorMessage != null
                    ? _buildErrorView()
                    : _household == null
                        ? _buildJoinCreateView()
                        : _buildHouseholdView(),
          ),
        ],
      ),
    );
  }

  Widget _buildErrorView() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(_errorMessage!, textAlign: TextAlign.center,
                style: TextStyle(color: AppColors.textSecondary)),
            const SizedBox(height: 12),
            ElevatedButton(onPressed: _loadHousehold, child: const Text('Tekrar Dene')),
          ],
        ),
      ),
    );
  }

  Widget _buildJoinCreateView() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Icon(Icons.family_restroom, size: 56, color: AppColors.primary),
          const SizedBox(height: 12),
          Text(
            'Henüz bir aileye üye değilsin',
            textAlign: TextAlign.center,
            style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700, color: AppColors.txt(context)),
          ),
          const SizedBox(height: 4),
          Text(
            'Bir aile oluştur ya da davet koduyla mevcut bir aileye katıl. Fiş/kategori/bütçelerin '
            'her zaman senin kalır, aile sadece toplu harcama görünümü sağlar.',
            textAlign: TextAlign.center,
            style: TextStyle(fontSize: 13, color: AppColors.textSecondary, height: 1.4),
          ),
          const SizedBox(height: 28),
          _buildCard(
            title: 'Yeni Aile Oluştur',
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                TextField(
                  controller: _createNameController,
                  decoration: InputDecoration(
                    hintText: 'örn. Yılmaz Ailesi',
                    filled: true,
                    fillColor: AppColors.surf(context),
                    border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                  ),
                ),
                const SizedBox(height: 12),
                ElevatedButton(
                  onPressed: _isSubmitting ? null : _handleCreate,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppColors.primary,
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(vertical: 14),
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                  ),
                  child: const Text('Aile Oluştur'),
                ),
              ],
            ),
          ),
          const SizedBox(height: 20),
          _buildCard(
            title: 'Davet Koduyla Katıl',
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                TextField(
                  controller: _joinCodeController,
                  textCapitalization: TextCapitalization.characters,
                  decoration: InputDecoration(
                    hintText: 'örn. AB12CD',
                    filled: true,
                    fillColor: AppColors.surf(context),
                    border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                  ),
                ),
                const SizedBox(height: 12),
                OutlinedButton(
                  onPressed: _isSubmitting ? null : _handleJoin,
                  style: OutlinedButton.styleFrom(
                    foregroundColor: AppColors.primary,
                    padding: const EdgeInsets.symmetric(vertical: 14),
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                  ),
                  child: const Text('Katıl'),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildCard({required String title, required Widget child}) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.surf(context),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppColors.brd(context)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(title, style: TextStyle(fontSize: 14, fontWeight: FontWeight.w700, color: AppColors.txt(context))),
          const SizedBox(height: 12),
          child,
        ],
      ),
    );
  }

  Widget _buildHouseholdView() {
    final household = _household!;
    return RefreshIndicator(
      onRefresh: () async {
        await _loadHousehold();
      },
      child: ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.all(20),
        children: [
          Container(
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(
              gradient: const LinearGradient(
                colors: [Color(0xFF6C63FF), Color(0xFF9C8FFF)],
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
              ),
              borderRadius: BorderRadius.circular(16),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(household.name,
                    style: const TextStyle(color: Colors.white, fontSize: 20, fontWeight: FontWeight.w800)),
                const SizedBox(height: 12),
                Row(
                  children: [
                    Expanded(
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                        decoration: BoxDecoration(
                          color: Colors.white.withOpacity(0.15),
                          borderRadius: BorderRadius.circular(10),
                        ),
                        child: Text(
                          'Davet kodu: ${household.inviteCode}',
                          style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 13),
                        ),
                      ),
                    ),
                    IconButton(
                      onPressed: () async {
                        await Clipboard.setData(ClipboardData(text: household.inviteCode));
                        if (mounted) {
                          ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(content: Text('Davet kodu kopyalandı')),
                          );
                        }
                      },
                      icon: const Icon(Icons.copy, color: Colors.white, size: 20),
                    ),
                    IconButton(
                      onPressed: _shareInviteCode,
                      icon: const Icon(Icons.ios_share, color: Colors.white, size: 20),
                    ),
                  ],
                ),
              ],
            ),
          ),
          const SizedBox(height: 20),
          Text('Üyeler (${household.members.length})',
              style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700, color: AppColors.txt(context))),
          const SizedBox(height: 10),
          ...household.members.map((m) => Container(
                margin: const EdgeInsets.only(bottom: 8),
                padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                decoration: BoxDecoration(
                  color: AppColors.surf(context),
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: AppColors.brd(context)),
                ),
                child: Row(
                  children: [
                    CircleAvatar(
                      radius: 16,
                      backgroundColor: AppColors.primDim(context),
                      child: Text(
                        (m.name?.isNotEmpty == true ? m.name![0] : m.email[0]).toUpperCase(),
                        style: const TextStyle(color: AppColors.primary, fontWeight: FontWeight.w700),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Text(
                        m.name?.isNotEmpty == true ? m.name! : m.email,
                        style: TextStyle(fontWeight: FontWeight.w600, color: AppColors.txt(context)),
                      ),
                    ),
                  ],
                ),
              )),
          const SizedBox(height: 24),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text('Bu Ayki Toplu Harcama',
                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700, color: AppColors.txt(context))),
              Row(
                children: [
                  IconButton(
                    onPressed: () => _changeMonth(-1),
                    icon: const Icon(Icons.chevron_left),
                    visualDensity: VisualDensity.compact,
                  ),
                  Text('${_monthLabel(_selectedMonth)} $_selectedYear',
                      style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600, color: AppColors.textSecondary)),
                  IconButton(
                    onPressed: () => _changeMonth(1),
                    icon: const Icon(Icons.chevron_right),
                    visualDensity: VisualDensity.compact,
                  ),
                ],
              ),
            ],
          ),
          const SizedBox(height: 10),
          _buildStatisticsSection(),
          const SizedBox(height: 32),
          OutlinedButton.icon(
            onPressed: _handleLeave,
            icon: const Icon(Icons.exit_to_app, size: 18),
            label: const Text('Aileden Ayrıl'),
            style: OutlinedButton.styleFrom(
              foregroundColor: AppColors.error,
              side: BorderSide(color: AppColors.error.withOpacity(0.4)),
              padding: const EdgeInsets.symmetric(vertical: 14),
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildStatisticsSection() {
    if (_isLoadingStats) {
      return const Center(
        child: Padding(
          padding: EdgeInsets.symmetric(vertical: 20),
          child: CircularProgressIndicator(color: AppColors.primary),
        ),
      );
    }
    if (_statsError != null) {
      return Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: AppColors.surf(context),
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: AppColors.brd(context)),
        ),
        child: Text(_statsError!, style: TextStyle(color: AppColors.textSecondary)),
      );
    }
    final stats = _statistics;
    if (stats == null) return const SizedBox();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Container(
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: AppColors.surf(context),
            borderRadius: BorderRadius.circular(16),
            border: Border.all(color: AppColors.brd(context)),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('Toplam', style: TextStyle(fontSize: 12, color: AppColors.textSecondary)),
              const SizedBox(height: 4),
              Text(
                '${stats.totalAmount.toStringAsFixed(2)} TL',
                style: const TextStyle(fontSize: 22, fontWeight: FontWeight.w800, color: AppColors.primary),
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),
        Text('Üye Bazlı', style: TextStyle(fontSize: 13, fontWeight: FontWeight.w700, color: AppColors.txt(context))),
        const SizedBox(height: 8),
        if (stats.byMember.isEmpty)
          Text('Bu ay veri yok', style: TextStyle(fontSize: 12, color: AppColors.textSecondary))
        else
          ...stats.byMember.map((m) => _buildTotalRow(
                label: m.name?.isNotEmpty == true ? m.name! : m.email,
                amount: m.totalAmount,
                total: stats.totalAmount,
              )),
        const SizedBox(height: 16),
        Text('Kategori Bazlı', style: TextStyle(fontSize: 13, fontWeight: FontWeight.w700, color: AppColors.txt(context))),
        const SizedBox(height: 8),
        if (stats.byCategory.isEmpty)
          Text('Bu ay veri yok', style: TextStyle(fontSize: 12, color: AppColors.textSecondary))
        else
          ...stats.byCategory.map((c) => _buildTotalRow(
                label: c.categoryName,
                amount: c.totalAmount,
                total: stats.totalAmount,
              )),
      ],
    );
  }

  Widget _buildTotalRow({required String label, required double amount, required double total}) {
    final percentage = total > 0 ? amount / total : 0.0;
    return Container(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: AppColors.surf(context),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: AppColors.brd(context)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Expanded(
                child: Text(label,
                    style: TextStyle(fontWeight: FontWeight.w600, fontSize: 13, color: AppColors.txt(context)),
                    maxLines: 1, overflow: TextOverflow.ellipsis),
              ),
              Text('${amount.toStringAsFixed(2)} TL',
                  style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 13, color: AppColors.primary)),
            ],
          ),
          const SizedBox(height: 6),
          ClipRRect(
            borderRadius: BorderRadius.circular(4),
            child: LinearProgressIndicator(
              value: percentage.clamp(0, 1).toDouble(),
              backgroundColor: AppColors.brd(context),
              valueColor: const AlwaysStoppedAnimation<Color>(AppColors.primary),
              minHeight: 5,
            ),
          ),
        ],
      ),
    );
  }
}
