-- Đồng bộ NPC Bảng Tin Sự Kiện & Danh Vọng vào 3 làng chính và khu luyện tập.
-- Chạy script này nếu DB chưa có NPC id 106 hoặc map chưa hiển thị bảng tin.

INSERT INTO npc_template (id, NAME, head, body, leg, avatar)
VALUES (106, 'Bảng Tin Sự Kiện', 64, 0, 6, 64)
ON DUPLICATE KEY UPDATE
    NAME = VALUES(NAME),
    head = VALUES(head),
    body = VALUES(body),
    leg = VALUES(leg),
    avatar = VALUES(avatar);

-- Làng Trái Đất/Kakarot, Làng Mori, Làng Aru và khu luyện tập.
-- map_npcs dùng dạng [[npcId,x,y]]. Điều kiện LIKE giúp không thêm trùng NPC 106.
UPDATE map_template
SET map_npcs = CASE
    WHEN map_npcs IS NULL OR map_npcs = '' OR map_npcs = '[]' THEN '[[106,760,336]]'
    ELSE REPLACE(map_npcs, ']', ',[106,760,336]]')
END
WHERE id IN (0, 7, 14, 191, 192, 193)
  AND (map_npcs IS NULL OR map_npcs NOT LIKE '%[106,%');
