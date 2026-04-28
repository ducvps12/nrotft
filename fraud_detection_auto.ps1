# =====================================================
# SCRIPT PHÁT HIỆN VÀ BAN GIAN LẬN TỰ ĐỘNG
# Kết nối database + Phát hiện + Xác nhận + Ban
# =====================================================

param(
    [switch]$AutoBan = $false,
    [switch]$CheckBuaX2 = $true
)

# Cấu hình database
$dbHost = "103.157.204.182"
$dbUser = "root"
$dbPass = "Nro@2026!"
$dbName = "nrotft"
$dbPort = 3306

# Màu sắc
$colors = @{
    "Success" = "Green"
    "Warning" = "Yellow"
    "Error" = "Red"
    "Info" = "Cyan"
    "Header" = "Magenta"
}

function Write-ColorOutput {
    param(
        [string]$Message,
        [string]$Color = "White"
    )
    Write-Host $Message -ForegroundColor $Color
}

function Test-DatabaseConnection {
    Write-ColorOutput "`n=== KIỂM TRA KẾT NỐI DATABASE ===" $colors["Header"]
    
    try {
        $connectionString = "Server=$dbHost;Port=$dbPort;Uid=$dbUser;Pwd=$dbPass;Database=$dbName;"
        $connection = New-Object MySql.Data.MySqlClient.MySqlConnection($connectionString)
        $connection.Open()
        
        Write-ColorOutput "✓ Kết nối thành công!" $colors["Success"]
        Write-ColorOutput "  Host: $dbHost" $colors["Info"]
        Write-ColorOutput "  Database: $dbName" $colors["Info"]
        
        $connection.Close()
        return $true
    }
    catch {
        Write-ColorOutput "✗ Lỗi kết nối: $_" $colors["Error"]
        return $false
    }
}

function Execute-Query {
    param(
        [string]$Query,
        [string]$Description = ""
    )
    
    try {
        $connectionString = "Server=$dbHost;Port=$dbPort;Uid=$dbUser;Pwd=$dbPass;Database=$dbName;"
        $connection = New-Object MySql.Data.MySqlClient.MySqlConnection($connectionString)
        $connection.Open()
        
        $command = $connection.CreateCommand()
        $command.CommandText = $Query
        $command.CommandTimeout = 300
        
        $adapter = New-Object MySql.Data.MySqlClient.MySqlDataAdapter($command)
        $dataSet = New-Object System.Data.DataSet
        $adapter.Fill($dataSet) | Out-Null
        
        $connection.Close()
        
        return $dataSet.Tables[0]
    }
    catch {
        Write-ColorOutput "✗ Lỗi thực thi query: $_" $colors["Error"]
        return $null
    }
}

function Quick-Check-Fraud {
    Write-ColorOutput "`n=== KIỂM TRA NHANH TÀI KHOẢN NGHI NGỜ ===" $colors["Header"]
    
    $queries = @(
        @{
            Name = "Danap = 0 nhưng có giao dịch"
            Query = "SELECT COUNT(*) as total FROM account a WHERE a.danap = 0 AND a.ban = 0 AND a.id IN (SELECT DISTINCT account_id FROM transaction_history WHERE type = 'RECHARGE' LIMIT 1000);"
        },
        @{
            Name = "Cùng IP (>2 tài khoản)"
            Query = "SELECT COUNT(DISTINCT ip_address) as total FROM account WHERE ip_address IS NOT NULL AND ip_address != '' AND ip_address != '127.0.0.1' AND ban = 0 GROUP BY ip_address HAVING COUNT(*) > 2;"
        },
        @{
            Name = "Giao dịch bất thường 24h"
            Query = "SELECT COUNT(DISTINCT account_id) as total FROM transaction_history WHERE created_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR) AND type IN ('TRANSFER_GOLD', 'TRANSFER_GEM') GROUP BY account_id HAVING COUNT(*) > 10;"
        },
        @{
            Name = "Cash/VND bất thường"
            Query = "SELECT COUNT(*) as total FROM account WHERE (cash < 0 OR vnd < 0 OR (cash > 1000000 AND danap = 0)) AND ban = 0;"
        }
    )
    
    foreach ($q in $queries) {
        $result = Execute-Query $q.Query
        if ($result) {
            $count = $result.Rows[0]["total"]
            Write-ColorOutput "  • $($q.Name): $count" $colors["Warning"]
        }
    }
}

