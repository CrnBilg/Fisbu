---
description: Board'a bakarak günlük durum raporu üretir — dün tamamlananlar, bugün yapılacaklar, varsa engeller.
---
Günlük durum raporu üret:
1. `.sdlc/sprints/sprint-<N>/board.md`'ye bak — hangi story hangi durumda
2. Dün "Done" olan story'leri listele
3. Bugün "In Progress" olan story'leri listele
4. Varsa blocker'ları (3 kere geri dönen story'ler gibi) belirt
5. Raporu `.sdlc/reports/standup/<tarih>.md` olarak kaydet