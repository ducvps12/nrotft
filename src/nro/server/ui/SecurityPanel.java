package nro.server.ui;

import firewall.*;
import utils.Logger;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Panel Bảo Mật & Firewall - Nâng cấp toàn diện
 * - Toggle Firewall, Geo-Block, Rate Limit, Telegram Alert
 * - Cấu hình Rate Limit, Block Duration
 * - Danh sách IP bị chặn / whitelist
 * - Thống kê real-time
 * - Telegram bot setup
 */
public class SecurityPanel extends JPanel {

    // --- UI Components ---
    private JTable tableBlocked;
    private DefaultTableModel modelBlocked;
    private JTextArea logArea;
    private JToggleButton btnFirewall, btnGeoBlock, btnRateLimit, btnTelegram, btnLockdown;
    private JTextField txtMaxConnPerSec, txtMaxConcurrent, txtBlockDuration, txtBurstSize;
    private JTextField txtTelegramToken, txtTelegramChatId;
    private JLabel lblTotalConn, lblBlockedConn, lblGeoBlocked, lblActiveConn, lblBannedIPs;
    private JTextField txtWhitelistIP, txtBlacklistIP;

    private ScheduledExecutorService scheduler;

    public SecurityPanel() {
        setLayout(new BorderLayout(5, 5));
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // Load GeoIP database
        GeoIPFilter.getInstance().load();

        initUI();
        startStatsUpdater();
        loadConfigToUI();
    }

    private void initUI() {
        // ===== TOP: Toggle Controls =====
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.setOpaque(false);

        // Row 1: Main toggles
        JPanel togglesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        togglesPanel.setOpaque(false);
        togglesPanel.setBorder(ServerGuiUtils.createSectionBorder("🛡 Firewall Controls"));

        btnFirewall = createToggle("Firewall", new Color(40, 167, 69));
        btnGeoBlock = createToggle("Block Quốc Tế (VN Only)", new Color(0, 123, 255));
        btnRateLimit = createToggle("Rate Limit", new Color(255, 152, 0));
        btnTelegram = createToggle("Telegram Alert", new Color(0, 136, 204));
        btnLockdown = createToggle("🔒 LOCKDOWN", new Color(220, 53, 69));

        btnFirewall.addActionListener(e -> {
            FirewallConfig.getInstance().firewallEnabled = btnFirewall.isSelected();
            updateToggleState(btnFirewall, "Firewall", new Color(40, 167, 69));
            saveConfig();
            logFW(btnFirewall.isSelected() ? "✅ Firewall BẬT" : "❌ Firewall TẮT");
        });

        btnGeoBlock.addActionListener(e -> {
            FirewallConfig.getInstance().geoBlockEnabled = btnGeoBlock.isSelected();
            updateToggleState(btnGeoBlock, "Block Quốc Tế", new Color(0, 123, 255));
            saveConfig();
            logFW(btnGeoBlock.isSelected() ? "🌍 Geo-Block BẬT (Chỉ VN)" : "🌍 Geo-Block TẮT");
            if (btnGeoBlock.isSelected()) {
                TelegramAlert.getInstance().alertServerStatus(
                    "🌍 Geo-Block đã BẬT\nChỉ cho phép IP từ Việt Nam kết nối"
                );
            }
        });

        btnRateLimit.addActionListener(e -> {
            FirewallConfig.getInstance().rateLimitEnabled = btnRateLimit.isSelected();
            updateToggleState(btnRateLimit, "Rate Limit", new Color(255, 152, 0));
            saveConfig();
            logFW(btnRateLimit.isSelected() ? "⚡ Rate Limit BẬT" : "⚡ Rate Limit TẮT");
        });

        btnTelegram.addActionListener(e -> {
            FirewallConfig.getInstance().telegramAlertEnabled = btnTelegram.isSelected();
            updateToggleState(btnTelegram, "Telegram Alert", new Color(0, 136, 204));
            saveConfig();
            logFW(btnTelegram.isSelected() ? "📱 Telegram Alert BẬT" : "📱 Telegram Alert TẮT");
        });

        btnLockdown.addActionListener(e -> {
            if (btnLockdown.isSelected()) {
                int confirm = JOptionPane.showConfirmDialog(this,
                    "LOCKDOWN sẽ CHẶN TẤT CẢ kết nối mới!\nChỉ IP trong Whitelist được phép.\n\nBạn chắc chắn?",
                    "⚠ LOCKDOWN MODE", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (confirm != JOptionPane.YES_OPTION) {
                    btnLockdown.setSelected(false);
                    return;
                }
            }
            FirewallConfig.getInstance().lockdownMode = btnLockdown.isSelected();
            updateToggleState(btnLockdown, "🔒 LOCKDOWN", new Color(220, 53, 69));
            saveConfig();
            logFW(btnLockdown.isSelected() ? "🚨🔒 LOCKDOWN ACTIVATED!" : "🔓 Lockdown disabled");
            TelegramAlert.getInstance().alertServerStatus(
                btnLockdown.isSelected() ? "🚨 LOCKDOWN MODE BẬT - Chặn tất cả kết nối mới!" : "🔓 Lockdown đã tắt"
            );
        });

        togglesPanel.add(btnFirewall);
        togglesPanel.add(btnGeoBlock);
        togglesPanel.add(btnRateLimit);
        togglesPanel.add(btnTelegram);
        togglesPanel.add(btnLockdown);

        topPanel.add(togglesPanel, BorderLayout.NORTH);

        // Row 2: Stats bar
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 5));
        statsPanel.setOpaque(false);
        statsPanel.setBorder(ServerGuiUtils.createSectionBorder("📊 Real-time Statistics"));

