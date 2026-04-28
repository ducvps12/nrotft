-- =====================================================
-- LỆNH ALLOW IP VÀO MYSQL SERVER
-- Chạy trên server database (103.157.204.182)
-- =====================================================

-- Cách 1: Tạo user mới cho IP cụ thể (Khuyến nghị)
CREATE USER 'root'@'116.98.201.250' IDENTIFIED BY 'Nro@2026!';
GRANT ALL PRIVILEGES ON nrotft.* TO 'root'@'116.98.201.250';
FLUSH PRIVILEGES;

-- Cách 2: Cho phép root kết nối từ IP cụ thể
GRANT ALL PRIVILEGES ON *.* TO 'root'@'116.98.201.250' IDENTIFIED BY 'Nro@2026!' WITH GRANT OPTION;
FLUSH PRIVILEGES;

-- Cách 3: Cho phép root kết nối từ bất kỳ IP nào (KHÔNG AN TOÀN)
-- GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' IDENTIFIED BY 'Nro@2026!' WITH GRANT OPTION;
-- FLUSH PRIVILEGES;

-- Kiểm tra user đã được tạo chưa
SELECT User, Host FROM mysql.user WHERE User = 'root';

-- Xem quyền của user
SHOW GRANTS FOR 'root'@'116.98.201.250';
