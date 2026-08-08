import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../models/savings_goal.dart';
import '../services/savings_goal_service.dart';
import '../core/theme/app_colors.dart';
import '../core/utils/network_error.dart';

class SavingsGoalsScreen extends StatefulWidget {
  const SavingsGoalsScreen({super.key});

  @override
  State<SavingsGoalsScreen> createState() => _SavingsGoalsScreenState();
}

class _SavingsGoalsScreenState extends State<SavingsGoalsScreen> {
  final _currencyFormat = NumberFormat('#,##0.00', 'tr_TR');
  bool _isLoading = true;
  List<SavingsGoal> _goals = [];
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _loadGoals();
  }

  Future<void> _loadGoals() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });
    try {
      final goals = await SavingsGoalService.getGoals();
      setState(() => _goals = goals);
    } catch (e) {
      setState(() => _errorMessage = NetworkError.friendlyMessage(e, fallback: 'Hedefler alınamadı, lütfen tekrar deneyin.'));
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _showCreateGoalDialog() async {
    final nameController = TextEditingController();
    final amountController = TextEditingController();
    DateTime? targetDate;

    final result = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(24))),
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setSheetState) => Padding(
          padding: EdgeInsets.fromLTRB(20, 20, 20, MediaQuery.of(ctx).viewInsets.bottom + 24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text('Yeni Tasarruf Hedefi',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700, color: AppColors.txt(ctx))),
              const SizedBox(height: 16),
              TextField(
                controller: nameController,
                autofocus: true,
                decoration: InputDecoration(
                  labelText: 'Hedef Adı',
                  hintText: 'örn. Tatil, Yeni Telefon...',
                  border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                ),
              ),
              const SizedBox(height: 14),
              TextField(
                controller: amountController,
                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                decoration: InputDecoration(
                  labelText: 'Hedef Tutar (TL)',
                  border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                ),
              ),
              const SizedBox(height: 14),
              InkWell(
                onTap: () async {
                  final picked = await showDatePicker(
                    context: ctx,
                    initialDate: DateTime.now().add(const Duration(days: 90)),
                    firstDate: DateTime.now(),
                    lastDate: DateTime.now().add(const Duration(days: 365 * 5)),
                  );
                  if (picked != null) setSheetState(() => targetDate = picked);
                },
                borderRadius: BorderRadius.circular(12),
                child: InputDecorator(
                  decoration: InputDecoration(
                    labelText: 'Hedef Tarihi (opsiyonel)',
                    prefixIcon: const Icon(Icons.calendar_today_outlined),
                    border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                  ),
                  child: Text(targetDate == null
                      ? 'Seçilmedi'
                      : '${targetDate!.day}.${targetDate!.month}.${targetDate!.year}'),
                ),
              ),
              const SizedBox(height: 20),
              ElevatedButton(
                onPressed: () => Navigator.pop(ctx, true),
                style: ElevatedButton.styleFrom(
                  padding: const EdgeInsets.symmetric(vertical: 16),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                ),
                child: const Text('Oluştur'),
              ),
            ],
          ),
        ),
      ),
    );

    if (result != true) return;
    final name = nameController.text.trim();
    final amount = double.tryParse(amountController.text.trim().replaceAll(',', '.'));
    if (name.isEmpty || amount == null || amount <= 0) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Geçerli bir ad ve tutar gir')),
        );
      }
      return;
    }

    try {
      final targetDateStr = targetDate != null
          ? '${targetDate!.year}-${targetDate!.month.toString().padLeft(2, '0')}-${targetDate!.day.toString().padLeft(2, '0')}'
          : null;
      await SavingsGoalService.createGoal(name: name, targetAmount: amount, targetDate: targetDateStr);
      _loadGoals();
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Hedef oluşturulamadı: $e')));
      }
    }
  }

  Future<void> _showGoalDetail(SavingsGoal goal) async {
    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(24))),
      builder: (ctx) => _GoalDetailSheet(
        goal: goal,
        currencyFormat: _currencyFormat,
        onChanged: _loadGoals,
      ),
    );
  }

  Future<void> _deleteGoal(SavingsGoal goal) async {
    try {
      await SavingsGoalService.deleteGoal(goal.id);
      setState(() => _goals.removeWhere((g) => g.id == goal.id));
    } catch (e) {
      _loadGoals();
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Silinemedi: $e')));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Tasarruf Hedeflerim')),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator(color: AppColors.primary))
          : _errorMessage != null
              ? Center(child: Text(_errorMessage!, style: TextStyle(color: AppColors.textSecondary)))
              : _goals.isEmpty
                  ? Center(
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(Icons.savings_outlined, size: 72, color: AppColors.primary),
                          const SizedBox(height: 12),
                          Text('Henüz tasarruf hedefin yok',
                              style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700, color: AppColors.txt(context))),
                          const SizedBox(height: 6),
                          Text('Bir hedef belirlemek için + butonuna bas',
                              style: TextStyle(fontSize: 13, color: AppColors.textSecondary)),
                        ],
                      ),
                    )
                  : RefreshIndicator(
                      onRefresh: _loadGoals,
                      child: ListView.builder(
                        padding: const EdgeInsets.fromLTRB(16, 16, 16, 100),
                        itemCount: _goals.length,
                        itemBuilder: (context, index) {
                          final goal = _goals[index];
                          return Dismissible(
                            key: Key('goal_${goal.id}'),
                            direction: DismissDirection.endToStart,
                            background: Container(
                              margin: const EdgeInsets.only(bottom: 12),
                              decoration: BoxDecoration(
                                color: AppColors.error,
                                borderRadius: BorderRadius.circular(16),
                              ),
                              alignment: Alignment.centerRight,
                              padding: const EdgeInsets.only(right: 20),
                              child: const Icon(Icons.delete_outline, color: Colors.white),
                            ),
                            confirmDismiss: (_) async {
                              return await showDialog<bool>(
                                    context: context,
                                    builder: (context) => AlertDialog(
                                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
                                      title: const Text('Hedefi Sil'),
                                      content: Text('"${goal.name}" hedefini silmek istediğine emin misin?'),
                                      actions: [
                                        TextButton(
                                            onPressed: () => Navigator.pop(context, false),
                                            child: const Text('Vazgeç')),
                                        TextButton(
                                          onPressed: () => Navigator.pop(context, true),
                                          style: TextButton.styleFrom(foregroundColor: AppColors.error),
                                          child: const Text('Sil'),
                                        ),
                                      ],
                                    ),
                                  ) ??
                                  false;
                            },
                            onDismissed: (_) => _deleteGoal(goal),
                            child: _GoalCard(
                              goal: goal,
                              currencyFormat: _currencyFormat,
                              onTap: () => _showGoalDetail(goal),
                            ),
                          );
                        },
                      ),
                    ),
      floatingActionButton: FloatingActionButton(
        onPressed: _showCreateGoalDialog,
        child: const Icon(Icons.add),
      ),
    );
  }
}

