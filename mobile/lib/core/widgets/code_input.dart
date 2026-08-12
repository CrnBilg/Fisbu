import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../theme/app_colors.dart';

/// 6 haneli (varsayılan) tek haneli kutulardan oluşan kod giriş widget'ı.
/// E-posta doğrulama ve şifre sıfırlama ekranlarında ortak kullanılır.
class CodeInput extends StatefulWidget {
  final int length;
  final ValueChanged<String> onChanged;

  const CodeInput({super.key, this.length = 6, required this.onChanged});

  @override
  State<CodeInput> createState() => _CodeInputState();
}

class _CodeInputState extends State<CodeInput> {
  late final List<TextEditingController> _controllers;
  late final List<FocusNode> _focusNodes;

  @override
  void initState() {
    super.initState();
    _controllers = List.generate(widget.length, (_) => TextEditingController());
    _focusNodes = List.generate(widget.length, (_) => FocusNode());
  }

  @override
  void dispose() {
    for (final c in _controllers) {
      c.dispose();
    }
    for (final f in _focusNodes) {
      f.dispose();
    }
    super.dispose();
  }

  void _handleChanged(int index, String value) {
    if (value.isNotEmpty && index < widget.length - 1) {
      _focusNodes[index + 1].requestFocus();
    }
    if (value.isEmpty && index > 0) {
      _focusNodes[index - 1].requestFocus();
    }
    widget.onChanged(_controllers.map((c) => c.text).join());
  }

  @override
  Widget build(BuildContext context) {
    return AutofillGroup(
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: List.generate(widget.length, (index) {
          return SizedBox(
            width: 46,
            // fontSize 26'ya çıkarılırken yükseklik 56'dan 52'ye düşürülmüştü —
            // fiziksel cihazda görülen "bozuk glyph" görüntüsü aslında rakamın üstten/
            // alttan kırpılması olabilir (kutu, büyütülen fontu sığdıramıyor).
            // Yükseklik artırıldı, tekrar cihazda doğrulanmalı.
            height: 64,
            child: TextField(
              controller: _controllers[index],
              focusNode: _focusNodes[index],
              textAlign: TextAlign.center,
              // Fiziksel cihazda bazı girilen rakamların bozuk/ters glyph ile render
              // edildiği gözlemlendi (issue #47) — cihaz bölge/dil ayarı RTL ise BiDi
              // mirroring bazı karakterleri döndürebiliyor, LTR'yi açıkça zorlamak
              // buna karşı ucuz bir önlem
              textDirection: TextDirection.ltr,
              keyboardType: TextInputType.number,
              maxLength: 1,
              autocorrect: false,
              enableSuggestions: false,
              // AutofillHints.oneTimeCode kaldırıldı (issue #47 best-effort denemesi) —
              // bu widget'a özgü, uygulamanın başka hiçbir yerinde olmayan bir mekanizma;
              // SMS otomatik doldurma regresyon riski var, test planında ayrıca doğrulanmalı
              inputFormatters: [FilteringTextInputFormatter.digitsOnly],
              style: const TextStyle(
                fontFamily: 'Inter',
                color: Colors.white,
                fontSize: 26,
                fontWeight: FontWeight.w700,
              ),
              decoration: InputDecoration(
                counterText: '',
                enabledBorder: UnderlineInputBorder(
                  borderSide: BorderSide(color: Colors.white.withOpacity(0.25), width: 1.5),
                ),
                focusedBorder: UnderlineInputBorder(
                  borderSide: BorderSide(color: AppColors.primary, width: 2),
                ),
              ),
              onChanged: (value) => _handleChanged(index, value),
            ),
          );
        }),
      ),
    );
  }
}
