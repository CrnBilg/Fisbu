---
description: Architect'i çağırıp belirtilen konuda mimari karar verdirir, ADR olarak kaydeder.
argument-hint: <karar konusu>
---
$ARGUMENTS ile belirtilen konuda mimari karar sürecini başlat:
1. `architect`'i çağır — konuyu değerlendirsin, en az 2 seçenek karşılaştırsın
2. Kararı `.sdlc/adr/<numara>-<kisa-baslik>.md` olarak kaydet
3. Kararın kısa özetini `.sdlc/reports/architecture/` altına düşür