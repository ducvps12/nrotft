package nro.server.ui;

import jdbc.DBConnecter;
import nro.player.Player;
import nro.server.Client;
import nro.services.Service;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.*;

/**
 * Panel Quản Lý Giao Dịch - ATM Auto Match System
 * 
 * Chức năng:
 * 1. Hiển thị lịch sử giao dịch từ bank_transactions  
 * 2. Auto Cron: Quét giao dịch pending, match nội dung CK với pattern NAP{id}
 * 3. Tự động cộng tiền cho player khi match thành công
 * 4. Cấu hình ATM/Momo
 * 5. Thống kê doanh thu real-time
 * 6. Duyệt thủ công giao dịch
 */
public class TransactionPanel extends JPanel {

    private DefaultTableModel transactionTableModel;
    private JTable transactionTable;
    private JLabel lblTotalRevenue, lblTodayRevenue, lblWeekRevenue, lblMonthRevenue;
    private JLabel lblTotalTransactions, lblPendingCount;
    private JLabel lblCronStatus, lblLastCron, lblCronCount;
    private JTextArea txtLog;
    private JTextField txtSearchUser, txtSearchDate;
    private JToggleButton btnAutoCron;
    private JTextField tfCronInterval;
    
    // Auto Cron
    private ScheduledExecutorService cronScheduler;
    private volatile boolean cronRunning = false;
    private int cronMatchCount = 0;
    
    // ATM config
    private JTextField tfBankName, tfBankAccount, tfAccountHolder, tfAtmPrefix;
    private JTextField tfMomoPhone, tfMomoCuphap, tfCronUrl;
    private JTextField tfHeSoSuKien;
    
    // Refresh timer
    private ScheduledExecutorService refreshTimer;
    
