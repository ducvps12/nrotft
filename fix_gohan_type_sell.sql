UPDATE tab_shop
SET items = REPLACE(items, '"type_sell":2', '"type_sell":3')
WHERE shop_id = (SELECT id FROM shop WHERE tag_name='GOHAN_ULTRA' LIMIT 1);

SELECT id, tab_name,
       (LENGTH(items)-LENGTH(REPLACE(items,'"type_sell":3','')))/LENGTH('"type_sell":3') AS ruby_items,
       items LIKE '%"type_sell":2%' AS has_bad_type
FROM tab_shop
WHERE shop_id=(SELECT id FROM shop WHERE tag_name='GOHAN_ULTRA' LIMIT 1)
ORDER BY tab_index;
