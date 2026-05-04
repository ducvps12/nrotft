-- Tìm item hộp quà tháng 9 và tháng 9 VIP
SELECT id, NAME, TYPE, gold, gem FROM item_template WHERE NAME LIKE '%tháng 9%' OR NAME LIKE '%thang 9%';
