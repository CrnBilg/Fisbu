import 'dart:io';
import 'package:flutter/foundation.dart' show compute;
import 'package:image/image.dart' as img;

/// Fotoğraf çekildikten hemen sonra OCR'a göndermeden önce basit bir bulanıklık
/// kontrolü yapar (Laplacian varyansı yöntemi) — kullanıcı bulanık bir fişi
/// göndermeden önce uyarılır. Ağır işlem olduğu için ayrı bir isolate'te çalışır.
class ImageQualityService {
  // Laplacian varyansı bu değerin altındaysa fotoğraf muhtemelen bulanık.
  // Ampirik bir eşik — kesin bir bilimsel ölçüt değil, sadece kaba bir uyarı sağlar.
  static const double _blurVarianceThreshold = 100.0;

  static Future<bool> isBlurry(String imagePath) async {
    try {
      return await compute(_computeIsBlurry, imagePath);
    } catch (_) {
      return false; // Analiz başarısız olursa kullanıcıyı engelleme
    }
  }

  static bool _computeIsBlurry(String imagePath) {
    final bytes = File(imagePath).readAsBytesSync();
    final decoded = img.decodeImage(bytes);
    if (decoded == null) return false;

    final resized = img.copyResize(
      decoded,
      width: decoded.width > 600 ? 600 : decoded.width,
    );
    final gray = img.grayscale(resized);

    final width = gray.width;
    final height = gray.height;
    if (width < 3 || height < 3) return false;

    double sum = 0;
    double sumSquares = 0;
    int count = 0;

    for (int y = 1; y < height - 1; y++) {
      for (int x = 1; x < width - 1; x++) {
        final center = gray.getPixel(x, y).luminance;
        final top = gray.getPixel(x, y - 1).luminance;
        final bottom = gray.getPixel(x, y + 1).luminance;
        final left = gray.getPixel(x - 1, y).luminance;
        final right = gray.getPixel(x + 1, y).luminance;
        final laplacian = (4 * center - top - bottom - left - right).toDouble();

        sum += laplacian;
        sumSquares += laplacian * laplacian;
        count++;
      }
    }

    if (count == 0) return false;
    final mean = sum / count;
    final variance = (sumSquares / count) - (mean * mean);

    return variance < _blurVarianceThreshold;
  }
}
