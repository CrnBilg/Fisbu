import 'package:flutter/material.dart';
import '../models/receipt.dart';
import '../models/receipt_item.dart';
import '../services/receipt_service.dart';
import 'package:intl/intl.dart';
import '../core/utils/date_formatter.dart';
import '../core/utils/category_helper.dart';
import '../core/theme/app_colors.dart';
import 'split_bill_screen.dart';

class ReceiptDetailScreen extends StatefulWidget {
  final Receipt receipt;

  const ReceiptDetailScreen({super.key, required this.receipt});

  @override
  State<ReceiptDetailScreen> createState() => _ReceiptDetailScreenState();
}

class _ReceiptDetailScreenState extends State<ReceiptDetailScreen> {
  bool _isDeleting = false;
  bool _isSavingReminders = false;
  final _currencyFormat = NumberFormat('#,##0.00', 'tr_TR');
  late Receipt _receipt;

  @override
  void initState() {
    super.initState();
    _receipt = widget.receipt;
  }

  String _formatDate(DateTime date) =>
      '${date.year}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}';

  int? _daysUntil(String? isoDate) {
    if (isoDate == null) return null;
    try {
      final date = DateTime.parse(isoDate);
      final today = DateTime.now();
      return DateTime(date.year, date.month, date.day)
          .difference(DateTime(today.year, today.month, today.day))
          .inDays;
    } catch (_) {
      return null;
    }
  }

  String _reminderLabel(String isoDate, int daysLeft) {
    final formatted = DateFormatter.formatLong(isoDate);
    if (daysLeft < 0) return '$formatted (süresi doldu)';
    if (daysLeft == 0) return '$formatted (bugün)';
    return '$formatted ($daysLeft gün kaldı)';
  }

