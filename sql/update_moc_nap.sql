-- ============================================================
-- CẬP NHẬT QUÀ MỐC NẠP - 02/05/2026
-- Thêm: Đá Bảo Vệ(987), Phiếu GG(459), Capsule(380),
--   Mảnh Vỡ BT(933), Mảnh Vỡ BT C3(1855), Siêu Thần Thủy(727),
--   Hộp SKH TL(1703), Hộp SKH HD(1704), Bí Kíp TK(1229)
-- ============================================================

-- Mốc 1: 20K VNĐ
UPDATE moc_nap SET detail = '[{"temp_id":457,"quantity":2,"options":[{"id":30,"param":1}]},{"temp_id":861,"quantity":500,"options":[]},{"temp_id":987,"quantity":5,"options":[]}]' WHERE id = 1;

-- Mốc 2: 40K VNĐ
UPDATE moc_nap SET detail = '[{"temp_id":457,"quantity":5,"options":[{"id":30,"param":1}]},{"temp_id":861,"quantity":1000,"options":[]},{"temp_id":459,"quantity":3,"options":[]}]' WHERE id = 2;

-- Mốc 3: 60K VNĐ
UPDATE moc_nap SET detail = '[{"temp_id":457,"quantity":8,"options":[{"id":30,"param":1}]},{"temp_id":861,"quantity":2000,"options":[]},{"temp_id":987,"quantity":10,"options":[]},{"temp_id":727,"quantity":5,"options":[]}]' WHERE id = 3;

-- Mốc 4: 100K VNĐ
UPDATE moc_nap SET detail = '[{"temp_id":457,"quantity":12,"options":[{"id":30,"param":1}]},{"temp_id":861,"quantity":3000,"options":[]},{"temp_id":380,"quantity":3,"options":[]},{"temp_id":933,"quantity":500,"options":[]}]' WHERE id = 4;

-- Mốc 5: 140K VNĐ
UPDATE moc_nap SET detail = '[{"temp_id":457,"quantity":18,"options":[{"id":30,"param":1}]},{"temp_id":861,"quantity":5000,"options":[]},{"temp_id":987,"quantity":20,"options":[]},{"temp_id":459,"quantity":5,"options":[]}]' WHERE id = 5;

-- Mốc 6: 200K VNĐ
UPDATE moc_nap SET detail = '[{"temp_id":457,"quantity":25,"options":[{"id":30,"param":1}]},{"temp_id":861,"quantity":8000,"options":[]},{"temp_id":933,"quantity":1000,"options":[]},{"temp_id":1703,"quantity":1,"options":[]}]' WHERE id = 6;

-- Mốc 7: 400K VNĐ
UPDATE moc_nap SET detail = '[{"temp_id":457,"quantity":40,"options":[{"id":30,"param":1}]},{"temp_id":861,"quantity":15000,"options":[]},{"temp_id":987,"quantity":50,"options":[]},{"temp_id":1855,"quantity":500,"options":[]}]' WHERE id = 7;

-- Mốc 8: 800K VNĐ
UPDATE moc_nap SET detail = '[{"temp_id":457,"quantity":60,"options":[{"id":30,"param":1}]},{"temp_id":861,"quantity":25000,"options":[]},{"temp_id":1704,"quantity":1,"options":[]},{"temp_id":987,"quantity":100,"options":[]}]' WHERE id = 8;

-- Mốc 9: 1.2M VNĐ
UPDATE moc_nap SET detail = '[{"temp_id":457,"quantity":80,"options":[{"id":30,"param":1}]},{"temp_id":861,"quantity":30000,"options":[]},{"temp_id":1229,"quantity":2,"options":[]},{"temp_id":1855,"quantity":1000,"options":[]}]' WHERE id = 9;

-- Mốc 10: 1.6M VNĐ
UPDATE moc_nap SET detail = '[{"temp_id":457,"quantity":100,"options":[{"id":30,"param":1}]},{"temp_id":861,"quantity":32000,"options":[]},{"temp_id":1704,"quantity":2,"options":[]},{"temp_id":987,"quantity":200,"options":[]}]' WHERE id = 10;

-- Mốc 11: 2M VNĐ
UPDATE moc_nap SET detail = '[{"temp_id":457,"quantity":130,"options":[{"id":30,"param":1}]},{"temp_id":861,"quantity":32000,"options":[]},{"temp_id":1229,"quantity":3,"options":[]},{"temp_id":1855,"quantity":2000,"options":[]}]' WHERE id = 11;

-- Mốc 12: 2.6M VNĐ
UPDATE moc_nap SET detail = '[{"temp_id":457,"quantity":170,"options":[{"id":30,"param":1}]},{"temp_id":861,"quantity":32000,"options":[]},{"temp_id":1704,"quantity":3,"options":[]},{"temp_id":987,"quantity":500,"options":[]}]' WHERE id = 12;

-- Mốc 13: 4M VNĐ
UPDATE moc_nap SET detail = '[{"temp_id":457,"quantity":250,"options":[{"id":30,"param":1}]},{"temp_id":861,"quantity":32000,"options":[]},{"temp_id":1229,"quantity":5,"options":[]},{"temp_id":1704,"quantity":5,"options":[]}]' WHERE id = 13;

-- Mốc 14: 6M VNĐ
UPDATE moc_nap SET detail = '[{"temp_id":457,"quantity":400,"options":[{"id":30,"param":1}]},{"temp_id":861,"quantity":32000,"options":[]},{"temp_id":1704,"quantity":10,"options":[]},{"temp_id":987,"quantity":1000,"options":[]},{"temp_id":1855,"quantity":5000,"options":[]}]' WHERE id = 14;

-- Verify
SELECT id, detail FROM moc_nap ORDER BY id;
