---
name: testing-strategy
description: Dört test katmanının (unit/integration/component/E2E) amacını ve kullanım koşullarını tanımlar; qa ve fullstack-dev tarafından kullanılır.
---

## Unit Test
İş mantığı içeren her fonksiyon için yazılır. Dışarıdaki hiçbir sisteme (veritabanı, ağ) bağlı olmadan, sadece fonksiyonun kendi mantığını kontrol eder.

## Integration Test
API + veritabanı etkileşimi içeren akışlar için yazılır. Testler, in-memory (sahte) veritabanı değil, gerçek PostgreSQL'e karşı çalışır — ama production veritabanına değil, her test için oluşturulan geçici/izole bir veritabanına (Testcontainers ile).

## Component Test
Kullanıcı arayüzü bileşenleri için yazılır. Loading, error, empty, success durumlarının her biri ayrı ayrı test edilir.

## E2E Test
Sadece kritik kullanıcı akışları için yazılır (giriş, ana iş akışı gibi). Her story için E2E yazılmaz — "bu akış gerçekten kritik mi" sorusu sorulup karar verilir.