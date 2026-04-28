-- ================================================================
-- REBALANCE SANTA SHOP (tab 18=Cửa hàng, tab 19=Cải trang)
-- Tăng giá ngọc cho phù hợp 100k ngọc economy
-- UPDATE từ giá LỚN → NHỎ để tránh cascade
-- ================================================================

-- TAB 18: Cửa hàng Santa
UPDATE tab_shop SET items = REPLACE(items, '"cost":299,', '"cost":3000,') WHERE id = 18;
UPDATE tab_shop SET items = REPLACE(items, '"cost":99,', '"cost":1000,') WHERE id = 18;

-- TAB 19: Cải trang (ngọc type_sell=1)
UPDATE tab_shop SET items = REPLACE(items, '"cost":500,', '"cost":5000,') WHERE id = 19;
UPDATE tab_shop SET items = REPLACE(items, '"cost":200,', '"cost":2000,') WHERE id = 19;
UPDATE tab_shop SET items = REPLACE(items, '"cost":150,', '"cost":1500,') WHERE id = 19;
UPDATE tab_shop SET items = REPLACE(items, '"cost":100,', '"cost":1000,') WHERE id = 19;
UPDATE tab_shop SET items = REPLACE(items, '"cost":99,', '"cost":1000,') WHERE id = 19;
UPDATE tab_shop SET items = REPLACE(items, '"cost":10,', '"cost":100,') WHERE id = 19;

SELECT 'DONE - Santa shop rebalanced!' as status;