function Detect-Fraud-Accounts {
    Write-ColorOutput "`n=== PHÁT HIỆN TÀI KHOẢN GIAN LẬN ===" $colors["Header"]
    
    # Đọc script phát hiện
    $detectScript = Get-Content "sql/detect_and_ban_fraud_accounts.sql" -Raw
    
    Write-ColorOutput "Đang phát hiện... (có thể mất vài phút)" $colors["Info"]
    
    # Tách các query
    $queries = $detectScript -split ";" | Where-Object { $_.Trim() -and -not $_.Trim().StartsWith("--") }
    
    $count = 0
    foreach ($query in $queries) {
        if ($query.Trim()) {
            $result = Execute-Query $query
            $count++
            if ($count % 5 -eq 0) {
                Write-ColorOutput "  ✓ Đã xử lý $count bước" $colors["Info"]
            }
        }
    }
    
    Write-ColorOutput "✓ Phát hiện hoàn tất!" $colors["Success"]
}

function Check-Bua-X2-De {
    Write-ColorOutput "`n=== KIỂM TRA BÙA X2 ĐỆ ===" $colors["Header"]
    
    $query = @"
SELECT 
    p.id,
    p.name,
    a.username,
    a.ip_address,
    p.data_item_time,
    CASE 
        WHEN p.data_item_time LIKE '%x2%' THEN 'CÓ BÙA X2'
        WHEN p.data_item_time LIKE '%x3%' THEN 'CÓ BÙA X3'
        ELSE 'KHÔNG CÓ BÙA'
    END as bua_status
FROM player p
INNER JOIN account a ON a.id = p.account_id
WHERE a.ban = 0
LIMIT 20;
"@
    
    $result = Execute-Query $query
    
    if ($result -and $result.Rows.Count -gt 0) {
        Write-ColorOutput "Danh sách người chơi với bùa:" $colors["Info"]
        foreach ($row in $result.Rows) {
            $status = $row["bua_status"]
            $color = if ($status -like "*CÓ*") { $colors["Warning"] } else { $colors["Success"] }
            Write-ColorOutput "  • $($row['name']) ($($row['username'])): $status" $color
        }
    }
}

function Show-Suspicious-Accounts {
    Write-ColorOutput "`n=== DANH SÁCH TÀI KHOẢN NGHI NGỜ ===" $colors["Header"]
    
    $query = @"
SELECT 
    tier,
    COUNT(*) as total,
    GROUP_CONCAT(username SEPARATOR ', ') as usernames
FROM temp_suspicious_accounts
GROUP BY tier
ORDER BY tier;
"@
    
    $result = Execute-Query $query
    
    if ($result -and $result.Rows.Count -gt 0) {
        $totalBan = 0
        foreach ($row in $result.Rows) {
            $tier = $row["tier"]
            $total = $row["total"]
            $totalBan += $total
            
            $tierName = @{
                0 = "Tier 0 (Gốc)"
                1 = "Tier 1 (Cùng IP)"
                2 = "Tier 2 (Giao dịch)"
                3 = "Tier 3 (Cảnh báo)"
            }[$tier]
            
            Write-ColorOutput "  $tierName: $total tài khoản" $colors["Warning"]
        }
        
        Write-ColorOutput "`nTổng cộng: $totalBan tài khoản nghi ngờ" $colors["Error"]
        return $totalBan
    }
    
    return 0
}

