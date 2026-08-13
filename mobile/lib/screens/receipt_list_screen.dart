import 'dart:async';
import 'package:flutter/material.dart';
import 'add_receipt_screen.dart';
import 'receipt_detail_screen.dart';
import '../models/receipt.dart';
import '../models/category.dart' as model;
import '../services/receipt_service.dart';
import 'package:intl/intl.dart';
import '../core/utils/date_formatter.dart';
import '../core/utils/category_helper.dart';
import '../core/utils/network_error.dart';
import '../core/theme/app_colors.dart';
import '../core/widgets/offline_banner.dart';

const String _uncategorizedLabel = 'Kategorisiz';

class ReceiptListScreen extends StatefulWidget {
  const ReceiptListScreen({super.key});

  @override
  State<ReceiptListScreen> createState() => _ReceiptListScreenState();
}

class _ReceiptListScreenState extends State<ReceiptListScreen> {
  final ScrollController _scrollController = ScrollController();
  final TextEditingController _searchController = TextEditingController();
  Timer? _searchDebounce;

  List<Receipt> _receipts = [];
  List<model.Category> _categories = [];
  int _page = 0;
  bool _hasNext = false;
  bool _isLoading = true;
  bool _isLoadingMore = false;
  String _searchQuery = '';
  String? _selectedCategoryLabel;
  String? _errorMessage;

  // Arama/filtre değişince önceki (daha yavaş) isteğin geç gelen cevabının state'i
  // ezmesini önler — her _loadReceipts çağrısı kendi ID'sini alır, cevap geldiğinde
  // hâlâ en güncel istek o mu diye kontrol edilir
  int _requestId = 0;

  @override
  void initState() {
    super.initState();
    _scrollController.addListener(_onScroll);
    _loadCategories();
    _loadReceipts(reset: true);
  }

  @override
  void dispose() {
    _searchDebounce?.cancel();
    _scrollController.dispose();
    _searchController.dispose();
    super.dispose();
  }

  void _onScroll() {
    if (!_hasNext || _isLoadingMore || _isLoading) return;
    if (_scrollController.position.pixels >=
        _scrollController.position.maxScrollExtent - 300) {
      _loadReceipts();
    }
  }

  Future<void> _loadCategories() async {
    try {
      final categories = await ReceiptService.getCategories();
      if (mounted) setState(() => _categories = categories);
    } catch (_) {
      // Filtre çipleri için ikincil veri — sessizce yok say
    }
  }

  int? get _selectedCategoryId {
    if (_selectedCategoryLabel == null || _selectedCategoryLabel == _uncategorizedLabel) {
      return null;
    }
    return _categories
        .firstWhere((c) => c.name == _selectedCategoryLabel,
            orElse: () => model.Category(id: -1, name: ''))
        .id;
  }

  bool get _selectedUncategorized => _selectedCategoryLabel == _uncategorizedLabel;

  Future<void> _loadReceipts({bool reset = false}) async {
    final requestId = ++_requestId;
    final nextPage = reset ? 0 : _page + 1;
    setState(() {
      if (reset) {
        _isLoading = true;
        _errorMessage = null;
      } else {
        _isLoadingMore = true;
      }
    });

    try {
      final result = await ReceiptService.searchReceipts(
        query: _searchQuery,
        categoryId: _selectedCategoryId,
        uncategorized: _selectedUncategorized,
        page: nextPage,
      );
      if (!mounted || requestId != _requestId) return;
      setState(() {
        _receipts = reset ? result.content : [..._receipts, ...result.content];
        _page = result.page;
        _hasNext = result.hasNext;
        _isLoading = false;
        _isLoadingMore = false;
      });
    } catch (e) {
      if (!mounted || requestId != _requestId) return;
      setState(() {
        _isLoading = false;
        _isLoadingMore = false;
        if (reset) _errorMessage = NetworkError.friendlyMessage(e, fallback: 'Fişler yüklenemedi, lütfen tekrar deneyin.');
      });
    }
  }

  void _onSearchChanged(String value) {
    setState(() => _searchQuery = value);
    _searchDebounce?.cancel();
    _searchDebounce = Timer(const Duration(milliseconds: 400), () {
      _loadReceipts(reset: true);
    });
  }

  void _onCategorySelected(String? label) {
    setState(() => _selectedCategoryLabel = label);
    _loadReceipts(reset: true);
  }

