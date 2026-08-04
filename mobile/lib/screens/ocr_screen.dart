import 'dart:io';
import '../services/ocr_parser.dart';
import '../services/receipt_service.dart';
import '../models/restore_receipt_result.dart';
import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:image_picker/image_picker.dart';
import 'add_receipt_screen.dart';
import 'receipt_verification_screen.dart';
import '../core/theme/app_colors.dart';

class OcrScreen extends StatefulWidget {
  const OcrScreen({super.key});

  @override
  State<OcrScreen> createState() => _OcrScreenState();
}

class _OcrScreenState extends State<OcrScreen> {
  XFile? _selectedImage;
  String _recognizedText = '';
  String? _extractedAmount;
  String? _extractedDate;
  String? _extractedStoreName;
  bool _isProcessing = false;
  bool _hasText = false;

  bool _isRestoring = false;
  RestoreReceiptResult? _aiResult;
  String? _aiError;

  static const _channel = MethodChannel('com.fisbu/ocr');

  Future<void> _pickAndRecognize(ImageSource source) async {
    final picker = ImagePicker();
    final image = await picker.pickImage(source: source, imageQuality: 90);
    if (image == null) return;

    setState(() {
      _selectedImage = image;
      _isProcessing = true;
      _recognizedText = '';
      _hasText = false;
      _aiResult = null;
      _aiError = null;
    });

    try {
      if (kIsWeb) {
        setState(() {
          _recognizedText = 'OCR web tarayıcısında çalışmaz. Lütfen telefonda deneyin.';
          _isProcessing = false;
        });
        return;
      }

      final String text = await _channel.invokeMethod('recognizeText', {
        'imagePath': image.path,
      });

      setState(() {
        _recognizedText = text.isEmpty
            ? 'Metin bulunamadı. Daha net bir fotoğraf deneyin.'
            : text;
        _hasText = text.isNotEmpty;
        _extractedAmount = OcrParser.extractAmount(text)?.toStringAsFixed(2);
        _extractedDate = OcrParser.extractDate(text);
        _extractedStoreName = OcrParser.extractStoreName(text);
        _isProcessing = false;
      });
    } catch (e) {
      setState(() {
        _recognizedText = 'Hata oluştu: $e';
        _isProcessing = false;
      });
    }
  }

  Future<void> _restoreWithAi() async {
    setState(() {
      _isRestoring = true;
      _aiResult = null;
      _aiError = null;
    });

    try {
      final result = await ReceiptService.restoreReceipt(_recognizedText);
      setState(() => _aiResult = result);
    } catch (e) {
      setState(() => _aiError = 'AI restorasyonu başarısız oldu: $e');
    } finally {
      if (mounted) setState(() => _isRestoring = false);
    }
  }

  Color _confidenceColor(int score) {
    if (score >= 90) return AppColors.success;
    if (score >= 60) return AppColors.warning;
    return AppColors.error;
  }

  String _confidenceLabel(int score) {
    if (score >= 90) return 'Yüksek Güven';
    if (score >= 60) return 'Orta Güven';
    return 'Düşük Güven';
  }

