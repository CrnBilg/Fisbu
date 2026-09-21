import 'package:flutter/material.dart';
import '../theme/app_colors.dart';

/// Bir ekran/bölüm veri çekerken gösterilen tutarlı yükleniyor göstergesi.
/// Tipik bir kart-listesi şeklinde kayan (shimmer) yer tutucular gösterir —
/// düz bir spinner yerine, verinin nasıl görüneceğine dair bir ipucu verir.
class LoadingStateWidget extends StatefulWidget {
  const LoadingStateWidget({super.key});

  @override
  State<LoadingStateWidget> createState() => _LoadingStateWidgetState();
}

class _LoadingStateWidgetState extends State<LoadingStateWidget>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1200),
    )..repeat();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 20),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: List.generate(4, (index) {
            return Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: _ShimmerBar(
                controller: _controller,
                height: 64,
                widthFactor: index == 3 ? 0.6 : 1.0,
              ),
            );
          }),
        ),
      ),
    );
  }
}

class _ShimmerBar extends StatelessWidget {
  final AnimationController controller;
  final double height;
  final double widthFactor;

  const _ShimmerBar({
    required this.controller,
    required this.height,
    required this.widthFactor,
  });

  @override
  Widget build(BuildContext context) {
    final baseColor = AppColors.brd(context);
    final highlightColor = AppColors.surf(context);
    return FractionallySizedBox(
      widthFactor: widthFactor,
      child: AnimatedBuilder(
        animation: controller,
        builder: (context, child) {
          return ShaderMask(
            blendMode: BlendMode.srcATop,
            shaderCallback: (bounds) {
              final t = controller.value;
              return LinearGradient(
                colors: [baseColor, highlightColor, baseColor],
                stops: const [0.35, 0.5, 0.65],
                begin: Alignment(-1 - t * 2, 0),
                end: Alignment(1 - t * 2, 0),
              ).createShader(bounds);
            },
            child: Container(
              height: height,
              decoration: BoxDecoration(
                color: baseColor,
                borderRadius: BorderRadius.circular(16),
              ),
            ),
          );
        },
      ),
    );
  }
}
