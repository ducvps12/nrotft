-- ================================================================
-- REBALANCE URON SHOP (shop_id = 4, tab_id = 10,11,12,13)
-- Game tặng 2 tỉ vàng + 100k ngọc xanh
-- ⚠ REPLACE từ GIÁ LỚN → NHỎ để tránh cascade
-- ================================================================

-- ==============================
-- TAB 10: SÁCH VÕ (Galick, Dragon, etc.)
-- ==============================
UPDATE tab_shop SET items = REPLACE(items, '"cost":220,', '"cost":20000,') WHERE id = 10;
UPDATE tab_shop SET items = REPLACE(items, '"cost":200,', '"cost":18000,') WHERE id = 10;
UPDATE tab_shop SET items = REPLACE(items, '"cost":180,', '"cost":16000,') WHERE id = 10;
UPDATE tab_shop SET items = REPLACE(items, '"cost":160,', '"cost":14000,') WHERE id = 10;
UPDATE tab_shop SET items = REPLACE(items, '"cost":140,', '"cost":12000,') WHERE id = 10;
UPDATE tab_shop SET items = REPLACE(items, '"cost":120,', '"cost":10000,') WHERE id = 10;
UPDATE tab_shop SET items = REPLACE(items, '"cost":100,', '"cost":8000,') WHERE id = 10;
UPDATE tab_shop SET items = REPLACE(items, '"cost":90,', '"cost":7000,') WHERE id = 10;
UPDATE tab_shop SET items = REPLACE(items, '"cost":80,', '"cost":6000,') WHERE id = 10;
UPDATE tab_shop SET items = REPLACE(items, '"cost":70,', '"cost":5000,') WHERE id = 10;
UPDATE tab_shop SET items = REPLACE(items, '"cost":60,', '"cost":4000,') WHERE id = 10;
UPDATE tab_shop SET items = REPLACE(items, '"cost":50,', '"cost":3000,') WHERE id = 10;
UPDATE tab_shop SET items = REPLACE(items, '"cost":40,', '"cost":2500,') WHERE id = 10;
UPDATE tab_shop SET items = REPLACE(items, '"cost":35,', '"cost":2000,') WHERE id = 10;
UPDATE tab_shop SET items = REPLACE(items, '"cost":30,', '"cost":1500,') WHERE id = 10;
UPDATE tab_shop SET items = REPLACE(items, '"cost":25,', '"cost":1200,') WHERE id = 10;
UPDATE tab_shop SET items = REPLACE(items, '"cost":20,', '"cost":800,') WHERE id = 10;
UPDATE tab_shop SET items = REPLACE(items, '"cost":15,', '"cost":500,') WHERE id = 10;
UPDATE tab_shop SET items = REPLACE(items, '"cost":10,', '"cost":300,') WHERE id = 10;
UPDATE tab_shop SET items = REPLACE(items, '"cost":5,', '"cost":100,') WHERE id = 10;

-- ==============================
-- TAB 11: SÁCH CHƯỞNG
-- ==============================
UPDATE tab_shop SET items = REPLACE(items, '"cost":220,', '"cost":20000,') WHERE id = 11;
UPDATE tab_shop SET items = REPLACE(items, '"cost":200,', '"cost":18000,') WHERE id = 11;
UPDATE tab_shop SET items = REPLACE(items, '"cost":180,', '"cost":16000,') WHERE id = 11;
UPDATE tab_shop SET items = REPLACE(items, '"cost":160,', '"cost":14000,') WHERE id = 11;
UPDATE tab_shop SET items = REPLACE(items, '"cost":140,', '"cost":12000,') WHERE id = 11;
UPDATE tab_shop SET items = REPLACE(items, '"cost":120,', '"cost":10000,') WHERE id = 11;
UPDATE tab_shop SET items = REPLACE(items, '"cost":100,', '"cost":8000,') WHERE id = 11;
UPDATE tab_shop SET items = REPLACE(items, '"cost":70,', '"cost":5000,') WHERE id = 11;
UPDATE tab_shop SET items = REPLACE(items, '"cost":60,', '"cost":4000,') WHERE id = 11;
UPDATE tab_shop SET items = REPLACE(items, '"cost":50,', '"cost":3000,') WHERE id = 11;
UPDATE tab_shop SET items = REPLACE(items, '"cost":40,', '"cost":2500,') WHERE id = 11;
UPDATE tab_shop SET items = REPLACE(items, '"cost":30,', '"cost":1500,') WHERE id = 11;
UPDATE tab_shop SET items = REPLACE(items, '"cost":20,', '"cost":800,') WHERE id = 11;
UPDATE tab_shop SET items = REPLACE(items, '"cost":10,', '"cost":300,') WHERE id = 11;

