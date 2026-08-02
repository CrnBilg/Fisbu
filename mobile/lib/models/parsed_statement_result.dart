import 'imported_transaction.dart';

class ParsedStatementResult {
  final String sourceType;
  final List<ImportedTransaction> transactions;
  final List<String> warnings;

  ParsedStatementResult({
    required this.sourceType,
    required this.transactions,
    this.warnings = const [],
  });

  factory ParsedStatementResult.fromJson(Map<String, dynamic> json) {
    return ParsedStatementResult(
      sourceType: (json['sourceType'] as String?) ?? 'AI_EXTRACTED',
      transactions: (json['transactions'] as List<dynamic>?)
              ?.map((e) => ImportedTransaction.fromJson(e as Map<String, dynamic>))
              .toList() ??
          const [],
      warnings: (json['warnings'] as List<dynamic>?)?.map((e) => e as String).toList() ?? const [],
    );
  }
}
