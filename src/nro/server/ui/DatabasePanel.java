package nro.server.ui;

import jdbc.DBConnecter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Panel Quản Lý Database - Hiển thị thông tin kết nối, danh sách bảng,
 * và các công cụ backup/optimize database.
 */
public class DatabasePanel extends JPanel {

    private DefaultTableModel tableModel;
    private JLabel lblDbHost, lblDbName, lblDbPort, lblDbStatus;
    private JLabel lblTotalTables, lblTotalRecords, lblDbSize;
    private JTextArea txtLog;
    private JTable tableList;

    public DatabasePanel() {
        setLayout(new BorderLayout(0, 10));
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel topSection = new JPanel(new BorderLayout(0, 10));
        topSection.setOpaque(false);
        topSection.add(createConnectionInfoPanel(), BorderLayout.NORTH);
        topSection.add(createStatsPanel(), BorderLayout.CENTER);

        JPanel mainContent = new JPanel(new BorderLayout(0, 10));
        mainContent.setOpaque(false);
        mainContent.add(createTableListPanel(), BorderLayout.CENTER);
        mainContent.add(createActionsPanel(), BorderLayout.SOUTH);

        add(topSection, BorderLayout.NORTH);
        add(mainContent, BorderLayout.CENTER);
        add(createLogPanel(), BorderLayout.SOUTH);

        // Auto-load on init
        SwingUtilities.invokeLater(this::refreshDatabaseInfo);
    }

    // ===== Connection Info Panel =====
    private JPanel createConnectionInfoPanel() {
        JPanel p = new JPanel(new GridLayout(1, 4, 15, 0));
        p.setOpaque(false);
        p.setBorder(ServerGuiUtils.createSectionBorder("🔗 Thông Tin Kết Nối Database"));

        lblDbHost = createInfoLabel("Host", "Loading...");
        lblDbName = createInfoLabel("Database", "Loading...");
        lblDbPort = createInfoLabel("Port", "Loading...");
        lblDbStatus = createInfoLabel("Trạng thái", "Checking...");

        p.add(lblDbHost);
        p.add(lblDbName);
        p.add(lblDbPort);
        p.add(lblDbStatus);

        // Load from config
        try {
            Properties props = new Properties();
            props.load(new FileInputStream("data/config/config.properties"));
            lblDbHost.setText("<html><b>Host:</b> " + props.getProperty("database.host", "N/A") + "</html>");
            lblDbName.setText("<html><b>Database:</b> " + props.getProperty("database.name", "N/A") + "</html>");
            lblDbPort.setText("<html><b>Port:</b> " + props.getProperty("database.port", "N/A") + "</html>");
        } catch (Exception e) {
            log("Không thể đọc config: " + e.getMessage());
        }

        return p;
    }

    private JLabel createInfoLabel(String title, String value) {
        JLabel lbl = new JLabel("<html><b>" + title + ":</b> " + value + "</html>");
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lbl.setForeground(new Color(60, 60, 60));
        return lbl;
    }

    // ===== Stats Panel =====
    private JPanel createStatsPanel() {
        JPanel p = new JPanel(new GridLayout(1, 3, 15, 0));
        p.setOpaque(false);
        p.setBorder(ServerGuiUtils.createSectionBorder("📊 Thống Kê Database"));

        lblTotalTables = createStatCard("Tổng Bảng", "0", new Color(0, 120, 215));
        lblTotalRecords = createStatCard("Tổng Bản Ghi", "0", new Color(40, 167, 69));
        lblDbSize = createStatCard("Dung Lượng", "N/A", new Color(255, 152, 0));

        p.add(lblTotalTables);
        p.add(lblTotalRecords);
        p.add(lblDbSize);

        return p;
    }

