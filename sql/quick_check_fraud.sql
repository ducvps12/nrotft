-- =====================================================
-- SCRIPT KIỂM TRA NHANH TÀI KHOẢN NGHI NGỜ
-- Chạy script này để kiểm tra nhanh trước khi ban
-- =====================================================

-- 1. Kiểm tra tài khoản có danap = 0 nhưng có giao dịch
SELECT 
    '=== TÀI KHOẢN DANAP = 0 NHƯNG CÓ GIAO DỊCH ===' as check_type,
    a.id,
    a.username,
    a.ip_address,
    a.cash,
    a.vnd,
    a.danap,
    COUNT(t.id) as total_transactions,
    SUM(t.amount) as total_amount
FROM account a
LEFT JOIN transaction_history t ON t.account_id = a.id AND t.type = 'RECHARGE'
WHERE a.danap = 0 
  AND a.ban = 0
GROUP BY a.id
HAVING total_transactions > 0
ORDER BY total_amount DESC
LIMIT 20;

-- 2. Kiểm tra tài khoản cùng IP
SELECT 
    '=== TÀI KHOẢN CÙNG IP (>2 TÀI KHOẢN) ===' as check_type,
    a.ip_address,
    COUNT(*) as total_accounts,
    GROUP_CONCAT(a.username ORDER BY a.id SEPARATOR ', ') as usernames,
    SUM(a.cash) as total_cash,
    SUM(a.danap) as total_danap
FROM account a
WHERE a.ip_address IS NOT NULL 
  AND a.ip_address != '' 
  AND a.ip_address != '127.0.0.1'
  AND a.ban = 0
GROUP BY a.ip_address
HAVING COUNT(*) > 2
ORDER BY COUNT(*) DESC
LIMIT 20;

-- 3. Kiểm tra giao dịch bất thường trong 24h
SELECT 
    '=== GIAO DỊCH BẤT THƯỜNG 24H ===' as check_type,
    a.id,
    a.username,
    a.ip_address,
    COUNT(t.id) as total_transactions,
    SUM(CASE WHEN t.type = 'TRANSFER_GOLD' THEN t.amount ELSE 0 END) as total_gold_transfer,
    SUM(CASE WHEN t.type = 'TRANSFER_GEM' THEN t.amount ELSE 0 END) as total_gem_transfer
FROM account a
INNER JOIN transaction_history t ON t.account_id = a.id
WHERE t.created_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
  AND t.type IN ('TRANSFER_GOLD', 'TRANSFER_GEM')
  AND a.ban = 0
GROUP BY a.id
HAVING COUNT(t.id) > 10
ORDER BY COUNT(t.id) DESC
LIMIT 20;

-- 4. Kiểm tra tài khoản có cash/vnd bất thường
SELECT 
    '=== CASH/VND BẤT THƯỜNG ===' as check_type,
    a.id,
    a.username,
    a.ip_address,
    a.cash,
    a.vnd,
    a.danap,
    a.vang,
    CASE 
        WHEN a.cash < 0 THEN 'Cash âm'
        WHEN a.vnd < 0 THEN 'VND âm'
        WHEN a.cash > 1000000 AND a.danap = 0 THEN 'Cash cao nhưng danap = 0'
        WHEN a.vnd > 1000000 AND a.danap = 0 THEN 'VND cao nhưng danap = 0'
        ELSE 'Khác'
    END as reason
FROM account a
WHERE (a.cash < 0 OR a.vnd < 0 
       OR (a.cash > 1000000 AND a.danap = 0)
       OR (a.vnd > 1000000 AND a.danap = 0))
  AND a.ban = 0
ORDER BY a.cash DESC, a.vnd DESC
LIMIT 20;

-- 5. Kiểm tra tài khoản tạo nhiều nhân vật
SELECT 
    '=== TẠO NHIỀU NHÂN VẬT TRONG 1H ===' as check_type,
    a.id,
    a.username,
    a.ip_address,
    COUNT(p.id) as total_players,
    GROUP_CONCAT(p.name ORDER BY p.id SEPARATOR ', ') as player_names
FROM account a
INNER JOIN player p ON p.account_id = a.id
WHERE p.create_time >= DATE_SUB(NOW(), INTERVAL 1 HOUR)
  AND a.ban = 0
GROUP BY a.id
HAVING COUNT(p.id) > 3
ORDER BY COUNT(p.id) DESC
LIMIT 20;

-- 6. Tổng hợp thống kê
SELECT 
    '=== TỔNG HỢP ===' as summary,
    (SELECT COUNT(*) FROM account WHERE danap = 0 AND ban = 0 AND id IN (SELECT DISTINCT account_id FROM transaction_history WHERE type = 'RECHARGE')) as danap_zero_with_recharge,
    (SELECT COUNT(DISTINCT ip_address) FROM account WHERE ip_address IS NOT NULL AND ip_address != '' AND ip_address != '127.0.0.1' AND ban = 0 GROUP BY ip_address HAVING COUNT(*) > 2) as suspicious_ips,
    (SELECT COUNT(DISTINCT account_id) FROM transaction_history WHERE created_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR) AND type IN ('TRANSFER_GOLD', 'TRANSFER_GEM') GROUP BY account_id HAVING COUNT(*) > 10) as high_transaction_accounts,
    (SELECT COUNT(*) FROM account WHERE (cash < 0 OR vnd < 0 OR (cash > 1000000 AND danap = 0)) AND ban = 0) as abnormal_balance_accounts;
