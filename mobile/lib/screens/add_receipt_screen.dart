import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'dart:io' show File;
import 'package:flutter/foundation.dart' show kIsWeb;
import '../services/receipt_service.dart';
import '../models/category.dart';
import '../models/receipt_item.dart';
import '../core/theme/app_colors.dart';

class _ItemRow {
  final TextEditingController nameController;
  final TextEditingController priceController;
  final TextEditingController qtyController;

  _ItemRow({String name = '', String price = '', String qty = '1'})
      : nameController = TextEditingController(text: name),
        priceController = TextEditingController(text: price),
        qtyController = TextEditingController(text: qty);

  void dispose() {
    nameController.dispose();
    priceController.dispose();
    qtyController.dispose();
  }
}

class AddReceiptScreen extends StatefulWidget {
  final String? initialStoreName;
  final String? initialAmount;
  final String? initialDate;
  final String? initialImagePath;
  final int? initialCategoryId;
  final List<ReceiptItem>? initialItems;

  const AddReceiptScreen({
    super.key,
    this.initialStoreName,
    this.initialAmount,
    this.initialDate,
    this.initialImagePath,
    this.initialCategoryId,
    this.initialItems,
  });

  @override
  State<AddReceiptScreen> createState() => _AddReceiptScreenState();
}

class _AddReceiptScreenState extends State<AddReceiptScreen> {
  final TextEditingController _storeController = TextEditingController();
  final TextEditingController _amountController = TextEditingController();

  DateTime? _selectedDate;
  Category? _selectedCategory;
  List<Category> _categories = [];
  bool _isLoading = false;
  bool _isCategoriesLoading = true;
  XFile? _selectedImage;
  final List<_ItemRow> _itemRows = [];

  @override
  void initState() {
    super.initState();
    _loadCategories();

    if (widget.initialItems != null && widget.initialItems!.isNotEmpty) {
      for (final item in widget.initialItems!) {
        _itemRows.add(_ItemRow(
          name: item.productName,
          price: item.unitPrice.toStringAsFixed(2),
          qty: item.quantity.toStringAsFixed(0),
        ));
      }
    }

    // OCR'dan gelen verileri otomatik doldur
    if (widget.initialStoreName != null) {
      _storeController.text = widget.initialStoreName!;
    }
    if (widget.initialAmount != null) {
      _amountController.text = widget.initialAmount!;
    }
    if (widget.initialDate != null) {
      try {
        _selectedDate = DateTime.parse(widget.initialDate!);
      } catch (_) {}
    }
    if (widget.initialImagePath != null) {
      _selectedImage = XFile(widget.initialImagePath!);
    }
  }

  Future<void> _loadCategories() async {
    try {
      final categories = await ReceiptService.getCategories();
      Category? matched;
      if (widget.initialCategoryId != null) {
        for (final category in categories) {
          if (category.id == widget.initialCategoryId) {
            matched = category;
            break;
          }
        }
      }
      setState(() {
        _categories = categories;
        _isCategoriesLoading = false;
        _selectedCategory = matched;
      });
    } catch (e) {
      setState(() => _isCategoriesLoading = false);
    }
  }

  Future<void> _pickDate() async {
    final now = DateTime.now();
    final picked = await showDatePicker(
      context: context,
      initialDate: now,
      firstDate: DateTime(2020),
      lastDate: now,
    );
    if (picked != null) {
      setState(() => _selectedDate = picked);
    }
  }

  String _formatDate(DateTime date) {
    return '${date.year}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}';
  }

  String _formatDateDisplay(DateTime date) {
    return '${date.day.toString().padLeft(2, '0')}.${date.month.toString().padLeft(2, '0')}.${date.year}';
  }

  Future<void> _pickImageFromGallery() async {
    final picker = ImagePicker();
    final image = await picker.pickImage(
      source: ImageSource.gallery,
      imageQuality: 40,
    );
    if (image != null) {
      setState(() => _selectedImage = image);
    }
  }

