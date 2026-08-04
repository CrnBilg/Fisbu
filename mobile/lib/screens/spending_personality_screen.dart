import 'package:flutter/material.dart';
import '../models/spending_personality.dart';
import '../services/receipt_service.dart';
import '../core/theme/app_colors.dart';

class SpendingPersonalityScreen extends StatefulWidget {
  const SpendingPersonalityScreen({super.key});

  @override
  State<SpendingPersonalityScreen> createState() => _SpendingPersonalityScreenState();
}

class _SpendingPersonalityScreenState extends State<SpendingPersonalityScreen> {
  bool _isLoading = true;
  SpendingPersonality? _data;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });
    try {
      final data = await ReceiptService.getSpendingPersonality();
      setState(() => _data = data);
    } catch (e) {
      setState(() => _errorMessage = 'Alınamadı: $e');
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Harcama Kişiliğim')),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator(color: AppColors.primary))
          : _errorMessage != null
              ? Center(child: Text(_errorMessage!, style: TextStyle(color: AppColors.textSecondary)))
              : RefreshIndicator(
                  onRefresh: _load,
                  child: ListView(
                    physics: const AlwaysScrollableScrollPhysics(),
                    padding: const EdgeInsets.all(20),
                    children: [
                      Container(
                        padding: const EdgeInsets.all(24),
                        decoration: BoxDecoration(
                          gradient: const LinearGradient(
                            colors: [Color(0xFF6C63FF), Color(0xFF9C8FFF)],
                            begin: Alignment.topLeft,
                            end: Alignment.bottomRight,
                          ),
                          borderRadius: BorderRadius.circular(20),
                        ),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const Icon(Icons.auto_awesome, color: Colors.white, size: 28),
                            const SizedBox(height: 12),
                            Text(
                              _data?.persona.title ?? '',
                              style: const TextStyle(
                                  color: Colors.white, fontSize: 24, fontWeight: FontWeight.w800),
                            ),
                            const SizedBox(height: 8),
                            Text(
                              _data?.persona.description ?? '',
                              style: const TextStyle(color: Colors.white70, fontSize: 13, height: 1.4),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(height: 24),
                      Text('Rozetler',
                          style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700, color: AppColors.txt(context))),
                      const SizedBox(height: 4),
                      Text(
                        '${_data?.badges.where((b) => b.achieved).length ?? 0} / ${_data?.badges.length ?? 0} rozet kazanıldı',
                        style: TextStyle(fontSize: 12, color: AppColors.textSecondary),
                      ),
                      const SizedBox(height: 12),
                      GridView.builder(
                        shrinkWrap: true,
                        physics: const NeverScrollableScrollPhysics(),
                        gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                          crossAxisCount: 2,
                          mainAxisSpacing: 12,
                          crossAxisSpacing: 12,
                          childAspectRatio: 1.3,
                        ),
                        itemCount: _data?.badges.length ?? 0,
                        itemBuilder: (context, index) {
                          final badge = _data!.badges[index];
                          return Container(
                            padding: const EdgeInsets.all(14),
                            decoration: BoxDecoration(
                              color: badge.achieved ? AppColors.primDim(context) : AppColors.surf(context),
                              borderRadius: BorderRadius.circular(14),
                              border: Border.all(
                                color: badge.achieved ? AppColors.primary.withOpacity(0.4) : AppColors.brd(context),
                              ),
                            ),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Icon(
                                  badge.achieved ? Icons.military_tech : Icons.lock_outline,
                                  color: badge.achieved ? AppColors.primary : AppColors.textSecondary,
                                  size: 24,
                                ),
                                const Spacer(),
                                Text(
                                  badge.title,
                                  style: TextStyle(
                                    fontSize: 13,
                                    fontWeight: FontWeight.w700,
                                    color: badge.achieved ? AppColors.txt(context) : AppColors.textSecondary,
                                  ),
                                ),
                                const SizedBox(height: 2),
                                Text(
                                  badge.description,
                                  style: TextStyle(fontSize: 11, color: AppColors.textSecondary),
                                  maxLines: 2,
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ],
                            ),
                          );
                        },
                      ),
                    ],
                  ),
                ),
    );
  }
}
