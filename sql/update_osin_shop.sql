-- Cập nhật Shop Osin (tab_shop id=47)
-- 1. Bỏ Gogeta SSJ Blue (1859) và Vegito SSJ Blue (1860) bằng cách set is_sell = false
-- 2. Giảm giá Gohan Beast (1748) từ 100000 xuống 8500
-- 3. Giảm giá Vegito God (2022) từ 10000 xuống 8500

UPDATE tab_shop SET items = REPLACE(items,
  '"cost":100000,"type_sell":3,"is_new":true,"temp_id":1748',
  '"cost":8500,"type_sell":3,"is_new":true,"temp_id":1748'
) WHERE id = 47;

UPDATE tab_shop SET items = REPLACE(items,
  '"cost":10000,"type_sell":3,"is_new":true,"temp_id":2022',
  '"cost":8500,"type_sell":3,"is_new":true,"temp_id":2022'
) WHERE id = 47;

-- Bỏ Gogeta SSJ Blue (1859)
UPDATE tab_shop SET items = REPLACE(items,
  '"temp_id":1859,"item_spec":674,"options":[{"id":50,"param":30},{"id":77,"param":85},{"id":103,"param":85},{"id":33,"param":1}],"is_sell":true',
  '"temp_id":1859,"item_spec":674,"options":[{"id":50,"param":30},{"id":77,"param":85},{"id":103,"param":85},{"id":33,"param":1}],"is_sell":false'
) WHERE id = 47;

-- Bỏ Vegito SSJ Blue (1860)
UPDATE tab_shop SET items = REPLACE(items,
  '"temp_id":1860,"item_spec":674,"options":[{"id":50,"param":30},{"id":77,"param":85},{"id":103,"param":85},{"id":33,"param":1}],"is_sell":true',
  '"temp_id":1860,"item_spec":674,"options":[{"id":50,"param":30},{"id":77,"param":85},{"id":103,"param":85},{"id":33,"param":1}],"is_sell":false'
) WHERE id = 47;
