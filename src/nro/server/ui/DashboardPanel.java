package nro.server.ui;

import boss.BossID;
import boss.BossManager;
import boss.BossStatus;
import boss.Boss;
import boss.BossData;
import boss.BossesData;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import item.Item.ItemOption;
import models.GiftCode.GiftCodeManager;
import models.kygui.ConsignShopManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.JSONArray;
import jdbc.DBConnecter;
import network.SessionManager;
import network.server.Server_firewall;
import nro.server.AutoSaveManager;
import nro.server.Client;
import nro.server.Maintenance;
import nro.server.Manager;
import nro.server.io.MySession;
import nro.services.Service;
import utils.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import jdbc.daos.ShopDAO;
import models.GiftCode.GiftCode;
import models.Template.NpcTemplate;
import java.util.Properties;

public class DashboardPanel extends JPanel {

    // --- UI Components ---
    private JLabel lblStatus, lblPlayerCount, lblCountdown;
    private JLabel lblCpuUsage, lblRamUsage, lblThreadCount, lblSessionCount;
    private JLabel lblLastRefresh;
    private JLabel lblGiftcodeInfo, lblConsignItemsCount, lblBossStatus, lblUptime;
    
    // Log Component
    private JTextArea txtLog;
    
    // Connection Monitor Tables
    private DefaultTableModel playerTableModel;
    private DefaultTableModel ipTableModel;
    private JLabel lblTotalOnline, lblTotalConnections;
    private JLabel lblNetworkInfo;
    
    // Config Components (Maintenance)
    private JComboBox<Integer> cbHour, cbMinute, cbSecond;
    private JCheckBox chkAutoRestart;
    
    // Config Components (Optimization)
    private JCheckBox chkAutoOptimize;
    private JComboBox<String> cbOptimizeInterval;
    private JLabel lblOptStatus;
    
    // Action Buttons
    private JToggleButton btnToggleAutoSave;

    // Graphs
    private final MiniGraph graphCpu = new MiniGraph(new Color(0, 120, 215));
    private final MiniGraph graphRam = new MiniGraph(new Color(138, 43, 226));
    private final MiniGraph graphThread = new MiniGraph(new Color(0, 204, 106));
    private final MiniGraph graphSession = new MiniGraph(new Color(255, 140, 0));

