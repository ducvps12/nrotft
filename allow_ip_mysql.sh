#!/bin/bash
# =====================================================
# SCRIPT ALLOW IP VÀO MYSQL SERVER
# Chạy trên server Linux (103.157.204.182)
# =====================================================

echo "╔════════════════════════════════════════════════════════╗"
echo "║   ALLOW IP VÀO MYSQL SERVER                            ║"
echo "╚════════════════════════════════════════════════════════╝"
echo ""

# IP cần allow
IP_TO_ALLOW="116.98.201.250"
DB_PASSWORD="Nro@2026!"

echo "[*] IP cần allow: $IP_TO_ALLOW"
echo ""

# Bước 1: Kiểm tra MySQL đang chạy
echo "[1] Kiểm tra MySQL service..."
systemctl status mysql | grep "active (running)"
if [ $? -eq 0 ]; then
    echo "[✓] MySQL đang chạy"
else
    echo "[!] MySQL không chạy, đang khởi động..."
    systemctl start mysql
fi
echo ""

# Bước 2: Cấu hình MySQL cho phép remote connection
echo "[2] Cấu hình MySQL cho phép remote connection..."

# Backup file cấu hình
cp /etc/mysql/mysql.conf.d/mysqld.cnf /etc/mysql/mysql.conf.d/mysqld.cnf.backup.$(date +%Y%m%d_%H%M%S)

# Sửa bind-address
sed -i 's/bind-address.*=.*127.0.0.1/bind-address = 0.0.0.0/' /etc/mysql/mysql.conf.d/mysqld.cnf

# Hoặc comment dòng bind-address
# sed -i 's/^bind-address/#bind-address/' /etc/mysql/mysql.conf.d/mysqld.cnf

echo "[✓] Đã sửa bind-address = 0.0.0.0"
echo ""

# Bước 3: Tạo user cho IP cụ thể
echo "[3] Tạo user MySQL cho IP: $IP_TO_ALLOW..."

mysql -u root -p"$DB_PASSWORD" <<EOF
-- Tạo user cho IP cụ thể
CREATE USER IF NOT EXISTS 'root'@'$IP_TO_ALLOW' IDENTIFIED BY '$DB_PASSWORD';
GRANT ALL PRIVILEGES ON nrotft.* TO 'root'@'$IP_TO_ALLOW';
FLUSH PRIVILEGES;

-- Kiểm tra
SELECT User, Host FROM mysql.user WHERE Host = '$IP_TO_ALLOW';
EOF

echo "[✓] Đã tạo user cho IP: $IP_TO_ALLOW"
echo ""

# Bước 4: Cấu hình firewall
echo "[4] Cấu hình firewall..."

# UFW (Ubuntu/Debian)
if command -v ufw &> /dev/null; then
    echo "[*] Sử dụng UFW..."
    ufw allow from $IP_TO_ALLOW to any port 3306
    ufw reload
    echo "[✓] Đã allow port 3306 cho IP: $IP_TO_ALLOW"
fi

# Firewalld (CentOS/RHEL)
if command -v firewall-cmd &> /dev/null; then
    echo "[*] Sử dụng Firewalld..."
    firewall-cmd --permanent --add-rich-rule="rule family='ipv4' source address='$IP_TO_ALLOW' port protocol='tcp' port='3306' accept"
    firewall-cmd --reload
    echo "[✓] Đã allow port 3306 cho IP: $IP_TO_ALLOW"
fi

# iptables (fallback)
if ! command -v ufw &> /dev/null && ! command -v firewall-cmd &> /dev/null; then
    echo "[*] Sử dụng iptables..."
    iptables -A INPUT -p tcp -s $IP_TO_ALLOW --dport 3306 -j ACCEPT
    iptables-save > /etc/iptables/rules.v4
    echo "[✓] Đã allow port 3306 cho IP: $IP_TO_ALLOW"
fi

echo ""

# Bước 5: Restart MySQL
echo "[5] Restart MySQL service..."
systemctl restart mysql
sleep 2

if systemctl is-active --quiet mysql; then
    echo "[✓] MySQL đã restart thành công"
else
    echo "[!] Lỗi: MySQL không khởi động được"
    echo "Kiểm tra log: journalctl -u mysql -n 50"
    exit 1
fi
echo ""

# Bước 6: Test kết nối
echo "[6] Test kết nối từ IP: $IP_TO_ALLOW..."
echo ""
echo "Chạy lệnh sau từ máy client (IP: $IP_TO_ALLOW):"
echo ""
echo "  mysql -h 103.157.204.182 -u root -p'$DB_PASSWORD' -P 3306 -e \"SELECT 'Connected!' as status;\""
echo ""
echo "Hoặc:"
echo ""
echo "  telnet 103.157.204.182 3306"
echo ""

# Hiển thị thông tin
echo "╔════════════════════════════════════════════════════════╗"
echo "║   HOÀN TẤT CẤU HÌNH                                    ║"
echo "╚════════════════════════════════════════════════════════╝"
echo ""
echo "Thông tin kết nối:"
echo "  Host: 103.157.204.182"
echo "  Port: 3306"
echo "  User: root"
echo "  Password: $DB_PASSWORD"
echo "  Allowed IP: $IP_TO_ALLOW"
echo ""
echo "Kiểm tra user:"
echo "  mysql -u root -p -e \"SELECT User, Host FROM mysql.user WHERE Host = '$IP_TO_ALLOW';\""
echo ""
echo "Kiểm tra firewall:"
echo "  ufw status | grep 3306"
echo "  firewall-cmd --list-all | grep 3306"
echo "  iptables -L -n | grep 3306"
echo ""
