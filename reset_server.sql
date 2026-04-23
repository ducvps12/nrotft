-- ====================================================
-- NROTFT SERVER RESET SCRIPT - OPEN SERVER CHÍNH THỨC
-- Backup đã được tạo trước khi chạy script này.
-- ====================================================

-- Tắt foreign key check
SET FOREIGN_KEY_CHECKS = 0;

-- ====================================================
-- BƯỚC 1: TRUNCATE bảng dữ liệu NGƯỜI CHƠI
-- (Reset ID auto_increment về 1)
-- ====================================================

-- Bảng player (nhân vật game)
TRUNCATE TABLE player;

-- Bảng account (tài khoản)
TRUNCATE TABLE account;

-- Bảng clan (bang hội) 
TRUNCATE TABLE clan;

-- Bảng lịch sử giao dịch người chơi
TRUNCATE TABLE bank_transactions;
TRUNCATE TABLE recharge_log;
TRUNCATE TABLE history_bank;
TRUNCATE TABLE history_card;
TRUNCATE TABLE card_history;
TRUNCATE TABLE history_transaction;
TRUNCATE TABLE trans_log;
TRUNCATE TABLE history_items_diemdanh;

-- Bảng ATM/Banking
TRUNCATE TABLE atm_check;
TRUNCATE TABLE atm_lichsu;
TRUNCATE TABLE bank_history;
TRUNCATE TABLE bank_transfers;
TRUNCATE TABLE transaction_banking;
TRUNCATE TABLE buff_vnd_history;
TRUNCATE TABLE mbbank;
TRUNCATE TABLE napthe;
TRUNCATE TABLE naptien;
TRUNCATE TABLE payments;

-- Bảng web user
TRUNCATE TABLE comments;
TRUNCATE TABLE posts;
TRUNCATE TABLE feedback;
TRUNCATE TABLE zalo_users;

-- Bảng blocked IPs (reset)
TRUNCATE TABLE blocked_ips;

-- Bảng admin audit log
TRUNCATE TABLE admin_audit_log;

-- Bảng quỹ đổi
TRUNCATE TABLE hist_quydoitv;

-- Bảng shop ký gửi (của player)
TRUNCATE TABLE shop_ky_gui;

-- Bảng radar (của player)
TRUNCATE TABLE radar;

-- ====================================================
-- BƯỚC 2: TẠO LẠI 3 TÀI KHOẢN ADMIN
-- ID bắt đầu từ 1 (vì đã TRUNCATE)
-- ====================================================

INSERT INTO account (username, password, is_admin, admin, role, vnd, tongnap, cash, ban, active)
VALUES 
('longcute', '1', 1, 1, 99, 0, 0, 0, 0, 0),
('longcutevai', '1', 1, 1, 99, 0, 0, 0, 0, 0),
('longcutevl', '1', 1, 1, 99, 0, 0, 0, 0, 0);

-- ====================================================
-- BƯỚC 3: Bật lại foreign key check
-- ====================================================

SET FOREIGN_KEY_CHECKS = 1;

-- ====================================================
-- DONE! Kết quả:
-- - Tất cả data player/clan/giao dịch đã reset
-- - 3 tài khoản admin đã tạo (ID: 1, 2, 3)
-- - Giftcode, shop template, item template, event... GIỮ NGUYÊN
-- ====================================================

SELECT 'RESET COMPLETE!' AS status;
SELECT id, username, is_admin, admin, role FROM account;
