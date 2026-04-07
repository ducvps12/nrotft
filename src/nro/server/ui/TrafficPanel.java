package nro.server.ui;

import nro.server.Client;
import nro.server.ServerManager;
import nro.server.io.MySession;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Panel Phân Tích Traffic - Biểu đồ người chơi online theo thời gian,
 * thống kê IP, peak online, traffic flow.
 */
public class TrafficPanel extends JPanel {

    // Stats
    private JLabel lblCurrentOnline, lblPeakOnline, lblPeakTime;
    private JLabel lblTodayLogins, lblUniqueIPs, lblAvgSessionTime;

    // Online History Graph
    private final java.util.List<Integer> onlineHistory = Collections.synchronizedList(new ArrayList<>());
    private JPanel graphPanel;
    private int peakOnline = 0;
    private String peakTimeStr = "N/A";
    private int todayLogins = 0;

    // IP Table
    private DefaultTableModel ipTableModel;

    // Scheduler
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Instant startTime = Instant.now();

    public TrafficPanel() {
        setLayout(new BorderLayout(0, 10));
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(15, 15, 15, 15));

        add(createStatsCardsPanel(), BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        centerPanel.setOpaque(false);
        centerPanel.add(createOnlineGraphPanel(), BorderLayout.CENTER);
        centerPanel.add(createIPTrackingPanel(), BorderLayout.SOUTH);

        add(centerPanel, BorderLayout.CENTER);

        // Start monitoring
        startTrafficMonitor();
    }

    // ===== Stats Cards =====
    private JPanel createStatsCardsPanel() {
        JPanel p = new JPanel(new GridLayout(1, 6, 8, 0));
        p.setOpaque(false);
        p.setBorder(ServerGuiUtils.createSectionBorder("📊 Thống Kê Traffic Realtime"));

        lblCurrentOnline = createStatCard("Online Hiện Tại", "0", new Color(0, 120, 215));
        lblPeakOnline = createStatCard("Peak Online", "0", new Color(220, 53, 69));
        lblPeakTime = createStatCard("Thời Điểm Peak", "N/A", new Color(142, 68, 173));
        lblTodayLogins = createStatCard("Login Hôm Nay", "0", new Color(40, 167, 69));
        lblUniqueIPs = createStatCard("IP Duy Nhất", "0", new Color(255, 152, 0));
        lblAvgSessionTime = createStatCard("Uptime", "0m", new Color(108, 117, 125));

        p.add(lblCurrentOnline);
        p.add(lblPeakOnline);
        p.add(lblPeakTime);
        p.add(lblTodayLogins);
        p.add(lblUniqueIPs);
        p.add(lblAvgSessionTime);

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
                + "<span style='font-size:16px;color:" + toHex(color) + ";font-weight:bold;'>" + value + "</span>"
                + "</div></html>";
    }

