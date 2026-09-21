import 'package:flutter_test/flutter_test.dart';
import 'package:fisbu/screens/statistics_screen.dart';
import 'package:fisbu/core/theme/app_colors.dart';

void main() {
  group('changeSemanticColor', () {
    test('fiyat artisi (kotu) AppColors.error doner', () {
      expect(changeSemanticColor(5.0), AppColors.error);
    });

    test('fiyat azalisi (iyi) AppColors.success doner', () {
      expect(changeSemanticColor(-3.0), AppColors.success);
    });

    test('degisim yoksa null doner (cagiran notr renge duser)', () {
      expect(changeSemanticColor(0.0), isNull);
    });

    test('budget_screen ile ayni error/success token larini kullanir (tutarlilik)', () {
      // MOB-012 sonrasi bulgu: enflasyon artis/azalis rengi eskiden kendi
      // dekoratif hex'lerini kullaniyordu, artik butce asimi/uyarisiyla
      // AYNI semantik AppColors token'larini paylasiyor.
      expect(changeSemanticColor(1.0), AppColors.error);
      expect(changeSemanticColor(-1.0), AppColors.success);
    });
  });
}
