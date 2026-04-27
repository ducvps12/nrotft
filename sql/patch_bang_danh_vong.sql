-- Thêm NPC Bảng Danh Vọng/Cẩm Nang Tân Thủ vào Khu Vực Luyện Tập (map 191/192/193)
-- Chạy script này trên database nrotft nếu server không dùng auto updater.

INSERT INTO npc_template (id, NAME, head, body, leg, avatar)
VALUES (106, 'Bảng Danh Vọng', 64, 0, 6, 64)
ON DUPLICATE KEY UPDATE
    NAME = VALUES(NAME),
    head = VALUES(head),
    body = VALUES(body),
    leg = VALUES(leg),
    avatar = VALUES(avatar);

-- Theo cấu trúc map_template hiện tại, NPC tĩnh nằm ở cột map_npcs dạng [[npcId,x,y]].
-- Đặt bảng gần cụm Mr.PôPô để người mới vào khu luyện tập nhìn thấy ngay.
UPDATE map_template
SET map_npcs = '[[106,760,336]]'
WHERE id IN (191, 192, 193)
  AND (map_npcs IS NULL OR map_npcs = '[]' OR map_npcs NOT LIKE '%[106,%');
