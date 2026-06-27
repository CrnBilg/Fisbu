import 'dart:io';
import '../services/ocr_parser.dart';
import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:flutter/material.dart';
import 'package:google_mlkit_text_recognition/google_mlkit_text_recognition.dart';
import 'package:image_picker/image_picker.dart';

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

  final TextRecognizer _textRecognizer = TextRecognizer();

  Future<void> _pickAndRecognize(ImageSource source) async {
    final picker = ImagePicker();
    final image = await picker.pickImage(
      source: source,
      imageQuality: 90,
    );

    if (image == null) return;

    setState(() {
      _selectedImage = image;
      _isProcessing = true;
      _recognizedText = '';
    });

    try {
      if (kIsWeb) {
        setState(() {
          _recognizedText =
              'OCR web tarayıcısında çalışmaz. Lütfen telefonda deneyin.';
          _isProcessing = false;
        });
        return;
      }

      final inputImage = InputImage.fromFilePath(image.path);
      final recognized = await _textRecognizer.processImage(inputImage);

      final text = recognized.text;
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
            style: const TextStyle(
              fontSize: 13,
              color: Color(0xFF9E9EBF),
              fontWeight: FontWeight.w600,
            ),
          ),
          Expanded(
            child: Text(
              value,
              style: const TextStyle(
                fontSize: 13,
                fontWeight: FontWeight.w700,
                color: Color(0xFF1A1A2E),
              ),
            ),
          ),
        ],
      ),
    );
  }

  @override
  void dispose() {
    _textRecognizer.close();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF8F7FF),
      appBar: AppBar(
        title: const Text('Fişi Tara'),
        backgroundColor: const Color(0xFF6C63FF),
        foregroundColor: Colors.white,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // Fotoğraf önizleme alanı
            Container(
              height: 220,
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(16),
                border: Border.all(color: const Color(0xFFEEEEF5)),
              ),
              child: _selectedImage == null
                  ? Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Container(
                          padding: const EdgeInsets.all(16),
                          decoration: BoxDecoration(
                            color: const Color(0xFF6C63FF).withOpacity(0.08),
                            shape: BoxShape.circle,
                          ),
                          child: const Icon(
                            Icons.document_scanner_outlined,
                            size: 40,
                            color: Color(0xFF6C63FF),
                          ),
                        ),
                        const SizedBox(height: 12),
                        const Text(
                          'Fiş fotoğrafı seç veya çek',
                          style: TextStyle(
                            fontSize: 15,
                            fontWeight: FontWeight.w600,
                            color: Color(0xFF6C63FF),
                          ),
                        ),
                        const SizedBox(height: 4),
                        const Text(
                          'OCR ile metni otomatik okuyacağız',
                          style: TextStyle(
                            fontSize: 12,
                            color: Color(0xFF9E9EBF),
                          ),
                        ),
                      ],
                    )
                  : ClipRRect(
                      borderRadius: BorderRadius.circular(15),
                      child: kIsWeb
                          ? Image.network(
                              _selectedImage!.path,
                              fit: BoxFit.cover,
                              width: double.infinity,
                            )
                          : Image.file(
                              File(_selectedImage!.path),
                              fit: BoxFit.cover,
                              width: double.infinity,
                            ),
                    ),
            ),
            const SizedBox(height: 16),

            // Butonlar
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
                      backgroundColor: const Color(0xFF6C63FF),
                      foregroundColor: Colors.white,
                      padding: const EdgeInsets.symmetric(vertical: 14),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
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
                      backgroundColor: const Color(0xFF00BFA6),
                      foregroundColor: Colors.white,
                      padding: const EdgeInsets.symmetric(vertical: 14),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 24),

            // OCR sonucu
            if (_isProcessing)
              const Center(
                child: Column(
                  children: [
                    CircularProgressIndicator(color: Color(0xFF6C63FF)),
                    SizedBox(height: 12),
                    Text(
                      'Metin okunuyor...',
                      style: TextStyle(
                        color: Color(0xFF9E9EBF),
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
                  color: const Color(0xFF6C63FF).withOpacity(0.06),
                  borderRadius: BorderRadius.circular(16),
                  border: Border.all(
                    color: const Color(0xFF6C63FF).withOpacity(0.2),
                  ),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Row(
                      children: [
                        Icon(
                          Icons.auto_awesome,
                          color: Color(0xFF6C63FF),
                          size: 18,
                        ),
                        SizedBox(width: 8),
                        Text(
                          'Otomatik Çıkarılan Bilgiler',
                          style: TextStyle(
                            fontSize: 14,
                            fontWeight: FontWeight.w700,
                            color: Color(0xFF6C63FF),
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
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(16),
                  border: Border.all(color: const Color(0xFFEEEEF5)),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        const Icon(
                          Icons.text_snippet_outlined,
                          color: Color(0xFF6C63FF),
                          size: 20,
                        ),
                        const SizedBox(width: 8),
                        const Text(
                          'Okunan Metin',
                          style: TextStyle(
                            fontSize: 15,
                            fontWeight: FontWeight.w700,
                            color: Color(0xFF1A1A2E),
                          ),
                        ),
                        const Spacer(),
                        TextButton(
                          onPressed: () {
                            setState(() => _recognizedText = '');
                          },
                          child: const Text('Temizle'),
                        ),
                      ],
                    ),
                    const Divider(),
                    const SizedBox(height: 8),
                    SelectableText(
                      _recognizedText,
                      style: const TextStyle(
                        fontSize: 13,
                        color: Color(0xFF1A1A2E),
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
