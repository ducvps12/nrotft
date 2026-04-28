-- =====================================================
-- SCRIPT BAN NGAY TÀI KHOẢN BUG DANAP = 0
-- Chạy trực tiếp trong MySQL Workbench/HeidiSQL
-- =====================================================

-- BƯỚC 1: Tạo bảng tạm lưu tài khoản bug
DROP TEMPORARY TABLE IF EXISTS temp_bug_accounts;
CREATE TEMPORARY TABLE temp_bug_accounts (
    account_id INT PRIMARY KEY,
    username VARCHAR(255),
    ip_address VARCHAR(50),
    reason TEXT,
    cash INT,
    vnd INT,
    danap INT,
    total_recharge INT
);

-- BƯỚC 2: Tìm tài khoản có danap = 0 nhưng có giao dịch nạp
INSERT INTO temp_bug_accounts (account_id, username, ip_address, reason, cash, vnd, danap, total_recharge)
SELECT 
    a.id,
    a.username,
    a.ip_address,
    'Danap = 0 nhưng có lịch sử nạp thẻ (BUG hoặc HACK)' as reason,
    a.cash,
    a.vnd,
    a.danap,
    COALESCE(SUM(t.amount), 0) as total_recharge
FROM account a
LEFT JOIN transaction_history t ON t.account_id = a.id AND t.type = 'RECHARGE'
WHERE a.danap = 0 
  AND a.ban = 0
  AND a.is_admin = 0
GROUP BY a.id
HAVING total_recharge > 0
ORDER BY total_recharge DESC;

-- BƯỚC 3: Hiển thị danh sách sẽ bị ban
SELECT 
    '=== DANH SÁCH TÀI KHOẢN SẼ BỊ BAN ===' as warning,
    COUNT(*) as total_accounts,
    SUM(total_recharge) as total_recharge_amount
FROM temp_bug_accounts;

SELECT 
    account_id,
    username,
    ip_address,
    cash,
    vnd,
    danap,
    total_recharge,
    reason
FROM temp_bug_accounts
ORDER BY total_recharge DESC;

-- BƯỚC 4: Tìm tài khoản cùng IP
INSERT IGNORE INTO temp_bug_accounts (account_id, username, ip_address, reason, cash, vnd, danap, total_recharge)
SELECT 
    a.id,
    a.username,
    a.ip_address,
    CONCAT('Cùng IP với tài khoản bug: ', t.username) as reason,
    a.cash,
    a.vnd,
    a.danap,
    0
FROM account a
INNER JOIN temp_bug_accounts t ON t.ip_address = a.ip_address
WHERE a.id != t.account_id
  AND a.ban = 0
  AND a.is_admin = 0
  AND a.ip_address IS NOT NULL
  AND a.ip_address != ''
  AND a.ip_address != '127.0.0.1';

-- BƯỚC 5: Hiển thị tổng hợp
SELECT 
    '=== TỔNG HỢP ===' as summary,
    COUNT(*) as total_will_ban,
    SUM(CASE WHEN reason LIKE '%Danap = 0%' THEN 1 ELSE 0 END) as bug_danap,
    SUM(CASE WHEN reason LIKE '%Cùng IP%' THEN 1 ELSE 0 END) as same_ip
FROM temp_bug_accounts;

-- =====================================================
-- BƯỚC 6: THỰC THI BAN (XÁC NHẬN TRƯỚC KHI CHẠY!)
-- Bỏ comment các dòng dưới để thực thi
-- =====================================================

/*
-- 6.1: Ban tất cả tài khoản bug
UPDATE account a
INNER JOIN temp_bug_accounts t ON t.account_id = a.id
SET 
    a.ban = 1,
    a.ban_reason = CONCAT('[AUTO-BAN-BUG] ', t.reason),
    a.ban_time = NOW(),
    a.active = 0
WHERE a.ban = 0;

-- 6.2: Reset tài sản
UPDATE account a
INNER JOIN temp_bug_accounts t ON t.account_id = a.id
SET 
    a.cash = 0,
    a.vnd = 0,
    a.vang = 0,
    a.thoi_vang = 0
WHERE a.ban = 1;

-- 6.3: Xóa vật phẩm
UPDATE player p
INNER JOIN temp_bug_accounts t ON t.account_id = p.account_id
SET 
    p.items_bag = '[]',
    p.items_box = '[]';

-- 6.4: Lưu log
CREATE TABLE IF NOT EXISTS ban_log (
    id INT AUTO_INCREMENT PRIMARY KEY,
    account_id INT,
    username VARCHAR(255),
    ip_address VARCHAR(50),
    reason TEXT,
    cash_before INT,
    vnd_before INT,
    danap_before INT,
    banned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_account (account_id),
    INDEX idx_banned (banned_at)
);

INSERT INTO ban_log (account_id, username, ip_address, reason, cash_before, vnd_before, danap_before)
SELECT 
    account_id,
    username,
    ip_address,
    reason,
    cash,
    vnd,
    danap
FROM temp_bug_accounts;

-- 6.5: Thống kê kết quả
SELECT 
    '=== KẾT QUẢ BAN ===' as result,
    COUNT(*) as total_banned,
    NOW() as banned_time
FROM account
WHERE ban = 1 
  AND ban_time >= DATE_SUB(NOW(), INTERVAL 1 MINUTE);

SELECT 
    '=== CHI TIẾT ===' as detail,
    a.id,
    a.username,
    a.ip_address,
    a.ban_reason,
    a.ban_time
FROM account a
INNER JOIN temp_bug_accounts t ON t.account_id = a.id
WHERE a.ban = 1
ORDER BY a.ban_time DESC;
*/

-- =====================================================
-- HƯỚNG DẪN SỬ DỤNG
-- =====================================================

SELECT 
    '=== HƯỚNG DẪN ===' as guide,
    '1. Chạy script này để xem danh sách' as step_1,
    '2. Xem xét kỹ danh sách tài khoản' as step_2,
    '3. Bỏ comment phần BƯỚC 6 để thực thi ban' as step_3,
    '4. Chạy lại script để ban' as step_4,
    '5. Restart server' as step_5;