  Future<void> _pickImageFromCamera() async {
    final picker = ImagePicker();
    final image = await picker.pickImage(
      source: ImageSource.camera,
      imageQuality: 40,
    );
    if (image != null) {
      setState(() => _selectedImage = image);
    }
  }

  void _showImagePickerOptions() {
    showModalBottomSheet(
      context: context,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (context) => Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              'Fiş Fotoğrafı',
              style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700),
            ),
            const SizedBox(height: 16),
            ListTile(
              leading: Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  color: AppColors.primDim(context),
                  borderRadius: BorderRadius.circular(10),
                ),
                child: const Icon(
                  Icons.photo_library_outlined,
                  color: AppColors.primary,
                ),
              ),
              title: Text('Galeriden Seç'),
              onTap: () {
                Navigator.pop(context);
                _pickImageFromGallery();
              },
            ),
            ListTile(
              leading: Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  color: AppColors.successDim,
                  borderRadius: BorderRadius.circular(10),
                ),
                child: const Icon(
                  Icons.camera_alt_outlined,
                  color: AppColors.success,
                ),
              ),
              title: Text('Kamerayla Çek'),
              onTap: () {
                Navigator.pop(context);
                _pickImageFromCamera();
              },
            ),
            if (_selectedImage != null)
              ListTile(
                leading: Container(
                  padding: const EdgeInsets.all(8),
                  decoration: BoxDecoration(
                    color: AppColors.errDim(context),
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: const Icon(
                    Icons.delete_outline,
                    color: AppColors.error,
                  ),
                ),
                title: Text(
                  'Fotoğrafı Kaldır',
                  style: TextStyle(color: AppColors.error),
                ),
                onTap: () {
                  Navigator.pop(context);
                  setState(() => _selectedImage = null);
                },
              ),
            const SizedBox(height: 8),
          ],
        ),
      ),
    );
  }

  Future<void> _handleSave({bool allowDuplicate = false}) async {
    final store = _storeController.text.trim();
    final amountText = _amountController.text.trim();

    if (store.isEmpty || amountText.isEmpty || _selectedDate == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Lütfen tüm alanları doldur')),
      );
      return;
    }

    final amount = double.tryParse(amountText.replaceAll(',', '.'));
    if (amount == null) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('Geçerli bir tutar gir')));
      return;
    }

    setState(() => _isLoading = true);

    try {
      final saved = await ReceiptService.submitReceipt(
        storeName: store,
        totalAmount: amount,
        receiptDate: _formatDate(_selectedDate!),
        categoryId: _selectedCategory?.id,
        image: _selectedImage,
        items: _collectItems(),
        allowDuplicate: allowDuplicate,
      );

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(
          content: Text(saved != null
              ? 'Fiş başarıyla eklendi!'
              : 'Bağlantı yok — fiş kaydedilmek üzere sıraya alındı'),
        ));
        Navigator.pop(context);
      }
    } on DuplicateReceiptException {
      if (mounted) {
        setState(() => _isLoading = false);
        final confirmed = await showDialog<bool>(
          context: context,
          builder: (context) => AlertDialog(
            title: const Text('Bu fiş zaten kayıtlı'),
            content: const Text(
              'Aynı mağaza, tutar ve tarihe sahip bir fiş zaten kayıtlı görünüyor. Yine de eklensin mi?',
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(context, false),
                child: const Text('Vazgeç'),
              ),
              TextButton(
                onPressed: () => Navigator.pop(context, true),
                child: const Text('Yine de Ekle'),
              ),
            ],
          ),
        );
        if (confirmed == true) {
          await _handleSave(allowDuplicate: true);
        }
      }
      return;
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('Hata: $e')));
      }
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  void dispose() {
    _storeController.dispose();
    _amountController.dispose();
    for (final row in _itemRows) {
      row.dispose();
    }
    super.dispose();
  }

  void _addItemRow() {
    setState(() => _itemRows.add(_ItemRow()));
  }

  void _removeItemRow(int index) {
    setState(() {
      _itemRows[index].dispose();
      _itemRows.removeAt(index);
    });
  }

  List<ReceiptItem> _collectItems() {
    final items = <ReceiptItem>[];
    for (final row in _itemRows) {
      final name = row.nameController.text.trim();
      final price = double.tryParse(row.priceController.text.trim().replaceAll(',', '.'));
      if (name.isEmpty || price == null || price <= 0) continue;
      final qty = double.tryParse(row.qtyController.text.trim().replaceAll(',', '.')) ?? 1;
      items.add(ReceiptItem(productName: name, unitPrice: price, quantity: qty));
    }
    return items;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('Fiş Ekle')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // Fotoğraf seçim alanı
            GestureDetector(
              onTap: _showImagePickerOptions,
              child: Container(
                height: 160,
                decoration: BoxDecoration(
                  color: AppColors.surf(context),
                  borderRadius: BorderRadius.circular(16),
                  border: Border.all(
                    color: _selectedImage != null
                        ? AppColors.primary
                        : AppColors.brd(context),
                    width: _selectedImage != null ? 2 : 1,
                  ),
                ),
                child: _selectedImage != null
                    ? ClipRRect(
                        borderRadius: BorderRadius.circular(15),
                        child: kIsWeb
                            ? Image.network(
                                _selectedImage!.path,
                                fit: BoxFit.cover,
                                width: double.infinity,
                              )
                            : Image.file(
                                File(_selectedImage!.path),
                                fit: BoxFit.cover,
                                width: double.infinity,
                              ),
                      )
                    : Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Container(
                            padding: const EdgeInsets.all(16),
                            decoration: BoxDecoration(
                              color: AppColors.primDim(context),
                              shape: BoxShape.circle,
                            ),
                            child: const Icon(
                              Icons.add_photo_alternate_outlined,
                              size: 32,
                              color: AppColors.primary,
                            ),
                          ),
                          const SizedBox(height: 12),
                          Text(
                            'Fiş Fotoğrafı Ekle',
                            style: TextStyle(
                              fontSize: 15,
                              fontWeight: FontWeight.w600,
                              color: AppColors.primary,
                            ),
                          ),
                          const SizedBox(height: 4),
                          Text(
                            'Galeriden seç veya kamerayla çek',
                            style: TextStyle(
                              fontSize: 12,
                              color: AppColors.textSecondary,
                            ),
                          ),
                        ],
                      ),
              ),
            ),
            const SizedBox(height: 20),

            // Mağaza adı
            TextField(
              controller: _storeController,
              decoration: InputDecoration(
                labelText: 'Mağaza Adı',
                prefixIcon: const Icon(Icons.store_outlined),
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
                filled: true,
                fillColor: AppColors.surf(context),
              ),
            ),
            const SizedBox(height: 16),

            // Tutar
            TextField(
              controller: _amountController,
              keyboardType: const TextInputType.numberWithOptions(
                decimal: true,
              ),
              decoration: InputDecoration(
                labelText: 'Tutar (TL)',
                prefixIcon: const Icon(Icons.payments_outlined),
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
                filled: true,
                fillColor: AppColors.surf(context),
              ),
            ),
            const SizedBox(height: 16),

            // Tarih seçici
            InkWell(
              onTap: _pickDate,
              borderRadius: BorderRadius.circular(12),
              child: InputDecorator(
                decoration: InputDecoration(
                  labelText: 'Tarih',
                  prefixIcon: const Icon(Icons.calendar_today_outlined),
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12),
                  ),
                  filled: true,
                  fillColor: AppColors.surf(context),
                ),
                child: Text(
                  _selectedDate == null
                      ? 'Tarih seç'
                      : _formatDateDisplay(_selectedDate!),
                  style: TextStyle(
                    fontSize: 16,
                    color: _selectedDate == null
                        ? AppColors.textTertiary
                        : AppColors.txt(context),
                  ),
                ),
              ),
            ),
            const SizedBox(height: 16),

            // Kategori seçici
            _isCategoriesLoading
                ? const Center(
                    child: CircularProgressIndicator(color: AppColors.primary),
                  )
                : DropdownButtonFormField<Category>(
                    value: _selectedCategory,
                    decoration: InputDecoration(
                      labelText: 'Kategori',
                      prefixIcon: const Icon(Icons.category_outlined),
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                      filled: true,
                      fillColor: AppColors.surf(context),
                    ),
                    hint: Text('Kategori seç'),
                    items: _categories.map((category) {
                      return DropdownMenuItem(
                        value: category,
                        child: Text(category.name),
                      );
                    }).toList(),
                    onChanged: (value) {
                      setState(() => _selectedCategory = value);
                    },
                  ),
            const SizedBox(height: 24),

            // Ürünler (opsiyonel) — kişisel enflasyon takibi için satır kalemleri
            Row(
              children: [
                Text(
                  'Ürünler (opsiyonel)',
                  style: TextStyle(
                    fontSize: 15,
                    fontWeight: FontWeight.w700,
                    color: AppColors.txt(context),
                  ),
                ),
                const Spacer(),
                TextButton.icon(
                  onPressed: _addItemRow,
                  icon: const Icon(Icons.add, size: 18),
                  label: const Text('Ürün Ekle'),
                  style: TextButton.styleFrom(foregroundColor: AppColors.primary),
                ),
              ],
            ),
            if (_itemRows.isEmpty)
              Text(
                'Ürün eklersen zamanla fiyat değişimini takip edebilirsin.',
                style: TextStyle(fontSize: 12, color: AppColors.textSecondary),
              ),
            ..._itemRows.asMap().entries.map((entry) {
              final index = entry.key;
              final row = entry.value;
              return Padding(
                padding: const EdgeInsets.only(top: 10),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.center,
                  children: [
                    Expanded(
                      flex: 4,
                      child: TextField(
                        controller: row.nameController,
                        decoration: InputDecoration(
                          labelText: 'Ürün adı',
                          isDense: true,
                          border: OutlineInputBorder(
                            borderRadius: BorderRadius.circular(10),
                          ),
                          filled: true,
                          fillColor: AppColors.surf(context),
                        ),
                      ),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      flex: 2,
                      child: TextField(
                        controller: row.priceController,
                        keyboardType: const TextInputType.numberWithOptions(decimal: true),
                        decoration: InputDecoration(
                          labelText: 'Fiyat',
                          isDense: true,
                          border: OutlineInputBorder(
                            borderRadius: BorderRadius.circular(10),
                          ),
                          filled: true,
                          fillColor: AppColors.surf(context),
                        ),
                      ),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      flex: 2,
                      child: TextField(
                        controller: row.qtyController,
                        keyboardType: const TextInputType.numberWithOptions(decimal: true),
                        decoration: InputDecoration(
                          labelText: 'Adet',
                          isDense: true,
                          border: OutlineInputBorder(
                            borderRadius: BorderRadius.circular(10),
                          ),
                          filled: true,
                          fillColor: AppColors.surf(context),
                        ),
                      ),
                    ),
                    IconButton(
                      onPressed: () => _removeItemRow(index),
                      icon: const Icon(Icons.close, size: 18),
                      color: AppColors.error,
                    ),
                  ],
                ),
              );
            }),

            const SizedBox(height: 32),

            // Kaydet butonu
            ElevatedButton(
              onPressed: _isLoading ? null : _handleSave,
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.primary,
                foregroundColor: Colors.white,
                padding: const EdgeInsets.symmetric(vertical: 16),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
              ),
              child: _isLoading
                  ? const SizedBox(
                      height: 20,
                      width: 20,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: Colors.white,
                      ),
                    )
                  : Text('Kaydet', style: TextStyle(fontSize: 16)),
            ),
          ],
        ),
      ),
    );
  }
}
