SET NAMES utf8mb4;

INSERT INTO `shop` (`id`, `npc_id`, `tag_name`, `type_shop`) VALUES
(53, 75, 'GOHAN_ULTRA', 0)
ON DUPLICATE KEY UPDATE
  `npc_id` = VALUES(`npc_id`),
  `tag_name` = VALUES(`tag_name`),
  `type_shop` = VALUES(`type_shop`);

INSERT INTO `tab_shop` (`id`, `shop_id`, `tab_name`, `tab_index`, `items`, `Mô tả`) VALUES
(73, 53, 'Ultra<>Cai trang', 1,
 '[{"cost":199,"type_sell":2,"is_new":true,"temp_id":1113,"item_spec":0,"options":[{"id":50,"param":12},{"id":77,"param":10},{"id":103,"param":10},{"id":14,"param":3},{"id":93,"param":30},{"id":30,"param":0}],"is_sell":true},{"cost":399,"type_sell":2,"is_new":true,"temp_id":1541,"item_spec":0,"options":[{"id":50,"param":15},{"id":77,"param":12},{"id":103,"param":12},{"id":14,"param":5},{"id":93,"param":30},{"id":30,"param":0}],"is_sell":true},{"cost":799,"type_sell":2,"is_new":true,"temp_id":1885,"item_spec":0,"options":[{"id":50,"param":18},{"id":77,"param":15},{"id":103,"param":15},{"id":14,"param":7},{"id":93,"param":30},{"id":30,"param":0}],"is_sell":true}]',
 'Premium 30 ngay khoa giao dich'),
(74, 53, 'Ultra<>Danh hieu', 2,
 '[{"cost":99,"type_sell":2,"is_new":true,"temp_id":1287,"item_spec":0,"options":[{"id":50,"param":5},{"id":77,"param":5},{"id":103,"param":5},{"id":93,"param":30},{"id":30,"param":0}],"is_sell":true},{"cost":199,"type_sell":2,"is_new":true,"temp_id":1457,"item_spec":0,"options":[{"id":50,"param":10},{"id":77,"param":10},{"id":103,"param":10},{"id":93,"param":30},{"id":30,"param":0}],"is_sell":true},{"cost":299,"type_sell":2,"is_new":true,"temp_id":1754,"item_spec":0,"options":[{"id":50,"param":13},{"id":77,"param":13},{"id":103,"param":13},{"id":14,"param":5},{"id":93,"param":30},{"id":30,"param":0}],"is_sell":true}]',
 'Danh hieu buff 30 ngay'),
(75, 53, 'Ho<>tro', 3,
 '[{"cost":20,"type_sell":2,"is_new":false,"temp_id":521,"item_spec":0,"options":[{"id":1,"param":20}],"is_sell":true},{"cost":99,"type_sell":2,"is_new":false,"temp_id":1523,"item_spec":0,"options":[{"id":1,"param":200}],"is_sell":true},{"cost":199,"type_sell":2,"is_new":false,"temp_id":529,"item_spec":0,"options":[{"id":9,"param":0}],"is_sell":true}]',
 'Vat pham ho tro tieu ruby')
ON DUPLICATE KEY UPDATE
  `shop_id` = VALUES(`shop_id`),
  `tab_name` = VALUES(`tab_name`),
  `tab_index` = VALUES(`tab_index`),
  `items` = VALUES(`items`),
  `Mô tả` = VALUES(`Mô tả`);

ALTER TABLE `shop` AUTO_INCREMENT = 54;
ALTER TABLE `tab_shop` AUTO_INCREMENT = 76;

SELECT id, npc_id, tag_name, type_shop
FROM shop
WHERE tag_name = 'GOHAN_ULTRA';

SELECT id, shop_id, tab_name, tab_index
FROM tab_shop
WHERE shop_id = 53
ORDER BY tab_index;
