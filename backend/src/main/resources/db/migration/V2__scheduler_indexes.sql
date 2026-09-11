-- PERF-002: WarrantyReminderScheduler her gün return_deadline/warranty_expiry_date
-- kolonlarını filtreliyor; bu kolonlarda index yoktu (tam tablo taraması riski).
-- CREATE INDEX (CONCURRENTLY değil) küçük/orta tablo boyutunda kısa bir kilit alır;
-- bu proje boyutunda (henüz düşük hacim) kabul edilebilir, kesinti riski yok.

CREATE INDEX idx_receipts_return_deadline ON receipts (return_deadline)
    WHERE return_reminder_sent = false;

CREATE INDEX idx_receipts_warranty_expiry_date ON receipts (warranty_expiry_date)
    WHERE warranty_reminder_sent = false;
