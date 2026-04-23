package nro.server.ui;

import boss.Boss;
import boss.BossManager;
import nro.player.Player;
import nro.server.Client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class UniverseMonitorPanel extends JPanel {

    private final JLabel lblLastRefresh;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    // UI Panels for each planet
    private final JPanel pnlTraiDat = createPlanetContainer("Trái Đất", new Color(0, 120, 215));
    private final JPanel pnlNamec = createPlanetContainer("Namec", new Color(40, 167, 69));
    private final JPanel pnlXayda = createPlanetContainer("Xayda", new Color(220, 53, 69));
    private final JPanel pnlOther1 = createPlanetContainer("Tương Lai", new Color(138, 43, 226));
    private final JPanel pnlOther2 = createPlanetContainer("Vũ Trụ Không Gian", new Color(108, 117, 125));

    // Stats
    private final Map<String, JLabel> statLabels = new HashMap<>();
    private final Map<String, DefaultListModel<String>> bossListModels = new HashMap<>();

    public UniverseMonitorPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(new EmptyBorder(0, 0, 15, 0));

        JLabel title = ServerGuiUtils.createStyledLabel("🌍 Radar Vũ Trụ MTDGame", 22, true);
        title.setForeground(new Color(40, 40, 40));
        
        JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightHeader.setOpaque(false);
        
        lblLastRefresh = new JLabel("Last Scan: " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        lblLastRefresh.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        lblLastRefresh.setForeground(Color.GRAY);
        
        JButton btnScan = ServerGuiUtils.createStyledButton("📡 Quét Radar Ngay", new Color(23, 162, 184), Color.WHITE);
        btnScan.addActionListener(e -> liveScanUniverse());
        
        rightHeader.add(lblLastRefresh);
        rightHeader.add(btnScan);

        headerPanel.add(title, BorderLayout.WEST);
        headerPanel.add(rightHeader, BorderLayout.EAST);

        // Grid for planets
        JPanel gridPanel = new JPanel(new GridLayout(2, 3, 15, 15));
        gridPanel.setOpaque(false);
        
        gridPanel.add(setupPlanetPanel(pnlTraiDat, "TraiDat"));
        gridPanel.add(setupPlanetPanel(pnlNamec, "Namec"));
        gridPanel.add(setupPlanetPanel(pnlXayda, "Xayda"));
        gridPanel.add(setupPlanetPanel(pnlOther1, "TuongLai"));
        gridPanel.add(setupPlanetPanel(pnlOther2, "VuTru"));

        add(headerPanel, BorderLayout.NORTH);
        add(new JScrollPane(gridPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);

        // Auto Scheduler
        scheduler.scheduleAtFixedRate(this::liveScanUniverse, 1, 5, TimeUnit.SECONDS);
    }

    private JPanel createPlanetContainer(String name, Color themeColor) {
        JPanel p = new JPanel(new BorderLayout(0, 5));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
                new EmptyBorder(10, 10, 10, 10)
        ));

        // Header Title Let
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, themeColor));

        JLabel lblTitle = new JLabel(name);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitle.setForeground(themeColor);
        header.add(lblTitle, BorderLayout.WEST);

        p.add(header, BorderLayout.NORTH);
        return p;
    }

    private JPanel setupPlanetPanel(JPanel container, String key) {
        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);

        // Top: stats
        JLabel lblPlayers = new JLabel("Nhân vật đang đứng: 0");
        lblPlayers.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblPlayers.setForeground(new Color(60, 60, 60));
        lblPlayers.setBorder(new EmptyBorder(5, 0, 5, 0));
        statLabels.put(key, lblPlayers);

        body.add(lblPlayers, BorderLayout.NORTH);

        // Center: Boss Radar
        DefaultListModel<String> listModel = new DefaultListModel<>();
        bossListModels.put(key, listModel);
        JList<String> list = new JList<>(listModel);
        list.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                lbl.setIcon(ServerGuiUtils.loadIcon("/icon/monster.png")); 
                lbl.setBorder(new EmptyBorder(2, 5, 2, 5));
                if (value.toString().contains("Đã chết")) {
                    lbl.setForeground(Color.GRAY);
                    lbl.setFont(new Font("Segoe UI", Font.ITALIC, 12));
                } else {
                    lbl.setForeground(new Color(220, 53, 69));
                }
                return lbl;
            }
        });

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(230, 230, 230)), 
                "Radar Quét Boss",
                javax.swing.border.TitledBorder.LEFT, 
                javax.swing.border.TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 11), 
                new Color(150, 150, 150)
        ));
        
        body.add(scroll, BorderLayout.CENTER);
        container.add(body, BorderLayout.CENTER);
        return container;
    }

    private void liveScanUniverse() {
        SwingUtilities.invokeLater(() -> {
            lblLastRefresh.setText("Last Scan: " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        });

        // 1. Quét Players
        int td = 0, nm = 0, xd = 0, tl = 0, vt = 0;
        
        if (Client.gI() != null) {
            List<Player> players = new ArrayList<>(Client.gI().getPlayers());
            for (Player p : players) {
                if (p != null && p.zone != null && p.zone.map != null) {
                    byte planet = p.zone.map.planetId; // Assuming 'planetId' is accessible
                    // Check Tương Lai / Hành tinh khác
                    int mapId = p.zone.map.mapId;
                    if (mapId >= 92 && mapId <= 104) {
                        tl++; // Tương lai
                    } else if (mapId >= 105 && mapId <= 111) {
                        vt++; // Vũ trụ
                    } else if (mapId >= 131 && mapId <= 133) {
                        vt++; // Yardart
                    } else if (planet == 0) {
                        td++;
                    } else if (planet == 1) {
                        nm++;
                    } else if (planet == 2) {
                        xd++;
                    } else {
                        vt++;
                    }
                }
            }
        }

        final int countTd = td, countNm = nm, countXd = xd, countTl = tl, countVt = vt;

        // 2. Quét Bosses
        List<String> bTd = new ArrayList<>(), bNm = new ArrayList<>(), bXd = new ArrayList<>(), bTl = new ArrayList<>(), bVt = new ArrayList<>();
        
        if (BossManager.gI() != null) {
            List<Boss> bosses = new ArrayList<>(BossManager.gI().getBosses());
            for (Boss b : bosses) {
                if (b != null) {
                    String status = " [" + b.name + "] ";
                    if (b.zone != null && b.zone.map != null) {
                        status += "- Map: " + b.zone.map.mapName + " (Khu: " + b.zone.zoneId + ")";
                        int mapId = b.zone.map.mapId;
                        byte planet = b.zone.map.planetId;
                        
                        if (mapId >= 92 && mapId <= 104) bTl.add(status);
                        else if (mapId >= 105 && mapId <= 111 || mapId >= 131 && mapId <= 133) bVt.add(status);
                        else if (planet == 0) bTd.add(status);
                        else if (planet == 1) bNm.add(status);
                        else if (planet == 2) bXd.add(status);
                        else bVt.add(status);
                    } else {
                        status += " - (Đang Chờ Res)";
                        bVt.add(status); // Đẩy vào vũ trụ chung
                    }
                }
            }
        }

        // Cập nhật UI an toàn trên EDT
        SwingUtilities.invokeLater(() -> {
            updatePlanetData("TraiDat", countTd, bTd);
            updatePlanetData("Namec", countNm, bNm);
            updatePlanetData("Xayda", countXd, bXd);
            updatePlanetData("TuongLai", countTl, bTl);
            updatePlanetData("VuTru", countVt, bVt);
        });
    }

    private void updatePlanetData(String key, int playerCount, List<String> bosses) {
        JLabel lbl = statLabels.get(key);
        if (lbl != null) {
            lbl.setText("Nhân vật đang đứng: " + playerCount);
        }
        
        DefaultListModel<String> model = bossListModels.get(key);
        if (model != null) {
            model.clear();
            if (bosses.isEmpty()) {
                model.addElement("Không phát hiện Boss.");
            } else {
                bosses.forEach(model::addElement);
            }
        }
    }
}
