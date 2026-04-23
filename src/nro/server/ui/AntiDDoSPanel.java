package nro.server.ui;

import firewall.*;
import utils.Logger;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.*;

/**
 * Panel Anti-DDoS Protection
 * - Nhập IP VPS + nhiều port cần bảo vệ
 * - Quick profiles (Web, Game, Custom)
 * - Bảng hiển thị các port đang được bảo vệ (status, stats)
 * - Auto-start khi server khởi động
 * - Dashboard thống kê tổng hợp real-time
 */
public class AntiDDoSPanel extends JPanel {

    // --- UI Components ---
    private JTextField txtTargetIP;
    private JTextField txtTargetPort;
    private JTextField txtListenPort;
    private JTextField txtLabel;
    private JTable tableProtected;
    private DefaultTableModel modelProtected;
    private JTextArea logArea;
    private JToggleButton btnAutoStart;

    // Stats labels
    private JLabel lblActiveProxies, lblTotalConn, lblTotalBlocked, lblActiveConn, lblProtectedPorts;

    // Quick Profile buttons
    private JButton btnProfileWeb, btnProfileGame, btnProfileCustom;

    private ScheduledExecutorService scheduler;

    public AntiDDoSPanel() {
        setLayout(new BorderLayout(5, 5));
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(8, 8, 8, 8));

