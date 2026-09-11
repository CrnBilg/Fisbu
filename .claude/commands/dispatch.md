---
description: Birden fazla story'yi PDM'in paralellik kararına göre başlatır — write-set ve sıcak dosya kontrolü yapılır.
argument-hint: [story-id listesi] (opsiyonel, boş bırakılırsa PDM tüm Ready story'leri değerlendirir)
---
$ARGUMENTS ile belirtilen story'leri (veya boşsa tüm Ready story'leri) değerlendir:
1. Her story'nin write-set'ini incele
2. Write-set'ler ve sıcak dosyalar açısından çakışma var mı kontrol et
3. Çakışmayan story'leri paralel, çakışanları sıralı olarak başlat
4. Kararı ve gerekçesini kullanıcıya bildir