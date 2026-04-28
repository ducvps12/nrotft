-- =====================================================
-- SCRIPT THỰC THI BAN TÀI KHOẢN SAU KHI XÁC NHẬN
-- Chạy script này SAU KHI đã xem xét danh sách nghi ngờ
-- =====================================================

-- BƯỚC 1: Kiểm tra xem đã chạy detect script chưa
SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN CONCAT('✓ Đã phát hiện ', COUNT(*), ' tài khoản nghi ngờ')
        ELSE '✗ CHƯA CHẠY SCRIPT PHÁT HIỆN! Hãy chạy detect_and_ban_fraud_accounts.sql trước'
    END as status
FROM temp_suspicious_accounts;

-- BƯỚC 2: Xem lại danh sách trước khi ban
SELECT 
    '=== DANH SÁCH SẼ BỊ BAN ===' as warning,
    tier,
    COUNT(*) as total,
    GROUP_CONCAT(username SEPARATOR ', ') as usernames
FROM temp_suspicious_accounts
GROUP BY tier
ORDER BY tier;

-- =====================================================
-- BƯỚC 3: THỰC HIỆN BAN (XÁC NHẬN TRƯỚC KHI CHẠY!)
-- =====================================================

-- 3.1: Ban TIER 0 (Tài khoản gốc - gian lận chính)
UPDATE account a
INNER JOIN temp_suspicious_accounts t ON t.account_id = a.id
SET 
    a.ban = 1,
    a.ban_reason = CONCAT('[AUTO-BAN-T0] ', t.reason),
    a.ban_time = NOW(),
    a.active = 0
WHERE t.tier = 0;

-- Log tier 0
UPDATE fraud_detection_log 
SET action = 'BANNED_TIER_0'
WHERE account_id IN (SELECT account_id FROM temp_suspicious_accounts WHERE tier = 0);

-- 3.2: Ban TIER 1 (Cùng IP với tài khoản gian lận)
UPDATE account a
INNER JOIN temp_suspicious_accounts t ON t.account_id = a.id
SET 
    a.ban = 1,
    a.ban_reason = CONCAT('[AUTO-BAN-T1] ', t.reason),
    a.ban_time = NOW(),
    a.active = 0
WHERE t.tier = 1;

-- Log tier 1
UPDATE fraud_detection_log 
SET action = 'BANNED_TIER_1'
WHERE account_id IN (SELECT account_id FROM temp_suspicious_accounts WHERE tier = 1);

-- 3.3: Ban TIER 2 (Giao dịch trực tiếp với gian lận)
UPDATE account a
INNER JOIN temp_suspicious_accounts t ON t.account_id = a.id
SET 
    a.ban = 1,
    a.ban_reason = CONCAT('[AUTO-BAN-T2] ', t.reason),
    a.ban_time = NOW(),
    a.active = 0
WHERE t.tier = 2;

-- Log tier 2
UPDATE fraud_detection_log 
SET action = 'BANNED_TIER_2'
WHERE account_id IN (SELECT account_id FROM temp_suspicious_accounts WHERE tier = 2);

-- 3.4: CẢNH BÁO TIER 3 (Không ban ngay, cần xem xét thêm)
UPDATE account a
INNER JOIN temp_suspicious_accounts t ON t.account_id = a.id
SET 
    a.ban_reason = CONCAT('[WARNING-T3] ', t.reason)
WHERE t.tier = 3
  AND a.ban = 0;

-- Log tier 3
UPDATE fraud_detection_log 
SET action = 'WARNING_TIER_3'
WHERE account_id IN (SELECT account_id FROM temp_suspicious_accounts WHERE tier = 3);

-- =====================================================
-- BƯỚC 4: THU HỒI TÀI SẢN (Optional)
-- =====================================================

-- 4.1: Reset vàng/ngọc của tài khoản bị ban
UPDATE account a
INNER JOIN temp_suspicious_accounts t ON t.account_id = a.id
SET 
    a.cash = 0,
    a.vnd = 0,
    a.vang = 0,
    a.thoi_vang = 0
WHERE t.tier IN (0, 1, 2)
  AND a.ban = 1;

-- 4.2: Xóa vật phẩm trong túi của nhân vật
UPDATE player p
INNER JOIN temp_suspicious_accounts t ON t.account_id = p.account_id
SET 
    p.items_bag = '[]',
    p.items_box = '[]'
WHERE t.tier IN (0, 1, 2);

-- =====================================================
-- BƯỚC 5: THỐNG KÊ KẾT QUẢ
-- =====================================================

SELECT 
    '=== KẾT QUẢ THỰC THI BAN ===' as result,
    (SELECT COUNT(*) FROM temp_suspicious_accounts WHERE tier = 0) as banned_tier_0,
    (SELECT COUNT(*) FROM temp_suspicious_accounts WHERE tier = 1) as banned_tier_1,
    (SELECT COUNT(*) FROM temp_suspicious_accounts WHERE tier = 2) as banned_tier_2,
    (SELECT COUNT(*) FROM temp_suspicious_accounts WHERE tier = 3) as warned_tier_3,
    (SELECT COUNT(*) FROM account WHERE ban = 1 AND ban_time >= DATE_SUB(NOW(), INTERVAL 1 HOUR)) as total_banned_last_hour;

-- Chi tiết tài khoản đã ban
SELECT 
    t.tier,
    a.id,
    a.username,
    a.ip_address,
    a.ban_reason,
    a.ban_time,
    a.cash as old_cash,
    a.vnd as old_vnd
FROM account a
INNER JOIN temp_suspicious_accounts t ON t.account_id = a.id
WHERE a.ban = 1
ORDER BY t.tier, a.id;

-- =====================================================
-- BƯỚC 6: THÔNG BÁO VÀ DỌN DẸP
-- =====================================================

-- Tạo thông báo cho admin
INSERT INTO admin_notifications (type, title, message, created_at)
SELECT 
    'FRAUD_DETECTION',
    'Đã ban tài khoản gian lận',
    CONCAT(
        'Đã ban ', COUNT(*), ' tài khoản gian lận:\n',
        '- Tier 0 (gốc): ', SUM(CASE WHEN tier = 0 THEN 1 ELSE 0 END), '\n',
        '- Tier 1 (cùng IP): ', SUM(CASE WHEN tier = 1 THEN 1 ELSE 0 END), '\n',
        '- Tier 2 (giao dịch): ', SUM(CASE WHEN tier = 2 THEN 1 ELSE 0 END), '\n',
        '- Tier 3 (cảnh báo): ', SUM(CASE WHEN tier = 3 THEN 1 ELSE 0 END)
    ),
    NOW()
FROM temp_suspicious_accounts;

-- Dọn dẹp bảng tạm (giữ lại để audit)
-- DROP TEMPORARY TABLE IF EXISTS temp_suspicious_accounts;

SELECT 
    '=== HOÀN TẤT ===' as status,
    'Đã ban tất cả tài khoản gian lận!' as message,
    'Hãy RESTART SERVER để kick người chơi đang online' as next_step;
