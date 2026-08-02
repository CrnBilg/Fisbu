import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:share_plus/share_plus.dart';
import '../models/receipt.dart';
import '../core/theme/app_colors.dart';

class _Participant {
  final TextEditingController nameController;
  final TextEditingController amountController;

  _Participant({String name = ''})
      : nameController = TextEditingController(text: name),
        amountController = TextEditingController();

  void dispose() {
    nameController.dispose();
    amountController.dispose();
  }
}

/// Toplu ödenen bir fişi (otel, yemek vb.) kişiler arasında böler ve sonucu
/// mesaj olarak paylaşır. Tamamen uygulama-içi hesaplama — backend'e hiç
/// istek atmaz, harici katılımcı/link yok.
class SplitBillScreen extends StatefulWidget {
  final Receipt receipt;

  const SplitBillScreen({super.key, required this.receipt});

  @override
  State<SplitBillScreen> createState() => _SplitBillScreenState();
}

class _SplitBillScreenState extends State<SplitBillScreen> {
  final _currencyFormat = NumberFormat('#,##0.00', 'tr_TR');
  final List<_Participant> _participants = [];
  bool _equalSplit = true;

  @override
  void initState() {
    super.initState();
    _participants.add(_Participant(name: 'Ben'));
    _participants.add(_Participant());
  }

  @override
  void dispose() {
    for (final p in _participants) {
      p.dispose();
    }
    super.dispose();
  }

  void _addParticipant() {
    setState(() => _participants.add(_Participant()));
  }

  void _removeParticipant(int index) {
    setState(() {
      _participants[index].dispose();
      _participants.removeAt(index);
    });
  }

  /// Kuruş küsuratını kaybetmeden toplamı eşit dağıtır (fazla kuruşlar ilk
  /// kişilere eklenir ki toplam her zaman fiş tutarıyla tam eşleşsin).
  List<double> _equalShares() {
    final totalCents = (widget.receipt.totalAmount * 100).round();
    final n = _participants.length;
    if (n == 0) return const [];
    final base = totalCents ~/ n;
    final remainder = totalCents % n;
    return List.generate(
      n,
      (i) => (base + (i < remainder ? 1 : 0)) / 100,
    );
  }

  List<double?> _customShares() {
    return _participants
        .map((p) => double.tryParse(p.amountController.text.trim().replaceAll(',', '.')))
        .toList();
  }

  double get _customTotal =>
      _customShares().fold(0.0, (sum, v) => sum + (v ?? 0));

  double get _customDifference => widget.receipt.totalAmount - _customTotal;

  bool get _canShare {
    if (_participants.length < 2) return false;
    if (_equalSplit) return true;
    return _customDifference.abs() < 0.01 &&
        _customShares().every((v) => v != null);
  }

  String _participantName(int index) {
    final text = _participants[index].nameController.text.trim();
    return text.isEmpty ? 'Kişi ${index + 1}' : text;
  }

  Future<void> _share() async {
    final shares = _equalSplit ? _equalShares() : _customShares().map((v) => v ?? 0).toList();

    final buffer = StringBuffer();
    buffer.writeln('🧾 ${widget.receipt.storeName} - ${_currencyFormat.format(widget.receipt.totalAmount)} TL');
    buffer.writeln('👥 ${_participants.length} kişi arasında bölüşüldü');
    buffer.writeln();
    for (var i = 0; i < _participants.length; i++) {
      buffer.writeln('${_participantName(i)}: ${_currencyFormat.format(shares[i])} TL');
    }
    buffer.writeln();
    buffer.write('FişBu ile paylaşıldı 📱');

    await SharePlus.instance.share(ShareParams(text: buffer.toString()));
  }

