-- Fix stacking cho các mảnh sưu tầm
-- Đảm bảo is_up_to_up = 1 để item có thể gộp quantity khi nhặt/nhận
UPDATE item_template SET is_up_to_up = 1 WHERE id IN (
    1204,  -- Mảnh Rồng thần Namếc
    1208,  -- Mảnh Rồng thần Namếc (variant)
    1901,  -- Mảnh Khí Oozaru
    956,   -- Mảnh Đội trưởng Vàng
    1173,  -- Mảnh Quỷ
    1855,  -- Mảnh vỡ Bông tai cấp 3
    1847,  -- Mảnh Đinh Ba
    1848   -- Mảnh Cung Tên
);

-- Kiểm tra kết quả
SELECT id, name, is_up_to_up FROM item_template WHERE id IN (1204, 1208, 1901, 956, 1173, 1855, 1847, 1848);
