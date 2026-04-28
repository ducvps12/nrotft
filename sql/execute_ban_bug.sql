-- =====================================================
-- THỰC THI BAN TÀI KHOẢN BUG
-- =====================================================

-- Ban tài khoản bug
UPDATE account
SET 
    ban = 1,
    ban_reason = '[AUTO-BAN] Có tiền nhưng danap = 0 (BUG hoặc HACK)',
    ban_time = NOW(),
    active = 0
WHERE danap = 0 
  AND (cash > 0 OR vnd > 0 OR vang > 100000)
  AND ban = 0
  AND is_admin = 0;

-- Reset tài sản
UPDATE account
SET 
    cash = 0,
    vnd = 0,
    vang = 0,
    thoi_vang = 0
WHERE ban = 1 
  AND ban_time >= DATE_SUB(NOW(), INTERVAL 1 MINUTE);

-- Kết quả
SELECT 
    '=== KẾT QUẢ BAN ===' as result,
    COUNT(*) as total_banned,
    NOW() as banned_time
FROM account
WHERE ban = 1 
  AND ban_time >= DATE_SUB(NOW(), INTERVAL 1 MINUTE);

-- Chi tiết tài khoản đã ban
SELECT 
    '=== CHI TIẾT ===' as detail,
    id,
    username,
    ip_address,
    ban_reason,
    ban_time
FROM account
WHERE ban = 1 
  AND ban_time >= DATE_SUB(NOW(), INTERVAL 1 MINUTE)
ORDER BY ban_time DESC;