-- ==============================
-- TAB 12: SÁCH ĐẶC BIỆT
-- ==============================
UPDATE tab_shop SET items = REPLACE(items, '"cost":220,', '"cost":20000,') WHERE id = 12;
UPDATE tab_shop SET items = REPLACE(items, '"cost":200,', '"cost":18000,') WHERE id = 12;
UPDATE tab_shop SET items = REPLACE(items, '"cost":180,', '"cost":16000,') WHERE id = 12;
UPDATE tab_shop SET items = REPLACE(items, '"cost":160,', '"cost":14000,') WHERE id = 12;
UPDATE tab_shop SET items = REPLACE(items, '"cost":140,', '"cost":12000,') WHERE id = 12;
UPDATE tab_shop SET items = REPLACE(items, '"cost":120,', '"cost":10000,') WHERE id = 12;
UPDATE tab_shop SET items = REPLACE(items, '"cost":100,', '"cost":8000,') WHERE id = 12;
UPDATE tab_shop SET items = REPLACE(items, '"cost":90,', '"cost":7000,') WHERE id = 12;
UPDATE tab_shop SET items = REPLACE(items, '"cost":80,', '"cost":6000,') WHERE id = 12;
UPDATE tab_shop SET items = REPLACE(items, '"cost":70,', '"cost":5000,') WHERE id = 12;
UPDATE tab_shop SET items = REPLACE(items, '"cost":60,', '"cost":4000,') WHERE id = 12;
UPDATE tab_shop SET items = REPLACE(items, '"cost":50,', '"cost":3000,') WHERE id = 12;
UPDATE tab_shop SET items = REPLACE(items, '"cost":40,', '"cost":2500,') WHERE id = 12;
UPDATE tab_shop SET items = REPLACE(items, '"cost":30,', '"cost":1500,') WHERE id = 12;

-- ==============================
-- TAB 13: PHỤ KIỆN
-- ==============================
UPDATE tab_shop SET items = REPLACE(items, '"cost":1500,', '"cost":15000,') WHERE id = 13;
UPDATE tab_shop SET items = REPLACE(items, '"cost":1000,', '"cost":10000,') WHERE id = 13;
UPDATE tab_shop SET items = REPLACE(items, '"cost":999,', '"cost":10000,') WHERE id = 13;
UPDATE tab_shop SET items = REPLACE(items, '"cost":599,', '"cost":6000,') WHERE id = 13;
UPDATE tab_shop SET items = REPLACE(items, '"cost":500,', '"cost":5000,') WHERE id = 13;
UPDATE tab_shop SET items = REPLACE(items, '"cost":499,', '"cost":5000,') WHERE id = 13;
UPDATE tab_shop SET items = REPLACE(items, '"cost":299,', '"cost":3000,') WHERE id = 13;
UPDATE tab_shop SET items = REPLACE(items, '"cost":199,', '"cost":2000,') WHERE id = 13;
UPDATE tab_shop SET items = REPLACE(items, '"cost":99,', '"cost":1000,') WHERE id = 13;
UPDATE tab_shop SET items = REPLACE(items, '"cost":70,', '"cost":700,') WHERE id = 13;
UPDATE tab_shop SET items = REPLACE(items, '"cost":50,', '"cost":500,') WHERE id = 13;
UPDATE tab_shop SET items = REPLACE(items, '"cost":10,', '"cost":100,') WHERE id = 13;
UPDATE tab_shop SET items = REPLACE(items, '"cost":5,', '"cost":50,') WHERE id = 13;
UPDATE tab_shop SET items = REPLACE(items, '"cost":1,', '"cost":10,') WHERE id = 13;

SELECT 'DONE - Uron shop rebalanced!' as status;
