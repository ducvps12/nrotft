package nro.server.ui;

import nro.server.ServerManager;
import nro.server.NotificationService;
import firewall.ProxyManager;
import nro.server.AutoSaveManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.time.Instant;

public class ServerManagerUI extends JFrame {

    // ===== Sidebar Item =====
    private static class NavItem {
        String name;
        Icon icon;
        String key;

        public NavItem(String name, String iconPath, String key) {
            this.name = name;
            this.key = key;
            this.icon = ServerGuiUtils.loadIcon(iconPath);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final Instant serverStartTime;
    private JPanel contentPanel;
    private CardLayout cardLayout;
    private JList<NavItem> sidebar;

    public static volatile boolean REQUEST_AUTO_RESTART = false;

    public ServerManagerUI() {
        super("Server Control Panel - Xuân Anh IT");

        ServerGuiUtils.setupTheme();

        initUI();
        startServerProcesses();

        this.serverStartTime = Instant.now();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (REQUEST_AUTO_RESTART) {
                triggerRestartProcess();
            }
        }));
    }

    // ===== Restart Server =====
    public void triggerRestartProcess() {
        int seconds = 5;
        System.out.println(">>> Restarting Server in " + seconds + "s...");
        try {
            String currentDir = System.getProperty("user.dir");
            String osName = System.getProperty("os.name").toLowerCase();

            ProcessBuilder pb;
            if (osName.contains("win")) {
                pb = new ProcessBuilder("cmd", "/c", "start", "cmd", "/c",
                        "timeout /t " + seconds + " /nobreak && run.bat");
            } else {
                pb = new ProcessBuilder("bash", "-c", "sleep " + seconds + "; ./run.sh &");
            }

            pb.directory(new File(currentDir));
            pb.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===== UI =====
    private void initUI() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(245, 245, 245));

        NavItem[] menuItems = {
                new NavItem("Bảng Điều Khiển", "/icon/dashboard.png", "Dashboard"),
                new NavItem("Quản Lý Tài Khoản", "/icon/Account.png", "Account"),
                new NavItem("Danh Sách Người Chơi", "/icon/user2.png", "Players"),
                new NavItem("Cửa Hàng (Shop)", "/icon/shop.png", "ShopEditor"),
                new NavItem("Quản Lý NPC", "/icon/shop.png", "NpcManager"),
                new NavItem("Quản Lý Giftcode", "/icon/gift.png", "Giftcode"),
                new NavItem("Nạp Thẻ & Thưởng", "/icon/topup.png", "TopupReward"),
                new NavItem("Sự Kiện (Events)", "/icon/calendar.png", "Events"),
                new NavItem("Hẹn Giờ Sự Kiện", "/icon/calendar.png", "EventTime"),
                new NavItem("Danh Hiệu (Badges)", "/icon/star.png", "Badges"),
                new NavItem("Dữ Liệu Bản Đồ", "/icon/map.png", "MapData"),
                new NavItem("Quản Lý Drop", "/icon/drop.png", "DropManager"),
                new NavItem("🎰 Vòng Quay Thượng Đế", "/icon/star.png", "LuckyRound"),
                new NavItem("Dữ Liệu Vật Phẩm", "/icon/item.png", "ItemData"),
                new NavItem("📋 Audit Vật Phẩm", "/icon/item.png", "ItemAudit"),
                new NavItem("🌍 Vũ Trụ MTDGame", "/icon/map.png", "Universe"),
                new NavItem("💰 Tài Chính Vũ Trụ", "/icon/transaction.png", "Economy"),
                new NavItem("Quản Lý Radar", "/icon/radar.png", "Radar"),
                new NavItem("Cấu Hình Boss", "/icon/monster.png", "BossConfig"),
                new NavItem("🔍 Duyệt Tài Nguyên", "/icon/item.png", "ResourceBrowser"),
                new NavItem("Bảo Mật & Firewall", "/icon/shield.png", "Security"),
                new NavItem("🛡 Anti-DDoS", "/icon/shield.png", "AntiDDoS"),
                new NavItem("Quản Lý Database", "/icon/database.png", "Database"),
                new NavItem("Phân Tích Traffic", "/icon/traffic.png", "Traffic"),
                new NavItem("Quản Lý Giao Dịch", "/icon/transaction.png", "Transaction"),
                new NavItem("Server Log Viewer", "/icon/traffic.png", "LogViewer"),
                new NavItem("🔥 Stress Test", "/icon/traffic.png", "StressTest"),
                new NavItem("Cài Đặt", "/icon/settings.png", "Settings")
        };

        sidebar = new JList<>(menuItems);
        sidebar.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sidebar.setSelectedIndex(0);
        sidebar.setFixedCellHeight(55);
        sidebar.setBackground(Color.WHITE);
        sidebar.setBorder(new EmptyBorder(10, 0, 10, 0));

        sidebar.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof NavItem item) {
                    lbl.setText(item.name);
                    if (item.icon != null)
                        lbl.setIcon(item.icon);
                }

                lbl.setBorder(new EmptyBorder(0, 20, 0, 0));
                lbl.setIconTextGap(15);
                lbl.setFont(new Font("Segoe UI", isSelected ? Font.BOLD : Font.PLAIN, 14));

                if (isSelected) {
                    lbl.setBackground(new Color(230, 242, 255));
                    lbl.setForeground(new Color(0, 102, 204));
                    lbl.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 4, 0, 0, new Color(0, 120, 215)),
                            new EmptyBorder(0, 16, 0, 0)));
                } else {
                    lbl.setBackground(Color.WHITE);
                    lbl.setForeground(new Color(60, 60, 60));
                }
                return lbl;
            }
        });

        JScrollPane scrollSidebar = new JScrollPane(sidebar);
        scrollSidebar.setPreferredSize(new Dimension(260, getHeight()));
        scrollSidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(220, 220, 220)));

        // ===== Footer minhluong =====
        JPanel sidebarContainer = new JPanel(new BorderLayout());
        sidebarContainer.add(scrollSidebar, BorderLayout.CENTER);

        JPanel footer = new JPanel();
        footer.setBackground(Color.WHITE);
        footer.setBorder(new EmptyBorder(10, 0, 10, 0));
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));

        JLabel devText = new JLabel("Developed by", SwingConstants.CENTER);
        devText.setAlignmentX(Component.CENTER_ALIGNMENT);
        devText.setForeground(new Color(120, 120, 120));

        JLabel brand = new JLabel("Xuân Anh IT", SwingConstants.CENTER);
        brand.setAlignmentX(Component.CENTER_ALIGNMENT);
        brand.setFont(new Font("Segoe UI", Font.BOLD, 18));
        brand.setForeground(new Color(0, 120, 215));

        JLabel phone = new JLabel("0376263452", SwingConstants.CENTER);
        phone.setAlignmentX(Component.CENTER_ALIGNMENT);
        phone.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        phone.setForeground(new Color(100, 100, 100));

        JLabel copy = new JLabel("© 2026 Xuân Anh IT", SwingConstants.CENTER);
        copy.setAlignmentX(Component.CENTER_ALIGNMENT);
        copy.setForeground(new Color(150, 150, 150));

        footer.add(devText);
        footer.add(brand);
        footer.add(phone);
        footer.add(copy);

        sidebarContainer.add(footer, BorderLayout.SOUTH);

        add(sidebarContainer, BorderLayout.WEST);

        // ===== Content =====
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(Color.WHITE);

        contentPanel.add(new DashboardPanel(), "Dashboard");
        contentPanel.add(new AccountPanel(), "Account");
        contentPanel.add(new PlayersPanel(), "Players");
        contentPanel.add(new ShopEditorPanel(), "ShopEditor");
        contentPanel.add(new NpcManagerPanel(), "NpcManager");
        contentPanel.add(new GiftcodePanel(), "Giftcode");
        contentPanel.add(new TopupRewardPanel(), "TopupReward");
        contentPanel.add(new EventPanel(), "Events");
        contentPanel.add(new EventTimeManagerPanel(), "EventTime");
        contentPanel.add(new BadgesPanel(), "Badges");
        contentPanel.add(new MapPanel(), "MapData");
        contentPanel.add(new DropItemPanel(), "DropManager");
        contentPanel.add(new LuckyRoundPanel(), "LuckyRound");
        contentPanel.add(new ItemPanel(), "ItemData");
        contentPanel.add(new ItemAuditPanel(), "ItemAudit");
        contentPanel.add(new UniverseMonitorPanel(), "Universe");
        contentPanel.add(new EconomyMonitorPanel(), "Economy");
        contentPanel.add(new RadarPanel(), "Radar");
        contentPanel.add(new BossEditorPanel(), "BossConfig");
        contentPanel.add(new ResourceBrowserPanel(), "ResourceBrowser");
        contentPanel.add(new SecurityPanel(), "Security");
        contentPanel.add(new AntiDDoSPanel(), "AntiDDoS");
        contentPanel.add(new DatabasePanel(), "Database");
        contentPanel.add(new TrafficPanel(), "Traffic");
        contentPanel.add(new TransactionPanel(), "Transaction");
        contentPanel.add(new LogViewerPanel(), "LogViewer");
        contentPanel.add(new StressTestPanel(), "StressTest");
        contentPanel.add(new SettingsPanel(), "Settings");

        add(contentPanel, BorderLayout.CENTER);

        sidebar.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                NavItem selected = sidebar.getSelectedValue();
                if (selected != null) {
                    cardLayout.show(contentPanel, selected.key);
                }
            }
        });

        setSize(1300, 850);
        setMinimumSize(new Dimension(1150, 750));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int confirm = JOptionPane.showConfirmDialog(
                        ServerManagerUI.this,
                        "Bạn có chắc muốn dừng Server và thoát chương trình?",
                        "Xác nhận tắt Server",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                if (confirm == JOptionPane.YES_OPTION) {
                    shutdownServer();
                }
            }
        });
    }

    private void startServerProcesses() {
        System.out.println(">> Starting Server Engine...");
        new Thread(() -> {
            ServerManager.gI().run();
            // Auto-start Anti-DDoS protection
            int autoStarted = ProxyManager.getInstance().autoStartFromConfig();
            if (autoStarted > 0) {
                System.out.println(">> Anti-DDoS: Auto-started " + autoStarted + " proxy(s)");
            }
            // Gửi thông báo Telegram: Server đã khởi động
            try { NotificationService.gI().notifyServerStart(); } catch (Exception ignored) {}
            EventQueue.invokeLater(() -> setVisible(true));
        }).start();
    }

    private void shutdownServer() {
        try {
            // Gửi thông báo Telegram: Server đang tắt
            try { NotificationService.gI().notifyServerStop("Admin shutdown"); } catch (Exception ignored) {}
            if (ProxyManager.getInstance() != null)
                ProxyManager.getInstance().stopAll();
            if (AutoSaveManager.getInstance() != null)
                AutoSaveManager.getInstance().stopAutoSave();
        } catch (Exception e) {
            System.err.println("Shutdown error: " + e.getMessage());
        }
        System.exit(0);
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(ServerManagerUI::new);
    }
}