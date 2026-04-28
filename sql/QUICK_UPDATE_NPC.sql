-- Quick Update NPC Bang Danh Vong
-- Copy va paste vao phpMyAdmin hoac MySQL Workbench

UPDATE `npc_template` 
SET `NAME` = 'Bảng Danh Vọng', 
    `head` = 1173, 
    `body` = 1174, 
    `leg` = 1175, 
    `avatar` = 9493 
WHERE `id` = 106;

-- Sau khi chay xong, restart server de load lai NPC template
