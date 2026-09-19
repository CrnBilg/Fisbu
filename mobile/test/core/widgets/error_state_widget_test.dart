import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;

import 'package:fisbu/core/widgets/error_state_widget.dart';

void main() {
  Widget wrap(Widget child) => MaterialApp(home: Scaffold(body: child));

  group('ErrorStateWidget', () {
    testWidgets('SocketException icin NetworkError.friendlyMessage ile aginin '
        'olmadigini bildiren mesaji gosterir (ham exception DEGIL)', (tester) async {
      await tester.pumpWidget(wrap(ErrorStateWidget(
        error: const SocketException('Failed host lookup'),
        onRetry: () {},
      )));

      expect(find.text('İnternet bağlantısı yok, lütfen tekrar deneyin.'), findsOneWidget);
      // Ham exception metni ("SocketException", "Failed host lookup") EKRANDA GÖRÜNMEMELİ.
      expect(find.textContaining('SocketException'), findsNothing);
      expect(find.textContaining('Failed host lookup'), findsNothing);
    });

    testWidgets('http.ClientException icin de ag hatasi mesaji gosterir', (tester) async {
      await tester.pumpWidget(wrap(ErrorStateWidget(
        error: http.ClientException('connection reset'),
        onRetry: () {},
      )));

      expect(find.text('İnternet bağlantısı yok, lütfen tekrar deneyin.'), findsOneWidget);
    });

    testWidgets('ag hatasi olmayan bir exception icin verilen fallback mesaji gosterir',
        (tester) async {
      await tester.pumpWidget(wrap(ErrorStateWidget(
        error: Exception('beklenmeyen sunucu hatasi'),
        onRetry: () {},
        fallback: 'Bütçe verisi alınamadı, lütfen tekrar deneyin.',
      )));

      expect(find.text('Bütçe verisi alınamadı, lütfen tekrar deneyin.'), findsOneWidget);
      expect(find.textContaining('beklenmeyen sunucu hatasi'), findsNothing);
    });

    testWidgets('fallback verilmezse varsayilan genel mesaji gosterir', (tester) async {
      await tester.pumpWidget(wrap(ErrorStateWidget(
        error: Exception('herhangi bir hata'),
        onRetry: () {},
      )));

      expect(find.text('Veri alınamadı, lütfen tekrar deneyin.'), findsOneWidget);
    });

    testWidgets('Tekrar Dene butonuna basinca onRetry cagrilir', (tester) async {
      var retried = false;
      await tester.pumpWidget(wrap(ErrorStateWidget(
        error: Exception('x'),
        onRetry: () => retried = true,
      )));

      expect(find.text('Tekrar Dene'), findsOneWidget);
      await tester.tap(find.text('Tekrar Dene'));
      await tester.pump();

      expect(retried, isTrue);
    });

    testWidgets('varsayilan ikon cloud_off_outlined, ozel ikon verilebilir', (tester) async {
      await tester.pumpWidget(wrap(ErrorStateWidget(error: Exception('x'), onRetry: () {})));
      expect(find.byIcon(Icons.cloud_off_outlined), findsOneWidget);

      await tester.pumpWidget(wrap(ErrorStateWidget(
        error: Exception('x'),
        onRetry: () {},
        icon: Icons.error_outline,
      )));
      expect(find.byIcon(Icons.error_outline), findsOneWidget);
    });
  });
}
