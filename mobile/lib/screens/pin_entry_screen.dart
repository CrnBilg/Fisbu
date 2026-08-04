import 'package:flutter/material.dart';
import 'package:flutter/services.dart' show FilteringTextInputFormatter;
import '../services/pin_service.dart';
import '../core/theme/app_colors.dart';

enum PinEntryMode { set, verify }

/// PIN belirleme (set) ya da doğrulama (verify) ekranı.
/// Başarılı olursa Navigator.pop(context, true) ile döner.
class PinEntryScreen extends StatefulWidget {
  final PinEntryMode mode;
  final bool allowCancel;

  const PinEntryScreen({super.key, required this.mode, this.allowCancel = true});

  @override
  State<PinEntryScreen> createState() => _PinEntryScreenState();
}

class _PinEntryScreenState extends State<PinEntryScreen> {
  final _pinController = TextEditingController();
  final _confirmController = TextEditingController();
  String? _firstStepPin;
  String? _errorMessage;
  bool _isSubmitting = false;

  bool get _isConfirmStep => widget.mode == PinEntryMode.set && _firstStepPin != null;

  @override
  void dispose() {
    _pinController.dispose();
    _confirmController.dispose();
    super.dispose();
  }

  Future<void> _handleSubmit() async {
    final controller = _isConfirmStep ? _confirmController : _pinController;
    final value = controller.text.trim();

    if (value.length < 4 || value.length > 6) {
      setState(() => _errorMessage = 'PIN 4-6 haneli olmalı');
      return;
    }

    if (widget.mode == PinEntryMode.verify) {
      setState(() {
        _isSubmitting = true;
        _errorMessage = null;
      });
      final correct = await PinService.verifyPin(value);
      if (!mounted) return;
      setState(() => _isSubmitting = false);
      if (correct) {
        Navigator.pop(context, true);
      } else {
        setState(() => _errorMessage = 'PIN hatalı, tekrar dene');
        _pinController.clear();
      }
      return;
    }

    // Set modu: ilk adımda PIN'i sakla, ikinci adımda eşleşmeyi kontrol et
    if (!_isConfirmStep) {
      setState(() {
        _firstStepPin = value;
        _errorMessage = null;
      });
      return;
    }

    if (value != _firstStepPin) {
      setState(() {
        _errorMessage = 'PIN\'ler eşleşmiyor, baştan dene';
        _firstStepPin = null;
        _pinController.clear();
        _confirmController.clear();
      });
      return;
    }

    setState(() => _isSubmitting = true);
    await PinService.setPin(value);
    if (!mounted) return;
    Navigator.pop(context, true);
  }

  @override
  Widget build(BuildContext context) {
    final isSet = widget.mode == PinEntryMode.set;
    final title = isSet
        ? (_isConfirmStep ? 'PIN\'i Doğrula' : 'Yeni PIN Belirle')
        : 'PIN ile Kilidi Aç';
    final subtitle = isSet
        ? (_isConfirmStep
            ? 'Az önce girdiğin PIN\'i tekrar gir'
            : 'Face ID/Touch ID başarısız olduğunda kullanacağın 4-6 haneli PIN\'i belirle')
        : 'Face ID/Touch ID çalışmıyorsa PIN kodunla giriş yapabilirsin';

    return PopScope(
      canPop: widget.allowCancel,
      child: Scaffold(
        appBar: widget.allowCancel ? AppBar(title: Text(isSet ? 'PIN Belirle' : 'PIN ile Aç')) : null,
        body: SafeArea(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(Icons.pin_outlined, size: 56, color: AppColors.primary),
                const SizedBox(height: 20),
                Text(title,
                    textAlign: TextAlign.center,
                    style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700, color: AppColors.txt(context))),
                const SizedBox(height: 8),
                Text(subtitle,
                    textAlign: TextAlign.center,
                    style: TextStyle(fontSize: 13, color: AppColors.textSecondary, height: 1.4)),
                const SizedBox(height: 28),
                TextField(
                  controller: _isConfirmStep ? _confirmController : _pinController,
                  autofocus: true,
                  obscureText: true,
                  textAlign: TextAlign.center,
                  keyboardType: TextInputType.number,
                  inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                  maxLength: 6,
                  style: const TextStyle(fontSize: 24, letterSpacing: 8),
                  decoration: InputDecoration(
                    counterText: '',
                    filled: true,
                    fillColor: AppColors.surf(context),
                    border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                  ),
                  onSubmitted: (_) => _handleSubmit(),
                ),
                if (_errorMessage != null) ...[
                  const SizedBox(height: 8),
                  Text(_errorMessage!, style: const TextStyle(color: AppColors.error, fontSize: 13)),
                ],
                const SizedBox(height: 20),
                ElevatedButton(
                  onPressed: _isSubmitting ? null : _handleSubmit,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppColors.primary,
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(vertical: 14, horizontal: 40),
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                  ),
                  child: _isSubmitting
                      ? const SizedBox(
                          height: 18, width: 18,
                          child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                      : Text(_isConfirmStep || widget.mode == PinEntryMode.verify ? 'Onayla' : 'Devam Et'),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
