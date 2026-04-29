-- =====================================================
-- SQL Migration: Hệ thống GÓI VIP TUẦN & GÓI ĐỆ TỬ NGÀY
-- Chạy script này trên database server trước khi deploy
-- =====================================================

-- Bảng lưu lịch sử mua gói VIP/Đệ Tử
-- (Table sẽ tự động tạo khi server khởi động qua VipPackageService.initTable())
-- Nhưng chạy script này trước để đảm bảo an toàn

CREATE TABLE IF NOT EXISTS history_vip_purchase (
    id INT AUTO_INCREMENT PRIMARY KEY,
    account_id INT NOT NULL,
    player_id INT NOT NULL,
    package_type VARCHAR(20) NOT NULL COMMENT 'VIP_TUAN hoặc DE_TU_NGAY',
    tier INT NOT NULL COMMENT 'Cấp gói (1-4)',
    price INT NOT NULL COMMENT 'Giá đã thanh toán (VNĐ)',
    purchased_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL,
    INDEX idx_account (account_id),
    INDEX idx_player (player_id),
    INDEX idx_type_expire (package_type, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
