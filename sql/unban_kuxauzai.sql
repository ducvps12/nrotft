-- =====================================================
-- UNBAN TÀI KHOẢN KUXAUZAI (clone01-04)
-- Player names: diepvien01, diepvien02, diepvien03, clone04
-- Database: nro_root
-- =====================================================

-- Đảm bảo tất cả kuxauzai accounts đều unban
UPDATE account 
SET 
    ban = 0, 
    ban_reason = NULL,
    isLock = 0, 
    isCan = 0, 
    active = 1 
WHERE username IN ('clone01', 'clone02', 'clone03', 'clone04');

-- Xác nhận kết quả
SELECT id, username, ban, isLock, isCan, active 
FROM account 
WHERE username IN ('clone01', 'clone02', 'clone03', 'clone04');

SELECT '=== HOÀN TẤT UNBAN KUXAUZAI ===' as status;
