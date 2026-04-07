package nro.server.ui;

import jdbc.DBConnecter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Panel Quản Lý Giao Dịch - Hiển thị lịch sử nạp tiền,
 * thống kê doanh thu, tìm kiếm giao dịch.
 */
public class TransactionPanel extends JPanel {

    private DefaultTableModel transactionTableModel;
    private JLabel lblTotalRevenue, lblTodayRevenue, lblWeekRevenue, lblMonthRevenue;
    private JLabel lblTotalTransactions, lblPendingCount;
    private JTextArea txtLog;
    private JTextField txtSearchUser, txtSearchDate;

    public TransactionPanel() {
        setLayout(new BorderLayout(0, 10));
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(15, 15, 15, 15));

        add(createRevenueCardsPanel(), BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        centerPanel.setOpaque(false);
        centerPanel.add(createSearchPanel(), BorderLayout.NORTH);
        centerPanel.add(createTransactionTablePanel(), BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);
        add(createLogPanel(), BorderLayout.SOUTH);

        // Auto-load
        SwingUtilities.invokeLater(this::refreshTransactions);
    }

    // ===== Revenue Stats =====
    private JPanel createRevenueCardsPanel() {
        JPanel p = new JPanel(new GridLayout(1, 6, 8, 0));
        p.setOpaque(false);
        p.setBorder(ServerGuiUtils.createSectionBorder("💰 Thống Kê Doanh Thu"));

        lblTotalRevenue = createStatCard("Tổng Doanh Thu", "0₫", new Color(0, 120, 215));
        lblTodayRevenue = createStatCard("Hôm Nay", "0₫", new Color(40, 167, 69));
        lblWeekRevenue = createStatCard("7 Ngày", "0₫", new Color(142, 68, 173));
        lblMonthRevenue = createStatCard("30 Ngày", "0₫", new Color(255, 152, 0));
        lblTotalTransactions = createStatCard("Tổng Giao Dịch", "0", new Color(108, 117, 125));
        lblPendingCount = createStatCard("Chờ Duyệt", "0", new Color(220, 53, 69));

        p.add(lblTotalRevenue);
        p.add(lblTodayRevenue);
        p.add(lblWeekRevenue);
        p.add(lblMonthRevenue);
        p.add(lblTotalTransactions);
        p.add(lblPendingCount);

        return p;
    }