  @override
  Widget build(BuildContext context) {
    final equalShares = _equalSplit ? _equalShares() : null;

    return Scaffold(
      appBar: AppBar(title: const Text('Fişi Böl')),
      body: Column(
        children: [
          Expanded(
            child: ListView(
              padding: const EdgeInsets.all(20),
              children: [
                Container(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: AppColors.primDim(context),
                    borderRadius: BorderRadius.circular(16),
                  ),
                  child: Row(
                    children: [
                      Expanded(
                        child: Text(
                          widget.receipt.storeName,
                          style: TextStyle(
                            fontSize: 15,
                            fontWeight: FontWeight.w700,
                            color: AppColors.txt(context),
                          ),
                        ),
                      ),
                      Text(
                        '${_currencyFormat.format(widget.receipt.totalAmount)} TL',
                        style: const TextStyle(
                          fontSize: 18,
                          fontWeight: FontWeight.w800,
                          color: AppColors.primary,
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 20),

                // Bölüşüm modu
                Row(
                  children: [
                    Expanded(
                      child: _ModeButton(
                        label: 'Eşit Böl',
                        selected: _equalSplit,
                        onTap: () => setState(() => _equalSplit = true),
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: _ModeButton(
                        label: 'Özel Tutar',
                        selected: !_equalSplit,
                        onTap: () => setState(() => _equalSplit = false),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 20),

                Row(
                  children: [
                    Text(
                      'Kişiler',
                      style: TextStyle(
                        fontSize: 15,
                        fontWeight: FontWeight.w700,
                        color: AppColors.txt(context),
                      ),
                    ),
                    const Spacer(),
                    TextButton.icon(
                      onPressed: _addParticipant,
                      icon: const Icon(Icons.add, size: 18),
                      label: const Text('Kişi Ekle'),
                      style: TextButton.styleFrom(foregroundColor: AppColors.primary),
                    ),
                  ],
                ),
                const SizedBox(height: 8),

                ..._participants.asMap().entries.map((entry) {
                  final index = entry.key;
                  final p = entry.value;
                  return Padding(
                    padding: const EdgeInsets.only(bottom: 10),
                    child: Row(
                      crossAxisAlignment: CrossAxisAlignment.center,
                      children: [
                        Expanded(
                          flex: 3,
                          child: TextField(
                            controller: p.nameController,
                            decoration: InputDecoration(
                              hintText: 'Kişi ${index + 1}',
                              isDense: true,
                              border: OutlineInputBorder(borderRadius: BorderRadius.circular(10)),
                              filled: true,
                              fillColor: AppColors.surf(context),
                            ),
                          ),
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          flex: 2,
                          child: _equalSplit
                              ? Container(
                                  padding: const EdgeInsets.symmetric(vertical: 14),
                                  alignment: Alignment.center,
                                  decoration: BoxDecoration(
                                    color: AppColors.surf(context),
                                    borderRadius: BorderRadius.circular(10),
                                    border: Border.all(color: AppColors.brd(context)),
                                  ),
                                  child: Text(
                                    '${_currencyFormat.format(equalShares![index])} TL',
                                    style: TextStyle(
                                      fontSize: 13,
                                      fontWeight: FontWeight.w700,
                                      color: AppColors.txt(context),
                                    ),
                                  ),
                                )
                              : TextField(
                                  controller: p.amountController,
                                  keyboardType: const TextInputType.numberWithOptions(decimal: true),
                                  onChanged: (_) => setState(() {}),
                                  decoration: InputDecoration(
                                    hintText: 'Tutar',
                                    isDense: true,
                                    border: OutlineInputBorder(borderRadius: BorderRadius.circular(10)),
                                    filled: true,
                                    fillColor: AppColors.surf(context),
                                  ),
                                ),
                        ),
                        if (_participants.length > 2)
                          IconButton(
                            onPressed: () => _removeParticipant(index),
                            icon: const Icon(Icons.close, size: 18),
                            color: AppColors.error,
                          ),
                      ],
                    ),
                  );
                }),

                if (!_equalSplit) ...[
                  const SizedBox(height: 8),
                  Container(
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: _customDifference.abs() < 0.01
                          ? AppColors.successDim
                          : AppColors.errDim(context),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Row(
                      children: [
                        Icon(
                          _customDifference.abs() < 0.01 ? Icons.check_circle_outline : Icons.error_outline,
                          size: 18,
                          color: _customDifference.abs() < 0.01 ? AppColors.success : AppColors.error,
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: Text(
                            _customDifference.abs() < 0.01
                                ? 'Tutarlar toplamı eşleşiyor'
                                : _customDifference > 0
                                    ? '${_currencyFormat.format(_customDifference)} TL eksik'
                                    : '${_currencyFormat.format(-_customDifference)} TL fazla',
                            style: TextStyle(
                              fontSize: 13,
                              fontWeight: FontWeight.w600,
                              color: _customDifference.abs() < 0.01 ? AppColors.success : AppColors.error,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ],
            ),
          ),
          SafeArea(
            top: false,
            child: Padding(
              padding: const EdgeInsets.fromLTRB(20, 8, 20, 16),
              child: ElevatedButton.icon(
                onPressed: _canShare ? _share : null,
                icon: const Icon(Icons.ios_share),
                label: const Text('Paylaş'),
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppColors.primary,
                  foregroundColor: Colors.white,
                  padding: const EdgeInsets.symmetric(vertical: 16),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _ModeButton extends StatelessWidget {
  final String label;
  final bool selected;
  final VoidCallback onTap;

  const _ModeButton({required this.label, required this.selected, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(12),
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 12),
        alignment: Alignment.center,
        decoration: BoxDecoration(
          color: selected ? AppColors.primDim(context) : AppColors.surf(context),
          borderRadius: BorderRadius.circular(12),
          border: Border.all(
            color: selected ? AppColors.primary : AppColors.brd(context),
            width: selected ? 2 : 1,
          ),
        ),
        child: Text(
          label,
          style: TextStyle(
            fontWeight: FontWeight.w700,
            fontSize: 13,
            color: selected ? AppColors.primary : AppColors.txt(context),
          ),
        ),
      ),
    );
  }
}