    private JLabel createStatCard(String title, String value, Color color) {
        JLabel lbl = new JLabel("<html><div style='text-align:center;'>"
                + "<span style='font-size:11px;color:#888;'>" + title + "</span><br>"
                + "<span style='font-size:18px;color:" + toHex(color) + ";font-weight:bold;'>" + value + "</span>"
                + "</div></html>", SwingConstants.CENTER);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lbl.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 230, 230), 1),
                new EmptyBorder(10, 10, 10, 10)));
        lbl.setOpaque(true);
        lbl.setBackground(Color.WHITE);
        return lbl;
    }

    private String toHex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    // ===== Table List Panel =====
    private JPanel createTableListPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 5));
        p.setOpaque(false);
        p.setBorder(ServerGuiUtils.createSectionBorder("📋 Danh Sách Bảng"));

        String[] cols = {"#", "Tên Bảng", "Số Bản Ghi", "Dung Lượng (KB)", "Engine", "Collation"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tableList = new JTable(tableModel);
        tableList.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tableList.setRowHeight(26);
        tableList.setShowGrid(true);
        tableList.setGridColor(new Color(240, 240, 240));
        tableList.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tableList.getTableHeader().setBackground(new Color(0, 120, 215));
        tableList.getTableHeader().setForeground(Color.WHITE);
        tableList.setSelectionBackground(new Color(230, 242, 255));
        tableList.getColumnModel().getColumn(0).setPreferredWidth(40);
        tableList.getColumnModel().getColumn(1).setPreferredWidth(180);
        tableList.getColumnModel().getColumn(2).setPreferredWidth(100);
        tableList.getColumnModel().getColumn(3).setPreferredWidth(120);
        tableList.getColumnModel().getColumn(4).setPreferredWidth(80);
        tableList.getColumnModel().getColumn(5).setPreferredWidth(150);

        // Color renderer for row count
        tableList.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected && value != null) {
                    try {
                        long count = Long.parseLong(value.toString().replaceAll("[^0-9]", ""));
                        if (count > 10000) {
                            c.setForeground(new Color(220, 53, 69));
                            c.setFont(new Font("Segoe UI", Font.BOLD, 12));
                        } else if (count > 1000) {
                            c.setForeground(new Color(255, 152, 0));
                        } else {
                            c.setForeground(new Color(40, 167, 69));
                        }
                    } catch (Exception ignored) {
                        c.setForeground(Color.DARK_GRAY);
                    }
                }
                setHorizontalAlignment(SwingConstants.RIGHT);
                return c;
            }
        });

        JScrollPane scroll = new JScrollPane(tableList);
        scroll.setPreferredSize(new Dimension(0, 300));

        // Top bar with search and refresh
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        topBar.setOpaque(false);

        JTextField txtSearch = new JTextField(20);
        txtSearch.setBorder(BorderFactory.createTitledBorder("Tìm bảng..."));
        txtSearch.getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override
            public void update() {
                filterTables(txtSearch.getText());
            }
        });

        JButton btnRefresh = ServerGuiUtils.createStyledButton("🔄 Làm Mới", new Color(0, 120, 215), Color.WHITE);
        btnRefresh.addActionListener(e -> refreshDatabaseInfo());

        topBar.add(txtSearch);
        topBar.add(btnRefresh);

        p.add(topBar, BorderLayout.NORTH);
        p.add(scroll, BorderLayout.CENTER);

        return p;
    }

    // ===== Actions Panel =====
    private JPanel createActionsPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        p.setOpaque(false);
        p.setBorder(ServerGuiUtils.createSectionBorder("⚡ Công Cụ Database"));

        JButton btnOptimize = ServerGuiUtils.createStyledButton("🔧 Optimize Tables", new Color(40, 167, 69), Color.WHITE);
        btnOptimize.addActionListener(e -> optimizeTables());

        JButton btnBackup = ServerGuiUtils.createStyledButton("💾 Backup Database", new Color(0, 123, 255), Color.WHITE);
        btnBackup.addActionListener(e -> backupDatabase());

        JButton btnTruncateLog = ServerGuiUtils.createStyledButton("🗑 Xóa History Cũ", new Color(220, 53, 69), Color.WHITE);
        btnTruncateLog.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this,
                    "Xóa tất cả dữ liệu trong bảng history_bank và card_history?",
                    "Xác nhận", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                truncateHistoryTables();
            }
        });

        JButton btnRunQuery = ServerGuiUtils.createStyledButton("📝 Chạy SQL", new Color(108, 117, 125), Color.WHITE);
        btnRunQuery.addActionListener(e -> showRunQueryDialog());

        p.add(btnOptimize);
        p.add(btnBackup);
        p.add(btnTruncateLog);
        p.add(btnRunQuery);

        return p;
    }

    // ===== Log Panel =====
    private JPanel createLogPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(ServerGuiUtils.createSectionBorder("📜 Database Log"));

        txtLog = new JTextArea(4, 50);
        txtLog.setEditable(false);
        txtLog.setFont(new Font("Monospaced", Font.PLAIN, 11));
        txtLog.setBackground(new Color(250, 250, 250));

        JScrollPane scroll = new JScrollPane(txtLog);
        scroll.setPreferredSize(new Dimension(0, 100));
        p.add(scroll, BorderLayout.CENTER);

        return p;
    }

    // ===== Data Loading Logic =====
    private java.util.List<Object[]> allTableData = new java.util.ArrayList<>();

    private void refreshDatabaseInfo() {
        new Thread(() -> {
            int totalRecords = 0;
            long totalSizeKB = 0;
            allTableData.clear();

            try (Connection con = DBConnecter.getConnectionServer()) {
                lblDbStatus.setText("<html><b>Trạng thái:</b> <span style='color:green;'>✅ Đã kết nối</span></html>");

                String dbName = con.getCatalog();
                DatabaseMetaData meta = con.getMetaData();

                // Get table info using INFORMATION_SCHEMA
                String sql = "SELECT TABLE_NAME, TABLE_ROWS, ROUND(DATA_LENGTH/1024, 1) AS size_kb, "
                        + "ENGINE, TABLE_COLLATION "
                        + "FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = ? ORDER BY TABLE_NAME";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, dbName);
                    try (ResultSet rs = ps.executeQuery()) {
                        int idx = 1;
                        while (rs.next()) {
                            String tableName = rs.getString("TABLE_NAME");
                            long rows = rs.getLong("TABLE_ROWS");
                            double sizeKB = rs.getDouble("size_kb");
                            String engine = rs.getString("ENGINE");
                            String collation = rs.getString("TABLE_COLLATION");

                            totalRecords += rows;
                            totalSizeKB += (long) sizeKB;

                            Object[] row = {idx++, tableName, String.format("%,d", rows),
                                    String.format("%.1f", sizeKB), engine, collation};
                            allTableData.add(row);
                        }
                    }
                }

                int finalTotalRecords = totalRecords;
                long finalTotalSizeKB = totalSizeKB;
                int tableCount = allTableData.size();

                SwingUtilities.invokeLater(() -> {
                    tableModel.setRowCount(0);
                    for (Object[] row : allTableData) {
                        tableModel.addRow(row);
                    }

                    lblTotalTables.setText(buildStatHtml("Tổng Bảng", String.valueOf(tableCount), new Color(0, 120, 215)));
                    lblTotalRecords.setText(buildStatHtml("Tổng Bản Ghi", String.format("%,d", finalTotalRecords), new Color(40, 167, 69)));
                    lblDbSize.setText(buildStatHtml("Dung Lượng", formatSize(finalTotalSizeKB), new Color(255, 152, 0)));

                    log("Đã tải thông tin database: " + tableCount + " bảng, " + String.format("%,d", finalTotalRecords) + " bản ghi");
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    lblDbStatus.setText("<html><b>Trạng thái:</b> <span style='color:red;'>❌ Lỗi kết nối</span></html>");
                    log("Lỗi kết nối database: " + e.getMessage());
                });
            }
        }).start();
    }

    private String buildStatHtml(String title, String value, Color color) {
        return "<html><div style='text-align:center;'>"
                + "<span style='font-size:11px;color:#888;'>" + title + "</span><br>"
                + "<span style='font-size:18px;color:" + toHex(color) + ";font-weight:bold;'>" + value + "</span>"
                + "</div></html>";
    }

    private String formatSize(long kb) {
        if (kb > 1024 * 1024) return String.format("%.1f GB", kb / 1024.0 / 1024.0);
        if (kb > 1024) return String.format("%.1f MB", kb / 1024.0);
        return kb + " KB";
    }

    private void filterTables(String search) {
        String lower = search.toLowerCase().trim();
        tableModel.setRowCount(0);
        int idx = 1;
        for (Object[] row : allTableData) {
            String tableName = row[1].toString().toLowerCase();
            if (tableName.contains(lower)) {
                Object[] newRow = row.clone();
                newRow[0] = idx++;
                tableModel.addRow(newRow);
            }
        }
    }

    // ===== Action Methods =====
    private void optimizeTables() {
        log("Bắt đầu optimize tables...");
        new Thread(() -> {
            try (Connection con = DBConnecter.getConnectionServer()) {
                String dbName = con.getCatalog();
                String sql = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = ?";
                java.util.List<String> tables = new java.util.ArrayList<>();
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, dbName);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            tables.add(rs.getString("TABLE_NAME"));
                        }
                    }
                }

                int count = 0;
                for (String table : tables) {
                    try (Statement st = con.createStatement()) {
                        st.execute("OPTIMIZE TABLE `" + table + "`");
                        count++;
                    } catch (Exception e) {
                        log("Lỗi optimize " + table + ": " + e.getMessage());
                    }
                }

                int finalCount = count;
                SwingUtilities.invokeLater(() -> {
                    log("Hoàn thành optimize " + finalCount + "/" + tables.size() + " bảng!");
                    refreshDatabaseInfo();
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> log("Lỗi optimize: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * Tìm đường dẫn mysqldump.exe từ các vị trí phổ biến.
     */
    private String findMysqldumpPath() {
        String[] commonPaths = {
            "c:\\xampp\\mysql\\bin\\mysqldump.exe",
            "c:\\wamp\\bin\\mysql\\mysql8.0.31\\bin\\mysqldump.exe",
            "c:\\wamp64\\bin\\mysql\\mysql8.0.31\\bin\\mysqldump.exe",
            "c:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysqldump.exe",
            "c:\\Program Files\\MySQL\\MySQL Server 5.7\\bin\\mysqldump.exe",
            "c:\\Program Files (x86)\\MySQL\\MySQL Server 8.0\\bin\\mysqldump.exe",
        };
        for (String path : commonPaths) {
            if (new File(path).exists()) {
                return path;
            }
        }
        // Also check WAMP with wildcard
        File wampMysql = new File("c:\\wamp64\\bin\\mysql");
        if (wampMysql.isDirectory()) {
            File[] dirs = wampMysql.listFiles(File::isDirectory);
            if (dirs != null) {
                for (File dir : dirs) {
                    File f = new File(dir, "bin\\mysqldump.exe");
                    if (f.exists()) return f.getAbsolutePath();
                }
            }
        }
        // Fallback: rely on PATH
        return "mysqldump";
    }

    private void backupDatabase() {
        log("Đang thực hiện backup...");
        new Thread(() -> {
            try {
                Properties props = new Properties();
                props.load(new FileInputStream("data/config/config.properties"));
                String dbHost = props.getProperty("database.host", "localhost");
                String dbPort = props.getProperty("database.port", "3306");
                String dbName = props.getProperty("database.name", "nrotft");
                String dbUser = props.getProperty("database.user", "root");
                String dbPass = props.getProperty("database.pass", "");

                // Create backup directory
                File backupDir = new File("backup");
                if (!backupDir.exists()) backupDir.mkdirs();

                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                String fileName = "backup/" + dbName + "_" + timestamp + ".sql";

                // Auto-detect mysqldump path
                String mysqldumpPath = findMysqldumpPath();
                log("Sử dụng mysqldump: " + mysqldumpPath);

                ProcessBuilder pb;
                if (dbPass.isEmpty()) {
                    pb = new ProcessBuilder(mysqldumpPath, "-h", dbHost, "-P", dbPort,
                            "-u", dbUser, "--databases", dbName, "--result-file=" + fileName);
                } else {
                    pb = new ProcessBuilder(mysqldumpPath, "-h", dbHost, "-P", dbPort,
                            "-u", dbUser, "-p" + dbPass, "--databases", dbName, "--result-file=" + fileName);
                }
                pb.redirectErrorStream(true);

                Process process = pb.start();
                String output = new String(process.getInputStream().readAllBytes());
                int exitCode = process.waitFor();

                SwingUtilities.invokeLater(() -> {
                    if (exitCode == 0) {
                        File f = new File(fileName);
                        log("✅ Backup thành công: " + fileName + " (" + formatSize(f.length() / 1024) + ")");
                        JOptionPane.showMessageDialog(this,
                                "Backup thành công!\nFile: " + new File(fileName).getAbsolutePath(),
                                "Backup Database", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        log("❌ Backup lỗi (exit=" + exitCode + "): " + output);
                        JOptionPane.showMessageDialog(this,
                                "Backup thất bại! Kiểm tra log.\n" + output,
                                "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    log("❌ Lỗi backup: " + e.getMessage());
                    JOptionPane.showMessageDialog(this,
                            "Không thể backup: " + e.getMessage() + "\nKiểm tra mysqldump đã cài chưa.",
                            "Lỗi", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    private void truncateHistoryTables() {
        new Thread(() -> {
            try (Connection con = DBConnecter.getConnectionServer()) {
                String[] tables = {"history_bank", "card_history"};
                for (String table : tables) {
                    try (Statement st = con.createStatement()) {
                        int deleted = st.executeUpdate("DELETE FROM `" + table + "`");
                        log("Đã xóa " + deleted + " bản ghi từ " + table);
                    } catch (Exception e) {
                        log("Lỗi xóa " + table + ": " + e.getMessage());
                    }
                }
                SwingUtilities.invokeLater(this::refreshDatabaseInfo);
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> log("Lỗi: " + e.getMessage()));
            }
        }).start();
    }

    private void showRunQueryDialog() {
        JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Chạy SQL Query", true);
        d.setSize(600, 400);
        d.setLocationRelativeTo(this);
        d.setLayout(new BorderLayout(5, 5));

        JTextArea txtQuery = new JTextArea(5, 40);
        txtQuery.setFont(new Font("Monospaced", Font.PLAIN, 13));
        txtQuery.setBorder(BorderFactory.createTitledBorder("SQL Query (chỉ SELECT):"));

        JTextArea txtResult = new JTextArea(8, 40);
        txtResult.setEditable(false);
        txtResult.setFont(new Font("Monospaced", Font.PLAIN, 11));
        txtResult.setBackground(new Color(250, 250, 250));

        JButton btnRun = ServerGuiUtils.createStyledButton("▶ Chạy Query", new Color(40, 167, 69), Color.WHITE);
        btnRun.addActionListener(e -> {
            String query = txtQuery.getText().trim();
            if (query.isEmpty()) return;
            // Only allow SELECT for safety
            if (!query.toUpperCase().startsWith("SELECT")) {
                txtResult.setText("⚠ Chỉ cho phép câu lệnh SELECT để đảm bảo an toàn!");
                return;
            }
            new Thread(() -> {
                try (Connection con = DBConnecter.getConnectionServer();
                     PreparedStatement ps = con.prepareStatement(query);
                     ResultSet rs = ps.executeQuery()) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int colCount = meta.getColumnCount();
                    StringBuilder sb = new StringBuilder();
                    for (int i = 1; i <= colCount; i++) {
                        sb.append(String.format("%-20s", meta.getColumnName(i)));
                    }
                    sb.append("\n").append("-".repeat(colCount * 20)).append("\n");
                    int rowCount = 0;
                    while (rs.next() && rowCount < 100) {
                        for (int i = 1; i <= colCount; i++) {
                            String val = rs.getString(i);
                            sb.append(String.format("%-20s", val != null ? (val.length() > 18 ? val.substring(0, 18) + ".." : val) : "NULL"));
                        }
                        sb.append("\n");
                        rowCount++;
                    }
                    sb.append("\n--- " + rowCount + " rows (max 100) ---");
                    SwingUtilities.invokeLater(() -> txtResult.setText(sb.toString()));
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> txtResult.setText("❌ Lỗi: " + ex.getMessage()));
                }
            }).start();
        });

        d.add(new JScrollPane(txtQuery), BorderLayout.NORTH);
        d.add(new JScrollPane(txtResult), BorderLayout.CENTER);
        d.add(btnRun, BorderLayout.SOUTH);
        d.setVisible(true);
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
