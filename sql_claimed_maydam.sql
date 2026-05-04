-- Fix NPC Phở Anh Hai: Tên NPC bị trống trong DB
UPDATE npc_template SET name = 'Phở Anh Hai' WHERE id = 87;

-- Thêm column claimed_maydam (nếu chưa có)
ALTER TABLE player ADD COLUMN IF NOT EXISTS claimed_maydam INT DEFAULT 0 AFTER total_damage_maydam;

-- Thêm 2 viên NRO 1 sao cho player "hades"
-- Item ID 16 = Ngọc Rồng 1 Sao
INSERT INTO item_mails_box_queue (player_name, item_id, quantity, note)
SELECT 'hades', 16, 2, 'Admin gift - 2 viên NRO 1 sao'
WHERE EXISTS (SELECT 1 FROM player WHERE name = 'hades');
