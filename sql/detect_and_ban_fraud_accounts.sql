-- =====================================================
-- SCRIPT PHÁT HIỆN VÀ BAN TÀI KHOẢN GIAN LẬN
-- Truy vết theo IP, giao dịch, và affiliate tầng 1-2-3
-- =====================================================

-- Bước 1: Tạo bảng tạm để lưu danh sách tài khoản nghi ngờ
DROP TEMPORARY TABLE IF EXISTS temp_suspicious_accounts;
CREATE TEMPORARY TABLE temp_suspicious_accounts (
    account_id INT PRIMARY KEY,
    username VARCHAR(255),
    ip_address VARCHAR(50),
    reason TEXT,
    tier INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- PHẦN 1: PHÁT HIỆN TÀI KHOẢN GỐC (TIER 0)
-- =====================================================

-- 1.1: Tài khoản có danap = 0 nhưng có giao dịch nạp thẻ
INSERT INTO temp_suspicious_accounts (account_id, username, ip_address, reason, tier)
SELECT DISTINCT 
    a.id,
    a.username,
    a.ip_address,
    'Danap = 0 nhưng có lịch sử nạp thẻ (bug hoặc gian lận)' as reason,
    0 as tier
FROM account a
WHERE a.danap = 0 
  AND a.id IN (
      SELECT DISTINCT account_id 
      FROM transaction_history 
      WHERE type = 'RECHARGE' 
        AND amount > 0
  )
  AND a.ban = 0;

-- 1.2: Tài khoản có nhiều giao dịch chuyển vàng/ngọc bất thường
INSERT IGNORE INTO temp_suspicious_accounts (account_id, username, ip_address, reason, tier)
SELECT DISTINCT 
    a.id,
    a.username,
    a.ip_address,
    CONCAT('Giao dịch chuyển tiền bất thường: ', COUNT(*), ' lần trong 24h') as reason,
    0 as tier
FROM account a
INNER JOIN player p ON p.account_id = a.id
INNER JOIN transaction_history t ON t.account_id = a.id
WHERE t.type IN ('TRANSFER_GOLD', 'TRANSFER_GEM')
  AND t.created_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
  AND a.ban = 0
GROUP BY a.id
HAVING COUNT(*) > 10;

-- 1.3: Tài khoản có cash/vnd bất thường (âm hoặc quá cao)
INSERT IGNORE INTO temp_suspicious_accounts (account_id, username, ip_address, reason, tier)
SELECT DISTINCT 
    a.id,
    a.username,
    a.ip_address,
    CONCAT('Cash/VND bất thường: cash=', a.cash, ', vnd=', a.vnd, ', danap=', a.danap) as reason,
    0 as tier
FROM account a
WHERE (a.cash < 0 OR a.vnd < 0 OR a.danap < 0 
       OR (a.cash > 1000000 AND a.danap = 0)
       OR (a.vnd > 1000000 AND a.danap = 0))
  AND a.ban = 0;

-- 1.4: Tài khoản tạo nhiều nhân vật trong thời gian ngắn
INSERT IGNORE INTO temp_suspicious_accounts (account_id, username, ip_address, reason, tier)
SELECT DISTINCT 
    a.id,
    a.username,
    a.ip_address,
    CONCAT('Tạo ', COUNT(p.id), ' nhân vật trong 1 giờ') as reason,
    0 as tier
FROM account a
INNER JOIN player p ON p.account_id = a.id
WHERE p.create_time >= DATE_SUB(NOW(), INTERVAL 1 HOUR)
  AND a.ban = 0
GROUP BY a.id
HAVING COUNT(p.id) > 5;

-- =====================================================
-- PHẦN 2: TRUY VẾT TẦNG 1 - TÀI KHOẢN CÙNG IP
-- =====================================================

INSERT IGNORE INTO temp_suspicious_accounts (account_id, username, ip_address, reason, tier)
SELECT DISTINCT 
    a.id,
    a.username,
    a.ip_address,
    CONCAT('Cùng IP với tài khoản gian lận: ', t0.username) as reason,
    1 as tier
FROM account a
INNER JOIN temp_suspicious_accounts t0 ON t0.ip_address = a.ip_address
WHERE t0.tier = 0
  AND a.id != t0.account_id
  AND a.ban = 0
  AND a.ip_address IS NOT NULL
  AND a.ip_address != ''
  AND a.ip_address != '127.0.0.1';

-- =====================================================
-- PHẦN 3: TRUY VẾT TẦNG 2 - GIAO DỊCH LIÊN QUAN
-- =====================================================

-- 3.1: Tài khoản nhận vàng/ngọc từ tài khoản tier 0 hoặc tier 1
INSERT IGNORE INTO temp_suspicious_accounts (account_id, username, ip_address, reason, tier)
SELECT DISTINCT 
    a.id,
    a.username,
    a.ip_address,
    CONCAT('Nhận giao dịch từ tài khoản gian lận tier ', t1.tier, ': ', t1.username) as reason,
    2 as tier
FROM account a
INNER JOIN transaction_history t ON t.to_account_id = a.id
INNER JOIN temp_suspicious_accounts t1 ON t1.account_id = t.account_id
WHERE t1.tier IN (0, 1)
  AND t.type IN ('TRANSFER_GOLD', 'TRANSFER_GEM', 'TRADE')
  AND t.created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
  AND a.ban = 0;

-- 3.2: Tài khoản gửi vàng/ngọc cho tài khoản tier 0 hoặc tier 1
INSERT IGNORE INTO temp_suspicious_accounts (account_id, username, ip_address, reason, tier)
SELECT DISTINCT 
    a.id,
    a.username,
    a.ip_address,
    CONCAT('Gửi giao dịch cho tài khoản gian lận tier ', t1.tier, ': ', t1.username) as reason,
    2 as tier
FROM account a
INNER JOIN transaction_history t ON t.account_id = a.id
INNER JOIN temp_suspicious_accounts t1 ON t1.account_id = t.to_account_id
WHERE t1.tier IN (0, 1)
  AND t.type IN ('TRANSFER_GOLD', 'TRANSFER_GEM', 'TRADE')
  AND t.created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
  AND a.ban = 0;

-- =====================================================
-- PHẦN 4: TRUY VẾT TẦNG 3 - GIAO DỊCH GIÁN TIẾP
-- =====================================================

-- 4.1: Tài khoản có giao dịch với tier 2
INSERT IGNORE INTO temp_suspicious_accounts (account_id, username, ip_address, reason, tier)
SELECT DISTINCT 
    a.id,
    a.username,
    a.ip_address,
    CONCAT('Giao dịch với tài khoản tier 2: ', t2.username) as reason,
    3 as tier
FROM account a
INNER JOIN transaction_history t ON (t.account_id = a.id OR t.to_account_id = a.id)
INNER JOIN temp_suspicious_accounts t2 ON (t2.account_id = t.account_id OR t2.account_id = t.to_account_id)
WHERE t2.tier = 2
  AND a.id != t2.account_id
  AND t.type IN ('TRANSFER_GOLD', 'TRANSFER_GEM', 'TRADE')
  AND t.created_at >= DATE_SUB(NOW(), INTERVAL 3 DAY)
  AND a.ban = 0;

-- =====================================================
-- PHẦN 5: HIỂN THỊ KẾT QUẢ
-- =====================================================

-- Thống kê tổng quan
SELECT 
    '=== THỐNG KÊ TỔNG QUAN ===' as info,
    COUNT(*) as total_suspicious,
    SUM(CASE WHEN tier = 0 THEN 1 ELSE 0 END) as tier_0_root,
    SUM(CASE WHEN tier = 1 THEN 1 ELSE 0 END) as tier_1_same_ip,
    SUM(CASE WHEN tier = 2 THEN 1 ELSE 0 END) as tier_2_transaction,
    SUM(CASE WHEN tier = 3 THEN 1 ELSE 0 END) as tier_3_indirect
FROM temp_suspicious_accounts;

-- Danh sách chi tiết
SELECT 
    tier,
    account_id,
    username,
    ip_address,
    reason,
    created_at
FROM temp_suspicious_accounts
ORDER BY tier ASC, account_id ASC;

-- =====================================================
-- PHẦN 6: THỰC HIỆN BAN (CHẠY SAU KHI XÁC NHẬN)
-- =====================================================

-- CẢNH BÁO: Chỉ chạy phần này sau khi đã xem xét kỹ danh sách!
-- Bỏ comment để thực hiện ban:

/*
-- Ban tất cả tài khoản tier 0 (gốc)
UPDATE account a
INNER JOIN temp_suspicious_accounts t ON t.account_id = a.id
SET a.ban = 1,
    a.ban_reason = CONCAT('[AUTO-BAN] ', t.reason),
    a.ban_time = NOW()
WHERE t.tier = 0;

-- Ban tất cả tài khoản tier 1 (cùng IP)
UPDATE account a
INNER JOIN temp_suspicious_accounts t ON t.account_id = a.id
SET a.ban = 1,
    a.ban_reason = CONCAT('[AUTO-BAN TIER-1] ', t.reason),
    a.ban_time = NOW()
WHERE t.tier = 1;

-- Ban tất cả tài khoản tier 2 (giao dịch trực tiếp)
UPDATE account a
INNER JOIN temp_suspicious_accounts t ON t.account_id = a.id
SET a.ban = 1,
    a.ban_reason = CONCAT('[AUTO-BAN TIER-2] ', t.reason),
    a.ban_time = NOW()
WHERE t.tier = 2;

-- Cảnh báo tier 3 (không ban ngay, cần xem xét thêm)
UPDATE account a
INNER JOIN temp_suspicious_accounts t ON t.account_id = a.id
SET a.ban_reason = CONCAT('[WARNING TIER-3] ', t.reason)
WHERE t.tier = 3
  AND a.ban = 0;

-- Kick tất cả người chơi đang online
-- (Cần chạy từ game server hoặc restart server)
*/

-- =====================================================
-- PHẦN 7: LƯU LOG
-- =====================================================

-- Tạo bảng log nếu chưa có
CREATE TABLE IF NOT EXISTS fraud_detection_log (
    id INT AUTO_INCREMENT PRIMARY KEY,
    account_id INT,
    username VARCHAR(255),
    ip_address VARCHAR(50),
    reason TEXT,
    tier INT,
    action VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_account (account_id),
    INDEX idx_created (created_at)
);

-- Lưu log
INSERT INTO fraud_detection_log (account_id, username, ip_address, reason, tier, action)
SELECT 
    account_id,
    username,
    ip_address,
    reason,
    tier,
    'DETECTED' as action
FROM temp_suspicious_accounts;

-- =====================================================
-- PHẦN 8: EXPORT DANH SÁCH ĐỂ XEM XÉT
-- =====================================================

-- Export ra file CSV (chạy từ MySQL client)
/*
SELECT 
    tier,
    account_id,
    username,
    ip_address,
    reason
FROM temp_suspicious_accounts
ORDER BY tier ASC, account_id ASC
INTO OUTFILE '/tmp/suspicious_accounts.csv'
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n';
*/

-- Kết thúc
SELECT '=== HOÀN TẤT PHÁT HIỆN ===' as status,
       'Xem xét danh sách trên trước khi chạy phần BAN!' as warning;
