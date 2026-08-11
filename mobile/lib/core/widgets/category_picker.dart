import 'package:flutter/material.dart';
import '../../models/category.dart';
import '../theme/app_colors.dart';

/// Kategori seçici — AddReceiptScreen, ReceiptVerificationScreen ve ekstre
/// içe aktarma inceleme ekranında aynı stil/davranışla yeniden kullanılır.
/// Tıklanınca kategori sayısı arttıkça kaydırmayı zorlaştırmamak için
/// aranabilir bir bottom sheet açar.
class CategoryPicker extends StatelessWidget {
  final List<Category> categories;
  final Category? value;
  final ValueChanged<Category?> onChanged;
  final bool isLoading;

  const CategoryPicker({
    super.key,
    required this.categories,
    required this.value,
    required this.onChanged,
    this.isLoading = false,
  });

  Future<void> _openPicker(BuildContext context) async {
    final selected = await showModalBottomSheet<Category>(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(24))),
      builder: (ctx) => _CategorySearchSheet(categories: categories),
    );
    if (selected != null) onChanged(selected);
  }

  @override
  Widget build(BuildContext context) {
    if (isLoading) {
      return const Center(child: CircularProgressIndicator(color: AppColors.primary));
    }
    return InkWell(
      borderRadius: BorderRadius.circular(12),
      onTap: () => _openPicker(context),
      child: InputDecorator(
        decoration: InputDecoration(
          labelText: 'Kategori',
          prefixIcon: const Icon(Icons.category_outlined),
          border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
          filled: true,
          fillColor: AppColors.surf(context),
          isDense: true,
        ),
        child: Text(
          value?.name ?? 'Kategori seç',
          style: TextStyle(
            color: value == null ? AppColors.textTertiary : AppColors.txt(context),
          ),
        ),
      ),
    );
  }
}

class _CategorySearchSheet extends StatefulWidget {
  final List<Category> categories;

  const _CategorySearchSheet({required this.categories});

  @override
  State<_CategorySearchSheet> createState() => _CategorySearchSheetState();
}

class _CategorySearchSheetState extends State<_CategorySearchSheet> {
  String _query = '';

  @override
  Widget build(BuildContext context) {
    final filtered = _query.isEmpty
        ? widget.categories
        : widget.categories
            .where((c) => c.name.toLowerCase().contains(_query.toLowerCase()))
            .toList();

    return Padding(
      padding: EdgeInsets.only(bottom: MediaQuery.of(context).viewInsets.bottom),
      child: SizedBox(
        height: MediaQuery.of(context).size.height * 0.7,
        child: Column(
          children: [
            const SizedBox(height: 12),
            Center(child: Container(width: 40, height: 4, decoration: BoxDecoration(color: AppColors.brd(context), borderRadius: BorderRadius.circular(2)))),
            Padding(
              padding: const EdgeInsets.fromLTRB(20, 16, 20, 12),
              child: TextField(
                autofocus: true,
                onChanged: (v) => setState(() => _query = v),
                decoration: InputDecoration(
                  hintText: 'Kategori ara',
                  prefixIcon: const Icon(Icons.search),
                  border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                  filled: true,
                  fillColor: AppColors.surf(context),
                  isDense: true,
                ),
              ),
            ),
            Expanded(
              child: filtered.isEmpty
                  ? Center(
                      child: Text(
                        'Kategori bulunamadı',
                        style: TextStyle(color: AppColors.textTertiary),
                      ),
                    )
                  : ListView.builder(
                      padding: const EdgeInsets.only(bottom: 12),
                      itemCount: filtered.length,
                      itemBuilder: (ctx, i) {
                        final category = filtered[i];
                        return ListTile(
                          leading: const Icon(Icons.category_outlined, color: AppColors.primary),
                          title: Text(category.name),
                          onTap: () => Navigator.pop(context, category),
                        );
                      },
                    ),
            ),
          ],
        ),
      ),
    );
  }
}
