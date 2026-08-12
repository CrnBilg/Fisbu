import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

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
    // AutofillGroup kaldırıldı — SMS autofill zaten devre dışıydı (autofillHints
    // kaldırılmıştı), ama grup sarmalayıcının kendisi iOS'a bunun tek bir
    // doğrulama kodu formu olduğunu işaretleyip kendi (bozuk) render/overlay
    // mekanizmasını tetikliyor olabilir.
    return Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: List.generate(widget.length, (index) {
          return SizedBox(
            width: 40,
            height: 56,
            // Tire her zaman sabit bir alt katman olarak çiziliyor (hintText'e
            // güvenmiyoruz — bazı kutularda hiç görünmüyordu). Rakam onun
            // üzerine, aynı Stack içinde ortalanmış şekilde biniyor.
            child: Stack(
              alignment: Alignment.center,
              children: [
                Text(
                  '-',
                  style: TextStyle(
                    color: Colors.white.withOpacity(0.35),
                    fontSize: 22,
                    fontWeight: FontWeight.w700,
                  ),
                ),
                TextField(
                  controller: _controllers[index],
                  focusNode: _focusNodes[index],
                  textAlign: TextAlign.center,
                  textDirection: TextDirection.ltr,
                  keyboardType: TextInputType.number,
                  maxLength: 1,
                  autocorrect: false,
                  enableSuggestions: false,
                  inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                  style: const TextStyle(
                    fontFamily: 'Inter',
                    color: Colors.white,
                    fontSize: 22,
                    fontWeight: FontWeight.w700,
                  ),
                  decoration: const InputDecoration(
                    counterText: '',
                    border: InputBorder.none,
                    enabledBorder: InputBorder.none,
                    focusedBorder: InputBorder.none,
                    isCollapsed: true,
                    filled: false,
                  ),
                  onChanged: (value) => _handleChanged(index, value),
                ),
              ],
            ),
          );
        }),
    );
  }
}
