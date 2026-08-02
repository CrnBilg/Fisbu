import 'package:flutter/material.dart';
import '../../models/category.dart';
import '../theme/app_colors.dart';

/// Kategori seçici dropdown — AddReceiptScreen ve ekstre içe aktarma
/// inceleme ekranında aynı stil/davranışla yeniden kullanılır.
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

  @override
  Widget build(BuildContext context) {
    if (isLoading) {
      return const Center(child: CircularProgressIndicator(color: AppColors.primary));
    }
    return DropdownButtonFormField<Category>(
      value: value,
      decoration: InputDecoration(
        labelText: 'Kategori',
        prefixIcon: const Icon(Icons.category_outlined),
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
        filled: true,
        fillColor: AppColors.surf(context),
        isDense: true,
      ),
      hint: const Text('Kategori seç'),
      items: categories.map((category) {
        return DropdownMenuItem(value: category, child: Text(category.name));
      }).toList(),
      onChanged: onChanged,
    );
  }
}
