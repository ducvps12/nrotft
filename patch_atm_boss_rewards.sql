-- Patch cân bằng quà nạp ATM và mốc săn boss cho NROTFT
-- Mục tiêu:
-- 1) Dùng lại luồng quà mốc nạp như thẻ cào nhưng đổi tên/logic sang ATM.
-- 2) Giảm lượng thỏi vàng/hồng ngọc quá lớn để hạn chế lạm phát.
-- 3) Quà săn boss/top boss ưu tiên vật phẩm hỗ trợ farm, không bơm quá nhiều tiền tệ.
--
-- Cách dùng:
-- - Backup DB trước.
-- - Import vào database nrotft.
-- - Restart server hoặc reload cache mốc nạp nếu có nút reload tương ứng.

START TRANSACTION;

-- =========================
-- MỐC NẠP ATM TÍCH LŨY
-- Mốc code hiện tại trong Archivement.java:
-- id 1..14 = 20k,40k,60k,100k,140k,200k,400k,800k,1m2,1m6,2m,2m6,4m,6m
-- =========================

UPDATE moc_nap SET info = 'Moc Nap ATM', detail = '[{"temp_id":457,"quantity":10,"options":[{"id":30,"param":1}]},{"temp_id":861,"quantity":1000,"options":[]}]' WHERE id = 1;
UPDATE moc_nap SET info = 'Moc Nap ATM', detail = '[{"temp_id":457,"quantity":20,"options":[{"id":30,"param":1}]},{"temp_id":861,"quantity":2500,"options":[]}]' WHERE id = 2;
UPDATE moc_nap SET info = 'Moc Nap ATM', detail = '[{"temp_id":457,"quantity":35,"options":[{"id":30,"param":1}]},{"temp_id":861,"quantity":4000,"options":[]},{"temp_id":1440,"quantity":5,"options":[]}]' WHERE id = 3;
UPDATE moc_nap SET info = 'Moc Nap ATM', detail = '[{"temp_id":457,"quantity":60,"options":[{"id":30,"param":1}]},{"temp_id":861,"quantity":7000,"options":[]},{"temp_id":1731,"quantity":2,"options":[]}]' WHERE id = 4;
UPDATE moc_nap SET info = 'Moc Nap ATM', detail = '[{"temp_id":457,"quantity":80,"options":[{"id":30,"param":1}]},{"temp_id":861,"quantity":10000,"options":[]},{"temp_id":1440,"quantity":10,"options":[]}]' WHERE id = 5;
UPDATE moc_nap SET info = 'Moc Nap ATM', detail = '[{"temp_id":457,"quantity":120,"options":[{"id":30,"param":1}]},{"temp_id":861,"quantity":15000,"options":[]},{"temp_id":1560,"quantity":10,"options":[]}]' WHERE id = 6;
UPDATE moc_nap SET info = 'Moc Nap ATM', detail = '[{"temp_id":457,"quantity":200,"options":[{"id":30,"param":1}]},{"temp_id":861,"quantity":25000,"options":[]},{"temp_id":1635,"quantity":2,"options":[{"id":30,"param":1}]}]' WHERE id = 7;
UPDATE moc_nap SET info = 'Moc Nap ATM', detail = '[{"temp_id":457,"quantity":350,"options":[{"id":30,"param":1}]},{"temp_id":861,"quantity":40000,"options":[]},{"temp_id":1703,"quantity":2,"options":[]}]' WHERE id = 8;
UPDATE moc_nap SET info = 'Moc Nap ATM', detail = '[{"temp_id":457,"quantity":500,"options":[{"id":30,"param":1}]},{"temp_id":861,"quantity":60000,"options":[]},{"temp_id":1458,"quantity":1,"options":[{"id":50,"param":10},{"id":77,"param":10},{"id":103,"param":10},{"id":0,"param":800}]}]' WHERE id = 9;
UPDATE moc_nap SET info = 'Moc Nap ATM', detail = '[{"temp_id":457,"quantity":700,"options":[{"id":30,"param":1}]},{"temp_id":861,"quantity":80000,"options":[]},{"temp_id":1628,"quantity":3,"options":[]},{"temp_id":1703,"quantity":3,"options":[]}]' WHERE id = 10;
UPDATE moc_nap SET info = 'Moc Nap ATM', detail = '[{"temp_id":457,"quantity":900,"options":[{"id":30,"param":1}]},{"temp_id":861,"quantity":100000,"options":[]},{"temp_id":1482,"quantity":1,"options":[{"id":50,"param":11},{"id":77,"param":11},{"id":103,"param":11},{"id":205,"param":1}]}]' WHERE id = 11;
UPDATE moc_nap SET info = 'Moc Nap ATM', detail = '[{"temp_id":457,"quantity":1200,"options":[{"id":30,"param":1}]},{"temp_id":861,"quantity":150000,"options":[]},{"temp_id":1632,"quantity":1,"options":[{"id":50,"param":18},{"id":77,"param":18},{"id":103,"param":18},{"id":5,"param":10},{"id":117,"param":3}]}]' WHERE id = 12;
UPDATE moc_nap SET info = 'Moc Nap ATM', detail = '[{"temp_id":457,"quantity":1800,"options":[{"id":30,"param":1}]},{"temp_id":861,"quantity":250000,"options":[]},{"temp_id":1628,"quantity":5,"options":[]},{"temp_id":1694,"quantity":1,"options":[{"id":50,"param":8},{"id":77,"param":8},{"id":103,"param":8},{"id":14,"param":8},{"id":117,"param":3}]}]' WHERE id = 13;
UPDATE moc_nap SET info = 'Moc Nap ATM', detail = '[{"temp_id":457,"quantity":2500,"options":[{"id":30,"param":1}]},{"temp_id":861,"quantity":350000,"options":[]},{"temp_id":1700,"quantity":1,"options":[{"id":50,"param":22},{"id":77,"param":22},{"id":103,"param":22},{"id":5,"param":14},{"id":117,"param":6}]},{"temp_id":1704,"quantity":5,"options":[]}]' WHERE id = 14;

