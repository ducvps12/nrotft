-- Tạo GiftCode Tân Thủ cho Open Beta
-- Code: OPENBETA2026 - mỗi tài khoản dùng 1 lần, giới hạn 9999 lượt
-- Phần thưởng: Thỏi Vàng x200, Hồng Ngọc x500, Đá Bảo Vệ x300, Ngọc Rồng 1★ x1
INSERT INTO giftcode (code, count_left, detail, datecreate, expired, type) 
VALUES (
    'OPENBETA2026', 
    9999, 
    '[{"temp_id":457,"quantity":200,"options":[{"id":30,"param":0}]},{"temp_id":861,"quantity":500000,"options":[]},{"temp_id":987,"quantity":300000000,"options":[]},{"temp_id":568,"quantity":1,"options":[]}]',
    NOW(), 
    '2026-04-20 23:59:59', 
    0
);

-- Kiểm tra kết quả
SELECT id, code, count_left, expired, type FROM giftcode WHERE code = 'OPENBETA2026';
