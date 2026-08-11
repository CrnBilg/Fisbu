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

    final media = MediaQuery.of(context);
    // Klavye açıkken (arama kutusu autofocus) sabit %70 yükseklik + klavye
    // yüksekliği üst üste binip sheet'i ekran dışına taşırabiliyordu — yükseklik
    // artık klavye sonrası kalan görünür alana göre üst sınırlanıyor.
    final availableHeight = media.size.height - media.viewInsets.bottom - media.padding.top;
    final sheetHeight = (media.size.height * 0.7).clamp(0.0, availableHeight * 0.92);

    return Padding(
      padding: EdgeInsets.only(bottom: media.viewInsets.bottom),
      child: SizedBox(
        height: sheetHeight,
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
