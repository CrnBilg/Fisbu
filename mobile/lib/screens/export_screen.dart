import 'dart:io';
import 'package:flutter/material.dart';
import 'package:path_provider/path_provider.dart';
import 'package:share_plus/share_plus.dart';
import '../services/receipt_service.dart';
import '../core/theme/app_colors.dart';

class ExportScreen extends StatefulWidget {
  const ExportScreen({super.key});

  @override
  State<ExportScreen> createState() => _ExportScreenState();
}

class _ExportFormat {
  final String value;
  final String label;
  final String extension;
  final IconData icon;

  const _ExportFormat(this.value, this.label, this.extension, this.icon);
}

class _ExportScreenState extends State<ExportScreen> {
  static const _formats = [
    _ExportFormat('pdf', 'PDF', 'pdf', Icons.picture_as_pdf_outlined),
    _ExportFormat('excel', 'Excel', 'xlsx', Icons.table_chart_outlined),
    _ExportFormat('csv', 'CSV', 'csv', Icons.insert_drive_file_outlined),
  ];

  DateTime _startDate = DateTime(DateTime.now().year, DateTime.now().month, 1);
  DateTime _endDate = DateTime.now();
  String _selectedFormat = 'pdf';
  bool _isExporting = false;
  String? _error;

  String _formatDate(DateTime date) =>
      '${date.year}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}';

  String _formatDateDisplay(DateTime date) =>
      '${date.day.toString().padLeft(2, '0')}.${date.month.toString().padLeft(2, '0')}.${date.year}';

  Future<void> _pickStartDate() async {
    final picked = await showDatePicker(
      context: context,
      initialDate: _startDate,
      firstDate: DateTime(2020),
      lastDate: DateTime.now(),
    );
    if (picked != null) {
      setState(() {
        _startDate = picked;
        if (_endDate.isBefore(_startDate)) {
          _endDate = _startDate;
        }
      });
    }
  }

  Future<void> _pickEndDate() async {
    final picked = await showDatePicker(
      context: context,
      initialDate: _endDate,
      firstDate: _startDate,
      lastDate: DateTime.now(),
    );
    if (picked != null) {
      setState(() => _endDate = picked);
    }
  }

  Future<void> _export() async {
    setState(() {
      _isExporting = true;
      _error = null;
    });

    try {
      final bytes = await ReceiptService.exportReceipts(
        format: _selectedFormat,
        start: _formatDate(_startDate),
        end: _formatDate(_endDate),
      );

      final format = _formats.firstWhere((f) => f.value == _selectedFormat);
      final filename =
          'fisler_${_formatDate(_startDate)}_${_formatDate(_endDate)}.${format.extension}';

      final dir = await getTemporaryDirectory();
      final file = File('${dir.path}/$filename');
      await file.writeAsBytes(bytes);

      if (!mounted) return;
      await SharePlus.instance.share(
        ShareParams(files: [XFile(file.path)], text: 'FişBu fiş raporu'),
      );
    } catch (e) {
      setState(() => _error = 'Dışa aktarma başarısız oldu: $e');
    } finally {
      if (mounted) setState(() => _isExporting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Fiş Raporu Dışa Aktar')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(
              'Tarih Aralığı',
              style: TextStyle(
                fontSize: 15,
                fontWeight: FontWeight.w700,
                color: AppColors.txt(context),
              ),
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: InkWell(
                    onTap: _pickStartDate,
                    borderRadius: BorderRadius.circular(12),
                    child: InputDecorator(
                      decoration: InputDecoration(
                        labelText: 'Başlangıç',
                        prefixIcon: const Icon(Icons.calendar_today_outlined),
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(12),
                        ),
                        filled: true,
                        fillColor: AppColors.surf(context),
                      ),
                      child: Text(_formatDateDisplay(_startDate)),
                    ),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: InkWell(
                    onTap: _pickEndDate,
                    borderRadius: BorderRadius.circular(12),
                    child: InputDecorator(
                      decoration: InputDecoration(
                        labelText: 'Bitiş',
                        prefixIcon: const Icon(Icons.calendar_today_outlined),
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(12),
                        ),
                        filled: true,
                        fillColor: AppColors.surf(context),
                      ),
                      child: Text(_formatDateDisplay(_endDate)),
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 24),
            Text(
              'Format',
              style: TextStyle(
                fontSize: 15,
                fontWeight: FontWeight.w700,
                color: AppColors.txt(context),
              ),
            ),
            const SizedBox(height: 12),
            Row(
              children: _formats.map((format) {
                final isSelected = _selectedFormat == format.value;
                return Expanded(
                  child: Padding(
                    padding: EdgeInsets.only(
                        right: format == _formats.last ? 0 : 12),
                    child: InkWell(
                      onTap: () => setState(() => _selectedFormat = format.value),
                      borderRadius: BorderRadius.circular(14),
                      child: Container(
                        padding: const EdgeInsets.symmetric(vertical: 16),
                        decoration: BoxDecoration(
                          color: isSelected
                              ? AppColors.primDim(context)
                              : AppColors.surf(context),
                          borderRadius: BorderRadius.circular(14),
                          border: Border.all(
                            color: isSelected
                                ? AppColors.primary
                                : AppColors.brd(context),
                            width: isSelected ? 2 : 1,
                          ),
                        ),
                        child: Column(
                          children: [
                            Icon(
                              format.icon,
                              color: isSelected
                                  ? AppColors.primary
                                  : AppColors.txtSecondary(context),
                            ),
                            const SizedBox(height: 6),
                            Text(
                              format.label,
                              style: TextStyle(
                                fontWeight: FontWeight.w700,
                                fontSize: 13,
                                color: isSelected
                                    ? AppColors.primary
                                    : AppColors.txt(context),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                );
              }).toList(),
            ),
            const SizedBox(height: 24),
            if (_error != null)
              Container(
                margin: const EdgeInsets.only(bottom: 16),
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: AppColors.errDim(context),
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: AppColors.error.withOpacity(0.3)),
                ),
                child: Row(
                  children: [
                    const Icon(Icons.error_outline, color: AppColors.error, size: 18),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(_error!,
                          style: const TextStyle(color: AppColors.error, fontSize: 13)),
                    ),
                  ],
                ),
              ),
            ElevatedButton.icon(
              onPressed: _isExporting ? null : _export,
              icon: _isExporting
                  ? const SizedBox(
                      height: 18,
                      width: 18,
                      child: CircularProgressIndicator(
                          strokeWidth: 2, color: Colors.white),
                    )
                  : const Icon(Icons.ios_share),
              label: Text(_isExporting ? 'Hazırlanıyor...' : 'Dışa Aktar ve Paylaş'),
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.primary,
                foregroundColor: Colors.white,
                padding: const EdgeInsets.symmetric(vertical: 16),
                shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12)),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
