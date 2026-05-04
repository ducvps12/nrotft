-- Thêm NPC Hắc Mị Nương (ID 64) vào bảng npc_template
-- Sử dụng head/body/leg từ cải trang CT Hắc Mị Nương (item 1557, icon 12901)
-- Avatar dùng iconID 12901 từ item template
INSERT INTO npc_template (id, name, head, body, leg, avatar)
VALUES (64, 'Hắc Mị Nương', 900, 901, 902, 12901)
ON DUPLICATE KEY UPDATE name = 'Hắc Mị Nương', avatar = 12901;