  Widget _buildExtractedRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Row(
        children: [
          Text(
            '$label: ',
            style: TextStyle(
              fontSize: 13,
              color: AppColors.txtSecondary(context),
              fontWeight: FontWeight.w600,
            ),
          ),
          Expanded(
            child: Text(
              value,
              style: TextStyle(
                fontSize: 13,
                fontWeight: FontWeight.w700,
                color: AppColors.txt(context),
              ),
            ),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Fişi Tara'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Container(
              height: 220,
              decoration: BoxDecoration(
                color: AppColors.surf(context),
                borderRadius: BorderRadius.circular(16),
                border: Border.all(color: AppColors.brd(context)),
              ),
              child: _selectedImage == null
                  ? Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Container(
                          padding: const EdgeInsets.all(16),
                          decoration: BoxDecoration(
                            color: AppColors.primary.withOpacity(0.08),
                            shape: BoxShape.circle,
                          ),
                          child: const Icon(
                            Icons.document_scanner_outlined,
                            size: 40,
                            color: AppColors.primary,
                          ),
                        ),
                        const SizedBox(height: 12),
                        const Text(
                          'Fiş fotoğrafı seç veya çek',
                          style: TextStyle(
                            fontSize: 15,
                            fontWeight: FontWeight.w600,
                            color: AppColors.primary,
                          ),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          'OCR ile metni otomatik okuyacağız',
                          style: TextStyle(
                            fontSize: 12,
                            color: AppColors.textSecondary,
                          ),
                        ),
                      ],
                    )
                  : ClipRRect(
                      borderRadius: BorderRadius.circular(15),
                      child: Image.file(
                        File(_selectedImage!.path),
                        fit: BoxFit.cover,
                        width: double.infinity,
                      ),
                    ),
            ),
            const SizedBox(height: 16),
            Row(
              children: [
                Expanded(
                  child: ElevatedButton.icon(
                    onPressed: _isProcessing
                        ? null
                        : () => _pickAndRecognize(ImageSource.gallery),
                    icon: const Icon(Icons.photo_library_outlined),
                    label: const Text('Galeriden Seç'),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: AppColors.primary,
                      foregroundColor: Colors.white,
                      padding: const EdgeInsets.symmetric(vertical: 14),
                      shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(12)),
                    ),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: ElevatedButton.icon(
                    onPressed: _isProcessing
                        ? null
                        : () => _pickAndRecognize(ImageSource.camera),
                    icon: const Icon(Icons.camera_alt_outlined),
                    label: const Text('Kamera'),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: AppColors.success,
                      foregroundColor: Colors.white,
                      padding: const EdgeInsets.symmetric(vertical: 14),
                      shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(12)),
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 24),
            if (_isProcessing)
              Center(
                child: Column(
                  children: [
                    const CircularProgressIndicator(color: AppColors.primary),
                    const SizedBox(height: 12),
                    Text(
                      'Metin okunuyor...',
                      style: TextStyle(
                        color: AppColors.txtSecondary(context),
                        fontSize: 14,
                      ),
                    ),
                  ],
                ),
              ),
            if (_extractedStoreName != null ||
                _extractedAmount != null ||
                _extractedDate != null)
              Container(
                margin: const EdgeInsets.only(bottom: 16),
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: AppColors.primDim(context).withOpacity(
                    Theme.of(context).brightness == Brightness.dark ? 1 : 0.6,
                  ),
                  borderRadius: BorderRadius.circular(16),
                  border: Border.all(
                      color: AppColors.primary.withOpacity(0.2)),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Row(
                      children: [
                        Icon(Icons.auto_awesome,
                            color: AppColors.primary, size: 18),
                        SizedBox(width: 8),
                        Text(
                          'Otomatik Çıkarılan Bilgiler',
                          style: TextStyle(
                            fontSize: 14,
                            fontWeight: FontWeight.w700,
                            color: AppColors.primary,
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 12),
                    if (_extractedStoreName != null)
                      _buildExtractedRow('Mağaza', _extractedStoreName!),
                    if (_extractedAmount != null)
                      _buildExtractedRow('Tutar', '$_extractedAmount TL'),
                    if (_extractedDate != null)
                      _buildExtractedRow('Tarih', _extractedDate!),
                  ],
                ),
              ),
            if (!_isProcessing && _hasText && _aiResult == null)
              Padding(
                padding: const EdgeInsets.only(bottom: 16),
                child: ElevatedButton.icon(
                  onPressed: _isRestoring ? null : _restoreWithAi,
                  icon: _isRestoring
                      ? const SizedBox(
                          height: 16,
                          width: 16,
                          child: CircularProgressIndicator(
                              strokeWidth: 2, color: Colors.white),
                        )
                      : const Icon(Icons.auto_awesome),
                  label: Text(_isRestoring ? 'AI okuyor...' : 'AI ile Güçlendir'),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppColors.secondary,
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(vertical: 14),
                    shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12)),
                  ),
                ),
              ),
            if (_aiError != null)
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
                      child: Text(_aiError!,
                          style: const TextStyle(color: AppColors.error, fontSize: 13)),
                    ),
                  ],
                ),
              ),
            if (_aiResult != null)
              Container(
                margin: const EdgeInsets.only(bottom: 16),
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: AppColors.secondaryDim,
                  borderRadius: BorderRadius.circular(16),
                  border: Border.all(color: AppColors.secondary.withOpacity(0.3)),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        const Icon(Icons.auto_awesome, color: AppColors.secondaryDark, size: 18),
                        const SizedBox(width: 8),
                        const Text(
                          'AI Restorasyonu',
                          style: TextStyle(
                            fontSize: 14,
                            fontWeight: FontWeight.w700,
                            color: AppColors.secondaryDark,
                          ),
                        ),
                        const Spacer(),
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                          decoration: BoxDecoration(
                            color: _confidenceColor(_aiResult!.confidenceScore).withOpacity(0.15),
                            borderRadius: BorderRadius.circular(20),
                          ),
                          child: Text(
                            '${_confidenceLabel(_aiResult!.confidenceScore)} · %${_aiResult!.confidenceScore}',
                            style: TextStyle(
                              fontSize: 11,
                              fontWeight: FontWeight.w700,
                              color: _confidenceColor(_aiResult!.confidenceScore),
                            ),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 12),
                    if (_aiResult!.storeName != null)
                      _buildExtractedRow('Mağaza', _aiResult!.storeName!),
                    if (_aiResult!.totalAmount != null)
                      _buildExtractedRow(
                          'Tutar', '${_aiResult!.totalAmount!.toStringAsFixed(2)} TL'),
                    if (_aiResult!.receiptDate != null)
                      _buildExtractedRow('Tarih', _aiResult!.receiptDate!),
                    if (_aiResult!.suggestedCategoryName != null)
                      _buildExtractedRow('Kategori', _aiResult!.suggestedCategoryName!),
                    if (_aiResult!.items.isNotEmpty) ...[
                      const SizedBox(height: 4),
                      Text(
                        'Ürünler (${_aiResult!.items.length})',
                        style: TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.w600,
                          color: AppColors.txtSecondary(context),
                        ),
                      ),
                      const SizedBox(height: 6),
                      ..._aiResult!.items.map(
                        (item) => Padding(
                          padding: const EdgeInsets.only(bottom: 4),
                          child: Row(
                            children: [
                              Expanded(
                                child: Text(
                                  item.productName,
                                  style: TextStyle(fontSize: 13, color: AppColors.txt(context)),
                                ),
                              ),
                              Text(
                                '${item.unitPrice.toStringAsFixed(2)} TL',
                                style: TextStyle(
                                  fontSize: 13,
                                  fontWeight: FontWeight.w600,
                                  color: AppColors.txt(context),
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),
                    ],
                    const SizedBox(height: 8),
                    Align(
                      alignment: Alignment.centerRight,
                      child: TextButton(
                        onPressed: () {
                          // Güven skoru düşükse (%60 altı) tüm alanları tek formda göstermek
                          // yerine tek tek doğrulatan bir akışa yönlendir
                          if (_aiResult!.confidenceScore < 60) {
                            Navigator.push(
                              context,
                              MaterialPageRoute(
                                builder: (context) => ReceiptVerificationScreen(
                                  aiResult: _aiResult!,
                                  imagePath: _selectedImage?.path,
                                ),
                              ),
                            );
                            return;
                          }
                          Navigator.push(
                            context,
                            MaterialPageRoute(
                              builder: (context) => AddReceiptScreen(
                                initialStoreName: _aiResult!.storeName ?? _extractedStoreName,
                                initialAmount: _aiResult!.totalAmount?.toStringAsFixed(2) ??
                                    _extractedAmount,
                                initialDate: _aiResult!.receiptDate ?? _extractedDate,
                                initialImagePath: _selectedImage?.path,
                                initialCategoryId: _aiResult!.matchedCategoryId,
                                initialItems: _aiResult!.items,
                              ),
                            ),
                          );
                        },
                        style: TextButton.styleFrom(foregroundColor: AppColors.secondaryDark),
                        child: Text(
                          _aiResult!.confidenceScore < 60
                              ? 'Bilgileri Tek Tek Doğrula →'
                              : 'AI Verisiyle Forma Aktar →',
                          style: const TextStyle(fontWeight: FontWeight.w700),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            if (!_isProcessing && _recognizedText.isNotEmpty)
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
                    Row(
                      children: [
                        const Icon(Icons.text_snippet_outlined,
                            color: AppColors.primary, size: 20),
                        const SizedBox(width: 8),
                        Text(
                          'Okunan Metin',
                          style: TextStyle(
                            fontSize: 15,
                            fontWeight: FontWeight.w700,
                            color: AppColors.txt(context),
                          ),
                        ),
                        const Spacer(),
                        TextButton(
                          onPressed: () => setState(() {
                            _recognizedText = '';
                            _extractedAmount = null;
                            _extractedDate = null;
                            _extractedStoreName = null;
                            _hasText = false;
                            _aiResult = null;
                            _aiError = null;
                          }),
                          child: const Text('Temizle'),
                        ),
                        TextButton(
                          onPressed: () => Navigator.push(
                            context,
                            MaterialPageRoute(
                              builder: (context) => AddReceiptScreen(
                                initialStoreName: _extractedStoreName,
                                initialAmount: _extractedAmount,
                                initialDate: _extractedDate,
                                initialImagePath: _selectedImage?.path,
                              ),
                            ),
                          ),
                          style: TextButton.styleFrom(
                              foregroundColor: AppColors.primary),
                          child: const Text(
                            'Forma Aktar →',
                            style: TextStyle(fontWeight: FontWeight.w700),
                          ),
                        ),
                      ],
                    ),
                    const Divider(),
                    const SizedBox(height: 8),
                    SelectableText(
                      _recognizedText,
                      style: TextStyle(
                        fontSize: 13,
                        color: AppColors.txt(context),
                        height: 1.6,
                      ),
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