  Future<void> _goToAddReceipt() async {
    final result = await Navigator.push(
      context,
      MaterialPageRoute(builder: (context) => const AddReceiptScreen()),
    );
    if (result == true) _loadReceipts(reset: true);
  }

  Future<void> _goToDetail(Receipt receipt) async {
    final result = await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => ReceiptDetailScreen(receipt: receipt),
      ),
    );
    if (result == true) _loadReceipts(reset: true);
  }

  Widget _buildFilterChip(String label, String? value) {
    final isSelected = _selectedCategoryLabel == value;
    final color = value != null
        ? CategoryHelper.getColor(value)
        : AppColors.primary;
    return GestureDetector(
      onTap: () => _onCategorySelected(value),
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 200),
        margin: const EdgeInsets.only(right: 8),
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
        decoration: BoxDecoration(
          color: isSelected ? color : AppColors.surf(context),
          borderRadius: BorderRadius.circular(20),
          border: Border.all(
            color: isSelected ? color : AppColors.brd(context),
          ),
        ),
        child: Text(
          label,
          style: TextStyle(
            fontSize: 12,
            fontWeight: FontWeight.w600,
            color: isSelected ? Colors.white : AppColors.textSecondary,
          ),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(

      appBar: AppBar(
        title: Text('Fişlerim'),
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(110),
          child: Padding(
            padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
            child: Column(
              children: [
                TextField(
                  controller: _searchController,
                  onChanged: _onSearchChanged,
                  decoration: InputDecoration(
                    hintText: 'Mağaza ara...',
                    prefixIcon: const Icon(Icons.search, size: 20),
                    suffixIcon: _searchQuery.isNotEmpty
                        ? IconButton(
                            icon: const Icon(Icons.clear, size: 18),
                            onPressed: () {
                              _searchController.clear();
                              _onSearchChanged('');
                            },
                          )
                        : null,
                    filled: true,
                    fillColor: AppColors.surf(context),
                    contentPadding: const EdgeInsets.symmetric(
                        horizontal: 16, vertical: 10),
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(12),
                      borderSide: BorderSide.none,
                    ),
                  ),
                ),
                const SizedBox(height: 8),
                SizedBox(
                  height: 32,
                  child: ListView(
                    scrollDirection: Axis.horizontal,
                    children: [
                      _buildFilterChip('Tümü', null),
                      ..._categories.map((c) => _buildFilterChip(c.name, c.name)),
                      _buildFilterChip(_uncategorizedLabel, _uncategorizedLabel),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
      body: Column(
        children: [
          const OfflineBanner(),
          Expanded(child: _buildBody()),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: _goToAddReceipt,
        child: const Icon(Icons.add),
      ),
    );
  }

  Widget _buildBody() {
    if (_isLoading) {
      return const Center(
        child: CircularProgressIndicator(color: AppColors.primary),
      );
    }

    if (_errorMessage != null) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                color: AppColors.errDim(context),
                shape: BoxShape.circle,
              ),
              child: const Icon(Icons.error_outline,
                  size: 48, color: AppColors.error),
            ),
            const SizedBox(height: 16),
            Text(
              _errorMessage!,
              textAlign: TextAlign.center,
              style: TextStyle(color: AppColors.textSecondary),
            ),
            const SizedBox(height: 16),
            ElevatedButton(
              onPressed: () => _loadReceipts(reset: true),
              child: Text('Tekrar Dene'),
            ),
          ],
        ),
      );
    }

   if (_receipts.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.receipt_long_outlined, size: 80, color: AppColors.primary),
            const SizedBox(height: 8),
            Text(
              'Henüz fiş yok',
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.w700,
                color: AppColors.txt(context),
              ),
            ),
            const SizedBox(height: 6),
            Text(
              'İlk fişini eklemek için + butonuna bas',
              style: TextStyle(
                fontSize: 14,
                color: AppColors.textSecondary,
              ),
            ),
          ],
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: () => _loadReceipts(reset: true),
      color: AppColors.primary,
      child: ListView.builder(
        controller: _scrollController,
        padding: const EdgeInsets.fromLTRB(16, 16, 16, 100),
        itemCount: _receipts.length + (_hasNext ? 1 : 0),
        itemBuilder: (context, index) {
          if (index >= _receipts.length) {
            return const Padding(
              padding: EdgeInsets.symmetric(vertical: 20),
              child: Center(
                child: SizedBox(
                  width: 24,
                  height: 24,
                  child: CircularProgressIndicator(
                      strokeWidth: 2, color: AppColors.primary),
                ),
              ),
            );
          }
          final receipt = _receipts[index];
          return Dismissible(
            key: Key('receipt_${receipt.id}'),
            direction: DismissDirection.endToStart,
            background: Container(
              margin: const EdgeInsets.only(bottom: 12),
              decoration: BoxDecoration(
                color: AppColors.error,
                borderRadius: BorderRadius.circular(16),
              ),
              alignment: Alignment.centerRight,
              padding: const EdgeInsets.only(right: 20),
              child: const Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(Icons.delete_outline, color: Colors.white, size: 26),
                  SizedBox(height: 4),
                  Text(
                    'Sil',
                    style: TextStyle(
                      color: Colors.white,
                      fontSize: 12,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ],
              ),
            ),
            confirmDismiss: (direction) async {
              return await showDialog<bool>(
                context: context,
                builder: (context) => AlertDialog(
                  shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(20)),
                  title: Text('Fişi Sil',
                      style: TextStyle(fontWeight: FontWeight.w700)),
                  content: Text(
                      '${receipt.storeName} fişini silmek istediğine emin misin?'),
                  actions: [
                    TextButton(
                      onPressed: () => Navigator.pop(context, false),
                      child: Text('Vazgeç'),
                    ),
                    TextButton(
                      onPressed: () => Navigator.pop(context, true),
                      style: TextButton.styleFrom(
                          foregroundColor: AppColors.error),
                      child: Text('Sil'),
                    ),
                  ],
                ),
              );
            },
            onDismissed: (direction) async {
              try {
                await ReceiptService.deleteReceipt(receipt.id);
                setState(() => _receipts.removeAt(index));
                if (mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(
                      content: Text('${receipt.storeName} silindi'),
                      behavior: SnackBarBehavior.floating,
                    ),
                  );
                }
              } catch (e) {
                _loadReceipts();
                if (mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(content: Text('Silinemedi: $e')),
                  );
                }
              }
            },
            child: _ReceiptCard(
              receipt: receipt,
              onTap: () => _goToDetail(receipt),
            ),
          );
        },
      ),
    );
  }
}

