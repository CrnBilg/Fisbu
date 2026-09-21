| ID | Başlık | Öncelik | Tahmin | Durum | Sprint |
|---|---|---|---|---|---|
| PERF-003 | (opsiyonel, ADR-002'de önerildi) Receipt create'e idempotency-key eklemek — sadece false-positive şikayetleri gerçekleşirse değerlendirilecek | Düşük | M | Draft | - |
| PERF-008 | (ARCH-006'da kapsam dışı bırakıldı) StatisticsService/PersonalInflationService'in tüm-geçmişi-yükleyip-Java'da-agregasyon deseni — DB-seviyesinde SUM/GROUP BY'a geçirilebilir. Sadece gerçek bir performans şikayeti/kanıtı olursa ele alınacak. | Düşük | M | Draft | - |
| MOB-007 | (MOB-006'da kapsam dışı bırakıldı) Merkezi state management'a geçiş (208 setState/27 ekran) — App Store submission'ını engellemiyor, submission SONRASINA bırakılacak büyük bir refactor. | Düşük (submission sonrası) | L | Draft | - |
| MOB-021 | (UI/UX denetimi "cila turu"nda kapsam dışı bırakıldı) Uygulama geneli mikro-etkileşim/animasyon stratejisi — şu an hiçbir yerde AnimationController/TweenAnimationBuilder/AnimatedSwitcher yok, tek ekran değil bütüncül bir tasarım kararı gerektiriyor | Düşük | L | Draft | - |
| MOB-022 | (UI/UX denetimi "cila turu"nda kapsam dışı bırakıldı) Uygulama geneli haptic feedback (silme/kaydetme/hata gibi aksiyonlarda) — çok sayıda dokunma noktasını kapsayan bir strateji gerektiriyor, tek ekran değil | Düşük | M | Draft | - |