    private String toHex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    // ===== Online History Graph =====
    private JPanel createOnlineGraphPanel() {
        JPanel container = new JPanel(new BorderLayout());
        container.setOpaque(false);
        container.setBorder(ServerGuiUtils.createSectionBorder("📈 Biểu Đồ Online (5 phút/điểm, tối đa 60 điểm)"));

        graphPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawOnlineGraph((Graphics2D) g);
            }
        };
        graphPanel.setBackground(new Color(248, 249, 250));
        graphPanel.setPreferredSize(new Dimension(0, 220));
        graphPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        container.add(graphPanel, BorderLayout.CENTER);
        return container;
    }

    private void drawOnlineGraph(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = graphPanel.getWidth() - 60;
        int h = graphPanel.getHeight() - 40;
        int ox = 50, oy = 10;

        if (onlineHistory.isEmpty()) {
            g2.setColor(new Color(150, 150, 150));
            g2.setFont(new Font("Segoe UI", Font.ITALIC, 14));
            g2.drawString("Đang thu thập dữ liệu...", w / 2 - 80, h / 2);
            return;
        }

        // Find max value
        int maxVal = Math.max(10, onlineHistory.stream().mapToInt(Integer::intValue).max().orElse(10));
        maxVal = ((maxVal / 5) + 1) * 5; // Round up to nearest 5

        // Draw grid
        g2.setColor(new Color(230, 230, 230));
        for (int i = 0; i <= 5; i++) {
            int y = oy + h - (h * i / 5);
            g2.drawLine(ox, y, ox + w, y);
            g2.setColor(new Color(150, 150, 150));
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            g2.drawString(String.valueOf(maxVal * i / 5), 5, y + 4);
            g2.setColor(new Color(230, 230, 230));
        }

        // Draw data
        synchronized (onlineHistory) {
            int size = onlineHistory.size();
            if (size < 2) return;

            int[] xPoints = new int[size];
            int[] yPoints = new int[size];

            for (int i = 0; i < size; i++) {
                xPoints[i] = ox + (w * i / (size - 1));
                yPoints[i] = oy + h - (int) ((long) h * onlineHistory.get(i) / maxVal);
            }

            // Fill area
            int[] fillX = new int[size + 2];
            int[] fillY = new int[size + 2];
            System.arraycopy(xPoints, 0, fillX, 0, size);
            System.arraycopy(yPoints, 0, fillY, 0, size);
            fillX[size] = xPoints[size - 1];
            fillY[size] = oy + h;
            fillX[size + 1] = xPoints[0];
            fillY[size + 1] = oy + h;

            g2.setColor(new Color(0, 120, 215, 30));
            g2.fillPolygon(fillX, fillY, size + 2);

            // Draw line
            g2.setColor(new Color(0, 120, 215));
            g2.setStroke(new BasicStroke(2.5f));
            g2.drawPolyline(xPoints, yPoints, size);

            // Draw dots
            g2.setColor(new Color(0, 120, 215));
            for (int i = 0; i < size; i++) {
                g2.fillOval(xPoints[i] - 3, yPoints[i] - 3, 6, 6);
            }

            // Draw last value
            if (size > 0) {
                int lastVal = onlineHistory.get(size - 1);
                g2.setColor(new Color(0, 120, 215));
                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                g2.drawString(String.valueOf(lastVal), xPoints[size - 1] + 5, yPoints[size - 1] - 5);
            }
        }

        // Draw axes
        g2.setColor(new Color(100, 100, 100));
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(ox, oy, ox, oy + h);
        g2.drawLine(ox, oy + h, ox + w, oy + h);
    }

    // ===== IP Tracking Panel =====
    private JPanel createIPTrackingPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 5));
        p.setOpaque(false);
        p.setBorder(ServerGuiUtils.createSectionBorder("🌐 Top IP Kết Nối"));

        String[] cols = {"#", "IP Address", "Số Kết Nối", "Player Name", "Trạng Thái"};
        ipTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        JTable ipTable = new JTable(ipTableModel);
        ipTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        ipTable.setRowHeight(24);
        ipTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        ipTable.getTableHeader().setBackground(new Color(40, 167, 69));
        ipTable.getTableHeader().setForeground(Color.WHITE);
        ipTable.setSelectionBackground(new Color(232, 245, 233));

        ipTable.getColumnModel().getColumn(0).setPreferredWidth(30);
        ipTable.getColumnModel().getColumn(1).setPreferredWidth(130);
        ipTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        ipTable.getColumnModel().getColumn(3).setPreferredWidth(120);
        ipTable.getColumnModel().getColumn(4).setPreferredWidth(100);

        // Color status column
        ipTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, val, sel, focus, r, c);
                if (!sel && val != null) {
                    String s = val.toString();
                    if (s.contains("Nhiều")) {
                        comp.setForeground(new Color(255, 152, 0));
                        comp.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    } else if (s.contains("Cảnh báo")) {
                        comp.setForeground(new Color(220, 53, 69));
                        comp.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    } else {
                        comp.setForeground(new Color(40, 167, 69));
                    }
                }
                return comp;
            }
        });

        JScrollPane scroll = new JScrollPane(ipTable);
        scroll.setPreferredSize(new Dimension(0, 180));
        p.add(scroll, BorderLayout.CENTER);

        return p;
    }

    // ===== Monitoring Logic =====
    private void startTrafficMonitor() {
        // Update stats every 3 seconds
        scheduler.scheduleAtFixedRate(() -> {
            try {
                int currentOnline = 0;
                Map<String, java.util.List<String>> ipToPlayers = new LinkedHashMap<>();

                if (Client.gI() != null) {
                    var players = Client.gI().getPlayers();
                    currentOnline = players.size();

                    for (var pl : players) {
                        if (pl != null) {
                            try {
                                String ip = "N/A";
                                String name = pl.name != null ? pl.name : "Unknown";
                                if (pl.getSession() != null && pl.getSession() instanceof MySession ms) {
                                    ip = ms.ipAddress != null ? ms.ipAddress : "N/A";
                                }
                                ipToPlayers.computeIfAbsent(ip, k -> new ArrayList<>()).add(name);
                            } catch (Exception ignored) {}
                        }
                    }
                }

                // Track peak
                if (currentOnline > peakOnline) {
                    peakOnline = currentOnline;
                    peakTimeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                }

                // Count login (approximate from unique IPs)
                int uniqueIPs = ipToPlayers.size();

                // Calculate uptime
                Duration uptime = Duration.between(startTime, Instant.now());
                String uptimeStr = String.format("%dh %dm", uptime.toHours(), uptime.toMinutesPart());

                int finalOnline = currentOnline;
                int finalUniqueIPs = uniqueIPs;

                SwingUtilities.invokeLater(() -> {
                    lblCurrentOnline.setText(buildStatHtml("Online Hiện Tại",
                            String.valueOf(finalOnline), new Color(0, 120, 215)));
                    lblPeakOnline.setText(buildStatHtml("Peak Online",
                            String.valueOf(peakOnline), new Color(220, 53, 69)));
                    lblPeakTime.setText(buildStatHtml("Thời Điểm Peak",
                            peakTimeStr, new Color(142, 68, 173)));
                    lblUniqueIPs.setText(buildStatHtml("IP Duy Nhất",
                            String.valueOf(finalUniqueIPs), new Color(255, 152, 0)));
                    lblAvgSessionTime.setText(buildStatHtml("Uptime",
                            uptimeStr, new Color(108, 117, 125)));

                    // Update IP table
                    ipTableModel.setRowCount(0);
                    int idx = 1;
                    var sortedIPs = new ArrayList<>(ipToPlayers.entrySet());
                    sortedIPs.sort((a, b) -> b.getValue().size() - a.getValue().size());

                    for (var entry : sortedIPs) {
                        String ip = entry.getKey();
                        int connCount = entry.getValue().size();
                        String playerNames = String.join(", ", entry.getValue());
                        if (playerNames.length() > 30) playerNames = playerNames.substring(0, 27) + "...";

                        String status;
                        if (connCount >= 10) status = "⚠ Cảnh báo";
                        else if (connCount >= 3) status = "⚡ Nhiều";
                        else status = "✔ Bình thường";

                        ipTableModel.addRow(new Object[]{idx++, ip, connCount, playerNames, status});
                    }
                });

            } catch (Exception ignored) {}
        }, 1, 3, TimeUnit.SECONDS);

        // Record online history every 5 minutes (for graph)
        scheduler.scheduleAtFixedRate(() -> {
            try {
                int online = Client.gI() != null ? Client.gI().getPlayers().size() : 0;
                synchronized (onlineHistory) {
                    onlineHistory.add(online);
                    if (onlineHistory.size() > 60) {
                        onlineHistory.remove(0);
                    }
                }
                todayLogins += online; // approximate metric
                SwingUtilities.invokeLater(() -> {
                    graphPanel.repaint();
                    lblTodayLogins.setText(buildStatHtml("Tổng Lượt (ước)",
                            String.valueOf(todayLogins), new Color(40, 167, 69)));
                });
            } catch (Exception ignored) {}
        }, 0, 30, TimeUnit.SECONDS); // Use 30s for demo, change to 300 (5min) in production
    }
}
