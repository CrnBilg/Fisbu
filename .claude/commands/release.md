---
description: Sprint'teki Done story'lerin release'e hazır olup olmadığını kontrol eder (henüz gerçek deployment değil, hazır olma kontrolü).
---
Release hazır olma kontrolü yap:
1. Sprint'teki tüm Done story'leri listele
2. Her birinin DoD'yi gerçekten karşıladığını doğrula
3. Migration sırasını kontrol et — birden fazla migration varsa doğru sırada mı
4. Geri alma (rollback) planı var mı kontrol et
5. `devops` ve `qa`'dan son onay iste
6. Raporu `.sdlc/reports/release/<sprint-N>.md` olarak kaydet