class _ReceiptCard extends StatelessWidget {
  final Receipt receipt;
  final VoidCallback onTap;

  const _ReceiptCard({required this.receipt, required this.onTap});

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
          boxShadow: [
            BoxShadow(
              color: AppColors.primary.withOpacity(0.04),
              blurRadius: 12,
              offset: const Offset(0, 4),
            ),
          ],
        ),
        child: Row(
          children: [
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: CategoryHelper.getColor(receipt.categoryName)
                    .withOpacity(0.12),
                borderRadius: BorderRadius.circular(12),
              ),
              child: Icon(
                CategoryHelper.getIcon(receipt.categoryName),
                color: CategoryHelper.getColor(receipt.categoryName),
                size: 22,
              ),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    receipt.storeName,
                    style: TextStyle(
                      fontSize: 15,
                      fontWeight: FontWeight.w700,
                      color: AppColors.txt(context),
                    ),
                  ),
                  const SizedBox(height: 4),
                  Row(
                    children: [
                      Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 8,
                          vertical: 2,
                        ),
                        decoration: BoxDecoration(
                          color: AppColors.primDim(context),
                          borderRadius: BorderRadius.circular(6),
                        ),
                        child: Text(
                          receipt.categoryName ?? 'Kategorisiz',
                          style: TextStyle(
                            fontSize: 11,
                            fontWeight: FontWeight.w600,
                            color: AppColors.primary,
                          ),
                        ),
                      ),
                      const SizedBox(width: 8),
                      Text(
                        DateFormatter.formatShort(receipt.receiptDate),
                        style: TextStyle(
                          fontSize: 12,
                          color: AppColors.textSecondary,
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
            Column(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Text(
                  '${NumberFormat('#,##0.00', 'tr_TR').format(receipt.totalAmount)} TL',
                  style: TextStyle(
                    fontSize: 15,
                    fontWeight: FontWeight.w800,
                    color: AppColors.primary,
                  ),
                ),
                const SizedBox(height: 4),
                const Icon(
                  Icons.chevron_right,
                  color: AppColors.textSecondary,
                  size: 18,
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}