class _GoalCard extends StatelessWidget {
  final SavingsGoal goal;
  final NumberFormat currencyFormat;
  final VoidCallback onTap;

  const _GoalCard({required this.goal, required this.currencyFormat, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        margin: const EdgeInsets.only(bottom: 12),
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: AppColors.surf(context),
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: AppColors.brd(context)),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(goal.achieved ? Icons.emoji_events : Icons.savings_outlined,
                    color: goal.achieved ? AppColors.warning : AppColors.primary, size: 22),
                const SizedBox(width: 10),
                Expanded(
                  child: Text(goal.name,
                      style: TextStyle(fontSize: 15, fontWeight: FontWeight.w700, color: AppColors.txt(context))),
                ),
                if (goal.targetDate != null)
                  Text(goal.targetDate!,
                      style: TextStyle(fontSize: 11, color: AppColors.textSecondary)),
              ],
            ),
            const SizedBox(height: 12),
            ClipRRect(
              borderRadius: BorderRadius.circular(4),
              child: LinearProgressIndicator(
                value: goal.progressPercent / 100,
                backgroundColor: AppColors.brd(context),
                valueColor: AlwaysStoppedAnimation<Color>(goal.achieved ? AppColors.warning : AppColors.primary),
                minHeight: 8,
              ),
            ),
            const SizedBox(height: 8),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  '${currencyFormat.format(goal.currentAmount)} / ${currencyFormat.format(goal.targetAmount)} TL',
                  style: TextStyle(fontSize: 12, color: AppColors.textSecondary),
                ),
                Text('%${goal.progressPercent.toStringAsFixed(0)}',
                    style: TextStyle(fontSize: 12, fontWeight: FontWeight.w700, color: AppColors.primary)),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _GoalDetailSheet extends StatefulWidget {
  final SavingsGoal goal;
  final NumberFormat currencyFormat;
  final VoidCallback onChanged;

  const _GoalDetailSheet({required this.goal, required this.currencyFormat, required this.onChanged});

  @override
  State<_GoalDetailSheet> createState() => _GoalDetailSheetState();
}

