-- ============================================
-- CẬP NHẬT SHOP OSIN (tab_shop id=47, shop_id=32)
-- ============================================
-- Thay đổi:
-- 1. Hộp SKH Hủy Diệt (1704): 100000 → 1500
-- 2. Xu NRO (1705): 1000 → 1
-- 3. Cải trang Gohan Beast (1748): 10Tr → 100k + HP 85%, KI 85%, SD 30%, khóa
-- 4. Cải trang Gogeta SSJ Blue (1859): 10Tr → 100k + HP 85%, KI 85%, SD 30%, khóa
-- 5. Cải trang Vegito SSJ Blue (1860): 1000 → 10000 + HP 85%, KI 85%, SD 30%, khóa
-- 6. Cải trang Vegito God (2022): 1Tr → 10000 + HP 85%, KI 85%, SD 30%, khóa
-- ============================================

UPDATE tab_shop SET items = '[
{"cost":5,"type_sell":3,"is_new":true,"temp_id":992,"item_spec":674,"options":[{"param":1,"id":30}],"is_sell":true},
{"cost":5,"type_sell":3,"is_new":true,"temp_id":1728,"item_spec":674,"options":[{"param":1,"id":30}],"is_sell":true},
{"cost":10,"type_sell":3,"is_new":true,"temp_id":1729,"item_spec":674,"options":[{"param":1,"id":30}],"is_sell":true},
{"cost":1,"type_sell":3,"is_new":true,"temp_id":1074,"item_spec":674,"options":[{"id":30,"param":0}],"is_sell":true},
{"cost":3,"type_sell":3,"is_new":true,"temp_id":1075,"item_spec":674,"options":[{"id":30,"param":0}],"is_sell":true},
{"cost":5,"type_sell":30,"is_new":true,"temp_id":1076,"item_spec":674,"options":[{"id":30,"param":0}],"is_sell":true},
{"cost":10,"type_sell":3,"is_new":true,"temp_id":1077,"item_spec":674,"options":[{"id":30,"param":0}],"is_sell":true},
{"cost":20,"type_sell":30,"is_new":true,"temp_id":1078,"item_spec":674,"options":[{"id":30,"param":0}],"is_sell":true},
{"cost":10,"type_sell":3,"is_new":true,"temp_id":1071,"item_spec":674,"options":[{"id":30,"param":0}],"is_sell":true},
{"cost":10,"type_sell":3,"is_new":true,"temp_id":1072,"item_spec":674,"options":[{"id":30,"param":0}],"is_sell":true},
{"cost":10,"type_sell":3,"is_new":true,"temp_id":1073,"item_spec":674,"options":[{"id":30,"param":0}],"is_sell":true},
{"cost":1,"type_sell":3,"is_new":true,"temp_id":1079,"item_spec":674,"options":[{"id":0,"param":0}],"is_sell":true},
{"cost":3,"type_sell":3,"is_new":true,"temp_id":1080,"item_spec":674,"options":[{"id":30,"param":0}],"is_sell":true},
{"cost":5,"type_sell":3,"is_new":true,"temp_id":1081,"item_spec":674,"options":[{"id":30,"param":0}],"is_sell":true},
{"cost":10,"type_sell":3,"is_new":true,"temp_id":1082,"item_spec":674,"options":[{"id":30,"param":0}],"is_sell":true},
{"cost":20,"type_sell":3,"is_new":true,"temp_id":1083,"item_spec":674,"options":[{"id":30,"param":0}],"is_sell":true},
{"cost":10000,"type_sell":3,"is_new":true,"temp_id":1054,"item_spec":674,"options":[{"id":30,"param":0}],"is_sell":true},
{"cost":500,"type_sell":3,"is_new":true,"temp_id":1703,"item_spec":674,"options":[{"id":30,"param":1}],"is_sell":true},
{"cost":1500,"type_sell":3,"is_new":true,"temp_id":1704,"item_spec":674,"options":[{"id":30,"param":1}],"is_sell":true},
{"cost":1,"type_sell":3,"is_new":true,"temp_id":1705,"item_spec":674,"options":[],"is_sell":true},
{"cost":100000,"type_sell":3,"is_new":true,"temp_id":1748,"item_spec":674,"options":[{"id":50,"param":30},{"id":77,"param":85},{"id":103,"param":85},{"id":33,"param":1}],"is_sell":true},
{"cost":100000,"type_sell":3,"is_new":true,"temp_id":1859,"item_spec":674,"options":[{"id":50,"param":30},{"id":77,"param":85},{"id":103,"param":85},{"id":33,"param":1}],"is_sell":true},
{"cost":10000,"type_sell":3,"is_new":true,"temp_id":1860,"item_spec":674,"options":[{"id":50,"param":30},{"id":77,"param":85},{"id":103,"param":85},{"id":33,"param":1}],"is_sell":true},
{"cost":10000,"type_sell":3,"is_new":true,"temp_id":2022,"item_spec":674,"options":[{"id":50,"param":30},{"id":77,"param":85},{"id":103,"param":85},{"id":33,"param":1}],"is_sell":true},
{"cost":200,"type_sell":3,"is_new":true,"temp_id":1751,"item_spec":674,"options":[{"id":30,"param":0}],"is_sell":true},
{"cost":100,"type_sell":3,"is_new":true,"temp_id":1212,"item_spec":674,"options":[{"id":21,"param":40},{"id":217,"param":0},{"id":30,"param":0},{"id":219,"param":5},{"id":212,"param":1000}],"is_sell":true},
{"cost":100,"type_sell":3,"is_new":true,"temp_id":1044,"item_spec":674,"options":[{"id":21,"param":40},{"id":217,"param":0},{"id":30,"param":0},{"id":219,"param":5},{"id":212,"param":1000}],"is_sell":true},
{"cost":100,"type_sell":3,"is_new":true,"temp_id":1211,"item_spec":674,"options":[{"id":21,"param":40},{"id":217,"param":0},{"id":30,"param":0},{"id":219,"param":5},{"id":212,"param":1000}],"is_sell":true}
]' WHERE id = 47;

-- Xác nhận kết quả
SELECT 'Updated OSIN shop successfully!' AS result;
