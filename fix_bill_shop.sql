-- ============================================================
-- BƯỚC 1: Xem danh sách items Hủy Diệt (level=14) theo gender
-- CHẠY CÁI NÀY TRƯỚC, GỬI KẾT QUẢ LẠI CHO TÔI
-- ============================================================
SELECT id, name, gender, type, level, strRequire
FROM item_template
WHERE level = 14
ORDER BY gender, type;

-- ============================================================
-- BƯỚC 2: Xem shop BILL hiện tại (tab nào chứa items gì)
-- ============================================================
SELECT ts.id, ts.tab_name, ts.tab_index, ts.items
FROM tab_shop ts
JOIN shop s ON ts.shop_id = s.id
WHERE s.tag_name = 'BILL'
ORDER BY ts.tab_index;
