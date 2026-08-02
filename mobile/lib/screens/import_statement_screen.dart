import 'dart:io';
import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import '../services/receipt_service.dart';
import '../core/theme/app_colors.dart';
import 'import_review_screen.dart';

class ImportStatementScreen extends StatefulWidget {
  const ImportStatementScreen({super.key});

  @override
  State<ImportStatementScreen> createState() => _ImportStatementScreenState();
}

class _ImportStatementScreenState extends State<ImportStatementScreen> {
  bool _isProcessing = false;
  String? _error;

  Future<void> _pickAndParse() async {
    final result = await FilePicker.platform.pickFiles(
      type: FileType.custom,
      allowedExtensions: ['pdf', 'csv'],
    );
    final path = result?.files.single.path;
    if (path == null) return;

    setState(() {
      _isProcessing = true;
      _error = null;
    });

    try {
      final parsed = await ReceiptService.importStatement(File(path));
      if (!mounted) return;

      if (parsed.transactions.isEmpty) {
        setState(() {
          _isProcessing = false;
          _error = parsed.warnings.isNotEmpty
              ? parsed.warnings.join('\n')
              : 'Dosyada harcama olarak tanınan bir işlem bulunamadı.';
        });
        return;
      }

      setState(() => _isProcessing = false);
      Navigator.push(
        context,
        MaterialPageRoute(builder: (context) => ImportReviewScreen(result: parsed)),
      );
    } catch (e) {
      setState(() {
        _isProcessing = false;
        _error = 'Hata: $e';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Ekstre İçe Aktar')),
      body: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: AppColors.primDim(context),
                borderRadius: BorderRadius.circular(16),
                border: Border.all(color: AppColors.primary.withOpacity(0.2)),
              ),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Icon(Icons.info_outline, color: AppColors.primary),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Text(
                      'Banka/kredi kartı ekstresi (PDF veya CSV) ya da FişBu\'dan dışa aktardığın CSV dosyasını '
                      'yükle. İşlemleri gözden geçirip onayladıklarını fiş olarak ekleyeceğiz.',
                      style: TextStyle(fontSize: 13, color: AppColors.txt(context)),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 24),
            ElevatedButton.icon(
              onPressed: _isProcessing ? null : _pickAndParse,
              icon: _isProcessing
                  ? const SizedBox(
                      height: 16,
                      width: 16,
                      child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                    )
                  : const Icon(Icons.upload_file_outlined),
              label: Text(_isProcessing ? 'Dosya işleniyor...' : 'PDF veya CSV Seç'),
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.primary,
                foregroundColor: Colors.white,
                padding: const EdgeInsets.symmetric(vertical: 16),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
              ),
            ),
            if (_error != null)
              Container(
                margin: const EdgeInsets.only(top: 16),
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
                      child: Text(_error!, style: const TextStyle(color: AppColors.error, fontSize: 13)),
                    ),
                  ],
                ),
              ),
          ],
        ),
      ),
    );
  }
}
