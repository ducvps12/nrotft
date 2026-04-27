-- Audit nhanh người chơi có vàng bất thường
-- Chạy trong database NRO hiện tại.
-- Ghi chú: data_inventory thường là JSON array, phần tử [0] là vàng.

-- 1) Top người chơi theo vàng trong data_inventory
SELECT
    p.id,
    p.account_id,
    p.name,
    p.power,
    CAST(JSON_UNQUOTE(JSON_EXTRACT(p.data_inventory, '$[0]')) AS UNSIGNED) AS gold,
    CAST(JSON_UNQUOTE(JSON_EXTRACT(p.data_inventory, '$[1]')) AS UNSIGNED) AS gem,
    CAST(JSON_UNQUOTE(JSON_EXTRACT(p.data_inventory, '$[2]')) AS UNSIGNED) AS ruby,
    a.username,
    a.tongnap,
    a.vnd,
    a.active,
    p.create_time,
    p.firstTimeLogin
FROM player p
LEFT JOIN account a ON a.id = p.account_id
WHERE JSON_VALID(p.data_inventory)
ORDER BY gold DESC
LIMIT 50;

-- 2) Người chơi vàng cao nhưng không có nạp hoặc nạp thấp
SELECT
    p.id,
    p.account_id,
    p.name,
    CAST(JSON_UNQUOTE(JSON_EXTRACT(p.data_inventory, '$[0]')) AS UNSIGNED) AS gold,
    a.username,
    COALESCE(a.tongnap, 0) AS tongnap,
    COALESCE(a.vnd, 0) AS vnd,
    p.power,
    p.create_time
FROM player p
LEFT JOIN account a ON a.id = p.account_id
WHERE JSON_VALID(p.data_inventory)
  AND CAST(JSON_UNQUOTE(JSON_EXTRACT(p.data_inventory, '$[0]')) AS UNSIGNED) >= 1000000000
  AND COALESCE(a.tongnap, 0) < 50000
ORDER BY gold DESC
LIMIT 100;

-- 3) Kiểm tra lịch sử giao dịch vàng nếu bảng tồn tại
-- Nếu báo lỗi table không tồn tại thì bỏ qua query này.
SELECT *
FROM transaction_history
WHERE player_id IN (
    SELECT id FROM player
    WHERE JSON_VALID(data_inventory)
      AND CAST(JSON_UNQUOTE(JSON_EXTRACT(data_inventory, '$[0]')) AS UNSIGNED) >= 1000000000
)
ORDER BY id DESC
LIMIT 200;

-- 4) Kiểm tra lịch sử ngân hàng/thẻ liên quan tài khoản đang giàu
SELECT *
FROM history_bank
WHERE account_id IN (
    SELECT account_id FROM player
    WHERE JSON_VALID(data_inventory)
      AND CAST(JSON_UNQUOTE(JSON_EXTRACT(data_inventory, '$[0]')) AS UNSIGNED) >= 1000000000
)
ORDER BY id DESC
LIMIT 200;

SELECT *
FROM card_history
WHERE account_id IN (
    SELECT account_id FROM player
    WHERE JSON_VALID(data_inventory)
      AND CAST(JSON_UNQUOTE(JSON_EXTRACT(data_inventory, '$[0]')) AS UNSIGNED) >= 1000000000
)
ORDER BY id DESC
LIMIT 200;