    public TransactionPanel() {
        setLayout(new BorderLayout(0, 10));
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));

        tabs.addTab("📋 Lịch sử giao dịch", createHistoryTab());
        tabs.addTab("⚙ Cấu hình ATM/Momo", createAtmConfigPanel());
        tabs.addTab("🤖 Auto Cron", createAutoCronTab());

        add(tabs, BorderLayout.CENTER);

        // Auto-load & start refresh
        SwingUtilities.invokeLater(this::refreshTransactions);
        startAutoRefresh();
    }

    // ===================================================================
    // TAB 1: LỊCH SỬ GIAO DỊCH
    // ===================================================================
    private JPanel createHistoryTab() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setOpaque(false);

        p.add(createRevenueCardsPanel(), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setOpaque(false);
        center.add(createSearchPanel(), BorderLayout.NORTH);
        center.add(createTransactionTablePanel(), BorderLayout.CENTER);

        p.add(center, BorderLayout.CENTER);
        p.add(createLogPanel(), BorderLayout.SOUTH);

        return p;
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
        
        JButton btnMatchAll = ServerGuiUtils.createStyledButton("🔄 Match Tất Cả Pending", new Color(220, 53, 69), Color.WHITE);
        btnMatchAll.addActionListener(e -> manualMatchAll());
        p.add(btnMatchAll);

        return p;
    }

    // ===== Transaction Table =====
    private JPanel createTransactionTablePanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(ServerGuiUtils.createSectionBorder("📋 Lịch Sử Giao Dịch"));

        String[] cols = {"#", "Username", "Số Tiền (₫)", "Nội Dung CK", "Người Gửi", "Trạng Thái", "Thời Gian", "Hành Động"};
        transactionTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return c == 7; // Only action column editable
            }
        };

        transactionTable = new JTable(transactionTableModel);
        transactionTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        transactionTable.setRowHeight(28);
        transactionTable.setShowGrid(true);
        transactionTable.setGridColor(new Color(240, 240, 240));
        transactionTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        transactionTable.getTableHeader().setBackground(new Color(40, 167, 69));
        transactionTable.getTableHeader().setForeground(Color.WHITE);
        transactionTable.setSelectionBackground(new Color(232, 245, 233));

        transactionTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        transactionTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        transactionTable.getColumnModel().getColumn(2).setPreferredWidth(110);
        transactionTable.getColumnModel().getColumn(3).setPreferredWidth(200);
        transactionTable.getColumnModel().getColumn(4).setPreferredWidth(120);
        transactionTable.getColumnModel().getColumn(5).setPreferredWidth(100);
        transactionTable.getColumnModel().getColumn(6).setPreferredWidth(130);
        transactionTable.getColumnModel().getColumn(7).setPreferredWidth(80);

        // Color renderer for amount column
        transactionTable.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
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
        transactionTable.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, val, sel, focus, r, c);
                if (!sel && val != null) {
                    String status = val.toString();
                    if (status.contains("success")) {
                        comp.setForeground(new Color(40, 167, 69));
                    } else if (status.contains("pending")) {
                        comp.setForeground(new Color(255, 152, 0));
                    } else if (status.contains("ignored")) {
                        comp.setForeground(new Color(220, 53, 69));
                    } else {
                        comp.setForeground(new Color(108, 117, 125));
                    }
                    comp.setFont(new Font("Segoe UI", Font.BOLD, 12));
                }
                return comp;
            }
        });

        // Action button renderer/editor
        transactionTable.getColumnModel().getColumn(7).setCellRenderer(new ButtonRenderer());
        transactionTable.getColumnModel().getColumn(7).setCellEditor(new ButtonEditor(new JCheckBox()));

        JScrollPane scroll = new JScrollPane(transactionTable);
        p.add(scroll, BorderLayout.CENTER);

        // Right-click context menu
        JPopupMenu popup = new JPopupMenu();
        JMenuItem miApprove = new JMenuItem("✅ Duyệt & Cộng tiền");
        miApprove.addActionListener(e -> approveSelectedTransaction());
        popup.add(miApprove);
        
        JMenuItem miIgnore = new JMenuItem("❌ Bỏ qua");
        miIgnore.addActionListener(e -> ignoreSelectedTransaction());
        popup.add(miIgnore);
        
        JMenuItem miDetail = new JMenuItem("📋 Chi tiết");
        miDetail.addActionListener(e -> showTransactionDetail());
        popup.add(miDetail);
        
        transactionTable.setComponentPopupMenu(popup);

        return p;
    }

    // ===== Log Panel =====
    private JPanel createLogPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(ServerGuiUtils.createSectionBorder("📜 Transaction Log"));

        txtLog = new JTextArea(4, 50);
        txtLog.setEditable(false);
        txtLog.setFont(new Font("Consolas", Font.PLAIN, 11));
        txtLog.setBackground(new Color(30, 30, 30));
        txtLog.setForeground(new Color(0, 255, 128));
        txtLog.setCaretColor(new Color(0, 255, 128));

        JScrollPane scroll = new JScrollPane(txtLog);
        scroll.setPreferredSize(new Dimension(0, 100));
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    // ===================================================================
    // TAB 2: CẤU HÌNH ATM/MOMO
    // ===================================================================
    private JPanel createAtmConfigPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(true);
        p.setBackground(Color.WHITE);
        p.setBorder(ServerGuiUtils.createSectionBorder("Cấu Hình Thanh Toán (ATM/MOMO)"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        Properties envProps = loadEnvFile();

        int row = 0;
        
        tfBankName = addConfigField(p, gbc, row++, "Tên Ngân Hàng (VD: ACB, MBBank..):", envProps.getProperty("BANK_NAME", ""));
        tfBankAccount = addConfigField(p, gbc, row++, "Số Tài Khoản Ngân Hàng:", envProps.getProperty("BANK_ACCOUNT", ""));
        tfAccountHolder = addConfigField(p, gbc, row++, "Tên Chủ Tài Khoản:", envProps.getProperty("ACCOUNT_HOLDER", ""));
        tfAtmPrefix = addConfigField(p, gbc, row++, "Cú Pháp Chuyển Khoản (VD: NAP):", envProps.getProperty("ATM_PREFIX", "NAP"));
        tfMomoPhone = addConfigField(p, gbc, row++, "Momo Phone:", envProps.getProperty("MOMO_PHONE", ""));
        tfMomoCuphap = addConfigField(p, gbc, row++, "Momo Cú Pháp:", envProps.getProperty("MOMO_CUPHAP", ""));
        tfCronUrl = addConfigField(p, gbc, row++, "Link Auto Cron (Sepay URL):", envProps.getProperty("CRON_URL", ""));
        tfHeSoSuKien = addConfigField(p, gbc, row++, "Hệ Số Sự Kiện (1.0 = bình thường, 2.0 = X2):", envProps.getProperty("HE_SO_SU_KIEN", "1.0"));

        // Hint
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        JLabel hint = new JLabel("💡 Cú pháp CK: NAP{accountId} - VD: 'NAP66' → cộng tiền cho account ID 66");
        hint.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        hint.setForeground(new Color(100, 150, 100));
        p.add(hint, gbc);

        gbc.gridx = 1; gbc.gridy = row; gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        JButton btnSave = ServerGuiUtils.createStyledButton("💾 Lưu Cấu Hình", new Color(0, 123, 255), Color.WHITE);
        btnSave.addActionListener(e -> saveAtmConfig());
        p.add(btnSave, gbc);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(p, BorderLayout.NORTH);
        return wrapper;
    }

    private JTextField addConfigField(JPanel p, GridBagConstraints gbc, int row, String label, String value) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.0; gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.WEST;
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        p.add(lbl, gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JTextField txt = new JTextField(value, 25);
        txt.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        p.add(txt, gbc);
        return txt;
    }

    // ===================================================================
    // TAB 3: AUTO CRON
    // ===================================================================
    private JPanel createAutoCronTab() {
        JPanel main = new JPanel(new BorderLayout(0, 10));
        main.setOpaque(false);
        main.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setOpaque(false);

        // === CRON STATUS ===
        JPanel statusSection = new JPanel(new BorderLayout(0, 5));
        statusSection.setOpaque(false);
        statusSection.setBorder(ServerGuiUtils.createSectionBorder("🤖 Auto Cron Status"));
        
        JPanel statusGrid = new JPanel(new GridLayout(1, 3, 15, 0));
        statusGrid.setOpaque(false);
        statusGrid.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        lblCronStatus = createStatCard("Trạng Thái", "⏹ TẮT", new Color(220, 53, 69));
        lblLastCron = createStatCard("Lần Quét Cuối", "N/A", new Color(108, 117, 125));
        lblCronCount = createStatCard("Đã Match", "0", new Color(40, 167, 69));
        
        statusGrid.add(lblCronStatus);
        statusGrid.add(lblLastCron);
        statusGrid.add(lblCronCount);
        
        statusSection.add(statusGrid, BorderLayout.CENTER);
        container.add(statusSection);
        container.add(Box.createVerticalStrut(10));

        // === CRON CONTROLS ===
        JPanel controlSection = new JPanel(new BorderLayout(0, 5));
        controlSection.setOpaque(false);
        controlSection.setBorder(ServerGuiUtils.createSectionBorder("⚙ Điều Khiển Auto Cron"));
        
        JPanel controlContent = new JPanel();
        controlContent.setLayout(new BoxLayout(controlContent, BoxLayout.Y_AXIS));
        controlContent.setOpaque(false);
        controlContent.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Row 1: Settings
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        row1.setOpaque(false);
        
        row1.add(makeLabel("Khoảng thời gian quét (giây):"));
        tfCronInterval = new JTextField("60", 5);
        tfCronInterval.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        row1.add(tfCronInterval);
        
        controlContent.add(row1);

        // Row 2: Buttons
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        row2.setOpaque(false);

        btnAutoCron = new JToggleButton("▶ Bật Auto Cron");
        btnAutoCron.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnAutoCron.setFocusPainted(false);
        btnAutoCron.setBackground(new Color(40, 167, 69));
        btnAutoCron.setForeground(Color.WHITE);
        btnAutoCron.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAutoCron.setPreferredSize(new Dimension(200, 35));
        btnAutoCron.addActionListener(e -> toggleAutoCron());
        row2.add(btnAutoCron);
        
        JButton btnManualScan = ServerGuiUtils.createStyledButton("🔍 Quét Ngay", new Color(0, 123, 255), Color.WHITE);
        btnManualScan.addActionListener(e -> runCronOnce());
        row2.add(btnManualScan);
        
        controlContent.add(row2);

        // Row 3: Description
        JPanel row3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        row3.setOpaque(false);

        JTextArea desc = new JTextArea(
            "🔄 Auto Cron sẽ:\n" +
            "  1. Quét bảng bank_transactions tìm giao dịch status='pending' hoặc 'ignored'\n" +
            "  2. Parse nội dung CK tìm pattern NAP{accountId} (VD: NAP66, NAP 123)\n" +
            "  3. Nếu tìm thấy account hợp lệ → Cộng tiền VNĐ vào tài khoản\n" +
            "  4. Cập nhật matched_username, matched_account_id, status='success'\n" +
            "  5. Gửi thông báo cho player online nếu có"
        );
        desc.setEditable(false);
        desc.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        desc.setForeground(new Color(80, 80, 80));
        desc.setOpaque(false);
        desc.setBorder(new EmptyBorder(5, 10, 5, 10));
        row3.add(desc);
        controlContent.add(row3);

        controlSection.add(controlContent, BorderLayout.CENTER);
        container.add(controlSection);

        JScrollPane scroll = new JScrollPane(container);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        main.add(scroll, BorderLayout.CENTER);
        return main;
    }

    // ===================================================================
    // AUTO CRON LOGIC
    // ===================================================================
    private void toggleAutoCron() {
        if (btnAutoCron.isSelected()) {
            startAutoCron();
        } else {
            stopAutoCron();
        }
    }

    private void startAutoCron() {
        int interval;
        try {
            interval = Integer.parseInt(tfCronInterval.getText().trim());
            if (interval < 10) interval = 10;
        } catch (NumberFormatException e) {
            interval = 60;
        }

        cronRunning = true;
        btnAutoCron.setText("⏹ Tắt Auto Cron");
        btnAutoCron.setBackground(new Color(220, 53, 69));
        lblCronStatus.setText(buildStatHtml("Trạng Thái", "▶ ĐANG CHẠY", new Color(40, 167, 69)));

        log("🤖 AUTO CRON: Đã bật! Quét mỗi " + interval + " giây");

        cronScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ATM-AutoCron");
            t.setDaemon(true);
            return t;
        });

        cronScheduler.scheduleAtFixedRate(() -> {
            try {
                processUnmatchedTransactions();
            } catch (Exception e) {
                log("❌ CRON ERROR: " + e.getMessage());
            }
        }, 0, interval, TimeUnit.SECONDS);
    }

    private void stopAutoCron() {
        cronRunning = false;
        if (cronScheduler != null) {
            cronScheduler.shutdownNow();
        }
        btnAutoCron.setText("▶ Bật Auto Cron");
        btnAutoCron.setBackground(new Color(40, 167, 69));
        btnAutoCron.setSelected(false);
        lblCronStatus.setText(buildStatHtml("Trạng Thái", "⏹ TẮT", new Color(220, 53, 69)));
        log("⏹ AUTO CRON: Đã tắt");
    }

    private void runCronOnce() {
        log("🔍 Quét thủ công...");
        new Thread(() -> {
            try {
                processUnmatchedTransactions();
                SwingUtilities.invokeLater(this::refreshTransactions);
            } catch (Exception e) {
                log("❌ Quét lỗi: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Core: Quét và match giao dịch pending/ignored với account
     */
    private void processUnmatchedTransactions() {
        try (Connection con = DBConnecter.getConnectionServer()) {
            // 1. Tìm giao dịch chưa match
            String sql = "SELECT id, amount, description, sender_name, status FROM bank_transactions " +
                         "WHERE (status = 'pending' OR status = 'ignored') " +
                         "AND matched_account_id IS NULL " +
                         "ORDER BY id ASC";

            List<int[]> matched = new ArrayList<>(); // [txId, accountId, amount]
            List<String[]> matchedInfo = new ArrayList<>(); // [username, description]

            try (PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    int txId = rs.getInt("id");
                    int amount = rs.getInt("amount");
                    String desc = rs.getString("description");
                    String sender = rs.getString("sender_name");

                    if (desc == null || desc.isEmpty()) continue;

                    // 2. Parse NAP{id} pattern từ nội dung CK
                    // Hỗ trợ: NAP66, NAP 66, nap66, NAP_66, nap-66
                    Matcher m = Pattern.compile("NAP[\\s_\\-]?(\\d+)", Pattern.CASE_INSENSITIVE).matcher(desc);
                    if (m.find()) {
                        int accountId = Integer.parseInt(m.group(1));

                        // 3. Verify account exists
                        try (PreparedStatement psCheck = con.prepareStatement(
                                "SELECT id, username FROM account WHERE id = ?")) {
                            psCheck.setInt(1, accountId);
                            try (ResultSet rsCheck = psCheck.executeQuery()) {
                                if (rsCheck.next()) {
                                    String username = rsCheck.getString("username");
                                    matched.add(new int[]{txId, accountId, amount});
                                    matchedInfo.add(new String[]{username, desc});
                                }
                            }
                        }
                    }
                }
            }

            // 4. Process matches
            double heSo = 1.0;
            try {
                if (tfHeSoSuKien != null) {
                    heSo = Double.parseDouble(tfHeSoSuKien.getText().trim());
                }
            } catch (Exception ignored) {}

            int successCount = 0;
            for (int i = 0; i < matched.size(); i++) {
                int txId = matched.get(i)[0];
                int accountId = matched.get(i)[1];
                int amount = matched.get(i)[2];
                String username = matchedInfo.get(i)[0];
                int soTienCong = (int) (amount * heSo);

                // Cộng tiền
                try (PreparedStatement psUpdate = con.prepareStatement(
                        "UPDATE account SET vnd = vnd + ?, tongnap = tongnap + ? WHERE id = ?")) {
                    psUpdate.setInt(1, soTienCong);
                    psUpdate.setInt(2, amount);
                    psUpdate.setInt(3, accountId);
                    int updated = psUpdate.executeUpdate();

                    if (updated > 0) {
                        // Cập nhật bank_transactions
                        try (PreparedStatement psTx = con.prepareStatement(
                                "UPDATE bank_transactions SET matched_username = ?, matched_account_id = ?, status = 'success' WHERE id = ?")) {
                            psTx.setString(1, username);
                            psTx.setInt(2, accountId);
                            psTx.setInt(3, txId);
                            psTx.executeUpdate();
                        }

                        // Gửi thông báo cho player online
                        Player pl = Client.gI().getPlayerByUser(accountId);
                        if (pl != null) {
                            try {
                                nro.server.CashAuditLog.logAdd(pl, soTienCong, "TX_PANEL_CRON", "TX#" + txId + " AutoMatch");
                                pl.getSession().cash += soTienCong;
                                pl.danap += amount;
                            } catch (Exception ignored) {}
                            Service.gI().sendThongBao(pl,
                                "Nạp thành công " + String.format("%,d", amount) + " VNĐ" +
                                (heSo > 1.0 ? " (nhận " + String.format("%,d", soTienCong) + " VNĐ, X" + heSo + ")" : ""));
                            Service.gI().sendMoney(pl);
                        }

                        successCount++;
                        cronMatchCount++;
                        log("✅ MATCH: TX#" + txId + " → " + username + " (ID:" + accountId + ") | +" + String.format("%,d", soTienCong) + "₫");
                        
                        // Send Telegram notification if enabled
                        try {
                            SettingsPanel.sendTelegramNotification(
                                "💰 *Nạp tiền ATM*\n" +
                                "👤 Account: " + username + " (ID:" + accountId + ")\n" +
                                "💵 Số tiền: " + String.format("%,d", amount) + "₫\n" +
                                "📝 Nội dung: " + matchedInfo.get(i)[1]);
                        } catch (Exception ignored) {}
                    }
                }
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            SwingUtilities.invokeLater(() -> {
                lblLastCron.setText(buildStatHtml("Lần Quét Cuối", timestamp, new Color(108, 117, 125)));
                lblCronCount.setText(buildStatHtml("Đã Match", String.valueOf(cronMatchCount), new Color(40, 167, 69)));
            });

            if (successCount > 0) {
                log("🤖 CRON: Đã match " + successCount + " giao dịch mới");
                SwingUtilities.invokeLater(this::refreshTransactions);
            } else if (matched.isEmpty()) {
                // Silent - no new transactions to match
            }

        } catch (Exception e) {
            log("❌ CRON ERROR: " + e.getMessage());
        }
    }

    // ===================================================================
    // DATA LOADING
    // ===================================================================
    private void refreshTransactions() {
        new Thread(() -> {
            try (Connection con = DBConnecter.getConnectionServer()) {
                loadTransactionData(con, null, null);
                loadRevenueStats(con);
            } catch (Exception e) {
                log("Lỗi: " + e.getMessage());
            }
        }).start();
    }

    private void loadTransactionData(Connection con, String filterUser, String filterDate) {
        try {
            StringBuilder sql = new StringBuilder(
                "SELECT id, transaction_number, amount, description, sender_name, " +
                "matched_username, matched_account_id, status, created_at " +
                "FROM bank_transactions WHERE 1=1 ");

            if (filterUser != null && !filterUser.trim().isEmpty()) {
                sql.append("AND (matched_username LIKE '%").append(filterUser.trim()).append("%' ")
                   .append("OR sender_name LIKE '%").append(filterUser.trim()).append("%') ");
            }
            if (filterDate != null && !filterDate.trim().isEmpty()) {
                sql.append("AND created_at >= '").append(filterDate.trim()).append("' ");
            }
            sql.append("ORDER BY created_at DESC LIMIT 500");

            try (PreparedStatement ps = con.prepareStatement(sql.toString());
                 ResultSet rs = ps.executeQuery()) {
                SwingUtilities.invokeLater(() -> transactionTableModel.setRowCount(0));
                int idx = 1;
                while (rs.next()) {
                    int id = rs.getInt("id");
                    long amount = rs.getLong("amount");
                    String desc = rs.getString("description");
                    String sender = rs.getString("sender_name");
                    String matchedUser = rs.getString("matched_username");
                    String status = rs.getString("status");
                    String time = rs.getString("created_at");

                    String displayUser = (matchedUser != null && !matchedUser.isEmpty()) ? matchedUser : "";
                    String statusDisplay = formatStatus(status);
                    String shortDesc = desc != null && desc.length() > 50 ? desc.substring(0, 47) + "..." : desc;

                    Object[] row = {idx++, displayUser, String.format("%,d", amount),
                            shortDesc, sender != null ? sender : "",
                            statusDisplay, time != null ? time : "", "Duyệt"};
                    final Object[] finalRow = row;
                    SwingUtilities.invokeLater(() -> transactionTableModel.addRow(finalRow));
                }
                int finalIdx = idx;
                SwingUtilities.invokeLater(() -> log("Đã tải " + (finalIdx - 1) + " giao dịch"));
            }
        } catch (Exception e) {
            log("Lỗi đọc giao dịch: " + e.getMessage());
        }
    }

    private void loadRevenueStats(Connection con) {
        try {
            long totalRevenue = queryLong(con, "SELECT COALESCE(SUM(amount), 0) FROM bank_transactions WHERE status = 'success'");
            long todayRevenue = queryLong(con, "SELECT COALESCE(SUM(amount), 0) FROM bank_transactions WHERE status = 'success' AND DATE(created_at) = CURDATE()");
            long weekRevenue = queryLong(con, "SELECT COALESCE(SUM(amount), 0) FROM bank_transactions WHERE status = 'success' AND created_at >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)");
            long monthRevenue = queryLong(con, "SELECT COALESCE(SUM(amount), 0) FROM bank_transactions WHERE status = 'success' AND created_at >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)");
            long totalTx = queryLong(con, "SELECT COUNT(*) FROM bank_transactions");
            long pending = queryLong(con, "SELECT COUNT(*) FROM bank_transactions WHERE status = 'pending' OR status = 'ignored'");

            SwingUtilities.invokeLater(() -> {
                lblTotalRevenue.setText(buildStatHtml("Tổng Doanh Thu", formatMoney(totalRevenue), new Color(0, 120, 215)));
                lblTodayRevenue.setText(buildStatHtml("Hôm Nay", formatMoney(todayRevenue), new Color(40, 167, 69)));
                lblWeekRevenue.setText(buildStatHtml("7 Ngày", formatMoney(weekRevenue), new Color(142, 68, 173)));
                lblMonthRevenue.setText(buildStatHtml("30 Ngày", formatMoney(monthRevenue), new Color(255, 152, 0)));
                lblTotalTransactions.setText(buildStatHtml("Tổng Giao Dịch", String.format("%,d", totalTx), new Color(108, 117, 125)));
                lblPendingCount.setText(buildStatHtml("Chờ Duyệt", String.valueOf(pending), new Color(220, 53, 69)));
            });
        } catch (Exception e) {
            log("Lỗi thống kê: " + e.getMessage());
        }
    }

    // ===================================================================
    // MANUAL ACTIONS
    // ===================================================================
    private void approveSelectedTransaction() {
        int row = transactionTable.getSelectedRow();
        if (row < 0) { log("⚠ Chọn một giao dịch trước!"); return; }

        String amountStr = transactionTableModel.getValueAt(row, 2).toString().replace(",", "");
        String desc = transactionTableModel.getValueAt(row, 3).toString();
        String status = transactionTableModel.getValueAt(row, 5).toString();

        if (status.contains("success")) {
            log("⚠ Giao dịch này đã được duyệt rồi!"); return;
        }

        // Try to find account ID from description
        String input = JOptionPane.showInputDialog(this, 
            "Nhập Account ID để cộng tiền:\n" +
            "Nội dung CK: " + desc + "\n" +
            "Số tiền: " + amountStr + "₫",
            "Duyệt Giao Dịch", JOptionPane.QUESTION_MESSAGE);
        
        if (input == null || input.trim().isEmpty()) return;

        new Thread(() -> {
            try {
                int accountId = Integer.parseInt(input.trim());
                int amount = Integer.parseInt(amountStr.trim());
                
                try (Connection con = DBConnecter.getConnectionServer()) {
                    // Get username
                    String username = "";
                    try (PreparedStatement ps = con.prepareStatement("SELECT username FROM account WHERE id = ?")) {
                        ps.setInt(1, accountId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) username = rs.getString("username");
                            else { log("❌ Không tìm thấy account ID: " + accountId); return; }
                        }
                    }

                    // Cộng tiền
                    try (PreparedStatement ps = con.prepareStatement(
                            "UPDATE account SET vnd = vnd + ?, tongnap = tongnap + ? WHERE id = ?")) {
                        ps.setInt(1, amount);
                        ps.setInt(2, amount);
                        ps.setInt(3, accountId);
                        ps.executeUpdate();
                    }

                    log("✅ Duyệt thủ công: " + username + " (ID:" + accountId + ") +" + String.format("%,d", amount) + "₫");

                    // Notify player
                    Player pl = Client.gI().getPlayerByUser(accountId);
                    if (pl != null) {
                        try { nro.server.CashAuditLog.logAdd(pl, amount, "TX_PANEL_MANUAL", "ManualApprove AccID:" + accountId); pl.getSession().cash += amount; pl.danap += amount; } catch (Exception ignored) {}
                        Service.gI().sendThongBao(pl, "Nạp thành công " + String.format("%,d", amount) + " VNĐ");
                        Service.gI().sendMoney(pl);
                    }

                    SwingUtilities.invokeLater(this::refreshTransactions);
                }
            } catch (NumberFormatException e) {
                log("❌ Account ID phải là số!");
            } catch (Exception e) {
                log("❌ Lỗi duyệt: " + e.getMessage());
            }
        }).start();
    }

    private void ignoreSelectedTransaction() {
        int row = transactionTable.getSelectedRow();
        if (row < 0) return;
        log("❌ Bỏ qua giao dịch #" + transactionTableModel.getValueAt(row, 0));
    }

    private void showTransactionDetail() {
        int row = transactionTable.getSelectedRow();
        if (row < 0) return;
        
        StringBuilder detail = new StringBuilder();
        for (int i = 0; i < transactionTableModel.getColumnCount() - 1; i++) {
            detail.append(transactionTableModel.getColumnName(i)).append(": ")
                  .append(transactionTableModel.getValueAt(row, i)).append("\n");
        }
        
        JTextArea ta = new JTextArea(detail.toString());
        ta.setEditable(false);
        ta.setFont(new Font("Consolas", Font.PLAIN, 12));
        JOptionPane.showMessageDialog(this, new JScrollPane(ta), "Chi Tiết Giao Dịch", JOptionPane.INFORMATION_MESSAGE);
    }

    private void manualMatchAll() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Quét và match tất cả giao dịch pending/ignored?\nGiao dịch có NAP{id} hợp lệ sẽ tự động cộng tiền.",
            "Match Tất Cả", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        
        log("🔄 Đang quét và match tất cả giao dịch pending/ignored...");
        new Thread(() -> {
            processUnmatchedTransactions();
            SwingUtilities.invokeLater(this::refreshTransactions);
        }).start();
    }

    // ===================================================================
    // HELPERS
    // ===================================================================
    private void searchTransactions() {
        new Thread(() -> {
            try (Connection con = DBConnecter.getConnectionServer()) {
                loadTransactionData(con, txtSearchUser.getText(), txtSearchDate.getText());
            } catch (Exception e) {
                log("Lỗi tìm kiếm: " + e.getMessage());
            }
        }).start();
    }

    private void exportToCSV() {
        if (transactionTableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Không có dữ liệu!", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("transactions_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (PrintWriter pw = new PrintWriter(fc.getSelectedFile())) {
                for (int i = 0; i < transactionTableModel.getColumnCount() - 1; i++) {
                    if (i > 0) pw.print(",");
                    pw.print("\"" + transactionTableModel.getColumnName(i) + "\"");
                }
                pw.println();
                for (int row = 0; row < transactionTableModel.getRowCount(); row++) {
                    for (int col = 0; col < transactionTableModel.getColumnCount() - 1; col++) {
                        if (col > 0) pw.print(",");
                        Object val = transactionTableModel.getValueAt(row, col);
                        pw.print("\"" + (val != null ? val : "") + "\"");
                    }
                    pw.println();
                }
                log("✅ Export: " + fc.getSelectedFile().getAbsolutePath());
            } catch (Exception e) {
                log("❌ Export error: " + e.getMessage());
            }
        }
    }

    private String formatStatus(String status) {
        if (status == null) return "N/A";
        return switch (status.toLowerCase()) {
            case "success" -> "✅ success";
            case "pending" -> "⏳ pending";
            case "ignored" -> "❌ ignored";
            case "failed" -> "⛔ failed";
            default -> status;
        };
    }

    private String formatMoney(long amount) {
        if (amount >= 1_000_000_000) return String.format("%.1fTỷ", amount / 1_000_000_000.0);
        if (amount >= 1_000_000) return String.format("%.1fTr", amount / 1_000_000.0);
        if (amount >= 1_000) return String.format("%,dK", amount / 1_000);
        return String.format("%,d₫", amount);
    }

    private long queryLong(Connection con, String sql) {
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getLong(1);
        } catch (Exception ignored) {}
        return 0;
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

    private JLabel makeLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        return lbl;
    }

    private Properties loadEnvFile() {
        Properties props = new Properties();
        File envFile = new File("C:/xampp/htdocs/.env");
        if (envFile.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(envFile))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    int eq = line.indexOf('=');
                    if (eq > 0) {
                        props.setProperty(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
                    }
                }
            } catch (Exception ignored) {}
        }
        return props;
    }

    private void saveAtmConfig() {
        try {
            File envFile = new File("C:/xampp/htdocs/.env");
            List<String> lines = envFile.exists() ? Files.readAllLines(envFile.toPath()) : new ArrayList<>();

            Map<String, String> updates = new LinkedHashMap<>();
            updates.put("BANK_NAME", tfBankName.getText());
            updates.put("BANK_ACCOUNT", tfBankAccount.getText());
            updates.put("ACCOUNT_HOLDER", tfAccountHolder.getText());
            updates.put("ATM_PREFIX", tfAtmPrefix.getText());
            updates.put("MOMO_PHONE", tfMomoPhone.getText());
            updates.put("MOMO_CUPHAP", tfMomoCuphap.getText());
            updates.put("CRON_URL", tfCronUrl.getText());
            updates.put("HE_SO_SU_KIEN", tfHeSoSuKien.getText());

            for (Map.Entry<String, String> entry : updates.entrySet()) {
                boolean found = false;
                for (int i = 0; i < lines.size(); i++) {
                    if (lines.get(i).startsWith(entry.getKey() + "=")) {
                        lines.set(i, entry.getKey() + "=" + entry.getValue());
                        found = true;
                        break;
                    }
                }
                if (!found) lines.add(entry.getKey() + "=" + entry.getValue());
            }

            Files.write(envFile.toPath(), lines);
            JOptionPane.showMessageDialog(this, "Đã lưu cấu hình ATM!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            log("💾 Đã lưu cấu hình ATM/Momo");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void startAutoRefresh() {
        refreshTimer = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "TX-AutoRefresh");
            t.setDaemon(true);
            return t;
        });
        refreshTimer.scheduleAtFixedRate(() -> {
            SwingUtilities.invokeLater(this::refreshTransactions);
        }, 30, 30, TimeUnit.SECONDS);
    }

    // ===================================================================
    // BUTTON RENDERER/EDITOR FOR TABLE
    // ===================================================================
    class ButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
            setFont(new Font("Segoe UI", Font.BOLD, 10));
            setBackground(new Color(0, 123, 255));
            setForeground(Color.WHITE);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean sel, boolean focus, int row, int col) {
            String status = table.getValueAt(row, 5).toString();
            if (status.contains("success")) {
                setText("✅ Done");
                setBackground(new Color(200, 200, 200));
                setEnabled(false);
            } else {
                setText("Duyệt");
                setBackground(new Color(0, 123, 255));
                setEnabled(true);
            }
            return this;
        }
    }

    class ButtonEditor extends DefaultCellEditor {
        private JButton button;
        private int currentRow;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton("Duyệt");
            button.setFont(new Font("Segoe UI", Font.BOLD, 10));
            button.setBackground(new Color(0, 123, 255));
            button.setForeground(Color.WHITE);
            button.addActionListener(e -> {
                fireEditingStopped();
                transactionTable.setRowSelectionInterval(currentRow, currentRow);
                approveSelectedTransaction();
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean sel, int row, int col) {
            currentRow = row;
            String status = table.getValueAt(row, 5).toString();
            button.setEnabled(!status.contains("success"));
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            return "Duyệt";
        }
    }
}
