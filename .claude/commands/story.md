---
description: Ready durumdaki bir story'yi alır, geliştirme (fullstack-dev → qa → reviewer → devops) sürecinden geçirir.
argument-hint: <story-id>
---

$ARGUMENTS ile belirtilen story'yi geliştirme sürecinden geçir:
1. `fullstack-dev`'i çağır — story dosyasındaki write-set'e göre kodu yazsın
2. `qa`'yı çağır — testleri yazsın ve çalıştırsın
3. `reviewer`'ı çağır — kodu incelesin
4. Gerekirse `devops`'u çağır — migration/config/deployment etkisi varsa
5. Quality gate kontrolü yap — DoD'nin tamamı karşılandıysa story'yi Done işaretle