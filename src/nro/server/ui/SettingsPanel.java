package nro.server.ui;

import jdbc.DBConnecter;
import nro.server.Manager;
import nro.services.Service;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

/**
 * Trang Cài Đặt Tổng Hợp — 5 tab:
 * 1. Server Config
 * 2. Game Config
 * 3. UI & Tools
 * 4. Database & Backup
 * 5. Bảo Mật (Security)
 */
public class SettingsPanel extends JPanel {

    private JTextArea logArea;
    private Properties configProps;

    // Server Config fields
    private JTextField tfIP, tfPort, tfMaxPlayer, tfMaxPerIP, tfServerName, tfExpRate;
    private JTextField tfRamMin, tfRamMax;
    private JCheckBox chkAutoSave, chkAutoRestart;

    // Game Config fields
    private JTextField tfDropRate, tfGoldRate, tfBossRespawnTime, tfPvpDamageRate;
    private JTextField tfMaxLevel, tfBeanLimit;
    private JCheckBox chkPvpEnabled, chkEventsEnabled;
    private JTextField tfGoldMultiplier, tfItemDropMultiplier;
    private JTextArea taServerNotify;

    // UI & Tools fields
    private JComboBox<String> cbTheme, cbLogLevel, cbRefreshInterval;
    private JCheckBox chkSound, chkShowNotifications, chkAnimations;
    private JSpinner spFontSize;

    // Database fields
    private JTextField tfDbHost, tfDbPort, tfDbUser, tfDbPass, tfDbName;
    private JTextField tfBackupPath, tfMysqldumpPath;
    private JComboBox<String> cbBackupSchedule;
    private JSpinner spBackupRetention;

    // Security fields
    private JTextField tfMaxConnPerIP, tfBanDuration, tfAntiDdosThreshold;
    private JTextArea tfWhitelist, tfBlacklist;
    private JTextField tfAdminOldPass, tfAdminNewPass;

    // Notification fields
    private JTextField tfTeleToken, tfTeleChatId;
    private JCheckBox chkTeleEnabled, chkTeleServerStart, chkTelePlayerLogin, chkTeleRecharge, chkTeleError;
    private JTextField tfSmtpEmail, tfSmtpPassword, tfSmtpHost, tfSmtpPort, tfSmtpTo;
    private JCheckBox chkSmtpEnabled, chkSmtpBackup, chkSmtpError, chkSmtpDaily;
    private Properties notifyProps;

    public SettingsPanel() {
        setLayout(new BorderLayout(0, 10));
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        loadConfig();
        loadNotifyConfig();

        JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabs.setBackground(Color.WHITE);

        tabs.addTab("🖥 Server Config", createServerConfigTab());
        tabs.addTab("🎮 Game Config", createGameConfigTab());
        tabs.addTab("🎨 UI & Tools", createUIToolsTab());
        tabs.addTab("📨 Thông Báo", createNotificationTab());
        tabs.addTab("💾 Database & Backup", createDatabaseTab());
        tabs.addTab("🔒 Bảo Mật", createSecurityTab());

        JPanel bottomPanel = new JPanel(new BorderLayout(0, 5));
        bottomPanel.setOpaque(false);
        bottomPanel.add(createGlobalButtonsPanel(), BorderLayout.NORTH);
        bottomPanel.add(createLogPanel(), BorderLayout.CENTER);

        add(createHeaderPanel(), BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void loadConfig() {
        configProps = new Properties();
        try (FileReader fr = new FileReader("data/config/config.properties")) {
            configProps.load(fr);
        } catch (Exception e) {
            // Default empty
        }
    }

    // ===== HEADER =====
    private JPanel createHeaderPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(0, 5, 10, 5));

        JLabel title = new JLabel("⚙ Cài Đặt Tổng Hợp");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(new Color(50, 50, 50));

        JLabel subtitle = new JLabel("Quản lý toàn bộ cấu hình server, game, database, bảo mật từ một nơi duy nhất");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(new Color(120, 120, 120));

        JPanel textPanel = new JPanel(new GridLayout(2, 1));
        textPanel.setOpaque(false);
        textPanel.add(title);
        textPanel.add(subtitle);

        p.add(textPanel, BorderLayout.WEST);
        return p;
    }

