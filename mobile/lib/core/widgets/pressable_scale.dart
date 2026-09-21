import 'package:flutter/material.dart';

/// Dokunulabilir kart/buton benzeri widget'lara tutarlı bir "basılı tutma"
/// mikro-etkileşimi ekler — parmak basılıyken hafifçe küçülür, bırakılınca
/// geri döner. Referans: Revolut/Monzo gibi fintech uygulamalarındaki kart
/// dokunma geri bildirimi.
class PressableScale extends StatefulWidget {
  final Widget child;
  final VoidCallback? onTap;
  final double scaleOnPress;

  const PressableScale({
    super.key,
    required this.child,
    required this.onTap,
    this.scaleOnPress = 0.96,
  });

  @override
  State<PressableScale> createState() => _PressableScaleState();
}

class _PressableScaleState extends State<PressableScale> {
  bool _pressed = false;

  void _setPressed(bool value) {
    if (_pressed != value) setState(() => _pressed = value);
  }

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      behavior: HitTestBehavior.opaque,
      onTap: widget.onTap,
      onTapDown: (_) => _setPressed(true),
      onTapUp: (_) => _setPressed(false),
      onTapCancel: () => _setPressed(false),
      child: AnimatedScale(
        scale: _pressed ? widget.scaleOnPress : 1.0,
        duration: const Duration(milliseconds: 100),
        curve: Curves.easeOut,
        child: widget.child,
      ),
    );
  }
}
