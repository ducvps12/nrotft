-- Sửa giá Hộp quà tháng 9: 1 tỉ, Hộp quà tháng 9 VIP: 2 tỉ
UPDATE tab_shop SET items = '[
 {"cost": 1000000000, "type_sell": 0, "is_new": true, "temp_id": 1695, "item_spec": 1, "options": [{"param": 1, "id": 30},{"param": 30, "id": 93}], "is_sell": true},
 {"cost": 2000000000, "type_sell": 0, "is_new": true, "temp_id": 1696, "item_spec": 1, "options": [{"param": 1, "id": 30},{"param": 30, "id": 93}], "is_sell": true},
 {"cost": 2000000000, "type_sell": 0, "is_new": true, "temp_id": 1631, "item_spec": 1, "options": [{"param": 7, "id": 50},{"param": 8, "id": 77},{"param": 8, "id": 103},{"param": 10, "id": 236},{"param": 10, "id": 101},{"param": 1, "id": 30}], "is_sell": true},
 {"cost": 200000000, "type_sell": 0, "is_new": true, "temp_id": 1874, "item_spec": 1, "options": [{"param": 1, "id": 30},{"param": 30, "id": 93}], "is_sell": true},
 {"cost": 200000000, "type_sell": 0, "is_new": true, "temp_id": 1873, "item_spec": 1, "options": [{"param": 1, "id": 30},{"param": 30, "id": 93}], "is_sell": true},
 {"cost": 500000000, "type_sell": 0, "is_new": true, "temp_id": 1591, "item_spec": 1, "options": [{"param": 1, "id": 30},{"param": 30, "id": 93}], "is_sell": true},
 {"cost": 500000000, "type_sell": 0, "is_new": true, "temp_id": 1758, "item_spec": 1, "options": [{"param": 1, "id": 30},{"param": 30, "id": 93}], "is_sell": true},
 {"cost": 200000000, "type_sell": 0, "is_new": true, "temp_id": 1666, "item_spec": 1, "options": [{"param": 1, "id": 30},{"param": 2, "id": 93}], "is_sell": true},
 {"cost": 200000000, "type_sell": 0, "is_new": true, "temp_id": 1665, "item_spec": 1, "options": [{"param": 1, "id": 30},{"param": 30, "id": 93}], "is_sell": true}
]
' WHERE id = 54;