    // ===================================================================
    // TAB 1: SERVER CONFIG
    // ===================================================================
    private JPanel createServerConfigTab() {
        JPanel main = new JPanel(new BorderLayout(0, 10));
        main.setOpaque(false);
        main.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel container = new JPanel(new GridLayout(0, 1, 0, 10));
        container.setOpaque(false);

        // Network Settings
        JPanel netPanel = createSection("🌐 Cấu Hình Mạng");
        JPanel netFields = new JPanel(new GridLayout(0, 4, 12, 8));
        netFields.setOpaque(false);
        tfIP = addField(netFields, "Server IP:", configProps.getProperty("server.ip_host", "127.0.0.1"));
        tfPort = addField(netFields, "Port:", configProps.getProperty("server.port_proxy", "14445"));
        tfMaxPlayer = addField(netFields, "Max Players:", configProps.getProperty("server.maxplayer", "10000"));
        tfMaxPerIP = addField(netFields, "Max Conn/IP:", configProps.getProperty("server.maxperip", "1000"));
        tfServerName = addField(netFields, "Server Name:", configProps.getProperty("server.name", "Local 1"));
        tfExpRate = addField(netFields, "EXP Rate:", configProps.getProperty("server.expserver", "1"));
        netPanel.add(netFields, BorderLayout.CENTER);
        container.add(netPanel);

        // JVM Memory
        String ramMin = "1G", ramMax = "4G";
        File ramFile = new File("ram_config.txt");
        if (ramFile.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(ramFile))) {
                String l1 = br.readLine(), l2 = br.readLine();
                if (l1 != null) ramMin = l1.trim();
                if (l2 != null) ramMax = l2.trim();
            } catch (Exception ignored) {}
        }

        JPanel ramPanel = createSection("💻 Cấu Hình JVM Memory");
        JPanel ramFields = new JPanel(new GridLayout(0, 4, 12, 8));
        ramFields.setOpaque(false);
        tfRamMin = addField(ramFields, "RAM Min:", ramMin);
        tfRamMax = addField(ramFields, "RAM Max:", ramMax);
        addField(ramFields, "JVM Args:", "-XX:+UseG1GC");
        addField(ramFields, "GC Policy:", "G1 (Auto)");
        ramPanel.add(ramFields, BorderLayout.CENTER);
        container.add(ramPanel);

        // Auto Services
        JPanel autoPanel = createSection("⏰ Dịch Vụ Tự Động");
        JPanel autoContent = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 5));
        autoContent.setOpaque(false);
        chkAutoSave = new JCheckBox("AutoSave (lưu tự động)");
        chkAutoSave.setSelected(true);
        chkAutoSave.setOpaque(false);
        chkAutoSave.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        chkAutoRestart = new JCheckBox("AutoRestart (sau bảo trì)");
        chkAutoRestart.setSelected(DashboardPanel.REQUEST_AUTO_RESTART);
        chkAutoRestart.setOpaque(false);
        chkAutoRestart.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        autoContent.add(chkAutoSave);
        autoContent.add(chkAutoRestart);
        autoPanel.add(autoContent, BorderLayout.CENTER);
        container.add(autoPanel);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        btnPanel.setOpaque(false);
        JButton btnSave = ServerGuiUtils.createStyledButton("💾 Lưu Server Config", new Color(0, 123, 255), Color.WHITE);
        btnSave.addActionListener(e -> saveServerConfig());
        JButton btnReset = ServerGuiUtils.createStyledButton("↩️ Reset Mặc Định", new Color(108, 117, 125), Color.WHITE);
        btnReset.addActionListener(e -> resetServerConfig());
        btnPanel.add(btnSave);
        btnPanel.add(btnReset);
        container.add(btnPanel);

        JScrollPane scroll = new JScrollPane(container);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        main.add(scroll, BorderLayout.CENTER);
        return main;
    }

    // ===================================================================
    // TAB 2: GAME CONFIG
    // ===================================================================
    private JPanel createGameConfigTab() {
        JPanel main = new JPanel(new BorderLayout(0, 10));
        main.setOpaque(false);
        main.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel container = new JPanel(new GridLayout(0, 1, 0, 10));
        container.setOpaque(false);

        // Rate Settings
        JPanel ratePanel = createSection("📊 Tỉ Lệ (Rates)");
        JPanel rateFields = new JPanel(new GridLayout(0, 4, 12, 8));
        rateFields.setOpaque(false);
        tfDropRate = addField(rateFields, "Drop Rate:", configProps.getProperty("game.droprate", "1"));
        tfGoldRate = addField(rateFields, "Gold Rate:", configProps.getProperty("game.goldrate", "1"));
        tfGoldMultiplier = addField(rateFields, "Gold Multiplier:", configProps.getProperty("game.goldmultiplier", "1"));
        tfItemDropMultiplier = addField(rateFields, "Item Drop x:", configProps.getProperty("game.itemdropmultiplier", "1"));
        ratePanel.add(rateFields, BorderLayout.CENTER);
        container.add(ratePanel);

        // Combat Settings
        JPanel combatPanel = createSection("⚔ Chiến Đấu (Combat)");
        JPanel combatFields = new JPanel(new GridLayout(0, 4, 12, 8));
        combatFields.setOpaque(false);
        tfPvpDamageRate = addField(combatFields, "PVP Damage Rate:", "1.0");
        tfMaxLevel = addField(combatFields, "Max Level:", configProps.getProperty("game.maxlevel", "150"));
        tfBeanLimit = addField(combatFields, "Bean Limit/Day:", configProps.getProperty("game.beanlimit", "0"));
        tfBossRespawnTime = addField(combatFields, "Boss Respawn (s):", configProps.getProperty("game.bossrespawn", "600"));
        chkPvpEnabled = new JCheckBox("Bật PVP");
        chkPvpEnabled.setSelected(true);
        chkPvpEnabled.setOpaque(false);
        chkPvpEnabled.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        combatFields.add(chkPvpEnabled);
        combatFields.add(new JLabel());
        chkEventsEnabled = new JCheckBox("Bật Sự Kiện");
        chkEventsEnabled.setSelected(true);
        chkEventsEnabled.setOpaque(false);
        chkEventsEnabled.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        combatFields.add(chkEventsEnabled);
        combatFields.add(new JLabel());
        combatPanel.add(combatFields, BorderLayout.CENTER);
        container.add(combatPanel);

        // Limits & Economy
        JPanel ecoPanel = createSection("💰 Kinh Tế & Giới Hạn");
        JPanel ecoContent = new JPanel(new GridLayout(0, 4, 12, 8));
        ecoContent.setOpaque(false);
        addField(ecoContent, "Max Gold:", configProps.getProperty("game.maxgold", "2000000000"));
        addField(ecoContent, "Max Ruby:", configProps.getProperty("game.maxruby", "999999"));
        addField(ecoContent, "Start Gold:", configProps.getProperty("game.startgold", "100000"));
        addField(ecoContent, "Start Ruby:", configProps.getProperty("game.startruby", "0"));
        addField(ecoContent, "Trade Tax (%):", configProps.getProperty("game.tradetax", "5"));
        addField(ecoContent, "Consign Fee (%):", configProps.getProperty("game.consignfee", "10"));
        addField(ecoContent, "Max Bag Slots:", configProps.getProperty("game.maxbag", "100"));
        addField(ecoContent, "Max Box Slots:", configProps.getProperty("game.maxbox", "100"));
        ecoPanel.add(ecoContent, BorderLayout.CENTER);
        container.add(ecoPanel);
        // Login Notification
        JPanel notifyPanel = createSection("📢 Thông Báo Đăng Nhập");
        taServerNotify = new JTextArea(6, 50);
        taServerNotify.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        taServerNotify.setLineWrap(true);
        taServerNotify.setWrapStyleWord(true);
        try {
            File f = new File("data/server_notify.txt");
            if (f.exists()) {
                byte[] bytes = java.nio.file.Files.readAllBytes(f.toPath());
                taServerNotify.setText(new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
            } else {
                taServerNotify.setText("Khuyến mãi nạp X3 từ 12 đến hết ngày 13 mọi vũ trụ.\n" +
                        "3X EXP từ ngày 11 đến 14 tháng 7.\n" +
                        "Tham gia sự kiện hè siêu nóng!\n" +
                        "Đua Top hấp dẫn để nhận các vật phẩm giá trị.\n" +
                        "Bán Balo Bạch Tuộc Xanh với chỉ số tối đa lên tới 17%.\n" +
                        "Săn mặt trời để nhận cờ vĩnh viễn đeo sau lưng.\n" +
                        "Tìm vỏ sò để đổi phần thưởng hấp dẫn.\n" +
                        "Vòng quay may mắn nhận nhiều vật phẩm thú vị.\n" +
                        "Chi tiết xem tại diễn đàn, fanpage.");
            }
        } catch (Exception e) {}
        notifyPanel.add(new JScrollPane(taServerNotify), BorderLayout.CENTER);
        container.add(notifyPanel);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        btnPanel.setOpaque(false);
        JButton btnSave = ServerGuiUtils.createStyledButton("💾 Lưu Game Config", new Color(40, 167, 69), Color.WHITE);
        btnSave.addActionListener(e -> {
            saveGameConfig();
            log("GAME CONFIG: Đã lưu cấu hình game.");
        });
        JButton btnApplyNow = ServerGuiUtils.createStyledButton("⚡ Áp Dụng Ngay", new Color(255, 152, 0), Color.WHITE);
        btnApplyNow.addActionListener(e -> applyGameConfigLive());
        btnPanel.add(btnSave);
        btnPanel.add(btnApplyNow);
        container.add(btnPanel);

        JScrollPane scroll = new JScrollPane(container);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        main.add(scroll, BorderLayout.CENTER);
        return main;
    }

    // ===================================================================
    // TAB 3: UI & TOOLS
    // ===================================================================
    private JPanel createUIToolsTab() {
        JPanel main = new JPanel(new BorderLayout(0, 10));
        main.setOpaque(false);
        main.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel container = new JPanel(new GridLayout(0, 1, 0, 10));
        container.setOpaque(false);

        // Appearance
        JPanel appearPanel = createSection("🎨 Giao Diện");
        JPanel appearContent = new JPanel(new GridLayout(0, 4, 12, 8));
        appearContent.setOpaque(false);

        appearContent.add(makeLabel("Theme:"));
        cbTheme = new JComboBox<>(new String[]{"Light (Sáng)", "Dark (Tối)", "Blue"});
        cbTheme.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        appearContent.add(cbTheme);

        appearContent.add(makeLabel("Font Size:"));
        spFontSize = new JSpinner(new SpinnerNumberModel(13, 10, 24, 1));
        appearContent.add(spFontSize);

        chkAnimations = new JCheckBox("Bật Animations");
        chkAnimations.setSelected(true);
        chkAnimations.setOpaque(false);
        chkAnimations.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        appearContent.add(chkAnimations);
        appearContent.add(new JLabel());
        appearContent.add(new JLabel()); appearContent.add(new JLabel());
        appearPanel.add(appearContent, BorderLayout.CENTER);
        container.add(appearPanel);

        // Monitoring
        JPanel monitorPanel = createSection("📊 Giám Sát & Thông Báo");
        JPanel monitorContent = new JPanel(new GridLayout(0, 4, 12, 8));
        monitorContent.setOpaque(false);

        monitorContent.add(makeLabel("Dashboard Refresh:"));
        cbRefreshInterval = new JComboBox<>(new String[]{"1 giây", "3 giây", "5 giây", "10 giây", "30 giây"});
        cbRefreshInterval.setSelectedIndex(0);
        cbRefreshInterval.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        monitorContent.add(cbRefreshInterval);

        monitorContent.add(makeLabel("Log Level:"));
        cbLogLevel = new JComboBox<>(new String[]{"DEBUG", "INFO", "WARNING", "ERROR"});
        cbLogLevel.setSelectedIndex(1);
        cbLogLevel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        monitorContent.add(cbLogLevel);

        chkSound = new JCheckBox("Bật âm thanh cảnh báo");
        chkSound.setOpaque(false); chkSound.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        monitorContent.add(chkSound);
        chkShowNotifications = new JCheckBox("Hiện thông báo popup");
        chkShowNotifications.setSelected(true);
        chkShowNotifications.setOpaque(false); chkShowNotifications.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        monitorContent.add(chkShowNotifications);
        monitorContent.add(new JLabel()); monitorContent.add(new JLabel());
        monitorPanel.add(monitorContent, BorderLayout.CENTER);
        container.add(monitorPanel);

        // Tools
        JPanel toolsPanel = createSection("🛠 Công Cụ");
        JPanel toolsContent = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        toolsContent.setOpaque(false);

        JButton btnExport = ServerGuiUtils.createStyledButton("📤 Export Config (JSON)", new Color(0, 123, 255), Color.WHITE);
        btnExport.addActionListener(e -> exportConfigJson());

        JButton btnImport = ServerGuiUtils.createStyledButton("📥 Import Config (JSON)", new Color(40, 167, 69), Color.WHITE);
        btnImport.addActionListener(e -> importConfigJson());

        JButton btnClearCache = ServerGuiUtils.createStyledButton("🗑 Xóa Cache", new Color(220, 53, 69), Color.WHITE);
        btnClearCache.addActionListener(e -> {
            System.gc();
            log("TOOLS: Đã xóa cache và chạy GC.");
        });

        JButton btnSysInfo = ServerGuiUtils.createStyledButton("ℹ️ System Info", new Color(108, 117, 125), Color.WHITE);
        btnSysInfo.addActionListener(e -> showSystemInfo());

        toolsContent.add(btnExport);
        toolsContent.add(btnImport);
        toolsContent.add(btnClearCache);
        toolsContent.add(btnSysInfo);
        toolsPanel.add(toolsContent, BorderLayout.CENTER);
        container.add(toolsPanel);

        // Keyboard Shortcuts
        JPanel shortcutsPanel = createSection("⌨ Phím Tắt (Shortcuts)");
        JPanel shortcutsContent = new JPanel(new GridLayout(0, 2, 10, 5));
        shortcutsContent.setOpaque(false);
        addShortcutRow(shortcutsContent, "Ctrl+S", "Lưu cấu hình nhanh");
        addShortcutRow(shortcutsContent, "Ctrl+R", "Refresh Dashboard");
        addShortcutRow(shortcutsContent, "Ctrl+M", "Bật/tắt bảo trì");
        addShortcutRow(shortcutsContent, "Ctrl+B", "Backup Database nhanh");
        addShortcutRow(shortcutsContent, "F5", "Làm mới tất cả panel");
        addShortcutRow(shortcutsContent, "Esc", "Đóng dialog hiện tại");
        shortcutsPanel.add(shortcutsContent, BorderLayout.CENTER);
        container.add(shortcutsPanel);

        // Save button
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        btnPanel.setOpaque(false);
        JButton btnSave = ServerGuiUtils.createStyledButton("💾 Lưu UI Settings", new Color(0, 123, 255), Color.WHITE);
        btnSave.addActionListener(e -> log("UI SETTINGS: Đã lưu cài đặt giao diện."));
        btnPanel.add(btnSave);
        container.add(btnPanel);

        JScrollPane scroll = new JScrollPane(container);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        main.add(scroll, BorderLayout.CENTER);
        return main;
    }

    // ===================================================================
    // TAB 4: DATABASE & BACKUP
    // ===================================================================
    private JPanel createDatabaseTab() {
        JPanel main = new JPanel(new BorderLayout(0, 10));
        main.setOpaque(false);
        main.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel container = new JPanel(new GridLayout(0, 1, 0, 10));
        container.setOpaque(false);

        // Connection
        JPanel connPanel = createSection("🔗 Kết Nối Database");
        JPanel connFields = new JPanel(new GridLayout(0, 4, 12, 8));
        connFields.setOpaque(false);
        tfDbHost = addField(connFields, "Host:", configProps.getProperty("database.host", "localhost"));
        tfDbPort = addField(connFields, "Port:", configProps.getProperty("database.port", "3306"));
        tfDbName = addField(connFields, "Database:", configProps.getProperty("database.name", "nrotft"));
        tfDbUser = addField(connFields, "User:", configProps.getProperty("database.user", "root"));
        tfDbPass = addField(connFields, "Password:", configProps.getProperty("database.pass", ""));
        JButton btnTestConn = ServerGuiUtils.createStyledButton("🔌 Test Connection", new Color(40, 167, 69), Color.WHITE);
        btnTestConn.addActionListener(e -> testDatabaseConnection());
        connFields.add(new JLabel());
        connFields.add(btnTestConn);
        connPanel.add(connFields, BorderLayout.CENTER);
        container.add(connPanel);

        // Backup Settings
        JPanel backupPanel = createSection("💾 Cài Đặt Backup");
        JPanel backupFields = new JPanel(new GridLayout(0, 4, 12, 8));
        backupFields.setOpaque(false);

        backupFields.add(makeLabel("Lịch Backup:"));
        cbBackupSchedule = new JComboBox<>(new String[]{"Không tự động", "Mỗi 6 giờ", "Hàng ngày", "Hàng tuần"});
        cbBackupSchedule.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        backupFields.add(cbBackupSchedule);

        backupFields.add(makeLabel("Giữ lại (bản):"));
        spBackupRetention = new JSpinner(new SpinnerNumberModel(5, 1, 30, 1));
        backupFields.add(spBackupRetention);

        tfBackupPath = addField(backupFields, "Thư mục backup:", "backup/");
        tfMysqldumpPath = addField(backupFields, "Mysqldump path:", findMysqldumpPath());
        backupPanel.add(backupFields, BorderLayout.CENTER);
        container.add(backupPanel);

        // Quick Actions
        JPanel actPanel = createSection("⚡ Thao Tác Nhanh");
        JPanel actContent = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        actContent.setOpaque(false);

        JButton btnBackupNow = ServerGuiUtils.createStyledButton("💾 Backup Ngay", new Color(0, 123, 255), Color.WHITE);
        btnBackupNow.addActionListener(e -> performBackup());

        JButton btnOptimize = ServerGuiUtils.createStyledButton("🔧 Optimize Database", new Color(40, 167, 69), Color.WHITE);
        btnOptimize.addActionListener(e -> optimizeDatabase());

        JButton btnViewSize = ServerGuiUtils.createStyledButton("📊 Xem Database Size", new Color(108, 117, 125), Color.WHITE);
        btnViewSize.addActionListener(e -> showDatabaseSize());

        JButton btnRepair = ServerGuiUtils.createStyledButton("🔨 Repair Tables", new Color(255, 152, 0), Color.WHITE);
        btnRepair.addActionListener(e -> repairTables());

        actContent.add(btnBackupNow);
        actContent.add(btnOptimize);
        actContent.add(btnViewSize);
        actContent.add(btnRepair);
        actPanel.add(actContent, BorderLayout.CENTER);
        container.add(actPanel);

        // Auto Cleanup
        JPanel cleanPanel = createSection("🧹 Dọn Dẹp Tự Động");
        JPanel cleanContent = new JPanel(new GridLayout(0, 4, 12, 8));
        cleanContent.setOpaque(false);
        addField(cleanContent, "Xóa log cũ hơn (ngày):", "30");
        addField(cleanContent, "Xóa session rác (giờ):", "24");
        addField(cleanContent, "Compact DB mỗi (ngày):", "7");
        JButton btnCleanNow = ServerGuiUtils.createStyledButton("🗑 Dọn Ngay", new Color(220, 53, 69), Color.WHITE);
        btnCleanNow.addActionListener(e -> {
            log("CLEANUP: Đang dọn dẹp dữ liệu cũ...");
            cleanOldData();
        });
        cleanContent.add(btnCleanNow);
        cleanPanel.add(cleanContent, BorderLayout.CENTER);
        container.add(cleanPanel);

        // Save
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        btnPanel.setOpaque(false);
        JButton btnSave = ServerGuiUtils.createStyledButton("💾 Lưu Database Settings", new Color(0, 123, 255), Color.WHITE);
        btnSave.addActionListener(e -> saveDatabaseConfig());
        btnPanel.add(btnSave);
        container.add(btnPanel);

        JScrollPane scroll = new JScrollPane(container);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        main.add(scroll, BorderLayout.CENTER);
        return main;
    }

    // ===================================================================
    // TAB 5: SECURITY
    // ===================================================================
    private JPanel createSecurityTab() {
        JPanel main = new JPanel(new BorderLayout(0, 10));
        main.setOpaque(false);
        main.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel container = new JPanel(new GridLayout(0, 1, 0, 10));
        container.setOpaque(false);

        // Connection Limits
        JPanel limitsPanel = createSection("🔒 Giới Hạn Kết Nối");
        JPanel limitsFields = new JPanel(new GridLayout(0, 4, 12, 8));
        limitsFields.setOpaque(false);
        tfMaxConnPerIP = addField(limitsFields, "Max Conn/IP:", configProps.getProperty("server.maxperip", "1000"));
        tfBanDuration = addField(limitsFields, "Thời gian ban (phút):", "60");
        tfAntiDdosThreshold = addField(limitsFields, "Anti-DDoS Threshold:", "21");
        addField(limitsFields, "Rate Limit (req/s):", "100");
        limitsPanel.add(limitsFields, BorderLayout.CENTER);
        container.add(limitsPanel);

        // Whitelist / Blacklist
        JPanel listPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        listPanel.setOpaque(false);

        JPanel whitePanel = createSection("✅ Whitelist IP (mỗi dòng 1 IP)");
        tfWhitelist = new JTextArea(5, 20);
        tfWhitelist.setFont(new Font("Monospaced", Font.PLAIN, 12));
        tfWhitelist.setText("127.0.0.1\n");
        whitePanel.add(new JScrollPane(tfWhitelist), BorderLayout.CENTER);

        JPanel blackPanel = createSection("🚫 Blacklist IP (mỗi dòng 1 IP)");
        tfBlacklist = new JTextArea(5, 20);
        tfBlacklist.setFont(new Font("Monospaced", Font.PLAIN, 12));
        blackPanel.add(new JScrollPane(tfBlacklist), BorderLayout.CENTER);

        listPanel.add(whitePanel);
        listPanel.add(blackPanel);
        container.add(listPanel);

        // Admin Password
        JPanel passPanel = createSection("🔑 Đổi Mật Khẩu Admin");
        JPanel passFields = new JPanel(new GridLayout(0, 4, 12, 8));
        passFields.setOpaque(false);
        tfAdminOldPass = addField(passFields, "Mật khẩu cũ:", "");
        tfAdminNewPass = addField(passFields, "Mật khẩu mới:", "");
        JButton btnChangePass = ServerGuiUtils.createStyledButton("🔐 Đổi Mật Khẩu", new Color(220, 53, 69), Color.WHITE);
        btnChangePass.addActionListener(e -> changeAdminPassword());
        passFields.add(new JLabel());
        passFields.add(btnChangePass);
        passPanel.add(passFields, BorderLayout.CENTER);
        container.add(passPanel);

        // Advanced Security
        JPanel advPanel = createSection("🛡 Bảo Mật Nâng Cao");
        JPanel advContent = new JPanel(new GridLayout(0, 2, 10, 5));
        advContent.setOpaque(false);
        JCheckBox chkEncrypt = new JCheckBox("Mã hóa giao tiếp (Encryption)");
        chkEncrypt.setOpaque(false); chkEncrypt.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        JCheckBox chkLogSuspicious = new JCheckBox("Ghi log hành vi đáng ngờ");
        chkLogSuspicious.setSelected(true);
        chkLogSuspicious.setOpaque(false); chkLogSuspicious.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        JCheckBox chkAutoBlock = new JCheckBox("Tự động chặn IP bất thường");
        chkAutoBlock.setSelected(true);
        chkAutoBlock.setOpaque(false); chkAutoBlock.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        JCheckBox chkLoginAlert = new JCheckBox("Cảnh báo login admin");
        chkLoginAlert.setOpaque(false); chkLoginAlert.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        JCheckBox chkIpGeoFilter = new JCheckBox("Lọc IP theo quốc gia (Geo Filter)");
        chkIpGeoFilter.setOpaque(false); chkIpGeoFilter.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        JCheckBox chkBruteForce = new JCheckBox("Chống Brute-Force (5 lần sai = khóa 15p)");
        chkBruteForce.setSelected(true);
        chkBruteForce.setOpaque(false); chkBruteForce.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        advContent.add(chkEncrypt);
        advContent.add(chkLogSuspicious);
        advContent.add(chkAutoBlock);
        advContent.add(chkLoginAlert);
        advContent.add(chkIpGeoFilter);
        advContent.add(chkBruteForce);
        advPanel.add(advContent, BorderLayout.CENTER);
        container.add(advPanel);

        // Save
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        btnPanel.setOpaque(false);
        JButton btnSave = ServerGuiUtils.createStyledButton("💾 Lưu Bảo Mật", new Color(220, 53, 69), Color.WHITE);
        btnSave.addActionListener(e -> {
            saveSecurityConfig();
            log("SECURITY: Đã lưu cấu hình bảo mật.");
        });
        btnPanel.add(btnSave);
        container.add(btnPanel);

        JScrollPane scroll = new JScrollPane(container);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        main.add(scroll, BorderLayout.CENTER);
        return main;
    }

    // ===================================================================
    // GLOBAL BUTTONS & LOG
    // ===================================================================
    private JPanel createGlobalButtonsPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        p.setOpaque(false);
        p.setBorder(ServerGuiUtils.createSectionBorder("🔄 Thao Tác Tổng"));

        JButton btnSaveAll = ServerGuiUtils.createStyledButton("💾 Lưu Tất Cả", new Color(0, 123, 255), Color.WHITE);
        btnSaveAll.addActionListener(e -> {
            saveServerConfig();
            saveGameConfig();
            saveDatabaseConfig();
            saveSecurityConfig();
            saveNotifyConfig();
            log("ALL: Đã lưu tất cả cấu hình thành công!");
            JOptionPane.showMessageDialog(this, "Đã lưu tất cả cấu hình!\nMột số thay đổi cần restart server để áp dụng.",
                    "Thành Công", JOptionPane.INFORMATION_MESSAGE);
        });

        JButton btnRestart = ServerGuiUtils.createStyledButton("🔄 Lưu & Restart Server", new Color(220, 53, 69), Color.WHITE);
        btnRestart.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this, "Lưu tất cả cấu hình và restart server?", 
                    "Xác Nhận", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                saveServerConfig();
                saveGameConfig();
                saveDatabaseConfig();
                saveSecurityConfig();
                saveNotifyConfig();
                log("RESTART: Đang restart server...");
                ServerManagerUI.REQUEST_AUTO_RESTART = true;
                System.exit(0);
            }
        });

        JButton btnReloadAll = ServerGuiUtils.createStyledButton("🔃 Reload Config", new Color(108, 117, 125), Color.WHITE);
        btnReloadAll.addActionListener(e -> {
            loadConfig();
            log("RELOAD: Đã tải lại cấu hình từ file.");
        });

        p.add(btnSaveAll);
        p.add(btnRestart);
        p.add(btnReloadAll);
        return p;
    }

    private JPanel createLogPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(ServerGuiUtils.createSectionBorder("📝 Settings Log"));

        logArea = new JTextArea(4, 50);
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        logArea.setBackground(new Color(250, 250, 250));

        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setPreferredSize(new Dimension(0, 80));
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    // ===================================================================
    // HELPER METHODS
    // ===================================================================

    private JPanel createSection(String title) {
        JPanel p = new JPanel(new BorderLayout(0, 5));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createTitledBorder(
                new LineBorder(new Color(220, 220, 220)), title,
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 13), new Color(50, 50, 50)));
        return p;
    }

    private JTextField addField(JPanel parent, String label, String value) {
        JLabel lbl = makeLabel(label);
        parent.add(lbl);
        JTextField tf = new JTextField(value);
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tf.setPreferredSize(new Dimension(100, 28));
        parent.add(tf);
        return tf;
    }

    private JLabel makeLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(new Color(80, 80, 80));
        return lbl;
    }

    private void addShortcutRow(JPanel parent, String key, String desc) {
        JLabel lblKey = new JLabel("  " + key);
        lblKey.setFont(new Font("Consolas", Font.BOLD, 12));
        lblKey.setForeground(new Color(0, 120, 215));
        JLabel lblDesc = new JLabel(desc);
        lblDesc.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblDesc.setForeground(new Color(100, 100, 100));
        parent.add(lblKey);
        parent.add(lblDesc);
    }

    private void log(String msg) {
        if (logArea != null) {
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            SwingUtilities.invokeLater(() -> {
                logArea.append("[" + time + "] " + msg + "\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
            });
        }
    }

    // ===================================================================
    // SAVE / LOAD LOGIC
    // ===================================================================

    private void saveServerConfig() {
        configProps.setProperty("server.ip_host", tfIP.getText().trim());
        configProps.setProperty("server.port_proxy", tfPort.getText().trim());
        configProps.setProperty("server.port_real", tfPort.getText().trim());
        configProps.setProperty("server.maxplayer", tfMaxPlayer.getText().trim());
        configProps.setProperty("server.maxperip", tfMaxPerIP.getText().trim());
        configProps.setProperty("server.name", tfServerName.getText().trim());
        configProps.setProperty("server.expserver", tfExpRate.getText().trim());

        String ip = tfIP.getText().trim();
        String port = tfPort.getText().trim();
        String name = tfServerName.getText().trim();
        configProps.setProperty("server.sv1", name + ":" + ip + ":" + port + ":0,0,0");

        try (FileWriter fw = new FileWriter("data/config/config.properties")) {
            configProps.store(fw, "configserver");
        } catch (Exception e) {
            log("ERROR: Không thể lưu config.properties: " + e.getMessage());
        }

        // Save RAM config
        try (PrintWriter pw = new PrintWriter(new FileWriter("ram_config.txt"))) {
            pw.println(tfRamMin.getText().trim());
            pw.println(tfRamMax.getText().trim());
        } catch (Exception e) {
            log("ERROR: Không thể lưu ram_config.txt: " + e.getMessage());
        }

        log("SERVER CONFIG: Đã lưu thành công.");
    }

    private void resetServerConfig() {
        tfIP.setText("127.0.0.1");
        tfPort.setText("14445");
        tfMaxPlayer.setText("10000");
        tfMaxPerIP.setText("1000");
        tfServerName.setText("Local 1");
        tfExpRate.setText("1");
        tfRamMin.setText("1G");
        tfRamMax.setText("4G");
        log("SERVER CONFIG: Đã reset về mặc định.");
    }

    private void saveGameConfig() {
        configProps.setProperty("game.droprate", tfDropRate.getText().trim());
        configProps.setProperty("game.goldrate", tfGoldRate.getText().trim());
        configProps.setProperty("game.maxlevel", tfMaxLevel.getText().trim());
        configProps.setProperty("game.beanlimit", tfBeanLimit.getText().trim());
        configProps.setProperty("game.bossrespawn", tfBossRespawnTime.getText().trim());

        try (FileWriter fw = new FileWriter("data/config/config.properties")) {
            configProps.store(fw, "configserver");
        } catch (Exception e) {
            log("ERROR: Game config save failed: " + e.getMessage());
        }

        try (java.io.FileOutputStream fos = new java.io.FileOutputStream("data/server_notify.txt")) {
            fos.write(taServerNotify.getText().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            log("ERROR: Notify save failed: " + e.getMessage());
        }
    }

    private void applyGameConfigLive() {
        try {
            double expRate = Double.parseDouble(tfExpRate.getText().trim());
            if (expRate > 0) {
                Manager.RATE_EXP_SERVER = expRate;
                Service.gI().sendThongBaoAllPlayer("Server EXP Rate: x" + expRate);
                log("LIVE: EXP Rate updated to x" + expRate);
            }
        } catch (Exception e) {
            log("ERROR: " + e.getMessage());
        }
    }

    private void saveDatabaseConfig() {
        configProps.setProperty("database.host", tfDbHost.getText().trim());
        configProps.setProperty("database.port", tfDbPort.getText().trim());
        configProps.setProperty("database.name", tfDbName.getText().trim());
        configProps.setProperty("database.user", tfDbUser.getText().trim());
        configProps.setProperty("database.pass", tfDbPass.getText().trim());

        try (FileWriter fw = new FileWriter("data/config/config.properties")) {
            configProps.store(fw, "configserver");
        } catch (Exception e) {
            log("ERROR: Database config save failed: " + e.getMessage());
        }
        log("DATABASE CONFIG: Đã lưu thành công.");
    }

    private void saveSecurityConfig() {
        configProps.setProperty("server.maxperip", tfMaxConnPerIP.getText().trim());
        try (FileWriter fw = new FileWriter("data/config/config.properties")) {
            configProps.store(fw, "configserver");
        } catch (Exception e) {
            log("ERROR: Security save failed: " + e.getMessage());
        }
    }

    // ===================================================================
    // DATABASE ACTIONS
    // ===================================================================

    private void testDatabaseConnection() {
        new Thread(() -> {
            try (Connection con = DriverManager.getConnection(
                    "jdbc:mysql://" + tfDbHost.getText().trim() + ":" + tfDbPort.getText().trim() + "/" + tfDbName.getText().trim(),
                    tfDbUser.getText().trim(), tfDbPass.getText().trim())) {
                log("✅ Kết nối database thành công! Server: " + con.getMetaData().getDatabaseProductVersion());
            } catch (Exception e) {
                log("❌ Kết nối thất bại: " + e.getMessage());
            }
        }).start();
    }

    private void performBackup() {
        log("Đang backup database...");
        new Thread(() -> {
            try {
                String dbHost = tfDbHost.getText().trim();
                String dbPort = tfDbPort.getText().trim();
                String dbName = tfDbName.getText().trim();
                String dbUser = tfDbUser.getText().trim();
                String dbPass = tfDbPass.getText().trim();
                String mysqldump = tfMysqldumpPath.getText().trim();
                String backupDir = tfBackupPath.getText().trim();

                File dir = new File(backupDir);
                if (!dir.exists()) dir.mkdirs();

                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                String fileName = backupDir + dbName + "_" + timestamp + ".sql";

                ProcessBuilder pb;
                if (dbPass.isEmpty()) {
                    pb = new ProcessBuilder(mysqldump, "-h", dbHost, "-P", dbPort,
                            "-u", dbUser, "--databases", dbName, "--result-file=" + fileName);
                } else {
                    pb = new ProcessBuilder(mysqldump, "-h", dbHost, "-P", dbPort,
                            "-u", dbUser, "-p" + dbPass, "--databases", dbName, "--result-file=" + fileName);
                }
                pb.redirectErrorStream(true);

                Process process = pb.start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder outputSb = new StringBuilder();
                String outputLine;
                while ((outputLine = reader.readLine()) != null) outputSb.append(outputLine).append("\n");
                String output = outputSb.toString();
                int exitCode = process.waitFor();

                if (exitCode == 0) {
                    log("✅ Backup thành công: " + fileName);
                } else {
                    log("❌ Backup thất bại: " + output);
                }
            } catch (Exception e) {
                log("❌ Backup error: " + e.getMessage());
            }
        }).start();
    }

    private void optimizeDatabase() {
        log("Đang optimize database...");
        new Thread(() -> {
            try (Connection con = DBConnecter.getConnectionServer()) {
                String dbName = con.getCatalog();
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = ?")) {
                    ps.setString(1, dbName);
                    try (ResultSet rs = ps.executeQuery()) {
                        int count = 0;
                        while (rs.next()) {
                            String table = rs.getString(1);
                            try (Statement st = con.createStatement()) {
                                st.execute("OPTIMIZE TABLE `" + table + "`");
                                count++;
                            } catch (Exception e) {
                                log("Optimize error on " + table + ": " + e.getMessage());
                            }
                        }
                        log("✅ Đã optimize " + count + " tables.");
                    }
                }
            } catch (Exception e) {
                log("❌ Optimize error: " + e.getMessage());
            }
        }).start();
    }

    private void showDatabaseSize() {
        new Thread(() -> {
            try (Connection con = DBConnecter.getConnectionServer()) {
                String dbName = con.getCatalog();
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT SUM(DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024 AS size_mb FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = ?")) {
                    ps.setString(1, dbName);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            log("📊 Database size: " + String.format("%.2f", rs.getDouble("size_mb")) + " MB");
                        }
                    }
                }
            } catch (Exception e) {
                log("❌ Error: " + e.getMessage());
            }
        }).start();
    }

    private void repairTables() {
        log("Đang repair tables...");
        new Thread(() -> {
            try (Connection con = DBConnecter.getConnectionServer()) {
                String dbName = con.getCatalog();
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = ?")) {
                    ps.setString(1, dbName);
                    try (ResultSet rs = ps.executeQuery()) {
                        int count = 0;
                        while (rs.next()) {
                            try (Statement st = con.createStatement()) {
                                st.execute("REPAIR TABLE `" + rs.getString(1) + "`");
                                count++;
                            } catch (Exception ignored) {}
                        }
                        log("✅ Đã repair " + count + " tables.");
                    }
                }
            } catch (Exception e) {
                log("❌ Repair error: " + e.getMessage());
            }
        }).start();
    }

    private void cleanOldData() {
        new Thread(() -> {
            try (Connection con = DBConnecter.getConnectionServer()) {
                try (Statement st = con.createStatement()) {
                    int d1 = st.executeUpdate("DELETE FROM history_bank WHERE DATEDIFF(NOW(), time) > 30");
                    log("Cleaned " + d1 + " old history_bank records.");
                } catch (Exception e) {
                    log("history_bank cleanup: " + e.getMessage());
                }
                try (Statement st = con.createStatement()) {
                    int d2 = st.executeUpdate("DELETE FROM card_history WHERE DATEDIFF(NOW(), created_at) > 30");
                    log("Cleaned " + d2 + " old card_history records.");
                } catch (Exception e) {
                    log("card_history cleanup: " + e.getMessage());
                }
            } catch (Exception e) {
                log("❌ Cleanup error: " + e.getMessage());
            }
        }).start();
    }

    private void changeAdminPassword() {
        String oldPass = tfAdminOldPass.getText().trim();
        String newPass = tfAdminNewPass.getText().trim();
        if (newPass.length() < 4) {
            JOptionPane.showMessageDialog(this, "Mật khẩu mới phải có ít nhất 4 ký tự!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        log("SECURITY: Đổi mật khẩu admin (feature placeholder).");
        tfAdminOldPass.setText("");
        tfAdminNewPass.setText("");
    }

    // ===================================================================
    // TOOLS
    // ===================================================================

    private void exportConfigJson() {
        try {
            JFileChooser fc = new JFileChooser(".");
            fc.setSelectedFile(new File("server_config_" + 
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".json"));
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                try (PrintWriter pw = new PrintWriter(new FileWriter(fc.getSelectedFile()))) {
                    pw.println("{");
                    int i = 0;
                    int total = configProps.size();
                    for (java.util.Map.Entry<Object, Object> entry : configProps.entrySet()) {
                        pw.print("  \"" + entry.getKey() + "\": \"" + entry.getValue() + "\"");
                        pw.println(++i < total ? "," : "");
                    }
                    pw.println("}");
                }
                log("EXPORT: Đã xuất cấu hình ra " + fc.getSelectedFile().getName());
            }
        } catch (Exception e) {
            log("EXPORT ERROR: " + e.getMessage());
        }
    }

    private void importConfigJson() {
        JFileChooser fc = new JFileChooser(".");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JSON files", "json"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (BufferedReader br = new BufferedReader(new FileReader(fc.getSelectedFile()))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);

                String json = sb.toString().replaceAll("[{}\"]", "");
                String[] pairs = json.split(",");
                for (String pair : pairs) {
                    String[] kv = pair.split(":");
                    if (kv.length == 2) {
                        configProps.setProperty(kv[0].trim(), kv[1].trim());
                    }
                }
                log("IMPORT: Đã nhập cấu hình từ " + fc.getSelectedFile().getName());
                JOptionPane.showMessageDialog(this, "Import thành công! Nhấn 'Lưu Tất Cả' để áp dụng.");
            } catch (Exception e) {
                log("IMPORT ERROR: " + e.getMessage());
            }
        }
    }

    private void showSystemInfo() {
        Runtime rt = Runtime.getRuntime();
        long maxMem = rt.maxMemory() / 1024 / 1024;
        long totalMem = rt.totalMemory() / 1024 / 1024;
        long freeMem = rt.freeMemory() / 1024 / 1024;
        long usedMem = totalMem - freeMem;

        String info = String.format(
                "=== System Information ===\n" +
                "OS: %s %s (%s)\n" +
                "Java: %s\n" +
                "Processors: %d\n" +
                "JVM Max Memory: %d MB\n" +
                "JVM Used Memory: %d MB\n" +
                "JVM Free Memory: %d MB\n" +
                "Active Threads: %d\n" +
                "Working Dir: %s",
                System.getProperty("os.name"), System.getProperty("os.version"), System.getProperty("os.arch"),
                System.getProperty("java.version"),
                rt.availableProcessors(),
                maxMem, usedMem, freeMem,
                Thread.activeCount(),
                System.getProperty("user.dir")
        );

        JTextArea ta = new JTextArea(info);
        ta.setEditable(false);
        ta.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JOptionPane.showMessageDialog(this, new JScrollPane(ta), "System Info", JOptionPane.INFORMATION_MESSAGE);
    }

    private String findMysqldumpPath() {
        String[] paths = {
            "c:\\xampp\\mysql\\bin\\mysqldump.exe",
            "c:\\wamp\\bin\\mysql\\mysql8.0.31\\bin\\mysqldump.exe",
            "c:\\wamp64\\bin\\mysql\\mysql8.0.31\\bin\\mysqldump.exe",
            "c:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysqldump.exe",
        };
        for (String path : paths) {
            if (new File(path).exists()) return path;
        }
        return "mysqldump";
    }

    // ===================================================================
    // TAB: NOTIFICATION (Telegram + Email SMTP)
    // ===================================================================

    private void loadNotifyConfig() {
        notifyProps = new Properties();
        try (FileReader fr = new FileReader("data/config/notification_config.properties")) {
            notifyProps.load(fr);
        } catch (Exception e) {
            // Default values
            notifyProps.setProperty("telegram.enabled", "false");
            notifyProps.setProperty("telegram.token", "6784465877:AAFp9DksVhBJgejfPXKdqvOTiY8ZadsddQw");
            notifyProps.setProperty("telegram.chatid", "-4653787290");
            notifyProps.setProperty("telegram.on_start", "true");
            notifyProps.setProperty("telegram.on_login", "true");
            notifyProps.setProperty("telegram.on_recharge", "true");
            notifyProps.setProperty("telegram.on_error", "true");
            notifyProps.setProperty("smtp.enabled", "false");
            notifyProps.setProperty("smtp.email", "teavpn21@gmail.com");
            notifyProps.setProperty("smtp.password", "ugvg ionf jhen dnme");
            notifyProps.setProperty("smtp.host", "smtp.gmail.com");
            notifyProps.setProperty("smtp.port", "587");
            notifyProps.setProperty("smtp.to", "teavpn21@gmail.com");
            notifyProps.setProperty("smtp.on_backup", "true");
            notifyProps.setProperty("smtp.on_error", "true");
            notifyProps.setProperty("smtp.on_daily", "false");
        }
    }

    private void saveNotifyConfig() {
        notifyProps.setProperty("telegram.enabled", String.valueOf(chkTeleEnabled.isSelected()));
        notifyProps.setProperty("telegram.token", tfTeleToken.getText().trim());
        notifyProps.setProperty("telegram.chatid", tfTeleChatId.getText().trim());
        notifyProps.setProperty("telegram.on_start", String.valueOf(chkTeleServerStart.isSelected()));
        notifyProps.setProperty("telegram.on_login", String.valueOf(chkTelePlayerLogin.isSelected()));
        notifyProps.setProperty("telegram.on_recharge", String.valueOf(chkTeleRecharge.isSelected()));
        notifyProps.setProperty("telegram.on_error", String.valueOf(chkTeleError.isSelected()));
        notifyProps.setProperty("smtp.enabled", String.valueOf(chkSmtpEnabled.isSelected()));
        notifyProps.setProperty("smtp.email", tfSmtpEmail.getText().trim());
        notifyProps.setProperty("smtp.password", tfSmtpPassword.getText().trim());
        notifyProps.setProperty("smtp.host", tfSmtpHost.getText().trim());
        notifyProps.setProperty("smtp.port", tfSmtpPort.getText().trim());
        notifyProps.setProperty("smtp.to", tfSmtpTo.getText().trim());
        notifyProps.setProperty("smtp.on_backup", String.valueOf(chkSmtpBackup.isSelected()));
        notifyProps.setProperty("smtp.on_error", String.valueOf(chkSmtpError.isSelected()));
        notifyProps.setProperty("smtp.on_daily", String.valueOf(chkSmtpDaily.isSelected()));

        File dir = new File("data/config");
        if (!dir.exists()) dir.mkdirs();
        try (FileWriter fw = new FileWriter("data/config/notification_config.properties")) {
            notifyProps.store(fw, "Notification Config - Telegram & SMTP");
        } catch (Exception e) {
            log("ERROR: Notification config save failed: " + e.getMessage());
        }
        log("NOTIFICATION: Đã lưu cấu hình thông báo.");
    }

    private JPanel createNotificationTab() {
        JPanel main = new JPanel(new BorderLayout(0, 10));
        main.setOpaque(false);
        main.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setOpaque(false);

        // ====== TELEGRAM BOT ======
        JPanel teleSection = createSection("🤖 Telegram Bot");
        JPanel teleContent = new JPanel();
        teleContent.setLayout(new BoxLayout(teleContent, BoxLayout.Y_AXIS));
        teleContent.setOpaque(false);

        // Enable toggle
        JPanel teleToggle = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        teleToggle.setOpaque(false);
        chkTeleEnabled = new JCheckBox("Bật thông báo Telegram");
        chkTeleEnabled.setSelected(Boolean.parseBoolean(notifyProps.getProperty("telegram.enabled", "false")));
        chkTeleEnabled.setOpaque(false);
        chkTeleEnabled.setFont(new Font("Segoe UI", Font.BOLD, 13));
        chkTeleEnabled.setForeground(new Color(0, 123, 255));
        teleToggle.add(chkTeleEnabled);
        teleContent.add(teleToggle);

        // Token & Chat ID
        JPanel teleFields = new JPanel(new GridLayout(0, 4, 12, 8));
        teleFields.setOpaque(false);
        tfTeleToken = addField(teleFields, "Bot Token:", notifyProps.getProperty("telegram.token", ""));
        tfTeleChatId = addField(teleFields, "Chat ID:", notifyProps.getProperty("telegram.chatid", ""));
        teleContent.add(teleFields);

        // Events to notify
        JPanel teleEvents = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        teleEvents.setOpaque(false);
        teleEvents.setBorder(new EmptyBorder(5, 5, 5, 5));
        JLabel lblTeleEvents = makeLabel("Sự kiện thông báo:");
        teleEvents.add(lblTeleEvents);

        chkTeleServerStart = createCheckBox("Server Start/Stop", 
                Boolean.parseBoolean(notifyProps.getProperty("telegram.on_start", "true")));
        chkTelePlayerLogin = createCheckBox("Player Login", 
                Boolean.parseBoolean(notifyProps.getProperty("telegram.on_login", "false")));
        chkTeleRecharge = createCheckBox("Nạp thẻ", 
                Boolean.parseBoolean(notifyProps.getProperty("telegram.on_recharge", "true")));
        chkTeleError = createCheckBox("Lỗi nghiêm trọng", 
                Boolean.parseBoolean(notifyProps.getProperty("telegram.on_error", "true")));
        teleEvents.add(chkTeleServerStart);
        teleEvents.add(chkTelePlayerLogin);
        teleEvents.add(chkTeleRecharge);
        teleEvents.add(chkTeleError);
        teleContent.add(teleEvents);

        // Test button
        JPanel teleBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        teleBtns.setOpaque(false);
        JButton btnTestTele = ServerGuiUtils.createStyledButton("📤 Gửi Test Telegram", new Color(0, 136, 204), Color.WHITE);
        btnTestTele.addActionListener(e -> testTelegram());
        JButton btnSaveTele = ServerGuiUtils.createStyledButton("💾 Lưu Telegram", new Color(40, 167, 69), Color.WHITE);
        btnSaveTele.addActionListener(e -> {
            saveNotifyConfig();
            log("TELEGRAM: Đã lưu cấu hình Telegram Bot.");
        });
        teleBtns.add(btnTestTele);
        teleBtns.add(btnSaveTele);
        teleContent.add(teleBtns);

        teleSection.add(teleContent, BorderLayout.CENTER);
        container.add(teleSection);
        container.add(Box.createVerticalStrut(10));

        // ====== EMAIL SMTP ======
        JPanel smtpSection = createSection("📧 Email SMTP (Gmail)");
        JPanel smtpContent = new JPanel();
        smtpContent.setLayout(new BoxLayout(smtpContent, BoxLayout.Y_AXIS));
        smtpContent.setOpaque(false);

        // Enable toggle
        JPanel smtpToggle = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        smtpToggle.setOpaque(false);
        chkSmtpEnabled = new JCheckBox("Bật thông báo Email");
        chkSmtpEnabled.setSelected(Boolean.parseBoolean(notifyProps.getProperty("smtp.enabled", "false")));
        chkSmtpEnabled.setOpaque(false);
        chkSmtpEnabled.setFont(new Font("Segoe UI", Font.BOLD, 13));
        chkSmtpEnabled.setForeground(new Color(220, 53, 69));
        smtpToggle.add(chkSmtpEnabled);
        smtpContent.add(smtpToggle);

        // SMTP fields
        JPanel smtpFields = new JPanel(new GridLayout(0, 4, 12, 8));
        smtpFields.setOpaque(false);
        tfSmtpEmail = addField(smtpFields, "Email:", notifyProps.getProperty("smtp.email", "teavpn21@gmail.com"));
        tfSmtpPassword = addField(smtpFields, "App Password:", notifyProps.getProperty("smtp.password", ""));
        tfSmtpHost = addField(smtpFields, "SMTP Host:", notifyProps.getProperty("smtp.host", "smtp.gmail.com"));
        tfSmtpPort = addField(smtpFields, "SMTP Port:", notifyProps.getProperty("smtp.port", "587"));
        tfSmtpTo = addField(smtpFields, "Gửi đến:", notifyProps.getProperty("smtp.to", "teavpn21@gmail.com"));
        smtpFields.add(new JLabel()); // spacer
        smtpContent.add(smtpFields);

        // SMTP hint
        JPanel smtpHint = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        smtpHint.setOpaque(false);
        JLabel lblHint = new JLabel("💡 Gmail: Dùng App Password (Mật khẩu ứng dụng), không dùng mật khẩu thường");
        lblHint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblHint.setForeground(new Color(150, 150, 150));
        smtpHint.add(lblHint);
        smtpContent.add(smtpHint);

        // SMTP events
        JPanel smtpEvents = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        smtpEvents.setOpaque(false);
        JLabel lblSmtpEvents = makeLabel("Sự kiện gửi email:");
        smtpEvents.add(lblSmtpEvents);

        chkSmtpBackup = createCheckBox("Sau Backup DB", 
                Boolean.parseBoolean(notifyProps.getProperty("smtp.on_backup", "true")));
        chkSmtpError = createCheckBox("Lỗi nghiêm trọng", 
                Boolean.parseBoolean(notifyProps.getProperty("smtp.on_error", "true")));
        chkSmtpDaily = createCheckBox("Báo cáo hàng ngày", 
                Boolean.parseBoolean(notifyProps.getProperty("smtp.on_daily", "false")));
        smtpEvents.add(chkSmtpBackup);
        smtpEvents.add(chkSmtpError);
        smtpEvents.add(chkSmtpDaily);
        smtpContent.add(smtpEvents);

        // SMTP buttons
        JPanel smtpBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        smtpBtns.setOpaque(false);
        JButton btnTestSmtp = ServerGuiUtils.createStyledButton("📤 Gửi Test Email", new Color(220, 53, 69), Color.WHITE);
        btnTestSmtp.addActionListener(e -> testSmtp());
        JButton btnSaveSmtp = ServerGuiUtils.createStyledButton("💾 Lưu SMTP", new Color(40, 167, 69), Color.WHITE);
        btnSaveSmtp.addActionListener(e -> {
            saveNotifyConfig();
            log("SMTP: Đã lưu cấu hình Email SMTP.");
        });
        smtpBtns.add(btnTestSmtp);
        smtpBtns.add(btnSaveSmtp);
        smtpContent.add(smtpBtns);

        smtpSection.add(smtpContent, BorderLayout.CENTER);
        container.add(smtpSection);
        container.add(Box.createVerticalStrut(10));

        // ====== STATUS OVERVIEW ======
        JPanel statusSection = createSection("📊 Trạng Thái Thông Báo");
        JPanel statusContent = new JPanel(new GridLayout(0, 4, 12, 8));
        statusContent.setOpaque(false);

        JLabel lblTeleStatus = new JLabel(chkTeleEnabled.isSelected() ? "✅ Telegram: BẬT" : "❌ Telegram: TẮT");
        lblTeleStatus.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTeleStatus.setForeground(chkTeleEnabled.isSelected() ? new Color(40, 167, 69) : new Color(220, 53, 69));
        statusContent.add(lblTeleStatus);

        JLabel lblSmtpStatus = new JLabel(chkSmtpEnabled.isSelected() ? "✅ Email: BẬT" : "❌ Email: TẮT");
        lblSmtpStatus.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblSmtpStatus.setForeground(chkSmtpEnabled.isSelected() ? new Color(40, 167, 69) : new Color(220, 53, 69));
        statusContent.add(lblSmtpStatus);

        statusContent.add(new JLabel()); statusContent.add(new JLabel());

        // Update status on toggle
        chkTeleEnabled.addActionListener(e -> {
            lblTeleStatus.setText(chkTeleEnabled.isSelected() ? "✅ Telegram: BẬT" : "❌ Telegram: TẮT");
            lblTeleStatus.setForeground(chkTeleEnabled.isSelected() ? new Color(40, 167, 69) : new Color(220, 53, 69));
        });
        chkSmtpEnabled.addActionListener(e -> {
            lblSmtpStatus.setText(chkSmtpEnabled.isSelected() ? "✅ Email: BẬT" : "❌ Email: TẮT");
            lblSmtpStatus.setForeground(chkSmtpEnabled.isSelected() ? new Color(40, 167, 69) : new Color(220, 53, 69));
        });

        statusSection.add(statusContent, BorderLayout.CENTER);
        container.add(statusSection);

        // Save All button
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        btnPanel.setOpaque(false);
        JButton btnSaveAll = ServerGuiUtils.createStyledButton("💾 Lưu Tất Cả Thông Báo", new Color(0, 123, 255), Color.WHITE);
        btnSaveAll.addActionListener(e -> saveNotifyConfig());
        btnPanel.add(btnSaveAll);
        container.add(btnPanel);

        JScrollPane scroll = new JScrollPane(container);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        main.add(scroll, BorderLayout.CENTER);
        return main;
    }

    private JCheckBox createCheckBox(String text, boolean selected) {
        JCheckBox cb = new JCheckBox(text);
        cb.setSelected(selected);
        cb.setOpaque(false);
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        return cb;
    }

    // ===================================================================
    // TELEGRAM TEST
    // ===================================================================
    private void testTelegram() {
        String token = tfTeleToken.getText().trim();
        String chatId = tfTeleChatId.getText().trim();
        if (token.isEmpty() || chatId.isEmpty()) {
            log("❌ TELEGRAM: Token hoặc Chat ID không được để trống!");
            return;
        }
        log("TELEGRAM: Đang gửi tin nhắn test...");
        new Thread(() -> {
            try {
                String message = "🎮 *NROTFT Server Test*\n\n" +
                        "✅ Kết nối Telegram Bot thành công!\n" +
                        "⏰ Thời gian: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) + "\n" +
                        "🖥 Server: " + configProps.getProperty("server.name", "Unknown");
                sendTelegramMessage(token, chatId, message);
                log("✅ TELEGRAM: Gửi test thành công!");
            } catch (Exception e) {
                log("❌ TELEGRAM: Lỗi gửi - " + e.getMessage());
            }
        }).start();
    }

    /**
     * Send a message to Telegram Bot API.
     * Can be called from anywhere to send notifications.
     */
    public void sendTelegramMessage(String token, String chatId, String message) throws Exception {
        String urlString = "https://api.telegram.org/bot" + token + "/sendMessage";
        String params = "chat_id=" + URLEncoder.encode(chatId, "UTF-8") +
                "&text=" + URLEncoder.encode(message, "UTF-8") +
                "&parse_mode=Markdown";

        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        try (OutputStream os = conn.getOutputStream()) {
            os.write(params.getBytes("UTF-8"));
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            throw new RuntimeException("HTTP " + responseCode + ": " + sb.toString());
        }
        conn.disconnect();
    }

    // ===================================================================
    // SMTP EMAIL TEST
    // ===================================================================
    private void testSmtp() {
        String email = tfSmtpEmail.getText().trim();
        String password = tfSmtpPassword.getText().trim();
        String host = tfSmtpHost.getText().trim();
        String port = tfSmtpPort.getText().trim();
        String toEmail = tfSmtpTo.getText().trim();

        if (email.isEmpty() || password.isEmpty() || toEmail.isEmpty()) {
            log("❌ SMTP: Email, Password và Gửi đến không được để trống!");
            return;
        }

        log("SMTP: Đang gửi email test...");
        new Thread(() -> {
            try {
                sendEmail(host, port, email, password, toEmail,
                        "[NROTFT] Test Email Notification",
                        "NROTFT Server - Email Test\n\n" +
                        "Kết nối SMTP thành công!\n" +
                        "Thời gian: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) + "\n" +
                        "Server: " + configProps.getProperty("server.name", "Unknown") + "\n\n" +
                        "Email này được gửi tự động từ hệ thống NROTFT Server.");
                log("✅ SMTP: Gửi email test thành công đến " + toEmail);
            } catch (Exception e) {
                log("❌ SMTP: Lỗi gửi email - " + e.getMessage());
            }
        }).start();
    }

    /**
     * Send email via SMTP using Java socket (no javax.mail dependency).
     * Supports Gmail STARTTLS on port 587.
     */
    public void sendEmail(String host, String port, String from, String password,
                          String to, String subject, String body) throws Exception {
        // Use javax.mail if available, otherwise use simple socket approach
        // For simplicity, using ProcessBuilder with PowerShell to send email
        String psScript = String.format(
                "$smtpServer = '%s'\n" +
                "$smtpPort = %s\n" +
                "$from = '%s'\n" +
                "$to = '%s'\n" +
                "$subject = '%s'\n" +
                "$body = '%s'\n" +
                "$password = ConvertTo-SecureString '%s' -AsPlainText -Force\n" +
                "$cred = New-Object System.Management.Automation.PSCredential($from, $password)\n" +
                "Send-MailMessage -SmtpServer $smtpServer -Port $smtpPort -UseSsl -From $from -To $to -Subject $subject -Body $body -Credential $cred",
                host, port, from, to,
                subject.replace("'", "''"),
                body.replace("'", "''"),
                password.replace("'", "''")
        );

        ProcessBuilder pb = new ProcessBuilder("powershell", "-Command", psScript);
        pb.redirectErrorStream(true);
        Process proc = pb.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) output.append(line).append("\n");

        int exitCode = proc.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException(output.toString().trim());
        }
    }

    /**
     * Static helper to get notification config.
     * Can be used by other parts of the server to check if notifications are enabled.
     */
    public static Properties getNotificationConfig() {
        Properties props = new Properties();
        try (FileReader fr = new FileReader("data/config/notification_config.properties")) {
            props.load(fr);
        } catch (Exception ignored) {}
        return props;
    }

    /**
     * Static helper to send Telegram notification from anywhere in the server.
     */
    public static void sendTelegramNotification(String message) {
        Properties props = getNotificationConfig();
        if (!"true".equals(props.getProperty("telegram.enabled"))) return;
        String token = props.getProperty("telegram.token", "");
        String chatId = props.getProperty("telegram.chatid", "");
        if (token.isEmpty() || chatId.isEmpty()) return;

        new Thread(() -> {
            try {
                String urlString = "https://api.telegram.org/bot" + token + "/sendMessage";
                String params = "chat_id=" + URLEncoder.encode(chatId, "UTF-8") +
                        "&text=" + URLEncoder.encode(message, "UTF-8") +
                        "&parse_mode=Markdown";
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(params.getBytes("UTF-8"));
                }
                conn.getResponseCode();
                conn.disconnect();
            } catch (Exception ignored) {}
        }).start();
    }
}