  Future<void> _showRemindersSheet() async {
    DateTime? returnDeadline =
        _receipt.returnDeadline != null ? DateTime.tryParse(_receipt.returnDeadline!) : null;
    DateTime? warrantyExpiryDate =
        _receipt.warrantyExpiryDate != null ? DateTime.tryParse(_receipt.warrantyExpiryDate!) : null;

    final result = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(24))),
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setSheetState) => Padding(
          padding: EdgeInsets.fromLTRB(20, 20, 20, MediaQuery.of(ctx).viewInsets.bottom + 24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Center(
                child: Container(
                  width: 40,
                  height: 4,
                  decoration: BoxDecoration(
                    color: AppColors.brd(ctx),
                    borderRadius: BorderRadius.circular(2),
                  ),
                ),
              ),
              const SizedBox(height: 20),
              Text('Garanti / İade Hatırlatıcısı',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700, color: AppColors.txt(ctx))),
              const SizedBox(height: 16),
              _buildDateField(
                label: 'İade Son Tarihi',
                value: returnDeadline,
                onPick: () async {
                  final picked = await showDatePicker(
                    context: ctx,
                    initialDate: returnDeadline ?? DateTime.now(),
                    firstDate: DateTime.now().subtract(const Duration(days: 365)),
                    lastDate: DateTime.now().add(const Duration(days: 365 * 3)),
                  );
                  if (picked != null) setSheetState(() => returnDeadline = picked);
                },
                onClear: () => setSheetState(() => returnDeadline = null),
              ),
              const SizedBox(height: 14),
              _buildDateField(
                label: 'Garanti Bitiş Tarihi',
                value: warrantyExpiryDate,
                onPick: () async {
                  final picked = await showDatePicker(
                    context: ctx,
                    initialDate: warrantyExpiryDate ?? DateTime.now(),
                    firstDate: DateTime.now().subtract(const Duration(days: 365)),
                    lastDate: DateTime.now().add(const Duration(days: 365 * 10)),
                  );
                  if (picked != null) setSheetState(() => warrantyExpiryDate = picked);
                },
                onClear: () => setSheetState(() => warrantyExpiryDate = null),
              ),
              const SizedBox(height: 20),
              ElevatedButton(
                onPressed: () => Navigator.pop(ctx, true),
                style: ElevatedButton.styleFrom(
                  padding: const EdgeInsets.symmetric(vertical: 16),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                ),
                child: const Text('Kaydet', style: TextStyle(fontSize: 15, fontWeight: FontWeight.w600)),
              ),
            ],
          ),
        ),
      ),
    );

    if (result != true) return;

    setState(() => _isSavingReminders = true);
    try {
      final updated = await ReceiptService.setReminders(
        _receipt.id,
        returnDeadline: returnDeadline != null ? _formatDate(returnDeadline!) : null,
        warrantyExpiryDate: warrantyExpiryDate != null ? _formatDate(warrantyExpiryDate!) : null,
      );
      if (mounted) setState(() => _receipt = updated);
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Hatırlatıcı kaydedilemedi: $e')));
      }
    } finally {
      if (mounted) setState(() => _isSavingReminders = false);
    }
  }

  Widget _buildDateField({
    required String label,
    required DateTime? value,
    required VoidCallback onPick,
    required VoidCallback onClear,
  }) {
    return InkWell(
      onTap: onPick,
      borderRadius: BorderRadius.circular(12),
      child: InputDecorator(
        decoration: InputDecoration(
          labelText: label,
          prefixIcon: const Icon(Icons.event_outlined),
          suffixIcon: value != null
              ? IconButton(icon: const Icon(Icons.clear, size: 18), onPressed: onClear)
              : null,
          border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
        ),
        child: Text(value == null ? 'Seçilmedi' : _formatDate(value)),
      ),
    );
  }

  Future<void> _confirmDelete() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: Text('Fişi Sil',
            style: TextStyle(fontWeight: FontWeight.w700)),
        content: Text('Bu fişi silmek istediğine emin misin?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: Text('Vazgeç'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            style: TextButton.styleFrom(foregroundColor: AppColors.error),
            child: Text('Sil'),
          ),
        ],
      ),
    );
    if (confirmed == true) _deleteReceipt();
  }

  Future<void> _deleteReceipt() async {
    setState(() => _isDeleting = true);
    try {
      await ReceiptService.deleteReceipt(widget.receipt.id);
      if (!mounted) return;
      Navigator.pop(context, true);
    } catch (e) {
      setState(() => _isDeleting = false);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Fiş silinemedi: $e')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final receipt = _receipt;

    return Scaffold(

      appBar: AppBar(
        title: Text('Fiş Detayı'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // Üst kart — gradient
            Container(
              padding: const EdgeInsets.all(28),
              decoration: BoxDecoration(
                gradient: const LinearGradient(
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                  colors: [AppColors.primary, AppColors.primaryDark],
                ),
                borderRadius: BorderRadius.circular(24),
                boxShadow: [
                  BoxShadow(
                    color: AppColors.primary.withOpacity(0.3),
                    blurRadius: 20,
                    offset: const Offset(0, 8),
                  ),
                ],
              ),
              child: Column(
                children: [
                  Container(
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      color: Colors.white24,
                      borderRadius: BorderRadius.circular(16),
                    ),
                    child: Icon(
                      CategoryHelper.getIcon(receipt.categoryName),
                      size: 36,
                      color: Colors.white,
                    ),
                  ),
                  const SizedBox(height: 16),
                  Text(
                    receipt.storeName,
                    style: TextStyle(
                      fontSize: 22,
                      fontWeight: FontWeight.w800,
                      color: Colors.white,
                      letterSpacing: -0.3,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    '${_currencyFormat.format(receipt.totalAmount)} TL',
                    style: TextStyle(
                      fontSize: 36,
                      fontWeight: FontWeight.w800,
                      color: Colors.white,
                      letterSpacing: -1,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 24),

            // Detay kartı
            Container(
              decoration: BoxDecoration(
                color: AppColors.surf(context),
                borderRadius: BorderRadius.circular(20),
                border: Border.all(color: AppColors.brd(context)),
              ),
              child: Column(
                children: [
                  _buildDetailRow(
                    icon: Icons.category_outlined,
                    label: 'Kategori',
                    value: receipt.categoryName ?? 'Kategorisiz',
                    isFirst: true,
                  ),
                  _buildDetailRow(
                    icon: Icons.calendar_today_outlined,
                    label: 'Fiş Tarihi',
                    value: DateFormatter.formatLong(receipt.receiptDate),
                  ),
                  _buildDetailRow(
                    icon: Icons.access_time_outlined,
                    label: 'Eklenme Tarihi',
                    value: DateFormatter.formatLong(
                      receipt.createdAt?.split('T').first,
                    ),
                  ),
                  _buildDetailRow(
                    icon: Icons.store_outlined,
                    label: 'Mağaza',
                    value: receipt.storeName,
                    isLast: receipt.returnDeadline == null && receipt.warrantyExpiryDate == null,
                  ),
                  if (receipt.returnDeadline != null)
                    _buildDetailRow(
                      icon: Icons.assignment_return_outlined,
                      label: 'İade Son Tarihi',
                      value: _reminderLabel(receipt.returnDeadline!, _daysUntil(receipt.returnDeadline!) ?? 0),
                      isLast: receipt.warrantyExpiryDate == null,
                    ),
                  if (receipt.warrantyExpiryDate != null)
                    _buildDetailRow(
                      icon: Icons.verified_user_outlined,
                      label: 'Garanti Bitişi',
                      value: _reminderLabel(receipt.warrantyExpiryDate!, _daysUntil(receipt.warrantyExpiryDate!) ?? 0),
                      isLast: true,
                    ),
                ],
              ),
            ),

            // Ürünler (opsiyonel eklenmişse)
            if (receipt.items.isNotEmpty) ...[
              const SizedBox(height: 24),
              _buildItemsCard(receipt.items),
            ],
            const SizedBox(height: 24),

            // Fiş fotoğrafı (varsa göster)
            if (receipt.imageUrl != null && receipt.imageUrl!.isNotEmpty) ...[
              GestureDetector(
                onTap: () {
                  Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (context) => _FullScreenImage(
                        imageUrl: receipt.imageUrl!,
                        storeName: receipt.storeName,
                      ),
                    ),
                  );
                },
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(20),
                  child: Stack(
                    children: [
                      Image.network(
                        receipt.imageUrl!,
                        width: double.infinity,
                        height: 300,
                        fit: BoxFit.contain,
                        loadingBuilder: (context, child, loadingProgress) {
                          if (loadingProgress == null) return child;
                          return Container(
                            height: 200,
                            decoration: BoxDecoration(
                              color: AppColors.primDim(context),
                              borderRadius: BorderRadius.circular(20),
                            ),
                            child: const Center(
                              child: CircularProgressIndicator(
                                  color: AppColors.primary),
                            ),
                          );
                        },
                        errorBuilder: (context, error, stackTrace) {
                          return Container(
                            height: 120,
                            decoration: BoxDecoration(
                              color: AppColors.primDim(context),
                              borderRadius: BorderRadius.circular(20),
                            ),
                            child: const Center(
                              child: Column(
                                mainAxisAlignment: MainAxisAlignment.center,
                                children: [
                                  Icon(Icons.broken_image_outlined,
                                      color: AppColors.textSecondary, size: 32),
                                  SizedBox(height: 8),
                                  Text('Fotoğraf yüklenemedi',
                                      style: TextStyle(
                                          color: AppColors.textSecondary,
                                          fontSize: 13)),
                                ],
                              ),
                            ),
                          );
                        },
                      ),
                      Positioned(
                        bottom: 10,
                        right: 10,
                        child: Container(
                          padding: const EdgeInsets.symmetric(
                              horizontal: 10, vertical: 6),
                          decoration: BoxDecoration(
                            color: Colors.black54,
                            borderRadius: BorderRadius.circular(20),
                          ),
                          child: const Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Icon(Icons.fullscreen,
                                  color: Colors.white, size: 16),
                              SizedBox(width: 4),
                              Text('Büyüt',
                                  style: TextStyle(
                                      color: Colors.white, fontSize: 12)),
                            ],
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 24),
            ],

            if (receipt.splitParticipants != null && receipt.splitParticipants!.isNotEmpty) ...[
              Container(
                margin: const EdgeInsets.only(bottom: 12),
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: AppColors.surf(context),
                  borderRadius: BorderRadius.circular(14),
                  border: Border.all(color: AppColors.brd(context)),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        const Icon(Icons.call_split, color: AppColors.primary, size: 18),
                        const SizedBox(width: 8),
                        Text(
                          '${receipt.splitParticipants!.length} kişi arasında bölüşüldü',
                          style: TextStyle(
                            fontWeight: FontWeight.w700,
                            fontSize: 14,
                            color: AppColors.txt(context),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 10),
                    for (final p in receipt.splitParticipants!)
                      Padding(
                        padding: const EdgeInsets.only(bottom: 4),
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Text(p.name, style: TextStyle(color: AppColors.txtSecondary(context), fontSize: 13)),
                            Text(
                              '${_currencyFormat.format(p.amount)} TL',
                              style: TextStyle(fontWeight: FontWeight.w600, fontSize: 13, color: AppColors.txt(context)),
                            ),
                          ],
                        ),
                      ),
                  ],
                ),
              ),
            ],

            // Garanti/iade hatırlatıcı butonu
            GestureDetector(
              onTap: _isSavingReminders ? null : _showRemindersSheet,
              child: Container(
                margin: const EdgeInsets.only(bottom: 12),
                padding: const EdgeInsets.symmetric(vertical: 16),
                decoration: BoxDecoration(
                  color: AppColors.surf(context),
                  borderRadius: BorderRadius.circular(14),
                  border: Border.all(color: AppColors.brd(context)),
                ),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    _isSavingReminders
                        ? const SizedBox(
                            height: 18,
                            width: 18,
                            child: CircularProgressIndicator(strokeWidth: 2, color: AppColors.primary),
                          )
                        : Icon(Icons.notifications_active_outlined, color: AppColors.txt(context), size: 20),
                    const SizedBox(width: 8),
                    Text(
                      receipt.returnDeadline != null || receipt.warrantyExpiryDate != null
                          ? 'Hatırlatıcıyı Düzenle'
                          : 'Garanti/İade Hatırlatıcısı Ekle',
                      style: TextStyle(
                        color: AppColors.txt(context),
                        fontSize: 15,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ],
                ),
              ),
            ),

            // Böl / Paylaştır butonu
            GestureDetector(
              onTap: () async {
                final result = await Navigator.push<Receipt>(
                  context,
                  MaterialPageRoute(
                    builder: (context) => SplitBillScreen(receipt: receipt),
                  ),
                );
                if (result != null && mounted) {
                  setState(() => _receipt = result);
                }
              },
              child: Container(
                margin: const EdgeInsets.only(bottom: 12),
                padding: const EdgeInsets.symmetric(vertical: 16),
                decoration: BoxDecoration(
                  color: AppColors.primDim(context),
                  borderRadius: BorderRadius.circular(14),
                  border: Border.all(color: AppColors.primary.withOpacity(0.3)),
                ),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    const Icon(Icons.call_split, color: AppColors.primary, size: 20),
                    const SizedBox(width: 8),
                    Text(
                      'Böl / Paylaştır',
                      style: TextStyle(
                        color: AppColors.primary,
                        fontSize: 15,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ],
                ),
              ),
            ),

            // Sil butonu
            GestureDetector(
              onTap: _isDeleting ? null : _confirmDelete,
              child: Container(
                padding: const EdgeInsets.symmetric(vertical: 16),
                decoration: BoxDecoration(
                  color: AppColors.errDim(context),
                  borderRadius: BorderRadius.circular(14),
                  border: Border.all(color: AppColors.error.withOpacity(0.3)),
                ),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    _isDeleting
                        ? const SizedBox(
                            height: 18,
                            width: 18,
                            child: CircularProgressIndicator(
                                strokeWidth: 2, color: AppColors.error),
                          )
                        : const Icon(Icons.delete_outline,
                            color: AppColors.error, size: 20),
                    const SizedBox(width: 8),
                    Text(
                      'Fişi Sil',
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
    );
  }

  Widget _buildItemsCard(List<ReceiptItem> items) {
    return Container(
      decoration: BoxDecoration(
        color: AppColors.surf(context),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: AppColors.brd(context)),
      ),
      child: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(20, 16, 20, 0),
            child: Row(
              children: [
                Text('Ürünler',
                    style: TextStyle(
                        fontSize: 15,
                        fontWeight: FontWeight.w700,
                        color: AppColors.txt(context))),
              ],
            ),
          ),
          for (int i = 0; i < items.length; i++)
            _buildItemRow(items[i], isLast: i == items.length - 1),
        ],
      ),
    );
  }

  Widget _buildItemRow(ReceiptItem item, {bool isLast = false}) {
    final qty = item.quantity == item.quantity.roundToDouble()
        ? item.quantity.toStringAsFixed(0)
        : item.quantity.toStringAsFixed(2);
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
      decoration: BoxDecoration(
        border: isLast
            ? null
            : Border(bottom: BorderSide(color: AppColors.brd(context))),
      ),
      child: Row(
        children: [
          Expanded(
            child: Text('${item.productName} × $qty',
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: TextStyle(
                    fontSize: 14,
                    color: AppColors.txt(context),
                    fontWeight: FontWeight.w500)),
          ),
          Text('${_currencyFormat.format(item.unitPrice * item.quantity)} TL',
              style: TextStyle(
                  fontSize: 15,
                  fontWeight: FontWeight.w700,
                  color: AppColors.txt(context))),
        ],
      ),
    );
  }

  Widget _buildDetailRow({
    required IconData icon,
    required String label,
    required String value,
    bool isFirst = false,
    bool isLast = false,
  }) {
    return Container(
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
          Text(label,
              style: TextStyle(
                  fontSize: 14,
                  color: AppColors.txtSecondary(context),
                  fontWeight: FontWeight.w500)),
          Expanded(
            child: Text(value,
                textAlign: TextAlign.right,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: TextStyle(
                    fontSize: 15,
                    fontWeight: FontWeight.w700,
                    color: AppColors.txt(context))),
          ),
        ],
      ),
    );
  }
}

class _FullScreenImage extends StatelessWidget {
  final String imageUrl;
  final String storeName;

  const _FullScreenImage({required this.imageUrl, required this.storeName});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      appBar: AppBar(
        backgroundColor: Colors.black,
        foregroundColor: Colors.white,
        title: Text(storeName,
            style: TextStyle(color: Colors.white)),
      ),
      body: InteractiveViewer(
        minScale: 0.5,
        maxScale: 4.0,
        child: Center(
          child: Image.network(
            imageUrl,
            fit: BoxFit.contain,
            loadingBuilder: (context, child, loadingProgress) {
              if (loadingProgress == null) return child;
              return const Center(
                child: CircularProgressIndicator(color: Colors.white),
              );
            },
          ),
        ),
      ),
    );
  }
}