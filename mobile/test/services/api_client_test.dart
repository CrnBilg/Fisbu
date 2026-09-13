import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:fisbu/services/api_client.dart';

void main() {
  group('ApiClient.errorMessage', () {
    test('body bos ise fallback doner', () {
      final response = http.Response('', 500);
      expect(ApiClient.errorMessage(response), 'Bilinmeyen hata');
    });

    test('body gecerli JSON degilse fallback doner', () {
      final response = http.Response('not json', 500);
      expect(ApiClient.errorMessage(response), 'Bilinmeyen hata');
    });

    test('body {"error": "..."} iceriyorsa o mesaji doner', () {
      final response = http.Response('{"error": "E-posta zaten kayitli"}', 409);
      expect(ApiClient.errorMessage(response), 'E-posta zaten kayitli');
    });

    test('body JSON ama error alani yoksa fallback doner', () {
      final response = http.Response('{"status": 500}', 500);
      expect(ApiClient.errorMessage(response), 'Bilinmeyen hata');
    });

    test('ozel fallback parametresi kullanilir', () {
      final response = http.Response('', 401);
      expect(
        ApiClient.errorMessage(response, fallback: 'Oturum suresi doldu'),
        'Oturum suresi doldu',
      );
    });
  });
}
