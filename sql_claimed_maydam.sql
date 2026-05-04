-- ============================================
-- CHẠY NGAY ĐỂ FIX LỖI SERVER CRASH
-- ============================================

-- Thêm cột claimed_maydam vào bảng player
ALTER TABLE player ADD COLUMN claimed_maydam INT DEFAULT 0;

-- Fix NPC Phở Anh Hai tên trống/lỗi unicode
UPDATE npc_template SET name = 'Pho Anh Hai' WHERE id = 87;

-- Sau khi chạy xong, restart server
