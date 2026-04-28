-- =====================================================
-- SCRIPT BAN TÀI KHOẢN BUG ĐƠN GIẢN
-- Không cần bảng transaction_history
-- =====================================================

-- Bước 1: Tìm tài khoản có cash/vnd > 0 nhưng danap = 0 (BUG thực sự)
SELECT 
    '=== TÀI KHOẢN BUG (CÓ TIỀN NHƯNG DANAP = 0) ===' as info;

SELECT 
    id,
    username,
    ip_address,
    cash,
    vnd,
    danap,
    vang,
    'BUG: Có tiền nhưng danap = 0' as reason
FROM account
WHERE danap = 0 
  AND (cash > 0 OR vnd > 0 OR vang > 100000)
  AND ban = 0
  AND is_admin = 0
ORDER BY cash DESC, vnd DESC;

-- Bước 2: Đếm số lượng
SELECT 
    '=== THỐNG KÊ ===' as info,
    COUNT(*) as total_bug_accounts
FROM account
WHERE danap = 0 
  AND (cash > 0 OR vnd > 0 OR vang > 100000)
  AND ban = 0
  AND is_admin = 0;

-- =====================================================
-- BƯỚC 3: BAN (Bỏ comment để thực thi)
-- =====================================================

/*
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
*/
