import 'dart:io';
import '../services/ocr_parser.dart';
import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:image_picker/image_picker.dart';
import 'add_receipt_screen.dart';
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

  static const _channel = MethodChannel('com.fisbu/ocr');

  Future<void> _pickAndRecognize(ImageSource source) async {
    final picker = ImagePicker();
    final image = await picker.pickImage(source: source, imageQuality: 90);
    if (image == null) return;

    setState(() {
      _selectedImage = image;
      _isProcessing = true;
      _recognizedText = '';
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