    private JLabel createStatCard(String title, String value, Color color) {
        JLabel lbl = new JLabel(buildStatHtml(title, value, color), SwingConstants.CENTER);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 230, 230)),
                new EmptyBorder(8, 5, 8, 5)));
        lbl.setOpaque(true);
        lbl.setBackground(Color.WHITE);
        return lbl;
    }

    private String buildStatHtml(String title, String value, Color color) {
        return "<html><div style='text-align:center;'>"
                + "<span style='font-size:10px;color:#888;'>" + title + "</span><br>"
                + "<span style='font-size:15px;color:" + toHex(color) + ";font-weight:bold;'>" + value + "</span>"
                + "</div></html>";
    }

    private String toHex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    // ===== Search Panel =====
    private JPanel createSearchPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        p.setOpaque(false);
        p.setBorder(ServerGuiUtils.createSectionBorder("🔍 Tìm Kiếm Giao Dịch"));

        p.add(new JLabel("Username:"));
        txtSearchUser = new JTextField(15);
        txtSearchUser.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        p.add(txtSearchUser);

        p.add(new JLabel("Từ ngày (yyyy-MM-dd):"));
        txtSearchDate = new JTextField(12);
        txtSearchDate.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtSearchDate.setText(LocalDateTime.now().minusDays(30).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        p.add(txtSearchDate);

        JButton btnSearch = ServerGuiUtils.createStyledButton("🔎 Tìm Kiếm", new Color(0, 120, 215), Color.WHITE);
        btnSearch.addActionListener(e -> searchTransactions());
        p.add(btnSearch);

        JButton btnRefresh = ServerGuiUtils.createStyledButton("🔄 Tải Lại", new Color(40, 167, 69), Color.WHITE);
        btnRefresh.addActionListener(e -> refreshTransactions());
        p.add(btnRefresh);

        JButton btnExport = ServerGuiUtils.createStyledButton("📤 Export CSV", new Color(108, 117, 125), Color.WHITE);
        btnExport.addActionListener(e -> exportToCSV());
        p.add(btnExport);

        return p;
    }

    // ===== Transaction Table =====
    private JPanel createTransactionTablePanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(ServerGuiUtils.createSectionBorder("📋 Lịch Sử Giao Dịch"));

        String[] cols = {"#", "Username", "Số Tiền (₫)", "Nội Dung CK", "Ngân Hàng", "Trạng Thái", "Thời Gian"};
        transactionTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        JTable table = new JTable(transactionTableModel);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setRowHeight(26);
        table.setShowGrid(true);
        table.setGridColor(new Color(240, 240, 240));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(40, 167, 69));
        table.getTableHeader().setForeground(Color.WHITE);
        table.setSelectionBackground(new Color(232, 245, 233));

        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(110);
        table.getColumnModel().getColumn(2).setPreferredWidth(120);
        table.getColumnModel().getColumn(3).setPreferredWidth(180);
        table.getColumnModel().getColumn(4).setPreferredWidth(80);
        table.getColumnModel().getColumn(5).setPreferredWidth(90);
        table.getColumnModel().getColumn(6).setPreferredWidth(140);

        // Color renderer for amount column
        table.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, val, sel, focus, r, c);
                if (!sel) {
                    comp.setForeground(new Color(40, 167, 69));
                    comp.setFont(new Font("Segoe UI", Font.BOLD, 12));
                }
                setHorizontalAlignment(SwingConstants.RIGHT);
                return comp;
            }
        });

        // Color renderer for status column
        table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, val, sel, focus, r, c);
                if (!sel && val != null) {
                    String status = val.toString();
                    if (status.contains("Thành công") || status.contains("success") || status.contains("1")) {
                        comp.setForeground(new Color(40, 167, 69));
                    } else if (status.contains("Chờ") || status.contains("pending") || status.contains("0")) {
                        comp.setForeground(new Color(255, 152, 0));
                    } else {
                        comp.setForeground(new Color(220, 53, 69));
                    }
                    comp.setFont(new Font("Segoe UI", Font.BOLD, 12));
                }
                return comp;
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        p.add(scroll, BorderLayout.CENTER);

        return p;
    }

    // ===== Log Panel =====
    private JPanel createLogPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(ServerGuiUtils.createSectionBorder("📜 Transaction Log"));

        txtLog = new JTextArea(3, 50);
        txtLog.setEditable(false);
        txtLog.setFont(new Font("Monospaced", Font.PLAIN, 11));
        txtLog.setBackground(new Color(250, 250, 250));

        JScrollPane scroll = new JScrollPane(txtLog);
        scroll.setPreferredSize(new Dimension(0, 80));
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    // ===== Data Loading =====
    private void refreshTransactions() {
        new Thread(() -> {
            try (Connection con = DBConnecter.getConnectionServer()) {
                // Try different table names (bank_transactions or history_bank)
                String tableName = findTransactionTable(con);
                if (tableName == null) {
                    SwingUtilities.invokeLater(() -> log("Không tìm thấy bảng giao dịch (bank_transactions / history_bank)"));
                    return;
                }

                // Get column info to adapt queries
                java.util.List<String> columns = getTableColumns(con, tableName);
                log("Bảng giao dịch: " + tableName + " | Cột: " + columns);

                // Load transactions
                loadTransactionData(con, tableName, columns, null, null);

                // Load revenue stats
                loadRevenueStats(con, tableName, columns);

            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> log("Lỗi: " + e.getMessage()));
            }
        }).start();
    }

    private String findTransactionTable(Connection con) {
        String[] candidates = {"bank_transactions", "history_bank", "bank_history", "bank_transfers", "atm_check"};
        try {
            DatabaseMetaData meta = con.getMetaData();
            for (String table : candidates) {
                try (ResultSet rs = meta.getTables(null, null, table, null)) {
                    if (rs.next()) return table;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private java.util.List<String> getTableColumns(Connection con, String tableName) {
        java.util.List<String> cols = new java.util.ArrayList<>();
        try {
            DatabaseMetaData meta = con.getMetaData();
            try (ResultSet rs = meta.getColumns(null, null, tableName, null)) {
                while (rs.next()) {
                    cols.add(rs.getString("COLUMN_NAME").toLowerCase());
                }
            }
        } catch (Exception ignored) {}
        return cols;
    }

    private void loadTransactionData(Connection con, String tableName, java.util.List<String> columns,
                                      String filterUser, String filterDate) {
        try {
            // Build adaptive query
            String userCol = findColumn(columns, "username", "user", "account", "player");
            String amountCol = findColumn(columns, "amount", "money", "so_tien", "sotien", "transferAmount");
            String contentCol = findColumn(columns, "content", "description", "noi_dung", "noidung", "transferContent", "note");
            String bankCol = findColumn(columns, "bank", "ngan_hang", "gateway", "bankName");
            String statusCol = findColumn(columns, "status", "trang_thai", "state", "processed");
            String timeCol = findColumn(columns, "time", "created_at", "date", "created", "ngay", "transactionDate", "create_time");

            StringBuilder sql = new StringBuilder("SELECT ");
            sql.append(userCol != null ? "`" + userCol + "`" : "'N/A'").append(" as username, ");
            sql.append(amountCol != null ? "`" + amountCol + "`" : "0").append(" as amount, ");
            sql.append(contentCol != null ? "`" + contentCol + "`" : "'N/A'").append(" as content, ");
            sql.append(bankCol != null ? "`" + bankCol + "`" : "'N/A'").append(" as bank, ");
            sql.append(statusCol != null ? "`" + statusCol + "`" : "'N/A'").append(" as status, ");
            sql.append(timeCol != null ? "`" + timeCol + "`" : "NOW()").append(" as time_col ");
            sql.append("FROM `").append(tableName).append("` WHERE 1=1 ");

            if (filterUser != null && !filterUser.trim().isEmpty() && userCol != null) {
                sql.append("AND `").append(userCol).append("` LIKE '%").append(filterUser.trim()).append("%' ");
            }
            if (filterDate != null && !filterDate.trim().isEmpty() && timeCol != null) {
                sql.append("AND `").append(timeCol).append("` >= '").append(filterDate.trim()).append("' ");
            }

            if (timeCol != null) {
                sql.append("ORDER BY `").append(timeCol).append("` DESC ");
            }
            sql.append("LIMIT 500");

            try (PreparedStatement ps = con.prepareStatement(sql.toString());
                 ResultSet rs = ps.executeQuery()) {
                SwingUtilities.invokeLater(() -> transactionTableModel.setRowCount(0));
                int idx = 1;
                while (rs.next()) {
                    String user = rs.getString("username");
                    long amount = 0;
                    try { amount = rs.getLong("amount"); } catch (Exception ignored) {}
                    String content = rs.getString("content");
                    String bank = rs.getString("bank");
                    String status = rs.getString("status");
                    String time = rs.getString("time_col");

                    String statusDisplay;
                    if (status == null) statusDisplay = "N/A";
                    else if (status.equals("1") || status.equalsIgnoreCase("success")) statusDisplay = "✅ Thành công";
                    else if (status.equals("0") || status.equalsIgnoreCase("pending")) statusDisplay = "⏳ Chờ duyệt";
                    else statusDisplay = status;

                    Object[] row = {idx++, user, String.format("%,d", amount),
                            content != null ? content : "N/A", bank != null ? bank : "N/A",
                            statusDisplay, time != null ? time : "N/A"};
                    final Object[] finalRow = row;
                    SwingUtilities.invokeLater(() -> transactionTableModel.addRow(finalRow));
                }
                int finalIdx = idx;
                SwingUtilities.invokeLater(() -> log("Đã tải " + (finalIdx - 1) + " giao dịch từ " + tableName));
            }
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> log("Lỗi đọc giao dịch: " + e.getMessage()));
        }
    }

    private void loadRevenueStats(Connection con, String tableName, java.util.List<String> columns) {
        try {
            String amountCol = findColumn(columns, "amount", "money", "so_tien", "sotien", "transferAmount");
            String timeCol = findColumn(columns, "time", "created_at", "date", "created", "ngay", "transactionDate", "create_time");
            String statusCol = findColumn(columns, "status", "trang_thai", "state", "processed");

            if (amountCol == null) {
                SwingUtilities.invokeLater(() -> log("Không tìm thấy cột amount"));
                return;
            }

            String statusFilter = statusCol != null ? " AND (`" + statusCol + "` = '1' OR `" + statusCol + "` = 'success')" : "";

            // Total revenue
            String sql = "SELECT COALESCE(SUM(`" + amountCol + "`), 0) FROM `" + tableName + "` WHERE 1=1" + statusFilter;
            long totalRevenue = queryLong(con, sql);

            // Today
            String todayFilter = timeCol != null ? " AND DATE(`" + timeCol + "`) = CURDATE()" : "";
            long todayRevenue = queryLong(con, "SELECT COALESCE(SUM(`" + amountCol + "`), 0) FROM `" + tableName + "` WHERE 1=1" + statusFilter + todayFilter);

            // 7 days
            String weekFilter = timeCol != null ? " AND `" + timeCol + "` >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)" : "";
            long weekRevenue = queryLong(con, "SELECT COALESCE(SUM(`" + amountCol + "`), 0) FROM `" + tableName + "` WHERE 1=1" + statusFilter + weekFilter);

            // 30 days
            String monthFilter = timeCol != null ? " AND `" + timeCol + "` >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)" : "";
            long monthRevenue = queryLong(con, "SELECT COALESCE(SUM(`" + amountCol + "`), 0) FROM `" + tableName + "` WHERE 1=1" + statusFilter + monthFilter);

            // Total transactions
            long totalTx = queryLong(con, "SELECT COUNT(*) FROM `" + tableName + "`");

            // Pending
            long pending = 0;
            if (statusCol != null) {
                pending = queryLong(con, "SELECT COUNT(*) FROM `" + tableName + "` WHERE `" + statusCol + "` = '0' OR `" + statusCol + "` = 'pending'");
            }

            long finalPending = pending;
            SwingUtilities.invokeLater(() -> {
                lblTotalRevenue.setText(buildStatHtml("Tổng Doanh Thu", formatMoney(totalRevenue), new Color(0, 120, 215)));
                lblTodayRevenue.setText(buildStatHtml("Hôm Nay", formatMoney(todayRevenue), new Color(40, 167, 69)));
                lblWeekRevenue.setText(buildStatHtml("7 Ngày", formatMoney(weekRevenue), new Color(142, 68, 173)));
                lblMonthRevenue.setText(buildStatHtml("30 Ngày", formatMoney(monthRevenue), new Color(255, 152, 0)));
                lblTotalTransactions.setText(buildStatHtml("Tổng Giao Dịch", String.format("%,d", totalTx), new Color(108, 117, 125)));
                lblPendingCount.setText(buildStatHtml("Chờ Duyệt", String.valueOf(finalPending), new Color(220, 53, 69)));
            });
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> log("Lỗi thống kê doanh thu: " + e.getMessage()));
        }
    }

    private long queryLong(Connection con, String sql) {
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getLong(1);
        } catch (Exception ignored) {}
        return 0;
    }

    private String findColumn(java.util.List<String> columns, String... candidates) {
        for (String candidate : candidates) {
            for (String col : columns) {
                if (col.equalsIgnoreCase(candidate)) return col;
            }
        }
        // Partial match
        for (String candidate : candidates) {
            for (String col : columns) {
                if (col.toLowerCase().contains(candidate.toLowerCase())) return col;
            }
        }
        return null;
    }

    private String formatMoney(long amount) {
        if (amount >= 1_000_000_000) return String.format("%.1fTỷ", amount / 1_000_000_000.0);
        if (amount >= 1_000_000) return String.format("%.1fTr", amount / 1_000_000.0);
        if (amount >= 1_000) return String.format("%,dK", amount / 1_000);
        return String.format("%,d₫", amount);
    }

    // ===== Search =====
    private void searchTransactions() {
        new Thread(() -> {
            try (Connection con = DBConnecter.getConnectionServer()) {
                String tableName = findTransactionTable(con);
                if (tableName == null) {
                    SwingUtilities.invokeLater(() -> log("Không tìm thấy bảng giao dịch"));
                    return;
                }
                java.util.List<String> columns = getTableColumns(con, tableName);
                loadTransactionData(con, tableName, columns,
                        txtSearchUser.getText(), txtSearchDate.getText());
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> log("Lỗi tìm kiếm: " + e.getMessage()));
            }
        }).start();
    }

    // ===== Export CSV =====
    private void exportToCSV() {
        if (transactionTableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Không có dữ liệu để export!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new java.io.File("transactions_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (java.io.PrintWriter pw = new java.io.PrintWriter(fc.getSelectedFile())) {
                // Header
                StringBuilder header = new StringBuilder();
                for (int i = 0; i < transactionTableModel.getColumnCount(); i++) {
                    if (i > 0) header.append(",");
                    header.append("\"").append(transactionTableModel.getColumnName(i)).append("\"");
                }
                pw.println(header);

                // Data
                for (int row = 0; row < transactionTableModel.getRowCount(); row++) {
                    StringBuilder line = new StringBuilder();
                    for (int col = 0; col < transactionTableModel.getColumnCount(); col++) {
                        if (col > 0) line.append(",");
                        Object val = transactionTableModel.getValueAt(row, col);
                        line.append("\"").append(val != null ? val.toString() : "").append("\"");
                    }
                    pw.println(line);
                }

                log("✅ Export thành công: " + fc.getSelectedFile().getAbsolutePath());
                JOptionPane.showMessageDialog(this,
                        "Export thành công!\n" + fc.getSelectedFile().getAbsolutePath(),
                        "Export CSV", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                log("❌ Lỗi export: " + e.getMessage());
            }
        }
    }

    private void log(String message) {
        if (txtLog != null) {
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            SwingUtilities.invokeLater(() -> {
                txtLog.append("[" + time + "] " + message + "\n");
                txtLog.setCaretPosition(txtLog.getDocument().getLength());
            });
        }
    }
}