        initUI();
        startStatsUpdater();
        loadProtectedPorts();
    }

    private void initUI() {
        // ===== TOP SECTION =====
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.setOpaque(false);

        // Header + Stats
        JPanel headerPanel = new JPanel(new BorderLayout(10, 5));
        headerPanel.setOpaque(false);

        // Title Bar
        JPanel titleBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        titleBar.setOpaque(false);
        JLabel title = new JLabel("🛡 Anti-DDoS Protection");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(new Color(0, 100, 200));
        titleBar.add(title);

        // Auto-start toggle
        btnAutoStart = new JToggleButton("Auto-Start: OFF");
        btnAutoStart.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnAutoStart.setFocusPainted(false);
        btnAutoStart.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAutoStart.setPreferredSize(new Dimension(160, 30));
        updateAutoStartToggle();
        btnAutoStart.addActionListener(e -> {
            FirewallConfig.getInstance().autoStartProtection = btnAutoStart.isSelected();
            updateAutoStartToggle();
            FirewallConfig.getInstance().save();
            logDDoS(btnAutoStart.isSelected() ? "✅ Auto-Start BẬT - Server khởi động sẽ tự bảo vệ" : "❌ Auto-Start TẮT");
        });
        titleBar.add(btnAutoStart);

        headerPanel.add(titleBar, BorderLayout.NORTH);

        // Stats bar
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        statsPanel.setOpaque(false);
        statsPanel.setBorder(ServerGuiUtils.createSectionBorder("📊 Thống Kê Real-time"));

        lblActiveProxies = createStatLabel("Proxy Active: 0", new Color(40, 167, 69));
        lblProtectedPorts = createStatLabel("Ports Saved: 0", new Color(0, 123, 255));
        lblTotalConn = createStatLabel("Total Conn: 0", new Color(255, 152, 0));
        lblTotalBlocked = createStatLabel("Blocked: 0", new Color(220, 53, 69));
        lblActiveConn = createStatLabel("Active Conn: 0", new Color(156, 39, 176));

        statsPanel.add(lblActiveProxies);
        statsPanel.add(lblProtectedPorts);
        statsPanel.add(lblTotalConn);
        statsPanel.add(lblTotalBlocked);
        statsPanel.add(lblActiveConn);

        headerPanel.add(statsPanel, BorderLayout.CENTER);
        topPanel.add(headerPanel, BorderLayout.NORTH);

        // ===== ADD PROTECTION FORM =====
        JPanel formPanel = new JPanel(new BorderLayout(5, 5));
        formPanel.setOpaque(false);

        // Quick Profiles
        JPanel profilePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));
        profilePanel.setOpaque(false);
        profilePanel.setBorder(ServerGuiUtils.createSectionBorder("⚡ Quick Profiles"));

        btnProfileWeb = createProfileButton("🌐 Web Server (80, 443)", new Color(0, 123, 255));
        btnProfileGame = createProfileButton("🎮 Game Server (14445)", new Color(220, 53, 69));
        btnProfileCustom = createProfileButton("➕ Custom Port", new Color(108, 117, 125));

        btnProfileWeb.addActionListener(e -> applyProfile("Web", new int[]{80, 443}, new int[]{10080, 10443}));
        btnProfileGame.addActionListener(e -> applyProfile("Game", new int[]{14445}, new int[]{24445}));
        btnProfileCustom.addActionListener(e -> {
            txtLabel.setText("");
            txtTargetPort.setText("");
            txtListenPort.setText("");
            txtTargetPort.requestFocus();
        });

        profilePanel.add(btnProfileWeb);
        profilePanel.add(btnProfileGame);
        profilePanel.add(btnProfileCustom);

        formPanel.add(profilePanel, BorderLayout.NORTH);

        // Input form
        JPanel inputForm = new JPanel(new GridBagLayout());
        inputForm.setOpaque(false);
        inputForm.setBorder(ServerGuiUtils.createSectionBorder("➕ Thêm Port Bảo Vệ"));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 5, 4, 5);
        g.fill = GridBagConstraints.HORIZONTAL;

        // Row 1: IP + Label
        g.gridx = 0; g.gridy = 0;
        inputForm.add(createFormLabel("IP VPS:"), g);
        g.gridx = 1;
        txtTargetIP = new JTextField("127.0.0.1", 14);
        txtTargetIP.setFont(new Font("Consolas", Font.PLAIN, 13));
        inputForm.add(txtTargetIP, g);

        g.gridx = 2;
        inputForm.add(createFormLabel("Tên:"), g);
        g.gridx = 3;
        txtLabel = new JTextField("", 10);
        txtLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        inputForm.add(txtLabel, g);

        // Row 2: Ports + Buttons
        g.gridx = 0; g.gridy = 1;
        inputForm.add(createFormLabel("Port Đích:"), g);
        g.gridx = 1;
        txtTargetPort = new JTextField("", 14);
        txtTargetPort.setToolTipText("Nhập port thực tế cần bảo vệ. VD: 80 hoặc 80,443,14445");
        txtTargetPort.setFont(new Font("Consolas", Font.PLAIN, 13));
        inputForm.add(txtTargetPort, g);

        g.gridx = 2;
        inputForm.add(createFormLabel("Port Proxy:"), g);
        g.gridx = 3;
        txtListenPort = new JTextField("", 14);
        txtListenPort.setToolTipText("Port proxy sẽ listen. VD: 10080 hoặc 10080,10443,24445");
        txtListenPort.setFont(new Font("Consolas", Font.PLAIN, 13));
        inputForm.add(txtListenPort, g);

        g.gridx = 4; g.gridy = 0; g.gridheight = 2;
        g.fill = GridBagConstraints.BOTH;
        JPanel btnFormPanel = new JPanel(new GridLayout(2, 1, 4, 4));
        btnFormPanel.setOpaque(false);

        JButton btnAdd = ServerGuiUtils.createStyledButton("💾 Thêm & Lưu", new Color(40, 167, 69), Color.WHITE);
        btnAdd.addActionListener(e -> addProtectedPorts());

        JButton btnAddStart = ServerGuiUtils.createStyledButton("🚀 Thêm & Bật", new Color(0, 123, 255), Color.WHITE);
        btnAddStart.addActionListener(e -> addAndStartProtectedPorts());

        btnFormPanel.add(btnAdd);
        btnFormPanel.add(btnAddStart);
        inputForm.add(btnFormPanel, g);

        formPanel.add(inputForm, BorderLayout.CENTER);
        topPanel.add(formPanel, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

        // ===== CENTER: Split Pane (Table + Log) =====
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.65);
        splitPane.setDividerSize(5);

        // Protected Ports Table
        JPanel tablePanel = new JPanel(new BorderLayout(3, 3));
        tablePanel.setOpaque(false);
        tablePanel.setBorder(ServerGuiUtils.createSectionBorder("🛡 Danh Sách Port Được Bảo Vệ"));

        String[] cols = {"Tên", "IP Đích", "Port Đích", "Port Proxy", "Trạng Thái", "Connections", "Blocked"};
        modelProtected = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tableProtected = new JTable(modelProtected);
        tableProtected.setRowHeight(28);
        tableProtected.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tableProtected.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tableProtected.setSelectionBackground(new Color(230, 242, 255));
        tableProtected.setShowGrid(true);
        tableProtected.setGridColor(new Color(230, 230, 230));

        // Custom renderer for Status column
        tableProtected.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                lbl.setHorizontalAlignment(CENTER);
                String status = value != null ? value.toString() : "";
                if (status.contains("Running") || status.contains("🟢")) {
                    lbl.setForeground(new Color(40, 167, 69));
                    lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
                } else if (status.contains("Stopped") || status.contains("🔴")) {
                    lbl.setForeground(new Color(220, 53, 69));
                } else {
                    lbl.setForeground(new Color(108, 117, 125));
                }
                return lbl;
            }
        });

        // Set column widths
        tableProtected.getColumnModel().getColumn(0).setPreferredWidth(120);
        tableProtected.getColumnModel().getColumn(1).setPreferredWidth(130);
        tableProtected.getColumnModel().getColumn(2).setPreferredWidth(80);
        tableProtected.getColumnModel().getColumn(3).setPreferredWidth(80);
        tableProtected.getColumnModel().getColumn(4).setPreferredWidth(100);
        tableProtected.getColumnModel().getColumn(5).setPreferredWidth(90);
        tableProtected.getColumnModel().getColumn(6).setPreferredWidth(80);

        JScrollPane scrollTable = new JScrollPane(tableProtected);
        tablePanel.add(scrollTable, BorderLayout.CENTER);

        // Action buttons dưới bảng
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        actionPanel.setOpaque(false);

        JButton btnStartSelected = ServerGuiUtils.createStyledButton("▶ Bật", new Color(40, 167, 69), Color.WHITE);
        btnStartSelected.addActionListener(e -> startSelectedProxy());

        JButton btnStopSelected = ServerGuiUtils.createStyledButton("⏹ Tắt", new Color(220, 53, 69), Color.WHITE);
        btnStopSelected.addActionListener(e -> stopSelectedProxy());

        JButton btnStartAll = ServerGuiUtils.createStyledButton("▶▶ Bật Tất Cả", new Color(0, 120, 215), Color.WHITE);
        btnStartAll.addActionListener(e -> startAllProxies());

        JButton btnStopAll = ServerGuiUtils.createStyledButton("⏹⏹ Tắt Tất Cả", new Color(255, 87, 34), Color.WHITE);
        btnStopAll.addActionListener(e -> stopAllProxies());

        JButton btnRemove = ServerGuiUtils.createStyledButton("🗑 Xóa", new Color(108, 117, 125), Color.WHITE);
        btnRemove.addActionListener(e -> removeSelectedPort());

        JButton btnRefresh = ServerGuiUtils.createStyledButton("🔄 Refresh", new Color(108, 117, 125), Color.WHITE);
        btnRefresh.addActionListener(e -> refreshTable());

        actionPanel.add(btnStartSelected);
        actionPanel.add(btnStopSelected);
        actionPanel.add(Box.createHorizontalStrut(10));
        actionPanel.add(btnStartAll);
        actionPanel.add(btnStopAll);
        actionPanel.add(Box.createHorizontalStrut(10));
        actionPanel.add(btnRemove);
        actionPanel.add(btnRefresh);

        tablePanel.add(actionPanel, BorderLayout.SOUTH);
        splitPane.setTopComponent(tablePanel);

        // Log area
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setOpaque(false);
        logPanel.setBorder(ServerGuiUtils.createSectionBorder("📋 Anti-DDoS Logs"));

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setBackground(new Color(25, 25, 35));
        logArea.setForeground(new Color(0, 255, 128));
        logArea.setCaretColor(Color.WHITE);

        JScrollPane scrollLog = new JScrollPane(logArea);

        JPanel logBtnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 3));
        logBtnPanel.setOpaque(false);
        JButton btnClearLog = ServerGuiUtils.createStyledButton("Clear", new Color(108, 117, 125), Color.WHITE);
        btnClearLog.addActionListener(e -> logArea.setText(""));
        logBtnPanel.add(btnClearLog);

        logPanel.add(scrollLog, BorderLayout.CENTER);
        logPanel.add(logBtnPanel, BorderLayout.SOUTH);
        splitPane.setBottomComponent(logPanel);

        add(splitPane, BorderLayout.CENTER);

        logDDoS("🛡 Anti-DDoS Protection Panel khởi tạo.");
    }

    // ===== QUICK PROFILES =====

    private void applyProfile(String profileName, int[] targetPorts, int[] listenPorts) {
        String ip = txtTargetIP.getText().trim();
        if (ip.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập IP VPS trước!", "Thiếu IP", JOptionPane.WARNING_MESSAGE);
            txtTargetIP.requestFocus();
            return;
        }

        StringBuilder tPorts = new StringBuilder();
        StringBuilder lPorts = new StringBuilder();
        StringBuilder labels = new StringBuilder();

        for (int i = 0; i < targetPorts.length; i++) {
            if (i > 0) { tPorts.append(","); lPorts.append(","); labels.append(","); }
            tPorts.append(targetPorts[i]);
            lPorts.append(listenPorts[i]);
        }

        txtTargetPort.setText(tPorts.toString());
        txtListenPort.setText(lPorts.toString());
        txtLabel.setText(profileName);

        logDDoS("📋 Profile [" + profileName + "] applied: ports " + tPorts);
    }

    // ===== ADD PORTS =====

    private void addProtectedPorts() {
        List<FirewallConfig.ProtectedPort> newPorts = parsePortInput();
        if (newPorts == null) return;

        FirewallConfig config = FirewallConfig.getInstance();
        for (FirewallConfig.ProtectedPort pp : newPorts) {
            // Kiểm tra trùng listen port
            boolean exists = config.protectedPorts.stream()
                    .anyMatch(p -> p.listenPort == pp.listenPort);
            if (exists) {
                logDDoS("⚠ Port proxy " + pp.listenPort + " đã tồn tại, bỏ qua.");
                continue;
            }
            pp.enabled = false;
            config.protectedPorts.add(pp);
            logDDoS("💾 Saved: " + pp.label + " [:" + pp.listenPort + " → " + pp.targetIP + ":" + pp.targetPort + "]");
        }
        config.save();
        refreshTable();
        clearInputForm();
    }

    private void addAndStartProtectedPorts() {
        List<FirewallConfig.ProtectedPort> newPorts = parsePortInput();
        if (newPorts == null) return;

        FirewallConfig config = FirewallConfig.getInstance();
        ProxyManager pm = ProxyManager.getInstance();

        for (FirewallConfig.ProtectedPort pp : newPorts) {
            // Kiểm tra trùng listen port
            boolean exists = config.protectedPorts.stream()
                    .anyMatch(p -> p.listenPort == pp.listenPort);
            if (exists) {
                logDDoS("⚠ Port proxy " + pp.listenPort + " đã tồn tại, bỏ qua.");
                continue;
            }
            // Start proxy ngay
            boolean ok = pm.startProxy(pp.targetIP, pp.targetPort, pp.listenPort);
            if (ok) {
                pp.enabled = true;
                config.protectedPorts.add(pp);
                logDDoS("🚀 Started: " + pp.label + " [:" + pp.listenPort + " → " + pp.targetIP + ":" + pp.targetPort + "]");
            } else {
                pp.enabled = false;
                config.protectedPorts.add(pp);
                logDDoS("❌ Failed to start port " + pp.listenPort + " (port đang bận?)");
            }
        }
        config.save();
        refreshTable();
        clearInputForm();
    }

    /**
     * Parse input: hỗ trợ nhập nhiều port cách nhau bởi dấu phẩy
     * VD: targetPort = "80,443,14445"  listenPort = "10080,10443,24445"
     */
    private List<FirewallConfig.ProtectedPort> parsePortInput() {
        String ip = txtTargetIP.getText().trim();
        String tPortStr = txtTargetPort.getText().trim();
        String lPortStr = txtListenPort.getText().trim();
        String label = txtLabel.getText().trim();

        if (ip.isEmpty() || tPortStr.isEmpty() || lPortStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ: IP, Port Đích, Port Proxy!", "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        String[] tParts = tPortStr.split("[,;\\s]+");
        String[] lParts = lPortStr.split("[,;\\s]+");

        if (tParts.length != lParts.length) {
            JOptionPane.showMessageDialog(this,
                "Số lượng Port Đích (" + tParts.length + ") phải bằng Port Proxy (" + lParts.length + ")!\n" +
                "VD: Port Đích = 80,443  |  Port Proxy = 10080,10443",
                "Không khớp", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        List<FirewallConfig.ProtectedPort> result = new ArrayList<>();
        try {
            for (int i = 0; i < tParts.length; i++) {
                int tPort = Integer.parseInt(tParts[i].trim());
                int lPort = Integer.parseInt(lParts[i].trim());

                if (tPort < 1 || tPort > 65535 || lPort < 1 || lPort > 65535) {
                    JOptionPane.showMessageDialog(this, "Port phải từ 1 - 65535!", "Port không hợp lệ", JOptionPane.ERROR_MESSAGE);
                    return null;
                }

                String portLabel = label.isEmpty() ? "Port-" + tPort : (tParts.length > 1 ? label + "-" + tPort : label);
                result.add(new FirewallConfig.ProtectedPort(portLabel, ip, tPort, lPort, false));
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Port phải là số nguyên!", "Lỗi nhập liệu", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        return result;
    }

    // ===== TABLE ACTIONS =====

    private void startSelectedProxy() {
        int row = tableProtected.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Chọn 1 port trong bảng!", "Chưa chọn", JOptionPane.WARNING_MESSAGE);
            return;
        }

        FirewallConfig config = FirewallConfig.getInstance();
        if (row >= config.protectedPorts.size()) return;
        FirewallConfig.ProtectedPort pp = config.protectedPorts.get(row);

        if (ProxyManager.getInstance().isRunning(pp.listenPort)) {
            logDDoS("⚠ Port " + pp.listenPort + " đã đang chạy.");
            return;
        }

        boolean ok = ProxyManager.getInstance().startProxy(pp.targetIP, pp.targetPort, pp.listenPort);
        if (ok) {
            pp.enabled = true;
            config.save();
            logDDoS("🚀 Started: " + pp.label + " [:" + pp.listenPort + "]");
            refreshTable();
        } else {
            logDDoS("❌ Không thể bật port " + pp.listenPort + " (đang bận?)");
        }
    }

    private void stopSelectedProxy() {
        int row = tableProtected.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Chọn 1 port trong bảng!", "Chưa chọn", JOptionPane.WARNING_MESSAGE);
            return;
        }

        FirewallConfig config = FirewallConfig.getInstance();
        if (row >= config.protectedPorts.size()) return;
        FirewallConfig.ProtectedPort pp = config.protectedPorts.get(row);

        boolean ok = ProxyManager.getInstance().stopProxy(pp.listenPort);
        if (ok) {
            pp.enabled = false;
            config.save();
            logDDoS("⏹ Stopped: " + pp.label + " [:" + pp.listenPort + "]");
            refreshTable();
        } else {
            logDDoS("⚠ Port " + pp.listenPort + " không đang chạy.");
        }
    }

    private void startAllProxies() {
        FirewallConfig config = FirewallConfig.getInstance();
        ProxyManager pm = ProxyManager.getInstance();
        int started = 0;

        for (FirewallConfig.ProtectedPort pp : config.protectedPorts) {
            if (!pm.isRunning(pp.listenPort)) {
                boolean ok = pm.startProxy(pp.targetIP, pp.targetPort, pp.listenPort);
                if (ok) {
                    pp.enabled = true;
                    started++;
                }
            }
        }
        config.save();
        refreshTable();
        logDDoS("🚀 Đã bật " + started + " proxy(s). Tổng đang chạy: " + pm.getActiveCount());

        TelegramAlert.getInstance().alertServerStatus(
            "🛡 Anti-DDoS: Bật tất cả\n" +
            "📡 " + pm.getActiveCount() + " proxy(s) đang hoạt động"
        );
    }

    private void stopAllProxies() {
        if (ProxyManager.getInstance().getActiveCount() == 0) {
            logDDoS("⚠ Không có proxy nào đang chạy.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "Tắt TẤT CẢ proxy đang chạy?\nCác kết nối qua proxy sẽ bị ngắt!",
            "⚠ Xác nhận", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            ProxyManager.getInstance().stopAll();
            FirewallConfig config = FirewallConfig.getInstance();
            for (FirewallConfig.ProtectedPort pp : config.protectedPorts) {
                pp.enabled = false;
            }
            config.save();
            refreshTable();
            logDDoS("⏹ Đã tắt tất cả proxy.");
        }
    }

    private void removeSelectedPort() {
        int row = tableProtected.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Chọn 1 port trong bảng!", "Chưa chọn", JOptionPane.WARNING_MESSAGE);
            return;
        }

        FirewallConfig config = FirewallConfig.getInstance();
        if (row >= config.protectedPorts.size()) return;

        FirewallConfig.ProtectedPort pp = config.protectedPorts.get(row);

        int confirm = JOptionPane.showConfirmDialog(this,
            "Xóa port " + pp.label + " (:" + pp.listenPort + ") khỏi danh sách?\nProxy sẽ bị dừng nếu đang chạy.",
            "Xác nhận xóa", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            // Stop proxy nếu đang chạy
            ProxyManager.getInstance().stopProxy(pp.listenPort);
            config.protectedPorts.remove(row);
            config.save();
            refreshTable();
            logDDoS("🗑 Đã xóa: " + pp.label + " [:" + pp.listenPort + "]");
        }
    }

    // ===== TABLE REFRESH =====

    private void refreshTable() {
        modelProtected.setRowCount(0);
        FirewallConfig config = FirewallConfig.getInstance();
        ProxyManager pm = ProxyManager.getInstance();

        for (FirewallConfig.ProtectedPort pp : config.protectedPorts) {
            boolean running = pm.isRunning(pp.listenPort);
            TCPProxy proxy = pm.getProxy(pp.listenPort);

            String status = running ? "🟢 Running" : "🔴 Stopped";
            String connections = proxy != null ? String.valueOf(proxy.getTotalConnections()) : "0";
            String blocked = proxy != null ? String.valueOf(proxy.getBlockedConnections()) : "0";

            modelProtected.addRow(new Object[]{
                pp.label,
                pp.targetIP,
                pp.targetPort,
                pp.listenPort,
                status,
                connections,
                blocked
            });
        }
    }

    private void loadProtectedPorts() {
        FirewallConfig config = FirewallConfig.getInstance();
        btnAutoStart.setSelected(config.autoStartProtection);
        updateAutoStartToggle();
        refreshTable();
        logDDoS("📋 Loaded " + config.protectedPorts.size() + " protected port(s) from config.");
    }

    // ===== HELPERS =====

    private void clearInputForm() {
        txtLabel.setText("");
        txtTargetPort.setText("");
        txtListenPort.setText("");
    }

    private void updateAutoStartToggle() {
        boolean on = btnAutoStart.isSelected();
        btnAutoStart.setText("Auto-Start: " + (on ? "ON" : "OFF"));
        btnAutoStart.setBackground(on ? new Color(40, 167, 69) : new Color(200, 200, 200));
        btnAutoStart.setForeground(on ? Color.WHITE : Color.DARK_GRAY);
    }

    private JButton createProfileButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(color.darker(), 1),
            new EmptyBorder(5, 12, 5, 12)
        ));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(color.brighter());
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(color);
            }
        });

        return btn;
    }

    private JLabel createFormLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(new Color(60, 60, 60));
        return lbl;
    }

    private JLabel createStatLabel(String text, Color color) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(color);
        lbl.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color.brighter(), 1),
            new EmptyBorder(3, 8, 3, 8)
        ));
        return lbl;
    }

    private void logDDoS(String msg) {
        if (logArea == null) return;
        SwingUtilities.invokeLater(() -> {
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            logArea.append("[" + time + "] " + msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void startStatsUpdater() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AntiDDoS-Stats");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(() -> {
            try {
                ProxyManager pm = ProxyManager.getInstance();
                FirewallConfig config = FirewallConfig.getInstance();

                SwingUtilities.invokeLater(() -> {
                    lblActiveProxies.setText("Proxy Active: " + pm.getActiveCount());
                    lblProtectedPorts.setText("Ports Saved: " + config.protectedPorts.size());
                    lblTotalConn.setText("Total Conn: " + pm.getTotalConnections());
                    lblTotalBlocked.setText("Blocked: " + pm.getTotalBlocked());
                    lblActiveConn.setText("Active Conn: " + pm.getTotalActiveConnections());

                    // Update table stats (connections + blocked) dynamically
                    for (int i = 0; i < modelProtected.getRowCount() && i < config.protectedPorts.size(); i++) {
                        FirewallConfig.ProtectedPort pp = config.protectedPorts.get(i);
                        boolean running = pm.isRunning(pp.listenPort);
                        TCPProxy proxy = pm.getProxy(pp.listenPort);

                        modelProtected.setValueAt(running ? "🟢 Running" : "🔴 Stopped", i, 4);
                        if (proxy != null) {
                            modelProtected.setValueAt(String.valueOf(proxy.getTotalConnections()), i, 5);
                            modelProtected.setValueAt(String.valueOf(proxy.getBlockedConnections()), i, 6);
                        }
                    }
                });
            } catch (Exception ignored) {}
        }, 1, 2, TimeUnit.SECONDS);
    }

    public void stop() {
        if (scheduler != null) scheduler.shutdownNow();
    }
}
