import 'dart:io';
import 'package:http/http.dart' as http;

/// Servislerin `catch (e)` bloklarında yakaladığı hataları kullanıcıya
/// gösterilecek Türkçe metne çevirir. Ham exception'ın (`SocketException:
/// Failed host lookup...` gibi) ekrana basılmasını önler — özellikle
/// bağlantı yokken (uçak modu vb.) her ekranda tutarlı bir mesaj gösterir.
class NetworkError {
  NetworkError._();

  static bool isNetworkFailure(Object e) {
    return e is SocketException || e is http.ClientException;
  }

  static String friendlyMessage(Object e, {String fallback = 'Veri alınamadı, lütfen tekrar deneyin.'}) {
    if (isNetworkFailure(e)) {
      return 'İnternet bağlantısı yok, lütfen tekrar deneyin.';
    }
    return fallback;
  }
}
