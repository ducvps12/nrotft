-- Patch: Fix NPC Bảng Danh Vọng template
-- Ngày: 2026-04-28
-- Mô tả: Cập nhật head, body, leg cho NPC Bảng Danh Vọng (ID 106)

-- Sử dụng head/body/leg của NPC Sự Kiện (ID 73) vì phù hợp với NPC thông tin
UPDATE `npc_template` 
SET `NAME` = 'Bảng Danh Vọng', 
    `head` = 1173, 
    `body` = 1174, 
    `leg` = 1175, 
    `avatar` = 9493 
WHERE `id` = 106;

-- Lưu ý: 
-- Head: 1173, Body: 1174, Leg: 1175, Avatar: 9493
-- Đây là bộ trang phục của NPC Sự Kiện, phù hợp cho NPC bảng tin/thông báo