function Confirm-Ban {
    Write-ColorOutput "`n=== XÁC NHẬN THỰC THI BAN ===" $colors["Header"]
    
    Write-ColorOutput "⚠️  CẢNH BÁO: Bạn sắp ban các tài khoản gian lận!" $colors["Error"]
    Write-ColorOutput "Hành động này KHÔNG THỂ HOÀN TÁC!" $colors["Error"]
    
    $response = Read-Host "`nBạn có chắc chắn muốn tiếp tục? (yes/no)"
    
    if ($response -eq "yes") {
        return $true
    } else {
        Write-ColorOutput "Đã hủy thực thi ban." $colors["Info"]
        return $false
    }
}

function Execute-Ban {
    Write-ColorOutput "`n=== THỰC THI BAN ===" $colors["Header"]
    
    $banScript = Get-Content "sql/execute_ban_fraud_accounts.sql" -Raw
    
    Write-ColorOutput "Đang ban tài khoản... (có thể mất vài phút)" $colors["Info"]
    
    $queries = $banScript -split ";" | Where-Object { $_.Trim() -and -not $_.Trim().StartsWith("--") }
    
    $count = 0
    foreach ($query in $queries) {
        if ($query.Trim()) {
            $result = Execute-Query $query
            $count++
            if ($count % 5 -eq 0) {
                Write-ColorOutput "  ✓ Đã xử lý $count bước" $colors["Info"]
            }
        }
    }
    
    Write-ColorOutput "✓ Ban hoàn tất!" $colors["Success"]
}

function Show-Ban-Summary {
    Write-ColorOutput "`n=== THỐNG KÊ KẾT QUẢ ===" $colors["Header"]
    
    $query = @"
SELECT 
    COUNT(*) as total_banned,
    SUM(CASE WHEN ban_time >= DATE_SUB(NOW(), INTERVAL 1 HOUR) THEN 1 ELSE 0 END) as banned_last_hour
FROM account
WHERE ban = 1;
"@
    
    $result = Execute-Query $query
    
    if ($result -and $result.Rows.Count -gt 0) {
        $totalBanned = $result.Rows[0]["total_banned"]
        $bannedLastHour = $result.Rows[0]["banned_last_hour"]
        
        Write-ColorOutput "Tổng tài khoản đã ban: $totalBanned" $colors["Success"]
        Write-ColorOutput "Ban trong 1 giờ qua: $bannedLastHour" $colors["Success"]
    }
}

# =====================================================
# MAIN EXECUTION
# =====================================================

Write-ColorOutput "╔════════════════════════════════════════════════════════╗" $colors["Header"]
Write-ColorOutput "║   HỆ THỐNG PHÁT HIỆN VÀ BAN GIAN LẬN TỰ ĐỘNG          ║" $colors["Header"]
Write-ColorOutput "║   NRO TFT - $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')                    ║" $colors["Header"]
Write-ColorOutput "╚════════════════════════════════════════════════════════╝" $colors["Header"]

# Kiểm tra kết nối
if (-not (Test-DatabaseConnection)) {
    Write-ColorOutput "`n✗ Không thể kết nối database. Thoát." $colors["Error"]
    exit 1
}

# Kiểm tra nhanh
Quick-Check-Fraud

# Phát hiện gian lận
Detect-Fraud-Accounts

# Kiểm tra bùa x2 đệ
if ($CheckBuaX2) {
    Check-Bua-X2-De
}

# Hiển thị danh sách
$totalSuspicious = Show-Suspicious-Accounts

# Xác nhận và ban
if ($totalSuspicious -gt 0) {
    if ($AutoBan -or (Confirm-Ban)) {
        Execute-Ban
        Show-Ban-Summary
        
        Write-ColorOutput "`n⚠️  Hãy RESTART SERVER để kick người chơi đang online!" $colors["Warning"]
    }
} else {
    Write-ColorOutput "`n✓ Không phát hiện tài khoản gian lận!" $colors["Success"]
}

Write-ColorOutput "`n✓ Hoàn tất!" $colors["Success"]
