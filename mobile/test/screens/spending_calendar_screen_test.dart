import 'package:flutter_test/flutter_test.dart';
import 'package:fisbu/screens/spending_calendar_screen.dart';

void main() {
  group('calculateSpendingIntensity', () {
    test('harcama yoksa 0 doner', () {
      expect(calculateSpendingIntensity(0, 100), 0);
    });

    test('ayin en yuksek gunu 1.0 doner', () {
      expect(calculateSpendingIntensity(100, 100), 1.0);
    });

    test('dusuk harcama gunleri de en az 0.15 gorunurluk alir (tamamen kaybolmasin)', () {
      expect(calculateSpendingIntensity(1, 100), 0.15);
    });

    test('orantili ara deger doner', () {
      expect(calculateSpendingIntensity(50, 100), 0.5);
    });
  });

  group('calculateSpendingDotSize', () {
    test('yogunluk 0 iken en kucuk nokta boyutunu doner', () {
      expect(calculateSpendingDotSize(0), 3);
    });

    test('yogunluk 1 iken en buyuk nokta boyutunu doner', () {
      expect(calculateSpendingDotSize(1), 8);
    });

    test('daha yuksek yogunluk daha buyuk nokta anlamina gelir (renk-korlugunden bagimsiz ikinci ipucu)', () {
      final low = calculateSpendingDotSize(calculateSpendingIntensity(10, 100));
      final high = calculateSpendingDotSize(calculateSpendingIntensity(90, 100));
      expect(high, greaterThan(low));
    });
  });
}