        lblTotalConn = createStatLabel("Total: 0", new Color(40, 167, 69));
        lblBlockedConn = createStatLabel("Blocked: 0", new Color(220, 53, 69));
        lblGeoBlocked = createStatLabel("Geo-Block: 0", new Color(0, 123, 255));
        lblActiveConn = createStatLabel("Active: 0", new Color(255, 152, 0));
        lblBannedIPs = createStatLabel("Banned: 0", new Color(156, 39, 176));

        statsPanel.add(lblTotalConn);
        statsPanel.add(lblBlockedConn);
        statsPanel.add(lblGeoBlocked);
        statsPanel.add(lblActiveConn);
        statsPanel.add(lblBannedIPs);

        topPanel.add(statsPanel, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);

        // ===== CENTER: Tabbed Pane =====
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 12));

        // Tab 1: Blocked IPs
        tabs.addTab("🔴 Blocked IPs", createBlockedIPsTab());
        // Tab 2: Rate Limit Config
        tabs.addTab("⚡ Rate Limit", createRateLimitTab());
        // Tab 3: IP Management
        tabs.addTab("📋 IP Management", createIPManagementTab());
        // Tab 4: Telegram Setup
        tabs.addTab("📱 Telegram", createTelegramTab());
        // Tab 5: Logs
        tabs.addTab("📝 System Logs", createLogsTab());

        add(tabs, BorderLayout.CENTER);
    }

    // ===== TAB: BLOCKED IPs =====
    private JPanel createBlockedIPsTab() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setOpaque(false);

        modelBlocked = new DefaultTableModel(new String[]{"IP Address", "Reason", "Time Blocked", "Expires"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tableBlocked = new JTable(modelBlocked);
        tableBlocked.setRowHeight(25);
        tableBlocked.setFont(new Font("Consolas", Font.PLAIN, 12));

        JScrollPane scroll = new JScrollPane(tableBlocked);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        btnPanel.setOpaque(false);

        JButton btnUnblockSelected = ServerGuiUtils.createStyledButton("Unblock Selected", new Color(40, 167, 69), Color.WHITE);
        btnUnblockSelected.addActionListener(e -> unblockSelected());

        JButton btnUnblockAll = ServerGuiUtils.createStyledButton("Unblock All", new Color(0, 120, 215), Color.WHITE);
        btnUnblockAll.addActionListener(e -> unblockAll());

        JButton btnRefresh = ServerGuiUtils.createStyledButton("Refresh", new Color(108, 117, 125), Color.WHITE);
        btnRefresh.addActionListener(e -> refreshBlockedTable());

        btnPanel.add(btnUnblockSelected);
        btnPanel.add(btnUnblockAll);
        btnPanel.add(btnRefresh);

        panel.add(scroll, BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.SOUTH);
        return panel;
    }

    // ===== TAB: RATE LIMIT CONFIG =====
    private JPanel createRateLimitTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(20, 30, 20, 30));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(8, 8, 8, 8);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;

        FirewallConfig config = FirewallConfig.getInstance();

        // Max Connections/s per IP
        g.gridx = 0; g.gridy = 0;
        panel.add(new JLabel("Max Connections/s per IP:"), g);
        g.gridx = 1;
        txtMaxConnPerSec = new JTextField(String.valueOf(config.maxConnectionsPerSecond), 10);
        panel.add(txtMaxConnPerSec, g);
        g.gridx = 2;
        panel.add(new JLabel("(Mặc định: 10)"), g);

        // Max Concurrent per IP
        g.gridx = 0; g.gridy = 1;
        panel.add(new JLabel("Max Concurrent per IP:"), g);
        g.gridx = 1;
        txtMaxConcurrent = new JTextField(String.valueOf(config.maxConcurrentPerIP), 10);
        panel.add(txtMaxConcurrent, g);
        g.gridx = 2;
        panel.add(new JLabel("(Mặc định: 50)"), g);

        // Burst Size
        g.gridx = 0; g.gridy = 2;
        panel.add(new JLabel("Burst Size:"), g);
        g.gridx = 1;
        txtBurstSize = new JTextField(String.valueOf(config.burstSize), 10);
        panel.add(txtBurstSize, g);
        g.gridx = 2;
        panel.add(new JLabel("(Cho phép burst ngắn)"), g);

        // Block Duration
        g.gridx = 0; g.gridy = 3;
        panel.add(new JLabel("Block Duration (phút):"), g);
        g.gridx = 1;
        txtBlockDuration = new JTextField(String.valueOf(config.blockDurationMinutes), 10);
        panel.add(txtBlockDuration, g);
        g.gridx = 2;
        panel.add(new JLabel("(Thời gian auto-ban)"), g);

        // Save Button
        g.gridx = 1; g.gridy = 5;
        JButton btnSaveRate = ServerGuiUtils.createStyledButton("💾 Lưu Cấu Hình", new Color(40, 167, 69), Color.WHITE);
        btnSaveRate.addActionListener(e -> saveRateLimitConfig());
        panel.add(btnSaveRate, g);

        return panel;
    }

    // ===== TAB: IP MANAGEMENT =====
    private JPanel createIPManagementTab() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 10, 10));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Whitelist
        JPanel whitePanel = new JPanel(new BorderLayout(5, 5));
        whitePanel.setBorder(ServerGuiUtils.createSectionBorder("✅ Whitelist (Luôn cho phép)"));

        DefaultListModel<String> whiteModel = new DefaultListModel<>();
        for (String ip : FirewallConfig.getInstance().whitelistIPs) whiteModel.addElement(ip);
        JList<String> whiteList = new JList<>(whiteModel);

        JPanel whiteControls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        whiteControls.setOpaque(false);
        txtWhitelistIP = new JTextField(12);
        JButton btnAddWhite = ServerGuiUtils.createStyledButton("Add", new Color(40, 167, 69), Color.WHITE);
        JButton btnRemoveWhite = ServerGuiUtils.createStyledButton("Remove", new Color(220, 53, 69), Color.WHITE);

        btnAddWhite.addActionListener(e -> {
            String ip = txtWhitelistIP.getText().trim();
            if (!ip.isEmpty()) {
                FirewallConfig.getInstance().whitelistIPs.add(ip);
                whiteModel.addElement(ip);
                txtWhitelistIP.setText("");
                saveConfig();
                logFW("✅ Whitelist added: " + ip);
            }
        });
        btnRemoveWhite.addActionListener(e -> {
            String selected = whiteList.getSelectedValue();
            if (selected != null) {
                FirewallConfig.getInstance().whitelistIPs.remove(selected);
                whiteModel.removeElement(selected);
                saveConfig();
                logFW("❌ Whitelist removed: " + selected);
            }
        });

        whiteControls.add(txtWhitelistIP);
        whiteControls.add(btnAddWhite);
        whiteControls.add(btnRemoveWhite);
        whitePanel.add(new JScrollPane(whiteList), BorderLayout.CENTER);
        whitePanel.add(whiteControls, BorderLayout.SOUTH);

        // Blacklist
        JPanel blackPanel = new JPanel(new BorderLayout(5, 5));
        blackPanel.setBorder(ServerGuiUtils.createSectionBorder("🔴 Blacklist (Luôn chặn)"));

        DefaultListModel<String> blackModel = new DefaultListModel<>();
        for (String ip : FirewallConfig.getInstance().blacklistIPs) blackModel.addElement(ip);
        JList<String> blackList = new JList<>(blackModel);

        JPanel blackControls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        blackControls.setOpaque(false);
        txtBlacklistIP = new JTextField(12);
        JButton btnAddBlack = ServerGuiUtils.createStyledButton("Add", new Color(220, 53, 69), Color.WHITE);
        JButton btnRemoveBlack = ServerGuiUtils.createStyledButton("Remove", new Color(40, 167, 69), Color.WHITE);

        btnAddBlack.addActionListener(e -> {
            String ip = txtBlacklistIP.getText().trim();
            if (!ip.isEmpty()) {
                FirewallConfig.getInstance().blacklistIPs.add(ip);
                blackModel.addElement(ip);
                txtBlacklistIP.setText("");
                saveConfig();
                logFW("🔴 Blacklist added: " + ip);
                TelegramAlert.getInstance().alertBlocked(ip, "Manual blacklist");
            }
        });
        btnRemoveBlack.addActionListener(e -> {
            String selected = blackList.getSelectedValue();
            if (selected != null) {
                FirewallConfig.getInstance().blacklistIPs.remove(selected);
                blackModel.removeElement(selected);
                saveConfig();
                logFW("✅ Blacklist removed: " + selected);
            }
        });

        blackControls.add(txtBlacklistIP);
        blackControls.add(btnAddBlack);
        blackControls.add(btnRemoveBlack);
        blackPanel.add(new JScrollPane(blackList), BorderLayout.CENTER);
        blackPanel.add(blackControls, BorderLayout.SOUTH);

        panel.add(whitePanel);
        panel.add(blackPanel);
        return panel;
    }

    // ===== TAB: TELEGRAM SETUP =====
    private JPanel createTelegramTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(20, 30, 20, 30));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(8, 8, 8, 8);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;

        FirewallConfig config = FirewallConfig.getInstance();

        // Header
        g.gridx = 0; g.gridy = 0; g.gridwidth = 3;
        JLabel header = new JLabel("📱 Thiết lập Telegram Bot Alert");
        header.setFont(new Font("Segoe UI", Font.BOLD, 16));
        panel.add(header, g);
        g.gridwidth = 1;

        // Bot Token
        g.gridx = 0; g.gridy = 1;
        panel.add(new JLabel("Bot Token:"), g);
        g.gridx = 1; g.gridwidth = 2;
        txtTelegramToken = new JTextField(config.telegramBotToken, 35);
        panel.add(txtTelegramToken, g);
        g.gridwidth = 1;

        // Chat ID
        g.gridx = 0; g.gridy = 2;
        panel.add(new JLabel("Chat ID:"), g);
        g.gridx = 1;
        txtTelegramChatId = new JTextField(config.telegramChatId, 20);
        panel.add(txtTelegramChatId, g);
        g.gridx = 2;
        JButton btnGetChatId = ServerGuiUtils.createStyledButton("Auto Get", new Color(0, 136, 204), Color.WHITE);
        btnGetChatId.addActionListener(e -> autoGetChatId());
        panel.add(btnGetChatId, g);

        // Instructions
        g.gridx = 0; g.gridy = 3; g.gridwidth = 3;
        JTextArea instructions = new JTextArea(
            "Hướng dẫn:\n" +
            "1. Tạo bot tại @BotFather trên Telegram\n" +
            "2. Copy Bot Token và paste vào ô trên\n" +
            "3. Gửi tin nhắn bất kỳ cho bot\n" +
            "4. Nhấn 'Auto Get' để tự lấy Chat ID\n" +
            "5. Nhấn 'Test' để kiểm tra\n" +
            "6. Nhấn 'Lưu' và bật 'Telegram Alert' ở thanh trên"
        );
        instructions.setEditable(false);
        instructions.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        instructions.setBackground(new Color(240, 245, 250));
        instructions.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.add(instructions, g);
        g.gridwidth = 1;

        // Buttons
        g.gridx = 0; g.gridy = 5;
        JButton btnTest = ServerGuiUtils.createStyledButton("🔔 Test Alert", new Color(0, 136, 204), Color.WHITE);
        btnTest.addActionListener(e -> testTelegram());
        panel.add(btnTest, g);

        g.gridx = 1;
        JButton btnSave = ServerGuiUtils.createStyledButton("💾 Lưu", new Color(40, 167, 69), Color.WHITE);
        btnSave.addActionListener(e -> saveTelegramConfig());
        panel.add(btnSave, g);

        return panel;
    }

    // ===== TAB: LOGS =====
    private JPanel createLogsTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setBackground(new Color(30, 30, 30));
        logArea.setForeground(new Color(0, 255, 128));
        logArea.setCaretColor(Color.WHITE);

        JScrollPane scroll = new JScrollPane(logArea);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnPanel.setOpaque(false);
        JButton btnClear = ServerGuiUtils.createStyledButton("Clear Logs", new Color(108, 117, 125), Color.WHITE);
        btnClear.addActionListener(e -> logArea.setText(""));
        btnPanel.add(btnClear);

        panel.add(scroll, BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.SOUTH);
        return panel;
    }

    // ===== ACTIONS =====

    private void saveRateLimitConfig() {
        try {
            FirewallConfig config = FirewallConfig.getInstance();
            config.maxConnectionsPerSecond = Integer.parseInt(txtMaxConnPerSec.getText().trim());
            config.maxConcurrentPerIP = Integer.parseInt(txtMaxConcurrent.getText().trim());
            config.burstSize = Integer.parseInt(txtBurstSize.getText().trim());
            config.blockDurationMinutes = Integer.parseInt(txtBlockDuration.getText().trim());
            config.save();
            logFW("💾 Rate Limit config saved.");
            JOptionPane.showMessageDialog(this, "Đã lưu cấu hình!", "OK", JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập số hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveTelegramConfig() {
        FirewallConfig config = FirewallConfig.getInstance();
        config.telegramBotToken = txtTelegramToken.getText().trim();
        config.telegramChatId = txtTelegramChatId.getText().trim();
        config.save();
        logFW("💾 Telegram config saved.");
        JOptionPane.showMessageDialog(this, "Đã lưu cấu hình Telegram!", "OK", JOptionPane.INFORMATION_MESSAGE);
    }

    private void testTelegram() {
        FirewallConfig config = FirewallConfig.getInstance();
        config.telegramBotToken = txtTelegramToken.getText().trim();
        config.telegramChatId = txtTelegramChatId.getText().trim();

        if (config.telegramBotToken.isEmpty() || config.telegramChatId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập Bot Token và Chat ID!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Tạm bật để test
        boolean wasEnabled = config.telegramAlertEnabled;
        config.telegramAlertEnabled = true;

        TelegramAlert.getInstance().sendAlert(
            "✅ *TEST THÀNH CÔNG*\n" +
            "━━━━━━━━━━━━━━━\n" +
            "Firewall Alert System đang hoạt động!\n" +
            "Bot sẽ gửi cảnh báo khi:\n" +
            "• Phát hiện DDoS\n" +
            "• IP bị block\n" +
            "• Geo-block international IP\n" +
            "• Lockdown mode bật/tắt"
        );

        config.telegramAlertEnabled = wasEnabled;
        logFW("📱 Test alert sent to Telegram.");
        JOptionPane.showMessageDialog(this, "Đã gửi test alert! Kiểm tra Telegram.", "OK", JOptionPane.INFORMATION_MESSAGE);
    }

    private void autoGetChatId() {
        String token = txtTelegramToken.getText().trim();
        if (token.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nhập Bot Token trước!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        FirewallConfig.getInstance().telegramBotToken = token;

        String chatId = TelegramAlert.getInstance().getLastChatId();
        if (chatId != null) {
            txtTelegramChatId.setText(chatId);
            logFW("📱 Auto-detected Chat ID: " + chatId);
            JOptionPane.showMessageDialog(this, "Chat ID: " + chatId, "OK", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this,
                "Không tìm thấy Chat ID!\n" +
                "Hãy gửi tin nhắn bất kỳ cho bot trên Telegram\n" +
                "rồi nhấn 'Auto Get' lại.",
                "Chưa có tin nhắn", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void unblockSelected() {
        int row = tableBlocked.getSelectedRow();
        if (row < 0) return;
        String ip = (String) modelBlocked.getValueAt(row, 0);
        RateLimiter.getInstance().unban(ip);
        FirewallConfig.getInstance().blockedIPs.remove(ip);
        modelBlocked.removeRow(row);
        saveConfig();
        logFW("✅ Unblocked: " + ip);
    }

    private void unblockAll() {
        if (modelBlocked.getRowCount() == 0) return;
        int confirm = JOptionPane.showConfirmDialog(this, "Gỡ chặn tất cả IP?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            RateLimiter.getInstance().unbanAll();
            FirewallConfig.getInstance().blockedIPs.clear();
            modelBlocked.setRowCount(0);
            saveConfig();
            logFW("✅ All IPs unblocked.");
        }
    }

    private void refreshBlockedTable() {
        modelBlocked.setRowCount(0);
        Map<String, Long> banned = RateLimiter.getInstance().getTempBanned();
        for (Map.Entry<String, Long> entry : banned.entrySet()) {
            long remaining = (entry.getValue() - System.currentTimeMillis()) / 1000;
            if (remaining > 0) {
                modelBlocked.addRow(new Object[]{
                    entry.getKey(),
                    "Rate limit exceeded",
                    "Active",
                    remaining + "s remaining"
                });
            }
        }
        for (String ip : FirewallConfig.getInstance().blacklistIPs) {
            modelBlocked.addRow(new Object[]{ip, "Manual blacklist", "Permanent", "∞"});
        }
    }

    // ===== HELPERS =====

    private void loadConfigToUI() {
        FirewallConfig config = FirewallConfig.getInstance();
        btnFirewall.setSelected(config.firewallEnabled);
        btnGeoBlock.setSelected(config.geoBlockEnabled);
        btnRateLimit.setSelected(config.rateLimitEnabled);
        btnTelegram.setSelected(config.telegramAlertEnabled);
        btnLockdown.setSelected(config.lockdownMode);

        updateToggleState(btnFirewall, "Firewall", new Color(40, 167, 69));
        updateToggleState(btnGeoBlock, "Block Quốc Tế", new Color(0, 123, 255));
        updateToggleState(btnRateLimit, "Rate Limit", new Color(255, 152, 0));
        updateToggleState(btnTelegram, "Telegram Alert", new Color(0, 136, 204));
        updateToggleState(btnLockdown, "🔒 LOCKDOWN", new Color(220, 53, 69));

        logFW("System initialized. Firewall: " + (config.firewallEnabled ? "ON" : "OFF")
            + " | Geo-Block: " + (config.geoBlockEnabled ? "ON" : "OFF")
            + " | Rate Limit: " + (config.rateLimitEnabled ? "ON" : "OFF")
            + " | GeoIP ranges: " + GeoIPFilter.getInstance().getRangeCount());
    }

    private void saveConfig() {
        FirewallConfig.getInstance().save();
    }

    private JToggleButton createToggle(String label, Color activeColor) {
        JToggleButton btn = new JToggleButton(label + ": OFF");
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBackground(new Color(200, 200, 200));
        btn.setForeground(Color.DARK_GRAY);
        btn.setPreferredSize(new Dimension(180, 32));
        return btn;
    }

    private void updateToggleState(JToggleButton btn, String label, Color activeColor) {
        boolean on = btn.isSelected();
        btn.setText(label + ": " + (on ? "ON" : "OFF"));
        btn.setBackground(on ? activeColor : new Color(200, 200, 200));
        btn.setForeground(on ? Color.WHITE : Color.DARK_GRAY);
    }

    private JLabel createStatLabel(String text, Color color) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbl.setForeground(color);
        lbl.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color.brighter(), 1),
            new EmptyBorder(4, 10, 4, 10)
        ));
        return lbl;
    }

    private void logFW(String msg) {
        if (logArea == null) return;
        SwingUtilities.invokeLater(() -> {
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            logArea.append("[" + time + "] " + msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void startStatsUpdater() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "FW-Stats");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(() -> {
            try {
                RateLimiter rl = RateLimiter.getInstance();
                // Lấy stats từ tất cả proxy instances
                long totalConn = rl.getTotalAccepted() + rl.getTotalRejected();
                long blocked = rl.getTotalRejected();

                SwingUtilities.invokeLater(() -> {
                    lblTotalConn.setText("Total: " + totalConn);
                    lblBlockedConn.setText("Blocked: " + blocked);
                    lblActiveConn.setText("Active IPs: " + rl.getActiveIPCount());
                    lblBannedIPs.setText("Banned: " + rl.getBannedCount());
                });

                // Cleanup old entries
                rl.cleanup();
            } catch (Exception ignored) {}
        }, 1, 2, TimeUnit.SECONDS);
    }

    public void stop() {
        if (scheduler != null) scheduler.shutdownNow();
    }
}