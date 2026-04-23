-- =============================================
-- Event Ranking System - SQL Migration
-- =============================================

-- 1. Thêm cột diem_su_kien vào bảng player
ALTER TABLE player ADD COLUMN IF NOT EXISTS diem_su_kien INT DEFAULT 0;

-- 2. Tạo bảng moc_su_kien_top (phần thưởng top sự kiện)
CREATE TABLE IF NOT EXISTS moc_su_kien_top (
    id INT PRIMARY KEY,
    detail TEXT NOT NULL DEFAULT '[]'
);

-- 3. Thêm dữ liệu mẫu cho 10 top (có thể chỉnh sửa từ panel web)
INSERT IGNORE INTO moc_su_kien_top (id, detail) VALUES
(1, '[{"temp_id":457,"quantity":99,"options":[]},{"temp_id":674,"quantity":50,"options":[]},{"temp_id":1592,"quantity":20,"options":[]}]'),
(2, '[{"temp_id":457,"quantity":80,"options":[]},{"temp_id":674,"quantity":40,"options":[]},{"temp_id":1592,"quantity":15,"options":[]}]'),
(3, '[{"temp_id":457,"quantity":60,"options":[]},{"temp_id":674,"quantity":30,"options":[]},{"temp_id":1592,"quantity":10,"options":[]}]'),
(4, '[{"temp_id":457,"quantity":40,"options":[]},{"temp_id":674,"quantity":20,"options":[]}]'),
(5, '[{"temp_id":457,"quantity":30,"options":[]},{"temp_id":674,"quantity":15,"options":[]}]'),
(6, '[{"temp_id":457,"quantity":20,"options":[]},{"temp_id":674,"quantity":10,"options":[]}]'),
(7, '[{"temp_id":457,"quantity":15,"options":[]},{"temp_id":674,"quantity":8,"options":[]}]'),
(8, '[{"temp_id":457,"quantity":10,"options":[]},{"temp_id":674,"quantity":5,"options":[]}]'),
(9, '[{"temp_id":457,"quantity":8,"options":[]},{"temp_id":674,"quantity":3,"options":[]}]'),
(10, '[{"temp_id":457,"quantity":5,"options":[]},{"temp_id":674,"quantity":2,"options":[]}]');