-- =========================
-- ĐUA TOP NẠP ATM
-- Giảm tiền tệ, giữ quà top có giá trị nhưng không phá kinh tế.
-- =========================

UPDATE moc_nap_top SET info = 'Top Nap ATM', detail = '[{"temp_id":457,"quantity":300,"options":[{"id":30,"param":1}]},{"temp_id":861,"quantity":50000,"options":[]},{"temp_id":1704,"quantity":1,"options":[]},{"temp_id":1632,"quantity":1,"options":[{"id":50,"param":28},{"id":77,"param":28},{"id":103,"param":28},{"id":5,"param":12},{"id":14,"param":10},{"id":97,"param":18}]}]' WHERE id = 1;
UPDATE moc_nap_top SET info = 'Top Nap ATM', detail = '[{"temp_id":457,"quantity":220,"options":[{"id":30,"param":1}]},{"temp_id":861,"quantity":35000,"options":[]},{"temp_id":1703,"quantity":2,"options":[]},{"temp_id":1632,"quantity":1,"options":[{"id":50,"param":24},{"id":77,"param":24},{"id":103,"param":24},{"id":5,"param":10},{"id":14,"param":8},{"id":97,"param":15}]}]' WHERE id = 2;
UPDATE moc_nap_top SET info = 'Top Nap ATM', detail = '[{"temp_id":457,"quantity":150,"options":[{"id":30,"param":1}]},{"temp_id":861,"quantity":25000,"options":[]},{"temp_id":1703,"quantity":1,"options":[]},{"temp_id":1628,"quantity":3,"options":[]}]' WHERE id = 3;
UPDATE moc_nap_top SET info = 'Top Nap ATM', detail = '[{"temp_id":457,"quantity":100,"options":[{"id":30,"param":1}]},{"temp_id":861,"quantity":15000,"options":[]},{"temp_id":1628,"quantity":2,"options":[]}]' WHERE id = 4;
UPDATE moc_nap_top SET info = 'Top Nap ATM', detail = '[{"temp_id":457,"quantity":70,"options":[{"id":30,"param":1}]},{"temp_id":861,"quantity":10000,"options":[]}]' WHERE id BETWEEN 5 AND 10;

-- =========================
-- MỐC SĂN BOSS
-- Cân bằng lại: thưởng theo công sức săn boss, không quá nhiều thỏi vàng.
-- =========================

UPDATE moc_san_boss SET info = 'Moc San Boss', detail = '[{"temp_id":457,"quantity":10,"options":[{"id":30,"param":1}]},{"temp_id":1440,"quantity":5,"options":[]}]' WHERE id = 1;
UPDATE moc_san_boss SET info = 'Moc San Boss', detail = '[{"temp_id":457,"quantity":20,"options":[{"id":30,"param":1}]},{"temp_id":861,"quantity":3000,"options":[]},{"temp_id":1440,"quantity":8,"options":[]}]' WHERE id = 2;
UPDATE moc_san_boss SET info = 'Moc San Boss', detail = '[{"temp_id":457,"quantity":35,"options":[{"id":30,"param":1}]},{"temp_id":861,"quantity":5000,"options":[]},{"temp_id":1731,"quantity":2,"options":[]}]' WHERE id = 3;
UPDATE moc_san_boss SET info = 'Moc San Boss', detail = '[{"temp_id":457,"quantity":50,"options":[{"id":30,"param":1}]},{"temp_id":1560,"quantity":5,"options":[]}]' WHERE id = 4;
UPDATE moc_san_boss SET info = 'Moc San Boss', detail = '[{"temp_id":457,"quantity":70,"options":[{"id":30,"param":1}]},{"temp_id":861,"quantity":8000,"options":[]},{"temp_id":1440,"quantity":12,"options":[]}]' WHERE id = 5;
UPDATE moc_san_boss SET info = 'Moc San Boss', detail = '[{"temp_id":457,"quantity":100,"options":[{"id":30,"param":1}]},{"temp_id":861,"quantity":12000,"options":[]},{"temp_id":1560,"quantity":8,"options":[]},{"temp_id":1703,"quantity":1,"options":[]}]' WHERE id = 6;
UPDATE moc_san_boss SET info = 'Moc San Boss', detail = '[{"temp_id":457,"quantity":140,"options":[{"id":30,"param":1}]},{"temp_id":1560,"quantity":10,"options":[]},{"temp_id":1635,"quantity":2,"options":[{"id":30,"param":1}]}]' WHERE id = 7;
UPDATE moc_san_boss SET info = 'Moc San Boss', detail = '[{"temp_id":457,"quantity":180,"options":[{"id":30,"param":1}]},{"temp_id":861,"quantity":20000,"options":[]},{"temp_id":1453,"quantity":3,"options":[]}]' WHERE id = 8;
UPDATE moc_san_boss SET info = 'Moc San Boss', detail = '[{"temp_id":457,"quantity":230,"options":[{"id":30,"param":1}]},{"temp_id":1703,"quantity":2,"options":[]}]' WHERE id = 9;
UPDATE moc_san_boss SET info = 'Moc San Boss', detail = '[{"temp_id":457,"quantity":300,"options":[{"id":30,"param":1}]},{"temp_id":861,"quantity":35000,"options":[]},{"temp_id":1560,"quantity":15,"options":[]},{"temp_id":1628,"quantity":3,"options":[]}]' WHERE id = 10;

COMMIT;
