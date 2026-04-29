-- =====================================================
-- NÂNG GIÁ CẢI TRANG x10 - Shop 1, 2, 3 (Tab Cải trang)
-- Giá gem (ngọc xanh) tại position 10 (0-indexed 9) trong mỗi item array
-- Lý do: Tân thủ nhận 100K gem, ước rồng 1 sao cho 100K gem
--         Giá 50-1200 gem quá rẻ so với nguồn thu nhập gem
-- =====================================================

-- Backup trước khi update
-- SELECT shopId, arrItemShop INTO OUTFILE 'shop_backup.csv' FROM shop WHERE shopId IN (1,2,3);

-- Hiện tại không thể dùng SQL thuần để parse JSON array lồng nhau
-- Cần sử dụng script Java hoặc update trực tiếp từ admin panel

-- Danh sách giá cần sửa (nhân x10):
-- 50 -> 500
-- 200 -> 2000
-- 300 -> 3000
-- 500 -> 5000
-- 1000 -> 10000
-- 1100 -> 11000
-- 1200 -> 12000
-- 3000 -> 30000
-- 5000 -> 50000
-- 10000 -> 100000