class _GoalDetailSheetState extends State<_GoalDetailSheet> {
  final _amountController = TextEditingController();
  bool _isSubmitting = false;
  bool _isLoadingSuggestion = false;
  String? _suggestionComment;

  @override
  void dispose() {
    _amountController.dispose();
    super.dispose();
  }

  Future<void> _contribute(bool isAdd) async {
    final amount = double.tryParse(_amountController.text.trim().replaceAll(',', '.'));
    if (amount == null || amount <= 0 || _isSubmitting) return;
    setState(() => _isSubmitting = true);
    try {
      await SavingsGoalService.contribute(widget.goal.id, isAdd ? amount : -amount);
      widget.onChanged();
      if (mounted) Navigator.pop(context);
    } catch (e) {
      setState(() => _isSubmitting = false);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
      }
    }
  }

  Future<void> _fetchSuggestion() async {
    setState(() => _isLoadingSuggestion = true);
    try {
      final suggestion = await SavingsGoalService.getSuggestion(widget.goal.id);
      setState(() => _suggestionComment = suggestion.comment);
    } catch (e) {
      setState(() => _suggestionComment = NetworkError.friendlyMessage(e, fallback: 'Öneri alınamadı, lütfen tekrar deneyin.'));
    } finally {
      if (mounted) setState(() => _isLoadingSuggestion = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final goal = widget.goal;
    return Padding(
      padding: EdgeInsets.fromLTRB(20, 20, 20, MediaQuery.of(context).viewInsets.bottom + 24),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(goal.name, style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700, color: AppColors.txt(context))),
          const SizedBox(height: 4),
          Text(
            '${widget.currencyFormat.format(goal.currentAmount)} / ${widget.currencyFormat.format(goal.targetAmount)} TL',
            style: TextStyle(fontSize: 13, color: AppColors.textSecondary),
          ),
          const SizedBox(height: 16),
          TextField(
            controller: _amountController,
            keyboardType: const TextInputType.numberWithOptions(decimal: true),
            decoration: InputDecoration(
              labelText: 'Tutar (TL)',
              border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
            ),
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: OutlinedButton(
                  onPressed: _isSubmitting ? null : () => _contribute(false),
                  style: OutlinedButton.styleFrom(padding: const EdgeInsets.symmetric(vertical: 14)),
                  child: const Text('Para Çıkar'),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: ElevatedButton(
                  onPressed: _isSubmitting ? null : () => _contribute(true),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppColors.primary,
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(vertical: 14),
                  ),
                  child: const Text('Para Ekle'),
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          if (_suggestionComment != null)
            Container(
              padding: const EdgeInsets.all(14),
              margin: const EdgeInsets.only(bottom: 12),
              decoration: BoxDecoration(
                color: AppColors.primDim(context),
                borderRadius: BorderRadius.circular(12),
              ),
              child: Text(_suggestionComment!, style: TextStyle(fontSize: 13, color: AppColors.txt(context))),
            ),
          TextButton.icon(
            onPressed: _isLoadingSuggestion ? null : _fetchSuggestion,
            icon: _isLoadingSuggestion
                ? const SizedBox(height: 14, width: 14, child: CircularProgressIndicator(strokeWidth: 2))
                : const Icon(Icons.auto_awesome, size: 16),
            label: Text(_isLoadingSuggestion ? 'Hesaplanıyor...' : 'AI ile Ne Kadar Biriktirmeliyim?'),
            style: TextButton.styleFrom(foregroundColor: AppColors.primary),
          ),
        ],
      ),
    );
  }
}
