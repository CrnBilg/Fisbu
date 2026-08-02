class BulkImportError {
  final int index;
  final String error;

  BulkImportError({required this.index, required this.error});

  factory BulkImportError.fromJson(Map<String, dynamic> json) {
    return BulkImportError(
      index: (json['index'] as num?)?.toInt() ?? 0,
      error: (json['error'] as String?) ?? 'Hata oluştu',
    );
  }
}

class BulkImportResult {
  final int createdCount;
  final List<BulkImportError> failed;

  BulkImportResult({required this.createdCount, required this.failed});

  factory BulkImportResult.fromJson(Map<String, dynamic> json) {
    return BulkImportResult(
      createdCount: (json['created'] as List<dynamic>?)?.length ?? 0,
      failed: (json['failed'] as List<dynamic>?)
              ?.map((e) => BulkImportError.fromJson(e as Map<String, dynamic>))
              .toList() ??
          const [],
    );
  }
}
