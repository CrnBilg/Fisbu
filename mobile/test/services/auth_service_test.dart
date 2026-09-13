import 'package:flutter_test/flutter_test.dart';
import 'package:fisbu/services/auth_service.dart';

void main() {
  group('AuthService — sessizce yutulan hatalar artik raporlaniyor (MOB-003)', () {
    test(
        'logout(): secure-storage silme hatasi (plugin channel yok) VE '
        'Crashlytics raporlama hatasi (Firebase init edilmemis) ust uste '
        'gelse dahi logout() throw etmez',
        () async {
      // Bu test ortaminda ne flutter_secure_storage'in native channel'i ne de
      // Firebase mevcut — yani _reportSilently() içindeki Crashlytics çağrısı
      // da başarısız olacak. Beklenen davranış: her iki hata da sessizce
      // yutulur (raporlanmaya çalışılır, olmazsa olmaz) ve logout() normal
      // şekilde tamamlanır — çağıranın akışı asla bir exception'la kesilmez.
      await AuthService.logout().timeout(const Duration(seconds: 3));
    });

    test(
        'getProfile(): token yoksa (getToken null) network hic denenmeden null doner',
        () async {
      final result = await AuthService.getProfile().timeout(const Duration(seconds: 3));
      expect(result, isNull);
    });
  });
}
