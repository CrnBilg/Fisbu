-- Issue #46: Kullanıcı kategorilerinde tekrar (duplicate) kayıtlar var
--
-- Bu script, aynı kullanıcı için aynı isimde birden fazla kategori kaydı olan
-- durumları tekilleştirir. Her (user_id, name) grubunda en düşük id'ye sahip
-- kategori "kanonik" kabul edilir; diğer (duplicate) kategorilere bağlı
-- receipts/budgets kayıtları kanonik kategoriye taşınır, sonra duplicate
-- kategori satırları silinir.
--
-- İDEMPOTENT: tekrar çalıştırıldığında (artık duplicate kalmadığı için)
-- hiçbir şey değiştirmez.
--
-- KULLANIM: Önce SADECE 1. adımı (SELECT) çalıştırıp etkilenecek kullanıcı/
-- kategori/fiş/bütçe sayısını gözden geçirin. Sonuçlar beklendiği gibiyse
-- 2-4. adımları TEK BİR TRANSACTION içinde (aşağıdaki BEGIN/COMMIT ile)
-- çalıştırın. Bu backend'deki unique(user_id, name) constraint migration'ından
-- (Category.java) ÖNCE çalıştırılmalı, aksi halde o migration duplicate
-- satırlar yüzünden başarısız olur.

-- =====================================================================
-- 1. ÖNİZLEME — hiçbir şeyi değiştirmez, sadece etkilenecek kayıtları gösterir
-- =====================================================================
WITH duplicate_groups AS (
    SELECT user_id, name, MIN(id) AS canonical_id, ARRAY_AGG(id ORDER BY id) AS all_ids
    FROM categories
    GROUP BY user_id, name
    HAVING COUNT(*) > 1
)
SELECT
    dg.user_id,
    dg.name,
    dg.canonical_id,
    dg.all_ids,
    (SELECT COUNT(*) FROM receipts r WHERE r.category_id = ANY(dg.all_ids) AND r.category_id <> dg.canonical_id) AS affected_receipts,
    (SELECT COUNT(*) FROM budgets b WHERE b.category_id = ANY(dg.all_ids) AND b.category_id <> dg.canonical_id) AS affected_budgets
FROM duplicate_groups dg
ORDER BY dg.user_id, dg.name;

-- =====================================================================
-- Aşağıdaki gövdeyi (BEGIN...COMMIT) SADECE yukarıdaki önizleme gözden
-- geçirildikten sonra çalıştırın.
-- =====================================================================

-- BEGIN;

-- 2. Fişleri (receipts) duplicate kategoriden kanonik kategoriye taşı
WITH duplicate_groups AS (
    SELECT user_id, name, MIN(id) AS canonical_id, ARRAY_AGG(id) AS all_ids
    FROM categories
    GROUP BY user_id, name
    HAVING COUNT(*) > 1
)
UPDATE receipts r
SET category_id = dg.canonical_id
FROM duplicate_groups dg
WHERE r.category_id = ANY(dg.all_ids)
  AND r.category_id <> dg.canonical_id;

-- 3. Bütçeleri (budgets) taşı — ama kanonik kategoride aynı (year, month) için
-- zaten bir bütçe varsa unique(user_id, category_id, year, month) constraint'i
-- ihlal edilir; bu çakışan duplicate bütçe satırlarını taşımak yerine sil
-- (kanonik olan zaten doğru veriyi taşıyor kabul ediliyor).
WITH duplicate_groups AS (
    SELECT user_id, name, MIN(id) AS canonical_id, ARRAY_AGG(id) AS all_ids
    FROM categories
    GROUP BY user_id, name
    HAVING COUNT(*) > 1
),
conflicting AS (
    SELECT b.id
    FROM budgets b
    JOIN duplicate_groups dg ON b.category_id = ANY(dg.all_ids) AND b.category_id <> dg.canonical_id
    WHERE EXISTS (
        SELECT 1 FROM budgets canon
        WHERE canon.user_id = b.user_id
          AND canon.category_id = dg.canonical_id
          AND canon.year = b.year
          AND canon.month = b.month
    )
)
DELETE FROM budgets WHERE id IN (SELECT id FROM conflicting);

WITH duplicate_groups AS (
    SELECT user_id, name, MIN(id) AS canonical_id, ARRAY_AGG(id) AS all_ids
    FROM categories
    GROUP BY user_id, name
    HAVING COUNT(*) > 1
)
UPDATE budgets b
SET category_id = dg.canonical_id
FROM duplicate_groups dg
WHERE b.category_id = ANY(dg.all_ids)
  AND b.category_id <> dg.canonical_id;

-- 4. Artık hiçbir fiş/bütçe referans etmeyen duplicate kategori satırlarını sil
WITH duplicate_groups AS (
    SELECT user_id, name, MIN(id) AS canonical_id, ARRAY_AGG(id) AS all_ids
    FROM categories
    GROUP BY user_id, name
    HAVING COUNT(*) > 1
)
DELETE FROM categories c
USING duplicate_groups dg
WHERE c.id = ANY(dg.all_ids)
  AND c.id <> dg.canonical_id;

-- COMMIT;
-- Hata olursa ROLLBACK; yapıp sonucu inceleyin.
