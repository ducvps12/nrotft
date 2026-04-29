-- ====================================================
-- CẬP NHẬT PHẦN THƯỞNG THÀNH TÍCH (HỒNG NGỌC)
-- Tăng phần thưởng hấp dẫn hơn cho người chơi
-- Chạy SQL này trên database nrotft
-- ====================================================

UPDATE achievement_template SET money = 50   WHERE id = 1;   -- Gia nhập Vệ Binh (cũ: 1)
UPDATE achievement_template SET money = 200  WHERE id = 2;   -- Sức mạnh siêu cấp (cũ: 50)
UPDATE achievement_template SET money = 100  WHERE id = 3;   -- Nông dân chăm chỉ (cũ: 20)
UPDATE achievement_template SET money = 150  WHERE id = 4;   -- Trăm trận trăm thắng (cũ: 20)
UPDATE achievement_template SET money = 80   WHERE id = 5;   -- Nội công cao cường (cũ: 10)
UPDATE achievement_template SET money = 80   WHERE id = 6;   -- Khinh công thành thạo (cũ: 10)
UPDATE achievement_template SET money = 80   WHERE id = 7;   -- Thợ săn thiện xạ (cũ: 10)
UPDATE achievement_template SET money = 80   WHERE id = 8;   -- Tập luyện bài bản (cũ: 10)
UPDATE achievement_template SET money = 100  WHERE id = 9;   -- Hoạt động chăm chỉ (cũ: 20)
UPDATE achievement_template SET money = 200  WHERE id = 10;  -- Hỗ trợ đồng đội (cũ: 50)
UPDATE achievement_template SET money = 80   WHERE id = 11;  -- Trùm nhặt ve chai (cũ: 10)
UPDATE achievement_template SET money = 200  WHERE id = 12;  -- Lần đầu nạp ngọc (cũ: 50)
UPDATE achievement_template SET money = 80   WHERE id = 13;  -- Đánh bại siêu quái (cũ: 10)
UPDATE achievement_template SET money = 150  WHERE id = 14;  -- Thánh hồi sinh (cũ: 50)
UPDATE achievement_template SET money = 100  WHERE id = 15;  -- Kỹ năng thành thạo (cũ: 20)
UPDATE achievement_template SET money = 100  WHERE id = 16;  -- Trùm nhặt ngọc (cũ: 20)
UPDATE achievement_template SET money = 500  WHERE id = 17;  -- Đạt 15 triệu sức mạnh (cũ: 100)
UPDATE achievement_template SET money = 150  WHERE id = 18;  -- Tuyệt kỹ thành thạo (cũ: 50)
UPDATE achievement_template SET money = 100  WHERE id = 19;  -- Chăm sóc đặc biệt (cũ: 15)
UPDATE achievement_template SET money = 200  WHERE id = 20;  -- Trùm kết liễu Boss (cũ: 20)