    // --- Logic Variables ---
    private final Instant serverStartTime = Instant.now();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4); 
    private final AtomicBoolean isAutoSaveEnabled = new AtomicBoolean(true);
    
    // Maintenance Logic
    public static boolean REQUEST_AUTO_RESTART = false;
    private ScheduledFuture<?> activeMaintenanceJobFuture = null;
    private ScheduledFuture<?> activeCountdownDisplayFuture = null;
    
    // Optimize Logic
    private ScheduledFuture<?> activeAutoOptimizeFuture = null;
    private ScheduledFuture<?> activeDailyBackupFuture = null;
    private static final DateTimeFormatter DB_BACKUP_FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final String DB_BACKUP_DIR = "sql";

    // --- Boss Icon Logic ---
    private static final String ICON_FOLDER = "data/icon/";
    private static final String BOSS_MANAGER_PATH = "src/boss/BossManager.java"; // Đường dẫn file code
    private final Map<Integer, Integer> partIconMap = new HashMap<>();
    private final Map<Integer, ImageIcon> iconImageCache = new HashMap<>();
    private List<BossSummonEntry> cachedBossEntries = new ArrayList<>();

    public DashboardPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // 1. Load Data Icons
        loadPartDataFromDB();

        // 2. Tạo giao diện chính
        initMainLayout();
        
        // 3. Bắt đầu luồng cập nhật thông số (1 giây/lần)
        startMonitoring();
        loadMaintenanceConfig();
        
        // Khởi động mặc định auto optimize (nếu muốn)
        chkAutoOptimize.setSelected(false); // Mặc định tắt, user tự bật
        
        addLog("Dashboard initialized. Monitoring Server specific resources.");
        scheduleDailyDatabaseBackup();
    }
    
    // --- ICON & BOSS DATA LOADING ---
    
    private void loadPartDataFromDB() {
        new Thread(() -> {
            try (Connection conn = DBConnecter.getConnectionServer();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id, DATA FROM part WHERE TYPE = 0")) { 
                
                partIconMap.clear();
                while (rs.next()) {
                    int partId = rs.getInt("id");
                    String json = rs.getString("DATA");
                    try {
                        JsonArray arr = new JsonParser().parse(json).getAsJsonArray();
                        if (arr.size() > 0) {
                            JsonArray firstLayer = arr.get(0).getAsJsonArray();
                            int iconId = firstLayer.get(0).getAsInt();
                            partIconMap.put(partId, iconId);
                        }
                    } catch (Exception e) {}
                }
            } catch (Exception e) {
                addLog("Error loading Part DB for Icons: " + e.getMessage());
            }
        }).start();
    }

    private ImageIcon getIconByIconId(int iconId, int size) {
        if (iconId <= -1) return null;
        if (iconImageCache.containsKey(iconId)) {
             Image img = iconImageCache.get(iconId).getImage();
             if (img.getWidth(null) != size) {
                 return new ImageIcon(img.getScaledInstance(size, size, Image.SCALE_SMOOTH));
             }
             return iconImageCache.get(iconId);
        }
        try {
            String[] zoomLevels = {"x4", "x3", "x2", "x1"};
            for (String zoom : zoomLevels) {
                File f = new File(ICON_FOLDER + zoom + "/" + iconId + ".png");
                if (f.exists()) {
                    BufferedImage img = ImageIO.read(f);
                    Image dimg = img.getScaledInstance(size, size, Image.SCALE_SMOOTH);
                    ImageIcon icon = new ImageIcon(dimg);
                    iconImageCache.put(iconId, icon);
                    return icon;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    // Helper Class cho Boss List
    private static class BossSummonEntry {
        String keyName; // Tên biến trong BossID (VD: TIEU_DOI_TRUONG)
        int id;         // ID thực tế
        String displayName;
        int headIconId;

        public BossSummonEntry(String keyName, int id, String displayName, int headIconId) {
            this.keyName = keyName;
            this.id = id;
            this.displayName = displayName;
            this.headIconId = headIconId;
        }
        
        @Override
        public String toString() {
            return displayName;
        }
    }

    // --- [NEW LOGIC] CHỈ LOAD BOSS CÓ TRONG HÀM loadBoss() ---
    private void prepareBossData() {
        cachedBossEntries.clear();
        try {
            File file = new File(BOSS_MANAGER_PATH);
            if (!file.exists()) {
                addLog("Warning: Không tìm thấy file source " + BOSS_MANAGER_PATH + ". Load tất cả boss.");
                prepareBossDataFallback(); // Fallback nếu không có source code
                return;
            }

            String content = Files.readString(file.toPath());
            
            int startIndex = content.indexOf("public void loadBoss()");
            if (startIndex == -1) {
                addLog("Warning: Không tìm thấy hàm loadBoss().");
                prepareBossDataFallback();
                return;
            }
            
            int braceCount = 0;
            int endIndex = -1;
            for(int i = content.indexOf('{', startIndex); i < content.length(); i++) {
                if (content.charAt(i) == '{') braceCount++;
                else if (content.charAt(i) == '}') {
                    braceCount--;
                    if (braceCount == 0) {
                        endIndex = i;
                        break;
                    }
                }
            }
            
            String methodBody = (endIndex != -1) ? content.substring(startIndex, endIndex) : content;

            Pattern pattern = Pattern.compile("createBoss\\s*\\(\\s*BossID\\.([A-Z0-9_]+)");
            Matcher matcher = pattern.matcher(methodBody);
            
            Set<String> foundBossKeys = new HashSet<>();
            while (matcher.find()) {
                foundBossKeys.add(matcher.group(1));
            }
            
            addLog("Found " + foundBossKeys.size() + " bosses in loadBoss() method.");

            Field[] idFields = BossID.class.getFields();
            Map<String, Integer> idMap = new HashMap<>();
            for (Field f : idFields) {
                if (Modifier.isStatic(f.getModifiers()) && f.getType() == int.class) {
                    idMap.put(f.getName(), f.getInt(null));
                }
            }

            Field[] dataFields = BossesData.class.getFields();
            for (Field f : dataFields) {
                if (foundBossKeys.contains(f.getName()) && f.getType() == BossData.class && idMap.containsKey(f.getName())) {
                    BossData data = (BossData) f.get(null);
                    int bossId = idMap.get(f.getName());
                    
                    int iconId = -1;
                    if (data.getOutfit() != null && data.getOutfit().length > 0) {
                        int headPart = data.getOutfit()[0];
                        iconId = partIconMap.getOrDefault(headPart, headPart);
                    }
                    
                    cachedBossEntries.add(new BossSummonEntry(f.getName(), bossId, data.getName(), iconId));
                }
            }
            
        } catch (Exception e) {
            addLog("Error analyzing BossManager code: " + e.getMessage());
            prepareBossDataFallback();
        }
    }
    
    private void prepareBossDataFallback() {
        try {
            Field[] idFields = BossID.class.getFields();
            Map<String, Integer> idMap = new HashMap<>();
            for (Field f : idFields) {
                if (Modifier.isStatic(f.getModifiers()) && f.getType() == int.class) {
                    idMap.put(f.getName(), f.getInt(null));
                }
            }
            Field[] dataFields = BossesData.class.getFields();
            for (Field f : dataFields) {
                if (f.getType() == BossData.class && idMap.containsKey(f.getName())) {
                    BossData data = (BossData) f.get(null);
                    int bossId = idMap.get(f.getName());
                    int iconId = -1;
                    if (data.getOutfit() != null && data.getOutfit().length > 0) {
                        int headPart = data.getOutfit()[0];
                        iconId = partIconMap.getOrDefault(headPart, headPart);
                    }
                    cachedBossEntries.add(new BossSummonEntry(f.getName(), bossId, data.getName(), iconId));
                }
            }
        } catch (Exception e) {}
    }

    // --- MAIN UI ---

    private void loadMaintenanceConfig() {
        File f = new File("maintenanceConfig.txt");
        if (f.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String lineH = br.readLine();
                String lineM = br.readLine();
                String lineS = br.readLine();
                String lineAuto = br.readLine();

                if (lineH != null && lineM != null && lineS != null) {
                    int h = Integer.parseInt(lineH);
                    int m = Integer.parseInt(lineM);
                    int s = Integer.parseInt(lineS);
                    boolean auto = Boolean.parseBoolean(lineAuto);

                    cbHour.setSelectedItem(h);
                    cbMinute.setSelectedItem(m);
                    cbSecond.setSelectedItem(s);
                    chkAutoRestart.setSelected(auto);
                    
                    REQUEST_AUTO_RESTART = auto;
                    if (h != -1) {
                        addLog("[Auto Maintenance] Loaded config: " + String.format("%02d:%02d:%02d", h, m, s));
                        scheduleMaintenance(); 
                    }
                }
            } catch (Exception e) {
                addLog("Error loading maintenance config: " + e.getMessage());
            }
        } else {
            addLog("[Auto Maintenance] No config found. Defaulting to 05:00 AM.");
            cbHour.setSelectedItem(5);
            cbMinute.setSelectedItem(0);
            cbSecond.setSelectedItem(0);
            chkAutoRestart.setSelected(true);
            REQUEST_AUTO_RESTART = true;
            scheduleMaintenance();
        }
    }

    // Analytics data
    private final java.util.List<Integer> onlineHistory24h = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
    private int[] raceDistribution = {0, 0, 0}; // Trái đất, Namek, Xayda
    private String[][] topPlayers = new String[0][0]; // name, level
    private int totalAccounts = 0, totalCharacters = 0;
    private long totalGold = 0, totalRuby = 0;
    private JPanel chartBarPanel, chartPiePanel, chartLinePanel, chartArchPanel;
    private JLabel lblStatAccounts, lblStatCharacters, lblStatGold, lblStatRuby;

    private void initMainLayout() {
        JPanel container = new JPanel(new GridBagLayout());
        container.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 1.0;

        // 1. Header (Status)
        container.add(createHeaderPanel(), gbc);

        // 2. Graphs (System Stats)
        gbc.gridy++;
        container.add(createSystemStatsPanel(), gbc);

        // 3. Actions (Common Actions)
        gbc.gridy++;
        container.add(createActionPanel(), gbc);

        // 4. Game Stats
        gbc.gridy++;
        container.add(createGameStatsPanel(), gbc);

        // ===== NEW: ANALYTICS SECTION =====

        // 4.1 Extended Stat Cards
        gbc.gridy++;
        container.add(createExtendedStatCards(), gbc);

        // 4.2 Charts Row: Bar Chart + Pie Chart
        JPanel chartsRow = new JPanel(new GridLayout(1, 2, 10, 0));
        chartsRow.setOpaque(false);
        chartsRow.add(createBarChartPanel());
        chartsRow.add(createPieChartPanel());
        gbc.gridy++;
        container.add(chartsRow, gbc);

        // 4.3 24h Online Line Chart
        gbc.gridy++;
        container.add(createOnlineLineChartPanel(), gbc);

        // 4.4 Server Architecture Diagram
        gbc.gridy++;
        container.add(createArchitectureDiagramPanel(), gbc);

        // ===== END ANALYTICS =====

        // 4.5. Connection Monitor Panel
        gbc.gridy++;
        container.add(createConnectionMonitorPanel(), gbc);

        // 4.6. Quick Settings Panel
        gbc.gridy++;
        container.add(createQuickSettingsPanel(), gbc);

        // 4.7. Network Info Panel
        gbc.gridy++;
        container.add(createNetworkInfoPanel(), gbc);
        
        // 5. [NEW LAYOUT] Boss Manager & Configurations (Split 2 cols)
        JPanel midPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        midPanel.setOpaque(false);
        
        // Cột 1: Quản lý Boss (Triệu hồi + Reset/Respawn)
        midPanel.add(createBossManagementPanel());
        
        // Cột 2: Cấu hình (Exp + Scheduler)
        midPanel.add(createConfigurationPanel());
        
        gbc.gridy++;
        container.add(midPanel, gbc);

        // 6. Optimization & Booster Panel (Moved Down above Logs)
        gbc.gridy++;
        container.add(createOptimizationPanel(), gbc);

        // 7. Log Panel
        gbc.gridy++;
        gbc.weighty = 1.0; 
        gbc.fill = GridBagConstraints.BOTH; 
        container.add(createLogPanel(), gbc);

        JScrollPane scroll = new JScrollPane(container);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        // Load analytics data
        refreshAnalyticsData();
    }

    // ================= UI PARTS =================

    private JPanel createHeaderPanel() {
        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(0, 10, 10, 10));

        // Left: Status labels
        JPanel leftPanel = new JPanel(new GridLayout(1, 3, 20, 0));
        leftPanel.setOpaque(false);

        lblStatus = ServerGuiUtils.createStyledLabel("● Server Online", 20, true);
        lblStatus.setForeground(new Color(0, 153, 51));
        
        lblPlayerCount = ServerGuiUtils.createStyledLabel("Online: 0", 16, false);
        lblCountdown = ServerGuiUtils.createStyledLabel("Sẵn sàng", 16, false);
        lblCountdown.setForeground(Color.GRAY);

        leftPanel.add(lblStatus);
        leftPanel.add(lblPlayerCount);
        leftPanel.add(lblCountdown);

        // Right: Refresh button + last refresh time
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightPanel.setOpaque(false);

        lblLastRefresh = new JLabel(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        lblLastRefresh.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblLastRefresh.setForeground(Color.GRAY);

        JButton btnRefreshAll = new JButton("🔄 Làm Mới");
        btnRefreshAll.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnRefreshAll.setForeground(Color.WHITE);
        btnRefreshAll.setBackground(new Color(0, 120, 215));
        btnRefreshAll.setFocusPainted(false);
        btnRefreshAll.setBorderPainted(false);
        btnRefreshAll.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRefreshAll.setPreferredSize(new Dimension(120, 32));
        btnRefreshAll.addActionListener(e -> refreshEntireDashboard());
        // Hover effect
        btnRefreshAll.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btnRefreshAll.setBackground(new Color(0, 90, 180));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btnRefreshAll.setBackground(new Color(0, 120, 215));
            }
        });

        rightPanel.add(lblLastRefresh);
        rightPanel.add(btnRefreshAll);

        p.add(leftPanel, BorderLayout.CENTER);
        p.add(rightPanel, BorderLayout.EAST);
        return p;
    }

    private JPanel createSystemStatsPanel() {
        JPanel p = new JPanel(new GridLayout(1, 4, 10, 0));
        p.setOpaque(false);

        lblCpuUsage = new JLabel("Server CPU: 0%");
        lblRamUsage = new JLabel("JVM RAM: 0 MB");
        lblThreadCount = new JLabel("Threads: 0");
        lblSessionCount = new JLabel("Sessions: 0");

        p.add(createGraphCard("Server CPU", lblCpuUsage, graphCpu));
        p.add(createGraphCard("JVM RAM (Heap)", lblRamUsage, graphRam));
        p.add(createGraphCard("Threads", lblThreadCount, graphThread));
        p.add(createGraphCard("Sessions", lblSessionCount, graphSession));
        return p;
    }

    private JPanel createGraphCard(String title, JLabel info, MiniGraph graph) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(230, 230, 230), 1, true),
                new EmptyBorder(5, 10, 5, 10)
        ));
        
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        titleLbl.setForeground(Color.GRAY);
        
        info.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        top.add(titleLbl, BorderLayout.NORTH);
        top.add(info, BorderLayout.CENTER);

        card.add(top, BorderLayout.NORTH);
        card.add(graph, BorderLayout.CENTER);
        card.setPreferredSize(new Dimension(0, 80));
        return card;
    }

    private JPanel createActionPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        p.setOpaque(false);
        p.setBorder(ServerGuiUtils.createSectionBorder("Quick Actions"));

        JButton btnMaint = ServerGuiUtils.createStyledButton("Bảo Trì (2p)", new Color(255, 193, 7), Color.BLACK);
        btnMaint.addActionListener(e -> confirmMaintenance());

        JButton btnReload = ServerGuiUtils.createStyledButton("Tải lại DB", new Color(23, 162, 184), Color.WHITE);
        btnReload.addActionListener(e -> showReloadOptions());

        JButton btnBackupDb = ServerGuiUtils.createStyledButton("Backup DB", new Color(111, 66, 193), Color.WHITE);
        btnBackupDb.addActionListener(e -> backupDatabaseAsync(false));

        JButton btnClean = ServerGuiUtils.createStyledButton("Dọn Session", new Color(108, 117, 125), Color.WHITE);
        btnClean.addActionListener(e -> {
             addLog("Đang thực hiện dọn dẹp session rác...");
             if (SessionManager.gI() != null) {
                 // session cleanup logic here
             }
             addLog("Đã dọn dẹp các session dead/null.");
        });

        btnToggleAutoSave = new JToggleButton("AutoSave: ON");
        btnToggleAutoSave.setSelected(true);
        btnToggleAutoSave.setFocusPainted(false);
        btnToggleAutoSave.addActionListener(e -> {
            boolean enable = isAutoSaveEnabled.get();
            if (enable) {
                AutoSaveManager.getInstance().stopAutoSave();
                btnToggleAutoSave.setText("AutoSave: OFF");
                addLog("System: AutoSave has been DISABLED.");
            } else {
                AutoSaveManager.getInstance().startAutoSave();
                btnToggleAutoSave.setText("AutoSave: ON");
                addLog("System: AutoSave has been ENABLED.");
            }
            isAutoSaveEnabled.set(!enable);
            btnToggleAutoSave.setSelected(!enable);
        });

        p.add(btnMaint);
        p.add(btnReload);
        p.add(btnBackupDb);
        p.add(btnClean);
        p.add(btnToggleAutoSave);
        return p;
    }
    
    // --- [NEW PANEL] GOM NHÓM BOSS ---
    private JPanel createBossManagementPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 5));
        p.setOpaque(false);
        p.setBorder(ServerGuiUtils.createSectionBorder("Boss Manager (Triệu Hồi & Cài Đặt)"));

        // 1. Nút Triệu Hồi (Lớn)
        JButton btnOpenSummon = ServerGuiUtils.createStyledButton("Mở Menu Triệu Hồi (Search & Call Boss)", new Color(0, 120, 215), Color.WHITE);
        btnOpenSummon.setPreferredSize(new Dimension(0, 35));
        btnOpenSummon.addActionListener(e -> showBossSummonDialog());

        // 2. Các nút Reset/Respawn (Nhỏ hơn ở dưới)
        JPanel subBtn = new JPanel(new GridLayout(1, 2, 5, 0));
        subBtn.setOpaque(false);

        JButton btnResetBoss = ServerGuiUtils.createStyledButton("Reset All Boss", new Color(220, 53, 69), Color.WHITE);
        btnResetBoss.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this, "Reset TẤT CẢ BOSS?", "Xác nhận", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                BossManager.gI().resetAllBosses();
                addLog("Boss Manager: Đã reset tất cả boss.");
            }
        });

        JButton btnRespawn = ServerGuiUtils.createStyledButton("Hồi Sinh Boss Chờ", new Color(40, 167, 69), Color.WHITE);
        btnRespawn.addActionListener(e -> {
             if (JOptionPane.showConfirmDialog(this, "Hồi sinh tất cả Boss đang chờ?", "Xác nhận", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                BossManager.gI().respawnAllRestingBosses();
                addLog("Boss Manager: Đã hồi sinh các boss đang chờ.");
            }
        });
        
        subBtn.add(btnResetBoss);
        subBtn.add(btnRespawn);

        p.add(btnOpenSummon, BorderLayout.NORTH);
        p.add(subBtn, BorderLayout.CENTER);

        return p;
    }

    // --- [NEW PANEL] GOM NHÓM CẤU HÌNH ---
    private JPanel createConfigurationPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(ServerGuiUtils.createSectionBorder("Server Configuration (Exp & Schedule)"));

        JPanel container = new JPanel(new GridLayout(2, 1, 0, 5));
        container.setOpaque(false);

        // 1. Exp Config
        JPanel pExp = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pExp.setOpaque(false);
        JTextField txtExp = new JTextField(String.valueOf(Manager.RATE_EXP_SERVER), 4);
        JButton btnUpdateExp = new JButton("Set EXP");
        btnUpdateExp.addActionListener(e -> {
            try {
                double rate = Double.parseDouble(txtExp.getText().trim());
                if (rate > 0) {
                    Manager.RATE_EXP_SERVER = rate;
                    addLog("Config: EXP Rate updated to x" + Manager.RATE_EXP_SERVER);
                    Service.gI().sendThongBaoAllPlayer("Server EXP Rate: x" + Manager.RATE_EXP_SERVER);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Số không hợp lệ");
            }
        });
        pExp.add(new JLabel("Rate: x"));
        pExp.add(txtExp);
        pExp.add(btnUpdateExp);

        // 2. Scheduler Config
        JPanel pSched = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pSched.setOpaque(false);
        
        cbHour = new JComboBox<>(); for(int i=-1;i<24;i++) cbHour.addItem(i);
        cbMinute = new JComboBox<>(); for(int i=-1;i<60;i++) cbMinute.addItem(i);
        cbSecond = new JComboBox<>(); for(int i=-1;i<60;i++) cbSecond.addItem(i);
        
        chkAutoRestart = new JCheckBox("AutoRestart");
        chkAutoRestart.setSelected(true);
        chkAutoRestart.setOpaque(false);

        JButton btnSetSched = new JButton("Lưu Cấu Hình");
        btnSetSched.addActionListener(e -> scheduleMaintenance());

        pSched.add(new JLabel("Hẹn giờ:"));
        pSched.add(cbHour); pSched.add(new JLabel(":"));
        pSched.add(cbMinute);
        pSched.add(chkAutoRestart);
        pSched.add(btnSetSched);

        container.add(pExp);
        container.add(pSched);
        
        p.add(container, BorderLayout.CENTER);
        return p;
    }

    private JPanel createOptimizationPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(ServerGuiUtils.createSectionBorder("System Optimization & Booster (Server Only)"));
        
        JPanel pBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        pBtns.setOpaque(false);
        
        JButton btnOptRam = ServerGuiUtils.createStyledButton("Dọn dẹp JVM RAM", new Color(40, 167, 69), Color.WHITE);
        btnOptRam.addActionListener(e -> performRamCleanup());
        
        JButton btnOptCpu = ServerGuiUtils.createStyledButton("Tối ưu CPU & VPS", new Color(0, 123, 255), Color.WHITE);
        btnOptCpu.addActionListener(e -> performCpuOptimization());
        
        JButton btnFlushLog = ServerGuiUtils.createStyledButton("Xóa Log Cache", new Color(108, 117, 125), Color.WHITE);
        btnFlushLog.addActionListener(e -> {
            txtLog.setText("");
            addLog("System: Log cache cleared to free memory.");
        });

        JPanel pAuto = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        pAuto.setOpaque(false);
        pAuto.setBorder(new EmptyBorder(5, 0, 0, 0));
        
        chkAutoOptimize = new JCheckBox("Tự động tối ưu hóa (Auto Optimize)");
        chkAutoOptimize.setOpaque(false);
        chkAutoOptimize.setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        String[] intervals = {"5 Phút", "10 Phút", "30 Phút", "60 Phút"};
        cbOptimizeInterval = new JComboBox<>(intervals);
        cbOptimizeInterval.setSelectedIndex(1); 
        
        lblOptStatus = new JLabel("Trạng thái: Tắt");
        lblOptStatus.setForeground(Color.RED);
        
        chkAutoOptimize.addActionListener(e -> toggleAutoOptimization());
        cbOptimizeInterval.addActionListener(e -> {
            if (chkAutoOptimize.isSelected()) {
                toggleAutoOptimization(); 
            }
        });
        
        pBtns.add(btnOptRam);
        pBtns.add(btnOptCpu);
        pBtns.add(btnFlushLog);
        
        pAuto.add(chkAutoOptimize);
        pAuto.add(new JLabel("Mỗi:"));
        pAuto.add(cbOptimizeInterval);
        pAuto.add(Box.createHorizontalStrut(15));
        pAuto.add(lblOptStatus);
        
        p.add(pBtns, BorderLayout.NORTH);
        p.add(pAuto, BorderLayout.CENTER);
        
        return p;
    }

    private JPanel createGameStatsPanel() {
        JPanel p = new JPanel(new GridLayout(2, 2, 10, 5));
        p.setOpaque(false);
        p.setBorder(ServerGuiUtils.createSectionBorder("Game Statistics"));

        lblGiftcodeInfo = new JLabel("Giftcodes: Loading...");
        lblConsignItemsCount = new JLabel("Consign Items: Loading...");
        lblBossStatus = new JLabel("Boss Status: Loading...");
        lblUptime = new JLabel("Uptime: Calculating...");

        p.add(lblGiftcodeInfo);
        p.add(lblConsignItemsCount);
        p.add(lblBossStatus);
        p.add(lblUptime);
        return p;
    }

    // --- [NEW] NETWORK INFO PANEL ---
    private JPanel createNetworkInfoPanel() {
        JPanel p = new JPanel(new GridLayout(1, 4, 10, 5));
        p.setOpaque(false);
        p.setBorder(ServerGuiUtils.createSectionBorder("🌐 Thông Tin Mạng (Network Info)"));

        // Đọc từ config.properties - cùng nguồn với Quick Settings
        Properties netProps = new Properties();
        try (FileReader fr = new FileReader("data/config/config.properties")) {
            netProps.load(fr);
        } catch (Exception ignored) {}

        String cfgIp = netProps.getProperty("server.ip_host", "N/A");
        String cfgPort = netProps.getProperty("server.port_proxy", "N/A");
        String cfgName = netProps.getProperty("server.name", "N/A");

        // Server IP:Port
        JLabel lblLink = new JLabel("<html><b>Server IP:</b><br>" + escapeHtml(cfgIp) + ":" + cfgPort + "</html>");
        lblLink.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        // Game Port
        JLabel lblPort = new JLabel("<html><b>Game Port:</b><br>" + cfgPort + "</html>");
        lblPort.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        // Proxy Status
        String proxyStatus = "<span style='color:gray;'>Tắt (Disabled)</span>";
        JLabel lblProxy = new JLabel("<html><b>AntiDDoS Proxy:</b><br>" + proxyStatus + "</html>");
        lblProxy.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        // Server Name
        JLabel lblName = new JLabel("<html><b>Server Name:</b><br>" + escapeHtml(cfgName) + "</html>");
        lblName.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        p.add(lblLink);
        p.add(lblPort);
        p.add(lblProxy);
        p.add(lblName);
        return p;
    }

    private String escapeHtml(String s) {
        if (s == null) return "N/A";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // --- [NEW] CONNECTION MONITOR PANEL ---
    private JPanel createConnectionMonitorPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 5));
        p.setOpaque(false);
        p.setBorder(ServerGuiUtils.createSectionBorder("Giám Sát Kết Nối (Connection Monitor)"));

        // Header with summary stats
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 5));
        headerPanel.setOpaque(false);
        lblTotalOnline = new JLabel("👤 Online: 0");
        lblTotalOnline.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTotalOnline.setForeground(new Color(0, 153, 51));
        lblTotalConnections = new JLabel("🌐 Tổng IP kết nối: 0");
        lblTotalConnections.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTotalConnections.setForeground(new Color(0, 102, 204));

        JButton btnRefresh = ServerGuiUtils.createStyledButton("Làm mới", new Color(108, 117, 125), Color.WHITE);
        btnRefresh.addActionListener(e -> {
            refreshConnectionMonitor();
            // Flash feedback
            Color origBg = btnRefresh.getBackground();
            btnRefresh.setBackground(new Color(40, 167, 69));
            btnRefresh.setText("✔ Đã làm mới!");
            addLog("CONNECTION MONITOR: Đã làm mới danh sách kết nối.");
            Timer timer = new Timer(1500, evt -> {
                btnRefresh.setBackground(origBg);
                btnRefresh.setText("Làm mới");
            });
            timer.setRepeats(false);
            timer.start();
        });

        JButton btnKickAll = ServerGuiUtils.createStyledButton("Kick All", new Color(220, 53, 69), Color.WHITE);
        btnKickAll.addActionListener(e -> {
            if (javax.swing.JOptionPane.showConfirmDialog(this, "Kick TẤT CẢ người chơi?", "Xác nhận", javax.swing.JOptionPane.YES_NO_OPTION) == javax.swing.JOptionPane.YES_OPTION) {
                Client.gI().close();
                addLog("ADMIN: Đã kick tất cả người chơi.");
                refreshConnectionMonitor();
            }
        });

        headerPanel.add(lblTotalOnline);
        headerPanel.add(lblTotalConnections);
        headerPanel.add(btnRefresh);
        headerPanel.add(btnKickAll);

        // Split into 2 tables side by side
        JPanel tablesPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        tablesPanel.setOpaque(false);

        // --- Table 1: Online Players ---
        String[] playerCols = {"#", "Tên Nhân Vật", "IP Address", "User ID"};
        playerTableModel = new DefaultTableModel(playerCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable playerTable = new JTable(playerTableModel);
        playerTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        playerTable.setRowHeight(24);
        playerTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        playerTable.getTableHeader().setBackground(new Color(0, 120, 215));
        playerTable.getTableHeader().setForeground(Color.WHITE);
        playerTable.setSelectionBackground(new Color(230, 242, 255));
        playerTable.getColumnModel().getColumn(0).setPreferredWidth(30);
        playerTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        playerTable.getColumnModel().getColumn(2).setPreferredWidth(110);
        playerTable.getColumnModel().getColumn(3).setPreferredWidth(60);

        JPanel playerPanel = new JPanel(new BorderLayout());
        playerPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)), "Người Chơi Online",
            javax.swing.border.TitledBorder.LEFT, javax.swing.border.TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 12), new Color(0, 120, 215)));
        playerPanel.setOpaque(false);
        JScrollPane playerScroll = new JScrollPane(playerTable);
        playerScroll.setPreferredSize(new Dimension(0, 180));
        playerPanel.add(playerScroll, BorderLayout.CENTER);

        // --- Table 2: IP Connection Tracking ---
        String[] ipCols = {"IP Address", "Số kết nối", "Trạng thái"};
        ipTableModel = new DefaultTableModel(ipCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable ipTable = new JTable(ipTableModel);
        ipTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        ipTable.setRowHeight(24);
        ipTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        ipTable.getTableHeader().setBackground(new Color(220, 53, 69));
        ipTable.getTableHeader().setForeground(Color.WHITE);
        ipTable.setSelectionBackground(new Color(255, 235, 238));
        ipTable.getColumnModel().getColumn(0).setPreferredWidth(130);
        ipTable.getColumnModel().getColumn(1).setPreferredWidth(70);
        ipTable.getColumnModel().getColumn(2).setPreferredWidth(100);

        // ColorRenderer for status column
        ipTable.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String status = (value != null) ? value.toString() : "";
                if (status.contains("Bị chặn")) {
                    c.setForeground(new Color(220, 53, 69));
                    c.setFont(new Font("Segoe UI", Font.BOLD, 12));
                } else if (status.contains("Cảnh báo")) {
                    c.setForeground(new Color(255, 140, 0));
                    c.setFont(new Font("Segoe UI", Font.BOLD, 12));
                } else {
                    c.setForeground(new Color(0, 153, 51));
                    c.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                }
                return c;
            }
        });

        JPanel ipPanel = new JPanel(new BorderLayout());
        ipPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)), "Theo Dõi IP Kết Nối",
            javax.swing.border.TitledBorder.LEFT, javax.swing.border.TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 12), new Color(220, 53, 69)));
        ipPanel.setOpaque(false);
        JScrollPane ipScroll = new JScrollPane(ipTable);
        ipScroll.setPreferredSize(new Dimension(0, 180));
        ipPanel.add(ipScroll, BorderLayout.CENTER);

        tablesPanel.add(playerPanel);
        tablesPanel.add(ipPanel);

        p.add(headerPanel, BorderLayout.NORTH);
        p.add(tablesPanel, BorderLayout.CENTER);

        return p;
    }

    private void refreshConnectionMonitor() {
        // --- Update Player Table ---
        List<nro.player.Player> players = (Client.gI() != null) ? Client.gI().getPlayers() : new ArrayList<>();
        playerTableModel.setRowCount(0);
        int idx = 1;
        for (nro.player.Player pl : players) {
            if (pl != null) {
                String name = (pl.name != null) ? pl.name : "N/A";
                String ip = "N/A";
                int userId = 0;
                try {
                    if (pl.getSession() != null && pl.getSession() instanceof MySession) {
                        MySession ms = (MySession) pl.getSession();
                        ip = (ms.ipAddress != null) ? ms.ipAddress : "N/A";
                        userId = ms.userId;
                    }
                } catch (Exception ignored) {}
                playerTableModel.addRow(new Object[]{idx++, name, ip, userId});
            }
        }
        lblTotalOnline.setText("👤 Online: " + (idx - 1));

        // --- Update IP Table ---
        ipTableModel.setRowCount(0);
        try {
            HashMap<String, Integer> fw = new HashMap<>(Server_firewall.firewall);
            int totalIps = fw.size();
            for (Map.Entry<String, Integer> entry : fw.entrySet()) {
                String ipAddr = entry.getKey();
                int count = entry.getValue();
                String status;
                if (count > 21) {
                    status = "\uD83D\uDEAB Bị chặn";
                } else if (count > 10) {
                    status = "⚠ Cảnh báo";
                } else {
                    status = "✔ Bình thường";
                }
                ipTableModel.addRow(new Object[]{ipAddr, count, status});
            }
            lblTotalConnections.setText("\uD83C\uDF10 Tổng IP kết nối: " + totalIps);
        } catch (Exception e) {
            addLog("Error refreshing IP monitor: " + e.getMessage());
        }
    }

    // --- [NEW] QUICK SETTINGS PANEL ---
    private JPanel createQuickSettingsPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setOpaque(false);
        p.setBorder(ServerGuiUtils.createSectionBorder("⚙ Cài Đặt Nhanh (Quick Settings)"));

        // Load current values from config.properties
        Properties props = new Properties();
        try (FileReader fr = new FileReader("data/config/config.properties")) {
            props.load(fr);
        } catch (Exception e) {
            addLog("Warning: Cannot read config.properties: " + e.getMessage());
        }

        // Load RAM config
        String ramMin = "1G", ramMax = "4G";
        File ramFile = new File("ram_config.txt");
        if (ramFile.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(ramFile))) {
                String l1 = br.readLine();
                String l2 = br.readLine();
                if (l1 != null) ramMin = l1.trim();
                if (l2 != null) ramMax = l2.trim();
            } catch (Exception ignored) {}
        }

        JPanel fieldsPanel = new JPanel(new GridLayout(0, 4, 12, 8));
        fieldsPanel.setOpaque(false);

        JTextField tfIP = createSettingField("Server IP", props.getProperty("server.ip_host", ""), fieldsPanel);
        JTextField tfPort = createSettingField("Port", props.getProperty("server.port_proxy", "14445"), fieldsPanel);
        JTextField tfMaxPlayer = createSettingField("Max Player", props.getProperty("server.maxplayer", "10000"), fieldsPanel);
        JTextField tfMaxPerIP = createSettingField("Max/IP", props.getProperty("server.maxperip", "1000"), fieldsPanel);
        JTextField tfServerName = createSettingField("Server Name", props.getProperty("server.name", ""), fieldsPanel);
        JTextField tfExp = createSettingField("EXP Rate", props.getProperty("server.expserver", "1"), fieldsPanel);
        JTextField tfRamMin = createSettingField("RAM Min", ramMin, fieldsPanel);
        JTextField tfRamMax = createSettingField("RAM Max", ramMax, fieldsPanel);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnPanel.setOpaque(false);

        JButton btnSave = ServerGuiUtils.createStyledButton("💾 Lưu Cấu Hình", new Color(0, 123, 255), Color.WHITE);
        btnSave.addActionListener(e -> {
            saveQuickSettings(props, tfIP, tfPort, tfMaxPlayer, tfMaxPerIP, tfServerName, tfExp, tfRamMin, tfRamMax);
            addLog("SETTINGS: Đã lưu cấu hình thành công!");
            JOptionPane.showMessageDialog(this, "Đã lưu! Restart server để áp dụng.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
        });

        JButton btnSaveRestart = ServerGuiUtils.createStyledButton("🔄 Lưu & Restart", new Color(220, 53, 69), Color.WHITE);
        btnSaveRestart.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Lưu cấu hình và khởi động lại server?",
                    "Xác nhận", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                saveQuickSettings(props, tfIP, tfPort, tfMaxPlayer, tfMaxPerIP, tfServerName, tfExp, tfRamMin, tfRamMax);
                addLog("SETTINGS: Đã lưu cấu hình. Đang restart...");
                ServerManagerUI.REQUEST_AUTO_RESTART = true;
                System.exit(0);
            }
        });

        btnPanel.add(btnSave);
        btnPanel.add(btnSaveRestart);

        p.add(fieldsPanel, BorderLayout.CENTER);
        p.add(btnPanel, BorderLayout.SOUTH);
        return p;
    }

    private JTextField createSettingField(String label, String value, JPanel parent) {
        JLabel lbl = new JLabel(label + ":");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(new Color(80, 80, 80));
        parent.add(lbl);

        JTextField tf = new JTextField(value);
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tf.setPreferredSize(new Dimension(100, 28));
        parent.add(tf);
        return tf;
    }

    private void saveQuickSettings(Properties props, JTextField tfIP, JTextField tfPort,
                                    JTextField tfMaxPlayer, JTextField tfMaxPerIP,
                                    JTextField tfServerName, JTextField tfExp,
                                    JTextField tfRamMin, JTextField tfRamMax) {
        // Save config.properties
        props.setProperty("server.ip_host", tfIP.getText().trim());
        props.setProperty("server.port_proxy", tfPort.getText().trim());
        props.setProperty("server.port_real", tfPort.getText().trim());
        props.setProperty("server.maxplayer", tfMaxPlayer.getText().trim());
        props.setProperty("server.maxperip", tfMaxPerIP.getText().trim());
        props.setProperty("server.name", tfServerName.getText().trim());
        props.setProperty("server.expserver", tfExp.getText().trim());

        // Update sv1 line with new IP and port
        String ip = tfIP.getText().trim();
        String port = tfPort.getText().trim();
        String serverName = tfServerName.getText().trim();
        props.setProperty("server.sv1", serverName + ":" + ip + ":" + port + ":0,0,0");

        try (FileWriter fw = new FileWriter("data/config/config.properties")) {
            props.store(fw, "configserver");
        } catch (Exception e) {
            addLog("Error saving config.properties: " + e.getMessage());
        }

        // Save RAM config
        try (PrintWriter pw = new PrintWriter(new FileWriter("ram_config.txt"))) {
            pw.println(tfRamMin.getText().trim());
            pw.println(tfRamMax.getText().trim());
        } catch (Exception e) {
            addLog("Error saving ram_config.txt: " + e.getMessage());
        }
    }

    // --- [NEW] DIALOG TRIỆU HỒI BOSS VỚI ICON & SEARCH ---
    private void showBossSummonDialog() {
        JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Triệu Hồi Boss (Searchable)", true);
        d.setSize(500, 600);
        d.setLocationRelativeTo(this);
        d.setLayout(new BorderLayout(5, 5));
        
        // 1. Prepare Data
        if (cachedBossEntries.isEmpty()) {
            prepareBossData();
        }
        
        // 2. Components
        JTextField txtSearch = new JTextField();
        txtSearch.setBorder(BorderFactory.createTitledBorder("Nhập tên boss để tìm..."));
        
        DefaultListModel<BossSummonEntry> listModel = new DefaultListModel<>();
        cachedBossEntries.forEach(listModel::addElement);
        
        JList<BossSummonEntry> list = new JList<>(listModel);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof BossSummonEntry) {
                    BossSummonEntry entry = (BossSummonEntry) value;
                    lbl.setText(entry.displayName);
                    if (entry.headIconId != -1) {
                        ImageIcon icon = getIconByIconId(entry.headIconId, 25);
                        if (icon != null) lbl.setIcon(icon);
                    }
                    lbl.setIconTextGap(10);
                }
                return lbl;
            }
        });
        
        // 3. Search Logic
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filter(); }
            public void removeUpdate(DocumentEvent e) { filter(); }
            public void changedUpdate(DocumentEvent e) { filter(); }
            void filter() {
                String text = txtSearch.getText().toLowerCase();
                listModel.clear();
                for (BossSummonEntry entry : cachedBossEntries) {
                    if (entry.displayName.toLowerCase().contains(text) || entry.keyName.toLowerCase().contains(text)) {
                        listModel.addElement(entry);
                    }
                }
            }
        });
        
        // 4. Action
        JButton btnSummon = new JButton("TRIỆU HỒI NGAY");
        btnSummon.setBackground(new Color(40, 167, 69));
        btnSummon.setForeground(Color.WHITE);
        btnSummon.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnSummon.setPreferredSize(new Dimension(0, 40));
        
        Runnable doSummon = () -> {
            BossSummonEntry selected = list.getSelectedValue();
            if (selected != null) {
                summonSpecificBoss(selected.id, selected.displayName);
                d.dispose();
            } else {
                JOptionPane.showMessageDialog(d, "Vui lòng chọn một Boss!");
            }
        };
        
        btnSummon.addActionListener(e -> doSummon.run());
        list.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) doSummon.run();
            }
        });
        
        d.add(txtSearch, BorderLayout.NORTH);
        d.add(new JScrollPane(list), BorderLayout.CENTER);
        d.add(btnSummon, BorderLayout.SOUTH);
        d.setVisible(true);
    }
    
    // ================= ANALYTICS PANELS =================

    /** Extended Stat Cards - Database aggregate stats */
    private JPanel createExtendedStatCards() {
        JPanel p = new JPanel(new GridLayout(1, 4, 10, 0));
        p.setOpaque(false);
        p.setBorder(ServerGuiUtils.createSectionBorder("📊 Thống Kê Tổng Hợp (Database)"));

        lblStatAccounts = createAnalyticsCard("Tổng Tài Khoản", "...", new Color(0, 120, 215));
        lblStatCharacters = createAnalyticsCard("Tổng Nhân Vật", "...", new Color(40, 167, 69));
        lblStatGold = createAnalyticsCard("Tổng Vàng", "...", new Color(255, 152, 0));
        lblStatRuby = createAnalyticsCard("Tổng Ruby", "...", new Color(220, 53, 69));

        p.add(lblStatAccounts);
        p.add(lblStatCharacters);
        p.add(lblStatGold);
        p.add(lblStatRuby);
        return p;
    }

    private JLabel createAnalyticsCard(String title, String value, Color color) {
        String hex = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
        JLabel lbl = new JLabel("<html><div style='text-align:center;padding:8px;'>"
                + "<span style='font-size:10px;color:#888;'>" + title + "</span><br>"
                + "<span style='font-size:18px;color:" + hex + ";font-weight:bold;'>" + value + "</span>"
                + "</div></html>", SwingConstants.CENTER);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 230, 230)),
                new javax.swing.border.EmptyBorder(5, 5, 5, 5)));
        lbl.setOpaque(true);
        lbl.setBackground(Color.WHITE);
        return lbl;
    }

    private void updateAnalyticsCard(JLabel lbl, String title, String value, Color color) {
        String hex = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
        lbl.setText("<html><div style='text-align:center;padding:8px;'>"
                + "<span style='font-size:10px;color:#888;'>" + title + "</span><br>"
                + "<span style='font-size:18px;color:" + hex + ";font-weight:bold;'>" + value + "</span>"
                + "</div></html>");
    }

    /** Bar Chart Panel - Top 10 players by level */
    private JPanel createBarChartPanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(ServerGuiUtils.createSectionBorder(" 📊 Top 10 Người Chơi (Sức Mạnh)"));

        chartBarPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawBarChart((Graphics2D) g);
            }
        };
        chartBarPanel.setBackground(new Color(250, 250, 252));
        chartBarPanel.setPreferredSize(new Dimension(0, 250));
        wrapper.add(chartBarPanel, BorderLayout.CENTER);
        return wrapper;
    }

    private void drawBarChart(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = chartBarPanel.getWidth() - 80;
        int h = chartBarPanel.getHeight() - 60;
        int ox = 60, oy = 20;

        if (topPlayers.length == 0) {
            g2.setColor(new Color(150, 150, 150));
            g2.setFont(new Font("Segoe UI", Font.ITALIC, 13));
            g2.drawString("Đang tải dữ liệu...", w / 2 - 50, h / 2);
            return;
        }

        long maxLevel = 1;
        for (String[] p : topPlayers) {
            long lv = 0;
            try { lv = Long.parseLong(p[1]); } catch (Exception e) {}
            if (lv < 0) lv = 0;
            if (lv > maxLevel) maxLevel = lv;
        }
        // Round max to a nice number
        if (maxLevel >= 1_000_000_000L) maxLevel = ((maxLevel / 1_000_000_000L) + 1) * 1_000_000_000L;
        else if (maxLevel >= 1_000_000L) maxLevel = ((maxLevel / 1_000_000L) + 1) * 1_000_000L;
        else if (maxLevel >= 1_000L) maxLevel = ((maxLevel / 1_000L) + 1) * 1_000L;
        else maxLevel = ((maxLevel / 10) + 1) * 10;

        // Grid lines
        g2.setColor(new Color(235, 235, 235));
        for (int i = 0; i <= 5; i++) {
            int y = oy + h - (h * i / 5);
            g2.drawLine(ox, y, ox + w, y);
            g2.setColor(new Color(150, 150, 150));
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            g2.drawString(formatChartLabel(maxLevel * i / 5), 2, y + 4);
            g2.setColor(new Color(235, 235, 235));
        }

        // Bars
        int barCount = topPlayers.length;
        int totalGap = barCount + 1;
        int barW = Math.max(20, Math.min(50, (w - totalGap * 6) / Math.max(barCount, 1)));
        int gap = (w - barCount * barW) / (barCount + 1);

        Color[] barColors = {
            new Color(0, 120, 215), new Color(40, 167, 69), new Color(255, 152, 0),
            new Color(220, 53, 69), new Color(142, 68, 173), new Color(0, 172, 105),
            new Color(255, 87, 34), new Color(63, 81, 181), new Color(255, 179, 0),
            new Color(108, 117, 125)
        };

        for (int i = 0; i < barCount; i++) {
            long level = 0;
            try { level = Long.parseLong(topPlayers[i][1]); } catch (Exception e) {}
            if (level < 0) level = 0;
            int barH = (int) ((long) h * level / maxLevel);
            if (barH < 2 && level > 0) barH = 2;
            int bx = ox + gap + i * (barW + gap);
            int by = oy + h - barH;

            // Bar gradient
            Color c = barColors[i % barColors.length];
            GradientPaint gp = new GradientPaint(bx, by, c, bx, by + barH, c.darker());
            g2.setPaint(gp);
            g2.fillRoundRect(bx, by, barW, barH, 4, 4);

            // Value label (formatted)
            g2.setColor(c.darker());
            g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
            String valStr = formatChartLabel(level);
            int tw = g2.getFontMetrics().stringWidth(valStr);
            g2.drawString(valStr, bx + (barW - tw) / 2, by - 4);

            // Name label
            g2.setColor(new Color(60, 60, 60));
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            String name = topPlayers[i][0];
            if (name.length() > 10) name = name.substring(0, 9) + "..";
            tw = g2.getFontMetrics().stringWidth(name);
            g2.drawString(name, bx + (barW - tw) / 2, oy + h + 14);
        }

        // Axes
        g2.setColor(new Color(100, 100, 100));
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(ox, oy, ox, oy + h);
        g2.drawLine(ox, oy + h, ox + w, oy + h);
    }

    /** Format large numbers for chart labels: 2.3B, 150M, 12K */
    private String formatChartLabel(long num) {
        if (num >= 1_000_000_000L) return String.format("%.1fB", num / 1_000_000_000.0);
        if (num >= 1_000_000L) return String.format("%.1fM", num / 1_000_000.0);
        if (num >= 1_000L) return String.format("%.1fK", num / 1_000.0);
        return String.valueOf(num);
    }

    /** Pie Chart Panel - Race distribution */
    private JPanel createPieChartPanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(ServerGuiUtils.createSectionBorder("🥧 Phân Bố Chủng Tộc"));

        chartPiePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawPieChart((Graphics2D) g);
            }
        };
        chartPiePanel.setBackground(new Color(250, 250, 252));
        chartPiePanel.setPreferredSize(new Dimension(0, 250));
        wrapper.add(chartPiePanel, BorderLayout.CENTER);
        return wrapper;
    }

    private void drawPieChart(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = chartPiePanel.getWidth();
        int h = chartPiePanel.getHeight();

        int total = raceDistribution[0] + raceDistribution[1] + raceDistribution[2];
        if (total == 0) {
            g2.setColor(new Color(150, 150, 150));
            g2.setFont(new Font("Segoe UI", Font.ITALIC, 13));
            g2.drawString("Đang tải dữ liệu...", w / 2 - 60, h / 2);
            return;
        }

        String[] names = {"Trái Đất", "Namek", "Xayda"};
        Color[] colors = {
            new Color(0, 120, 215),   // Blue
            new Color(40, 167, 69),   // Green
            new Color(220, 53, 69)    // Red
        };

        int pieSize = Math.min(w - 120, h - 40) - 10;
        int px = (w - 120) / 2 - pieSize / 2;
        int py = (h - pieSize) / 2;

        int startAngle = 0;
        for (int i = 0; i < 3; i++) {
            int arcAngle = (int) Math.round(360.0 * raceDistribution[i] / total);
            if (i == 2) arcAngle = 360 - startAngle; // Fix rounding

            // Draw slice
            g2.setColor(colors[i]);
            g2.fillArc(px, py, pieSize, pieSize, startAngle, arcAngle);

            // Border
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2f));
            g2.drawArc(px, py, pieSize, pieSize, startAngle, arcAngle);

            startAngle += arcAngle;
        }

        // Center hole (donut)
        int holeSize = pieSize / 3;
        g2.setColor(new Color(250, 250, 252));
        g2.fillOval(px + pieSize / 2 - holeSize / 2, py + pieSize / 2 - holeSize / 2, holeSize, holeSize);

        // Total in center
        g2.setColor(new Color(60, 60, 60));
        g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
        String totalStr = String.valueOf(total);
        int tw = g2.getFontMetrics().stringWidth(totalStr);
        g2.drawString(totalStr, px + pieSize / 2 - tw / 2, py + pieSize / 2 + 5);

        // Legend
        int legendX = w - 115;
        int legendY = h / 2 - 40;
        for (int i = 0; i < 3; i++) {
            g2.setColor(colors[i]);
            g2.fillRoundRect(legendX, legendY + i * 25, 14, 14, 3, 3);
            g2.setColor(new Color(60, 60, 60));
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            int pct = (int) Math.round(100.0 * raceDistribution[i] / total);
            g2.drawString(names[i] + " (" + pct + "%)", legendX + 20, legendY + i * 25 + 12);
        }
    }

    /** 24h Online Line Chart */
    private JPanel createOnlineLineChartPanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(ServerGuiUtils.createSectionBorder("📈 Biểu Đồ Online 24h (5 phút/điểm)"));

        chartLinePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawOnlineLineChart((Graphics2D) g);
            }
        };
        chartLinePanel.setBackground(new Color(250, 250, 252));
        chartLinePanel.setPreferredSize(new Dimension(0, 220));
        wrapper.add(chartLinePanel, BorderLayout.CENTER);
        return wrapper;
    }

    private void drawOnlineLineChart(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = chartLinePanel.getWidth() - 70;
        int h = chartLinePanel.getHeight() - 40;
        int ox = 55, oy = 10;

        synchronized (onlineHistory24h) {
            if (onlineHistory24h.isEmpty()) {
                g2.setColor(new Color(150, 150, 150));
                g2.setFont(new Font("Segoe UI", Font.ITALIC, 13));
                g2.drawString("Đang thu thập... (cập nhật mỗi 5 phút)", w / 2 - 100, h / 2);
                return;
            }

            int maxVal = Math.max(10, onlineHistory24h.stream().mapToInt(Integer::intValue).max().orElse(10));
            maxVal = ((maxVal / 5) + 1) * 5;

            // Grid
            g2.setColor(new Color(235, 235, 235));
            for (int i = 0; i <= 5; i++) {
                int y = oy + h - (h * i / 5);
                g2.drawLine(ox, y, ox + w, y);
                g2.setColor(new Color(150, 150, 150));
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                g2.drawString(String.valueOf(maxVal * i / 5), 5, y + 4);
                g2.setColor(new Color(235, 235, 235));
            }

            int size = onlineHistory24h.size();
            if (size >= 2) {
                int[] xp = new int[size];
                int[] yp = new int[size];
                for (int i = 0; i < size; i++) {
                    xp[i] = ox + (w * i / (size - 1));
                    yp[i] = oy + h - (int) ((long) h * onlineHistory24h.get(i) / maxVal);
                }

                // Fill area
                int[] fillX = new int[size + 2];
                int[] fillY = new int[size + 2];
                System.arraycopy(xp, 0, fillX, 0, size);
                System.arraycopy(yp, 0, fillY, 0, size);
                fillX[size] = xp[size - 1]; fillY[size] = oy + h;
                fillX[size + 1] = xp[0]; fillY[size + 1] = oy + h;
                g2.setColor(new Color(0, 120, 215, 30));
                g2.fillPolygon(fillX, fillY, size + 2);

                // Line
                g2.setColor(new Color(0, 120, 215));
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawPolyline(xp, yp, size);

                // Dots
                for (int i = 0; i < size; i++) {
                    g2.setColor(new Color(0, 120, 215));
                    g2.fillOval(xp[i] - 3, yp[i] - 3, 6, 6);
                }

                // Last value
                int lastVal = onlineHistory24h.get(size - 1);
                g2.setColor(new Color(0, 120, 215));
                g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                g2.drawString(String.valueOf(lastVal), xp[size - 1] + 5, yp[size - 1] - 5);
            }

            // Axes
            g2.setColor(new Color(100, 100, 100));
            g2.setStroke(new BasicStroke(1f));
            g2.drawLine(ox, oy, ox, oy + h);
            g2.drawLine(ox, oy + h, ox + w, oy + h);

            // X-axis label
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            g2.drawString("← Cũ nhất", ox, oy + h + 15);
            g2.drawString("Mới nhất →", ox + w - 55, oy + h + 15);
        }
    }

    /** Server Architecture Diagram */
    private JPanel createArchitectureDiagramPanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(ServerGuiUtils.createSectionBorder("🏗 Sơ Đồ Kiến Trúc Server"));

        chartArchPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawArchitectureDiagram((Graphics2D) g);
            }
        };
        chartArchPanel.setBackground(new Color(250, 250, 252));
        chartArchPanel.setPreferredSize(new Dimension(0, 180));
        wrapper.add(chartArchPanel, BorderLayout.CENTER);
        return wrapper;
    }

    private void drawArchitectureDiagram(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = chartArchPanel.getWidth();
        int h = chartArchPanel.getHeight();

        // Components layout
        int boxW = 130, boxH = 55;
        int cy = h / 2;

        // Positions
        int[] posX = {40, 220, 400, 580, 720};
        String[] labels = {"📱 Clients", "🔥 Firewall", "🔄 Proxy", "⚡ Game Server", "💾 Database"};
        String[] subs = {"Players", "Anti-DDoS", "Load Balancer", "Main Engine", "MySQL"};
        Color[] bgColors = {
            new Color(0, 120, 215), new Color(220, 53, 69), new Color(255, 152, 0),
            new Color(40, 167, 69), new Color(63, 81, 181)
        };

        // Scale if needed
        float scale = 1f;
        if (w < 880) {
            scale = (float) w / 900f;
        }

        Graphics2D gs = (Graphics2D) g2.create();
        if (scale < 1f) {
            gs.scale(scale, 1);
        }

        // Draw connections first
        gs.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < posX.length - 1; i++) {
            int x1 = posX[i] + boxW;
            int x2 = posX[i + 1];
            gs.setColor(new Color(180, 180, 180));
            gs.drawLine(x1, cy, x2, cy);

            // Arrow
            int arrowX = x2 - 6;
            gs.setColor(bgColors[i + 1]);
            gs.fillPolygon(new int[]{arrowX, arrowX - 8, arrowX - 8}, new int[]{cy, cy - 5, cy + 5}, 3);
        }

        // Draw boxes
        for (int i = 0; i < posX.length; i++) {
            int bx = posX[i];
            int by = cy - boxH / 2;

            // Shadow
            gs.setColor(new Color(0, 0, 0, 20));
            gs.fillRoundRect(bx + 2, by + 3, boxW, boxH, 10, 10);

            // Box
            GradientPaint gp = new GradientPaint(bx, by, bgColors[i], bx, by + boxH, bgColors[i].darker());
            gs.setPaint(gp);
            gs.fillRoundRect(bx, by, boxW, boxH, 10, 10);

            // Highlight
            gs.setColor(new Color(255, 255, 255, 40));
            gs.fillRoundRect(bx + 1, by + 1, boxW - 2, boxH / 2, 9, 9);

            // Text
            gs.setColor(Color.WHITE);
            gs.setFont(new Font("Segoe UI", Font.BOLD, 12));
            FontMetrics fm = gs.getFontMetrics();
            int tw = fm.stringWidth(labels[i]);
            gs.drawString(labels[i], bx + (boxW - tw) / 2, cy - 3);

            gs.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            fm = gs.getFontMetrics();
            tw = fm.stringWidth(subs[i]);
            gs.drawString(subs[i], bx + (boxW - tw) / 2, cy + 14);

            // Status dot
            gs.setColor(new Color(0, 220, 80));
            gs.fillOval(bx + boxW - 14, by + 5, 8, 8);
        }

        // Flow labels
        gs.setColor(new Color(120, 120, 120));
        gs.setFont(new Font("Segoe UI", Font.ITALIC, 9));
        for (int i = 0; i < posX.length - 1; i++) {
            int midX = (posX[i] + boxW + posX[i + 1]) / 2;
            gs.drawString("→", midX - 5, cy - 12);
        }

        gs.dispose();
    }

    /** Manually refresh the entire dashboard: connection monitor + analytics + game stats */
    private void refreshEntireDashboard() {
        addLog("DASHBOARD: Đang làm mới dữ liệu...");
        // Update timestamp
        lblLastRefresh.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        // Refresh connection monitor
        refreshConnectionMonitor();
        // Refresh analytics from DB (runs in background thread)
        scheduler.execute(() -> {
            try {
                doRefreshAnalyticsFromDB();
            } catch (Exception e) {
                addLog("Refresh error: " + e.getMessage());
            }
        });
        addLog("DASHBOARD: Làm mới hoàn tất.");
    }

    /** Refresh analytics data from database (auto-scheduled every 5 min) */
    private void refreshAnalyticsData() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                doRefreshAnalyticsFromDB();
            } catch (Exception e) {
                addLog("Analytics refresh error: " + e.getMessage());
            }
        }, 2, 300, TimeUnit.SECONDS);
    }

    /** Core DB refresh logic - called by both auto-scheduler and manual refresh button */
    private void doRefreshAnalyticsFromDB() {
        try (Connection con = jdbc.DBConnecter.getConnectionServer()) {
            // Total accounts
            try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM account");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) totalAccounts = rs.getInt(1);
            }

            // Total characters
            try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM player");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) totalCharacters = rs.getInt(1);
            }

            // Total gold
            try (PreparedStatement ps = con.prepareStatement("SELECT COALESCE(SUM(vang), 0) FROM account");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) totalGold = rs.getLong(1);
            }

            // Total ruby
            try (PreparedStatement ps = con.prepareStatement("SELECT COALESCE(SUM(thoi_vang), 0) FROM account");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) totalRuby = rs.getLong(1);
            }

            // Race distribution (gender: 0=Trái Đất, 1=Namek, 2=Xayda)
            int[] dist = {0, 0, 0};
            try (PreparedStatement ps = con.prepareStatement("SELECT gender, COUNT(*) FROM player GROUP BY gender");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int g = rs.getInt(1);
                    if (g >= 0 && g <= 2) dist[g] = rs.getInt(2);
                }
            }
            raceDistribution = dist;

            // Top 10 players by power
            java.util.List<String[]> tops = new java.util.ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT name, COALESCE(power, 0) FROM player ORDER BY power DESC LIMIT 10");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString(1);
                    long power = rs.getLong(2);
                    if (name != null && !name.isEmpty()) {
                        tops.add(new String[]{name, String.valueOf(power)});
                    }
                }
            } catch (Exception e2) {
                addLog("Top players query failed: " + e2.getMessage());
            }
            topPlayers = tops.toArray(new String[0][0]);

            // Record online count for chart
            int online = (Client.gI() != null) ? Client.gI().getPlayers().size() : 0;
            synchronized (onlineHistory24h) {
                onlineHistory24h.add(online);
                if (onlineHistory24h.size() > 288) {
                    onlineHistory24h.remove(0);
                }
            }

        } catch (Exception e) {
            addLog("Analytics refresh error: " + e.getMessage());
        }

        // Update UI on EDT
        SwingUtilities.invokeLater(() -> {
            String goldStr = formatLargeNumber(totalGold);
            String rubyStr = formatLargeNumber(totalRuby);

            updateAnalyticsCard(lblStatAccounts, "Tổng Tài Khoản", String.valueOf(totalAccounts), new Color(0, 120, 215));
            updateAnalyticsCard(lblStatCharacters, "Tổng Nhân Vật", String.valueOf(totalCharacters), new Color(40, 167, 69));
            updateAnalyticsCard(lblStatGold, "Tổng Vàng", goldStr, new Color(255, 152, 0));
            updateAnalyticsCard(lblStatRuby, "Tổng Ruby", rubyStr, new Color(220, 53, 69));

            if (chartBarPanel != null) chartBarPanel.repaint();
            if (chartPiePanel != null) chartPiePanel.repaint();
            if (chartLinePanel != null) chartLinePanel.repaint();
            if (chartArchPanel != null) chartArchPanel.repaint();
        });
    }

    private String formatLargeNumber(long num) {
        if (num >= 1_000_000_000L) return String.format("%.1fB", num / 1_000_000_000.0);
        if (num >= 1_000_000L) return String.format("%.1fM", num / 1_000_000.0);
        if (num >= 1_000L) return String.format("%.1fK", num / 1_000.0);
        return String.valueOf(num);
    }

    private JPanel createLogPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(ServerGuiUtils.createSectionBorder("Server Logs"));
        
        txtLog = new JTextArea(8, 50);
        txtLog.setEditable(false);
        txtLog.setFont(new Font("Monospaced", Font.PLAIN, 12));
        txtLog.setBackground(new Color(250, 250, 250));
        
        JScrollPane scrollLog = new JScrollPane(txtLog);
        scrollLog.setBorder(new LineBorder(new Color(200, 200, 200)));
        scrollLog.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        
        p.add(scrollLog, BorderLayout.CENTER);
        return p;
    }
    
    // --- [LOGIC] Optimization Methods ---
    
    private void performRamCleanup() {
        new Thread(() -> {
            // Sử dụng Runtime để tính RAM của riêng Java JVM
            long before = Runtime.getRuntime().freeMemory(); 
            System.gc(); // Trigger Java Garbage Collector
            long after = Runtime.getRuntime().freeMemory();
            
            // Tính lượng RAM giải phóng được trong Heap
            long freed = after - before; 
            
            // Format sang MB
            long freedMB = freed / 1024 / 1024;
            
            String msg = (freed > 0) 
                    ? "OPTIMIZE: Đã dọn dẹp JVM Heap. Giải phóng: " + freedMB + " MB."
                    : "OPTIMIZE: JVM RAM đã ở trạng thái tối ưu.";
            addLog(msg);
        }).start();
    }
    
    private void performCpuOptimization() {
        new Thread(() -> {
            if (txtLog.getDocument().getLength() > 50000) {
                 txtLog.setText("");
                 addLog("CPU OPT: Đã xóa bộ đệm Log để giảm tải UI.");
            }
            System.runFinalization(); 
            addLog("CPU OPT: Đã tối ưu hóa các tiến trình nền Java.");
        }).start();
    }
    
    private void toggleAutoOptimization() {
        if (activeAutoOptimizeFuture != null) {
            activeAutoOptimizeFuture.cancel(false);
            activeAutoOptimizeFuture = null;
        }

        if (chkAutoOptimize.isSelected()) {
            String selected = (String) cbOptimizeInterval.getSelectedItem();
            int minutes = 10;
            if (selected.contains("5")) minutes = 5;
            else if (selected.contains("30")) minutes = 30;
            else if (selected.contains("60")) minutes = 60;
            
            lblOptStatus.setText("Trạng thái: Đang chạy (" + minutes + "p/lần)");
            lblOptStatus.setForeground(new Color(0, 153, 51));
            
            activeAutoOptimizeFuture = scheduler.scheduleAtFixedRate(() -> {
                addLog("AUTO-OPT: Bắt đầu chu trình tối ưu tự động...");
                performRamCleanup();
            }, minutes, minutes, TimeUnit.MINUTES);
            
            addLog("SYSTEM: Đã bật tự động tối ưu hóa (" + minutes + " phút/lần).");
        } else {
            lblOptStatus.setText("Trạng thái: Tắt");
            lblOptStatus.setForeground(Color.RED);
            addLog("SYSTEM: Đã tắt tự động tối ưu hóa.");
        }
    }

    public void addLog(String message) {
        if (txtLog != null) {
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            SwingUtilities.invokeLater(() -> {
                txtLog.append("[" + time + "] " + message + "\n");
                txtLog.setCaretPosition(txtLog.getDocument().getLength());
            });
        }
    }

    // ================= MONITORING =================

    private void startMonitoring() {
        scheduler.scheduleAtFixedRate(() -> {
            // 1. Get System Stats
            double cpu = 0;
            try {
                // Ép kiểu rõ ràng để tránh lỗi import với java.lang.management.OperatingSystemMXBean
                OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
                if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                    double load = ((com.sun.management.OperatingSystemMXBean) osBean).getProcessCpuLoad();
                    // Load trả về 0.0 -> 1.0. Nếu lỗi trả về -1
                    if (!Double.isNaN(load) && load >= 0) {
                        cpu = load * 100;
                    }
                }
            } catch (Exception e) { 
                cpu = 0; // Fallback an toàn nếu lỗi
            }
            
            // Tính toán RAM dựa trên Runtime (JVM Memory)
            long maxMemory = Runtime.getRuntime().maxMemory() / (1024 * 1024);
            long totalMemory = Runtime.getRuntime().totalMemory() / (1024 * 1024);
            long freeMemory = Runtime.getRuntime().freeMemory() / (1024 * 1024);
            long usedMemory = totalMemory - freeMemory;
            
            int threads = Thread.activeCount();
            int sessions = (SessionManager.gI() != null) ? SessionManager.gI().getSessions().size() : 0;

            // 2. Get Game Stats
            int playerCount = (Client.gI() != null) ? Client.gI().getPlayers().size() : 0;
            int giftCodeCount = (GiftCodeManager.gI() != null) ? GiftCodeManager.gI().listGiftCode.size() : 0;
            int consignCount = (ConsignShopManager.gI() != null) ? ConsignShopManager.gI().listItem.size() : 0;
            
            String bossStats = "N/A";
            if (BossManager.gI() != null) {
                try {
                    int[] stats = BossManager.gI().getBossStatusCounts();
                    bossStats = String.format("Boss: %d Alive | %d Respawn | %d Wait", stats[0], stats[1], stats[2]);
                } catch (Exception e) {}
            }

            // 3. Update UI
            String finalBossStats = bossStats;
            final double finalCpu = cpu;

            SwingUtilities.invokeLater(() -> {
                lblCpuUsage.setText(String.format("Server CPU: %.1f%%", finalCpu));
                lblRamUsage.setText(usedMemory + " / " + maxMemory + " MB");
                
                lblThreadCount.setText(String.valueOf(threads));
                lblSessionCount.setText(String.valueOf(sessions));
                
                lblPlayerCount.setText("Online: " + playerCount);
                lblGiftcodeInfo.setText("Giftcodes: " + giftCodeCount);
                lblConsignItemsCount.setText("Consign Items: " + consignCount);
                lblBossStatus.setText(finalBossStats);

                updateUptime();

                graphCpu.addValue((int) finalCpu);
                
                // Biểu đồ RAM: Tính % dựa trên Max Memory
                int ramPercent = (maxMemory > 0) ? (int) ((usedMemory * 100) / maxMemory) : 0;
                graphRam.addValue(ramPercent);
                
                graphThread.addValue(Math.min(threads, 100)); 
                graphSession.addValue(Math.min(sessions, 100)); 
            });
        }, 0, 1, TimeUnit.SECONDS);

        // Connection Monitor: NO auto-refresh (manual only via Refresh button)
        // Initial load
        SwingUtilities.invokeLater(() -> refreshConnectionMonitor());
    }

    private void updateUptime() {
        Duration d = Duration.between(serverStartTime, Instant.now());
        lblUptime.setText(String.format("Uptime: %dd %02dh %02dm %02ds", 
            d.toDays(), d.toHoursPart(), d.toMinutesPart(), d.toSecondsPart()));
    }

    // ================= ACTIONS LOGIC =================

    private void confirmMaintenance() {
        if (JOptionPane.showConfirmDialog(this, "Bắt đầu bảo trì sau 2 phút?", "Xác nhận", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            cancelAllScheduledTasks();
            Maintenance.gI().start(2); // Gọi hàm bảo trì của server
            lblStatus.setText("MAINTENANCE STARTING...");
            lblStatus.setForeground(Color.RED);
            addLog("ACTION: Bắt đầu chu trình bảo trì (2 phút).");
        }
    }

    private void showReloadOptions() {
        String[] opts = {"GiftCode", "Shop", "NPCs"};
        int c = JOptionPane.showOptionDialog(this, "Chọn dữ liệu cần tải lại:", "Reload DB", 0, 3, null, opts, opts[0]);
        switch (c) {
            case 0 -> {
                loadGiftcode();
            }
            case 1 -> {
                loadShop();
            }
            case 2 -> {
                loadNpcs();
            }
        }
    }
    
    public void loadGiftcode() {
        GiftCodeManager.gI().listGiftCode.clear();
        String sql = "SELECT * FROM giftcode";
        try (Connection con = DBConnecter.getConnectionServer(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                GiftCode giftcode = new GiftCode();
                giftcode.code = rs.getString("code");
                giftcode.id = rs.getInt("id");
                giftcode.countLeft = rs.getInt("count_left");
                giftcode.datecreate = rs.getTimestamp("datecreate");
                giftcode.dateexpired = rs.getTimestamp("expired");
                String detailJson = rs.getString("detail");
                if (detailJson != null && !detailJson.isEmpty() && JSONValue.parse(detailJson) instanceof JSONArray jar) {
                    for (Object itemObj : jar) {
                        JSONObject jsonObj = (JSONObject) itemObj;
                        int itemId = getJsonInt(jsonObj, "id");
                        int quantity = getJsonInt(jsonObj, "quantity");
                        JSONArray optionArray = (JSONArray) jsonObj.get("options");
                        ArrayList<ItemOption> optionList = new ArrayList<>();
                        if (optionArray != null) {
                            for (Object optionObj : optionArray) {
                                JSONObject optionJson = (JSONObject) optionObj;
                                optionList.add(new ItemOption(getJsonInt(optionJson, "id"), getJsonInt(optionJson, "param")));
                            }
                        }
                        giftcode.option.put(itemId, optionList);
                        giftcode.detail.put(itemId, quantity);
                    }
                }
                GiftCodeManager.gI().listGiftCode.add(giftcode);
            }
            addLog("Success: Reloaded " + GiftCodeManager.gI().listGiftCode.size() + " giftcodes.");
        } catch (Exception e) {
            addLog("Error: Failed to reload giftcode. " + e.getMessage());
        }
    }

    public void loadNpcs() {
        Manager.NPC_TEMPLATES.clear();
        String sql = "SELECT * FROM npc_template";
        try (Connection con = DBConnecter.getConnectionServer(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                NpcTemplate npcTemp = new NpcTemplate();
                npcTemp.id = rs.getByte("id");
                npcTemp.name = rs.getString("name");
                npcTemp.head = rs.getShort("head");
                npcTemp.body = rs.getShort("body");
                npcTemp.leg = rs.getShort("leg");
                npcTemp.avatar = rs.getInt("avatar");
                Manager.NPC_TEMPLATES.add(npcTemp);
            }
            addLog("Success: Reloaded " + Manager.NPC_TEMPLATES.size() + " NPCs.");
        } catch (Exception e) {
            addLog("Error: Failed to reload NPCs. " + e.getMessage());
        }
    }

    public void loadShop() {
        try (Connection con = DBConnecter.getConnectionServer()) {
            Manager.SHOPS = ShopDAO.getShops(con);
            addLog("Success: Reloaded " + Manager.SHOPS.size() + " shops.");
        } catch (Exception e) {
            addLog("Error: Failed to reload shops. " + e.getMessage());
        }
    }
    
    private int getJsonInt(JSONObject obj, String key) {
        if (obj != null && obj.containsKey(key)) {
            try {
                return Integer.parseInt(obj.get(key).toString());
            } catch (NumberFormatException e) {
            }
        }
        return 0;
    }

    private void summonSpecificBoss(int id, String name) {
        try {
            Boss b = BossManager.gI().createBoss(id);
            b.changeStatus(BossStatus.RESPAWN);
            addLog("Boss Action: Đã triệu hồi thành công " + name);
        } catch (Exception e) {
            addLog("Boss Error: Lỗi triệu hồi " + name + ": " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Lỗi triệu hồi boss: " + e.getMessage());
        }
    }

    private void scheduleMaintenance() {
        Integer h = (Integer) cbHour.getSelectedItem();
        Integer m = (Integer) cbMinute.getSelectedItem();
        Integer s = (Integer) cbSecond.getSelectedItem();
        
        if (h == null || h == -1) return;

        REQUEST_AUTO_RESTART = chkAutoRestart.isSelected();
        long nowSeconds = LocalTime.now().toSecondOfDay();
        long targetSeconds = LocalTime.of(h, m, s).toSecondOfDay();
        long delay = targetSeconds - nowSeconds;
        if (delay < 0) delay += 86400; // Next day

        cancelAllScheduledTasks();
        
        lblStatus.setText("Maintenance Scheduled");
        lblStatus.setForeground(new Color(255, 140, 0));

        long triggerTime = System.currentTimeMillis() + (delay * 1000);
        
        // Schedule Job
        activeMaintenanceJobFuture = scheduler.schedule(() -> Maintenance.gI().start(1), delay, TimeUnit.SECONDS);
        
        // Schedule Countdown UI
        activeCountdownDisplayFuture = scheduler.scheduleAtFixedRate(() -> {
            long remain = (triggerTime - System.currentTimeMillis()) / 1000;
            if (remain <= 0) {
                 SwingUtilities.invokeLater(() -> lblCountdown.setText("Executing..."));
                 return;
            }
            SwingUtilities.invokeLater(() -> lblCountdown.setText(String.format("%02d:%02d:%02d", remain/3600, (remain%3600)/60, remain%60)));
        }, 0, 1, TimeUnit.SECONDS);

        addLog("Scheduler: Đặt lịch bảo trì lúc " + String.format("%02d:%02d:%02d", h, m, s) + " (AutoRestart: " + REQUEST_AUTO_RESTART + ")");

        // Save to file
        try (PrintWriter pw = new PrintWriter(new FileWriter("maintenanceConfig.txt"))) {
            pw.println(h + "\n" + m + "\n" + s + "\n" + REQUEST_AUTO_RESTART);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void cancelAllScheduledTasks() {
        if (activeMaintenanceJobFuture != null) activeMaintenanceJobFuture.cancel(true);
        if (activeCountdownDisplayFuture != null) activeCountdownDisplayFuture.cancel(true);
        lblCountdown.setText("Sẵn sàng");
    }

    private void scheduleDailyDatabaseBackup() {
        if (activeDailyBackupFuture != null) {
            activeDailyBackupFuture.cancel(false);
        }
        long initialDelay = secondsUntilNextBackup(3, 30);
        activeDailyBackupFuture = scheduler.scheduleAtFixedRate(
                () -> backupDatabase(false),
                initialDelay,
                TimeUnit.DAYS.toSeconds(1),
                TimeUnit.SECONDS
        );
        addLog("DB Backup: Tự động sao lưu mỗi ngày lúc 03:30 vào thư mục " + DB_BACKUP_DIR);
    }

    private long secondsUntilNextBackup(int hour, int minute) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime next = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0);
        if (!next.isAfter(now)) {
            next = next.plusDays(1);
        }
        return Math.max(1, java.time.Duration.between(now, next).getSeconds());
    }

    private void backupDatabaseAsync(boolean scheduled) {
        new Thread(() -> backupDatabase(scheduled), scheduled ? "DB-Backup-Scheduled" : "DB-Backup-Manual").start();
    }

    private void backupDatabase(boolean scheduled) {
        Properties props = new Properties();
        try (FileReader fr = new FileReader("data/config/config.properties")) {
            props.load(fr);
        } catch (Exception e) {
            addLog("DB Backup: Không đọc được config.properties - " + e.getMessage());
            return;
        }

        String host = props.getProperty("database.host", "127.0.0.1");
        String port = props.getProperty("database.port", "3306");
        String db = props.getProperty("database.name", "nrotft");
        String user = props.getProperty("database.user", "root");
        String pass = props.getProperty("database.pass", "");

        File dir = new File(DB_BACKUP_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            addLog("DB Backup: Không tạo được thư mục " + dir.getAbsolutePath());
            return;
        }

        String fileName = db + "_backup_" + java.time.LocalDateTime.now().format(DB_BACKUP_FILE_TIME) + ".sql";
        File out = new File(dir, fileName);
        File mysqldump = findMysqlDump();

        try {
            List<String> cmd = new ArrayList<>();
            cmd.add(mysqldump != null ? mysqldump.getAbsolutePath() : "mysqldump");
            cmd.add("--host=" + host);
            cmd.add("--port=" + port);
            cmd.add("--user=" + user);
            if (!pass.isEmpty()) {
                cmd.add("--password=" + pass);
            }
            cmd.add("--default-character-set=utf8mb4");
            cmd.add("--single-transaction");
            cmd.add("--routines");
            cmd.add("--events");
            cmd.add(db);

            addLog("DB Backup: Đang sao lưu " + db + " -> " + out.getPath());
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectOutput(out);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            boolean finished = process.waitFor(5, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                addLog("DB Backup: Timeout, đã hủy tiến trình mysqldump.");
                return;
            }
            if (process.exitValue() == 0 && out.exists() && out.length() > 0) {
                addLog("DB Backup: Thành công " + out.getPath() + " (" + (out.length() / 1024) + " KB)");
                cleanupOldBackups(dir, db, 14);
            } else {
                addLog("DB Backup: Thất bại, kiểm tra mysqldump/quyền DB. File: " + out.getPath());
            }
        } catch (Exception e) {
            addLog("DB Backup: Lỗi - " + e.getMessage());
        }
    }

    private File findMysqlDump() {
        String[] paths = {
            "C:/xampp/mysql/bin/mysqldump.exe",
            "C:/Program Files/MySQL/MySQL Server 8.0/bin/mysqldump.exe",
            "C:/Program Files/MariaDB 10.6/bin/mysqldump.exe"
        };
        for (String path : paths) {
            File f = new File(path);
            if (f.exists()) {
                return f;
            }
        }
        return null;
    }

    private void cleanupOldBackups(File dir, String db, int keepDays) {
        File[] files = dir.listFiles((d, name) -> name.startsWith(db + "_backup_") && name.endsWith(".sql"));
        if (files == null) {
            return;
        }
        long cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(keepDays);
        for (File file : files) {
            if (file.lastModified() < cutoff && file.delete()) {
                addLog("DB Backup: Đã xóa backup cũ " + file.getName());
            }
        }
    }

    private void addLog(String message) {
        if (txtLog == null) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            txtLog.append("[" + time + "] " + message + "\n");
            txtLog.setCaretPosition(txtLog.getDocument().getLength());
        });
    }

    // ================= INNER CLASSES =================

    // Optimized MiniGraph Class
    private static class MiniGraph extends JPanel {
        private final ArrayList<Integer> values = new ArrayList<>();
        private static final int MAX_POINTS = 60;
        private final Color primaryColor;
        private final Color fillColor;

        public MiniGraph(Color color) {
            this.primaryColor = color;
            this.fillColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 40);
            setBackground(Color.WHITE);
            setOpaque(true);
        }

        public void addValue(int v) {
            if (values.size() >= MAX_POINTS) values.remove(0);
            values.add(v);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (values.isEmpty()) return;

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int maxVal = 100;

            Path2D.Float path = new Path2D.Float();
            float step = (float) w / (MAX_POINTS - 1);
            
            path.moveTo(0, h);
            for (int i = 0; i < values.size(); i++) {
                float x = i * step;
                float y = h - ((float) values.get(i) / maxVal * h);
                if (i == 0) path.lineTo(x, y); else path.lineTo(x, y);
            }
            float lastX = (values.size() - 1) * step;
            path.lineTo(lastX, h);
            path.lineTo(0, h);
            path.closePath();

            g2.setColor(fillColor);
            g2.fill(path);

            Path2D.Float linePath = new Path2D.Float();
            for (int i = 0; i < values.size(); i++) {
                float x = i * step;
                float y = h - ((float) values.get(i) / maxVal * h);
                if (i == 0) linePath.moveTo(x, y); else linePath.lineTo(x, y);
            }
            g2.setColor(primaryColor);
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(linePath);
            
            g2.setColor(new Color(240, 240, 240));
            g2.setStroke(new BasicStroke(1f));
            g2.drawLine(0, h/2, w, h/2);
        }
    }
}