package nro.server.ui;

import jdbc.DBConnecter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.sql.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Bảng Quản Lý Tài Chính Vũ Trụ MTDGame
 * Giám sát kinh tế vĩ mô, cảnh báo lạm phát, phân tích phân phối tài sản
 */
public class EconomyMonitorPanel extends JPanel {

    // === STAT CARDS ===
    private JLabel lblTotalGoldCirculation, lblTotalGoldBar, lblTotalRuby, lblTotalGem;
    private JLabel lblAvgGoldPerPlayer, lblMedianGold, lblGiniIndex, lblInflationRisk;
    private JLabel lblTopHoarderPct, lblGoldBarExchangeRate, lblConsignVolume, lblLastScan;

    // === CHARTS ===
    private JPanel chartWealthDistribution;  // Biểu đồ phân phối tài sản
    private JPanel chartGoldByRace;          // Vàng theo chủng tộc
    private JPanel chartInflationGauge;      // Đồng hồ đo lạm phát

    // === TABLES ===
    private DefaultTableModel topRichModel;
    private DefaultTableModel wealthBracketModel;

    // === DATA ===
    private long totalGold = 0, totalGoldBar = 0, totalRuby = 0, totalGem = 0;
    private long avgGold = 0, medianGold = 0;
    private double giniCoeff = 0.0;
    private int totalPlayers = 0;
    private long[] raceGold = {0, 0, 0};    // Trái Đất, Namec, Xayda
    private int[] racePlayers = {0, 0, 0};
    private String[][] topRich = new String[0][];
    private int[] wealthBrackets = new int[6]; // 0-1M, 1M-100M, 100M-1B, 1B-100B, 100B-1T, >1T

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public EconomyMonitorPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel container = new JPanel(new GridBagLayout());
        container.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.weightx = 1.0;

        // Header
        gbc.gridy = 0;
        container.add(createHeader(), gbc);

        // Row 1: Tổng quan tiền tệ
        gbc.gridy++;
        container.add(createCurrencyOverview(), gbc);

        // Row 2: Chỉ số sức khỏe kinh tế
        gbc.gridy++;
        container.add(createHealthIndicators(), gbc);

        // Row 3: Charts (Phân phối tài sản + Vàng theo chủng tộc)
        JPanel chartsRow = new JPanel(new GridLayout(1, 3, 10, 0));
        chartsRow.setOpaque(false);
        chartsRow.add(createWealthDistributionChart());
        chartsRow.add(createGoldByRaceChart());
        chartsRow.add(createInflationGauge());
        gbc.gridy++;
        container.add(chartsRow, gbc);

        // Row 4: Bảng phân khúc tài sản
        gbc.gridy++;
        container.add(createWealthBracketTable(), gbc);

        // Row 5: Top 10 đại gia
        gbc.gridy++;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        container.add(createTopRichTable(), gbc);

        JScrollPane scroll = new JScrollPane(container);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        // Auto refresh
        scheduler.scheduleAtFixedRate(this::refreshData, 1, 60, TimeUnit.SECONDS);
    }

    // ====== HEADER ======
    private JPanel createHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(0, 0, 10, 0));

        JLabel title = ServerGuiUtils.createStyledLabel("💰 Quản Lý Tài Chính Vũ Trụ MTDGame", 22, true);
        title.setForeground(new Color(40, 40, 40));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        lblLastScan = new JLabel("Chưa quét");
        lblLastScan.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        lblLastScan.setForeground(Color.GRAY);

        JButton btnRefresh = ServerGuiUtils.createStyledButton("📡 Quét Kinh Tế", new Color(0, 120, 215), Color.WHITE);
        btnRefresh.addActionListener(e -> refreshData());

        right.add(lblLastScan);
        right.add(btnRefresh);

        p.add(title, BorderLayout.WEST);
        p.add(right, BorderLayout.EAST);
        return p;
    }

    // ====== ROW 1: TỔNG QUAN TIỀN TỆ ======
    private JPanel createCurrencyOverview() {
        JPanel p = new JPanel(new GridLayout(1, 4, 10, 0));
        p.setOpaque(false);
        p.setBorder(ServerGuiUtils.createSectionBorder("💎 Tổng Lượng Tiền Tệ Lưu Thông"));

        lblTotalGoldCirculation = createBigCard("Tổng Vàng Lưu Thông", "...", new Color(255, 152, 0), "🪙");
        lblTotalGoldBar = createBigCard("Tổng Thỏi Vàng (ATM)", "...", new Color(220, 53, 69), "💎");
        lblTotalRuby = createBigCard("Tổng Ruby", "...", new Color(142, 68, 173), "💠");
        lblTotalGem = createBigCard("Tổng Ngọc Xanh", "...", new Color(0, 120, 215), "🔷");

        p.add(lblTotalGoldCirculation);
        p.add(lblTotalGoldBar);
        p.add(lblTotalRuby);
        p.add(lblTotalGem);
        return p;
    }

    private JLabel createBigCard(String title, String value, Color color, String icon) {
        String hex = toHex(color);
        JLabel lbl = new JLabel("<html><div style='text-align:center;padding:10px;'>"
                + "<span style='font-size:22px;'>" + icon + "</span><br>"
                + "<span style='font-size:10px;color:#888;'>" + title + "</span><br>"
                + "<span style='font-size:16px;color:" + hex + ";font-weight:bold;'>" + value + "</span>"
                + "</div></html>", SwingConstants.CENTER);
        lbl.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color.brighter(), 1, true),
                new EmptyBorder(5, 5, 5, 5)));
        lbl.setOpaque(true);
        lbl.setBackground(Color.WHITE);
        return lbl;
    }

    // ====== ROW 2: CHỈ SỐ SỨC KHỎE KINH TẾ ======
    private JPanel createHealthIndicators() {
        JPanel p = new JPanel(new GridLayout(1, 4, 10, 0));
        p.setOpaque(false);
        p.setBorder(ServerGuiUtils.createSectionBorder("📊 Chỉ Số Sức Khỏe Kinh Tế Vũ Trụ"));

        lblAvgGoldPerPlayer = createHealthCard("Vàng TB / Người", "...", Color.GRAY);
        lblGiniIndex = createHealthCard("Hệ Số Gini (Bất Bình Đẳng)", "...", Color.GRAY);
        lblTopHoarderPct = createHealthCard("Top 10% Giữ Bao Nhiêu %", "...", Color.GRAY);
        lblInflationRisk = createHealthCard("Mức Rủi Ro Lạm Phát", "...", Color.GRAY);

        p.add(lblAvgGoldPerPlayer);
        p.add(lblGiniIndex);
        p.add(lblTopHoarderPct);
        p.add(lblInflationRisk);
        return p;
    }

    private JLabel createHealthCard(String title, String value, Color color) {
        String hex = toHex(color);
        JLabel lbl = new JLabel("<html><div style='text-align:center;padding:8px;'>"
                + "<span style='font-size:10px;color:#888;'>" + title + "</span><br>"
                + "<span style='font-size:15px;color:" + hex + ";font-weight:bold;'>" + value + "</span>"
                + "</div></html>", SwingConstants.CENTER);
        lbl.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(230, 230, 230), 1, true),
                new EmptyBorder(5, 5, 5, 5)));
        lbl.setOpaque(true);
        lbl.setBackground(Color.WHITE);
        return lbl;
    }

    // ====== CHARTS ======
    private JPanel createWealthDistributionChart() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(ServerGuiUtils.createSectionBorder("📊 Phân Bố Tài Sản"));

        chartWealthDistribution = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawWealthDistChart((Graphics2D) g);
            }
        };
        chartWealthDistribution.setBackground(new Color(250, 250, 252));
        chartWealthDistribution.setPreferredSize(new Dimension(0, 220));
        wrapper.add(chartWealthDistribution, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel createGoldByRaceChart() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(ServerGuiUtils.createSectionBorder("🥧 Vàng Theo Chủng Tộc"));

        chartGoldByRace = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawGoldByRaceChart((Graphics2D) g);
            }
        };
        chartGoldByRace.setBackground(new Color(250, 250, 252));
        chartGoldByRace.setPreferredSize(new Dimension(0, 220));
        wrapper.add(chartGoldByRace, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel createInflationGauge() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(ServerGuiUtils.createSectionBorder("🌡 Đồng Hồ Lạm Phát"));

        chartInflationGauge = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawInflationGauge((Graphics2D) g);
            }
        };
        chartInflationGauge.setBackground(new Color(250, 250, 252));
        chartInflationGauge.setPreferredSize(new Dimension(0, 220));
        wrapper.add(chartInflationGauge, BorderLayout.CENTER);
        return wrapper;
    }

    // ====== TOP RICH TABLE ======
    private JPanel createTopRichTable() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(ServerGuiUtils.createSectionBorder("🏆 Top 10 Đại Gia Vũ Trụ MTDGame"));

        String[] cols = {"#", "Tên Nhân Vật", "Chủng Tộc", "Vàng Túi", "Thỏi Vàng", "Ngọc Xanh", "Level"};
        topRichModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(topRichModel);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setRowHeight(28);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(255, 152, 0));
        table.getTableHeader().setForeground(Color.WHITE);
        table.setSelectionBackground(new Color(255, 243, 224));

        // Gold column bold
        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean f, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, val, sel, f, r, c);
                comp.setFont(new Font("Segoe UI", Font.BOLD, 12));
                comp.setForeground(new Color(255, 140, 0));
                ((JLabel) comp).setHorizontalAlignment(SwingConstants.RIGHT);
                return comp;
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(0, 250));
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    // ====== WEALTH BRACKET TABLE ======
    private JPanel createWealthBracketTable() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(ServerGuiUtils.createSectionBorder("📈 Phân Khúc Tài Sản Người Chơi"));

        String[] cols = {"Phân Khúc", "Số Người Chơi", "Phần Trăm", "Thanh Trạng Thái"};
        wealthBracketModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(wealthBracketModel);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setRowHeight(26);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(63, 81, 181));
        table.getTableHeader().setForeground(Color.WHITE);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(0, 180));
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    // ====== DRAW CHARTS ======
    private void drawWealthDistChart(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = chartWealthDistribution.getWidth() - 40;
        int h = chartWealthDistribution.getHeight() - 40;

        String[] labels = {"0-1M", "1M-100M", "100M-1B", "1B-100B", "100B-1T", ">1T"};
        Color[] colors = {
            new Color(76, 175, 80), new Color(0, 150, 136), new Color(33, 150, 243),
            new Color(156, 39, 176), new Color(255, 87, 34), new Color(220, 53, 69)
        };

        int max = 1;
        for (int v : wealthBrackets) if (v > max) max = v;
        max = ((max / 5) + 1) * 5;

        int barW = Math.min(50, (w - 20) / 6 - 8);
        int gap = (w - 6 * barW) / 7;

        for (int i = 0; i < 6; i++) {
            int barH = (max > 0) ? (int)((long) h * wealthBrackets[i] / max) : 0;
            int bx = 20 + gap + i * (barW + gap);
            int by = 15 + h - barH;

            GradientPaint gp = new GradientPaint(bx, by, colors[i], bx, by + barH, colors[i].darker());
            g2.setPaint(gp);
            g2.fillRoundRect(bx, by, barW, barH, 4, 4);

            g2.setColor(colors[i].darker());
            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
            String val = String.valueOf(wealthBrackets[i]);
            int tw = g2.getFontMetrics().stringWidth(val);
            g2.drawString(val, bx + (barW - tw) / 2, by - 3);

            g2.setColor(new Color(80, 80, 80));
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            tw = g2.getFontMetrics().stringWidth(labels[i]);
            g2.drawString(labels[i], bx + (barW - tw) / 2, 15 + h + 14);
        }
    }

    private void drawGoldByRaceChart(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = chartGoldByRace.getWidth();
        int h = chartGoldByRace.getHeight();

        long total = raceGold[0] + raceGold[1] + raceGold[2];
        if (total == 0) {
            g2.setColor(new Color(150, 150, 150));
            g2.setFont(new Font("Segoe UI", Font.ITALIC, 13));
            g2.drawString("Đang tải...", w / 2 - 30, h / 2);
            return;
        }

        String[] names = {"Trái Đất", "Namec", "Xayda"};
        Color[] colors = { new Color(0, 120, 215), new Color(40, 167, 69), new Color(220, 53, 69) };

        int pieSize = Math.min(w - 120, h - 40) - 10;
        int px = (w - 120) / 2 - pieSize / 2;
        int py = (h - pieSize) / 2;

        int startAngle = 0;
        for (int i = 0; i < 3; i++) {
            int arcAngle = (int) Math.round(360.0 * raceGold[i] / total);
            if (i == 2) arcAngle = 360 - startAngle;

            g2.setColor(colors[i]);
            g2.fillArc(px, py, pieSize, pieSize, startAngle, arcAngle);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2f));
            g2.drawArc(px, py, pieSize, pieSize, startAngle, arcAngle);
            startAngle += arcAngle;
        }

        // Donut hole
        int holeSize = pieSize / 3;
        g2.setColor(new Color(250, 250, 252));
        g2.fillOval(px + pieSize / 2 - holeSize / 2, py + pieSize / 2 - holeSize / 2, holeSize, holeSize);

        // Legend
        int legendX = w - 115;
        int legendY = h / 2 - 50;
        for (int i = 0; i < 3; i++) {
            g2.setColor(colors[i]);
            g2.fillRoundRect(legendX, legendY + i * 28, 14, 14, 3, 3);
            g2.setColor(new Color(60, 60, 60));
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            int pct = (int) Math.round(100.0 * raceGold[i] / total);
            g2.drawString(names[i] + " " + pct + "%", legendX + 20, legendY + i * 28 + 12);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
            g2.setColor(new Color(120, 120, 120));
            g2.drawString(formatBigNum(raceGold[i]), legendX + 20, legendY + i * 28 + 24);
        }
    }

    private void drawInflationGauge(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = chartInflationGauge.getWidth();
        int h = chartInflationGauge.getHeight();

        int cx = w / 2;
        int cy = h / 2 + 20;
        int radius = Math.min(w, h) / 2 - 25;

        // Background arc (180 degrees)
        g2.setStroke(new BasicStroke(18f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(230, 230, 230));
        g2.drawArc(cx - radius, cy - radius, radius * 2, radius * 2, 0, 180);

        // Colored segments
        Color[] segColors = {
            new Color(76, 175, 80),  // An toàn (0-20%)
            new Color(255, 193, 7),  // Cảnh giác (20-50%)
            new Color(255, 87, 34),  // Nguy hiểm (50-80%)
            new Color(220, 53, 69)   // Lạm phát cực điểm (80-100%)
        };
        int[] segDeg = {36, 54, 54, 36}; // out of 180
        int startDeg = 0;
        for (int i = 0; i < 4; i++) {
            g2.setColor(segColors[i]);
            g2.drawArc(cx - radius, cy - radius, radius * 2, radius * 2, startDeg, segDeg[i]);
            startDeg += segDeg[i];
        }

        // Needle 
        double inflationPct = Math.min(100, Math.max(0, giniCoeff * 100));
        double angle = Math.toRadians(180 - inflationPct * 1.8);
        int needleLen = radius - 15;
        int nx = cx + (int)(needleLen * Math.cos(angle));
        int ny = cy - (int)(needleLen * Math.sin(angle));

        g2.setColor(new Color(40, 40, 40));
        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(cx, cy, nx, ny);
        g2.fillOval(cx - 6, cy - 6, 12, 12);

        // Labels
        g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
        g2.setColor(new Color(60, 60, 60));
        String riskLabel;
        Color riskColor;
        if (giniCoeff < 0.3) { riskLabel = "AN TOÀN"; riskColor = new Color(76, 175, 80); }
        else if (giniCoeff < 0.5) { riskLabel = "CẢNH GIÁC"; riskColor = new Color(255, 193, 7); }
        else if (giniCoeff < 0.7) { riskLabel = "NGUY HIỂM"; riskColor = new Color(255, 87, 34); }
        else { riskLabel = "LẠM PHÁT!"; riskColor = new Color(220, 53, 69); }

        g2.setColor(riskColor);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(riskLabel, cx - fm.stringWidth(riskLabel) / 2, cy + 30);

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2.setColor(Color.GRAY);
        String giniStr = "Gini: " + String.format("%.2f", giniCoeff);
        g2.drawString(giniStr, cx - g2.getFontMetrics().stringWidth(giniStr) / 2, cy + 45);

        // Scale labels
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        g2.setColor(new Color(100, 100, 100));
        g2.drawString("0", cx - radius - 5, cy + 15);
        g2.drawString("100", cx + radius - 10, cy + 15);
    }

    // ====== DATA REFRESH ======
    private void refreshData() {
        try (Connection con = DBConnecter.getConnectionServer()) {
            // 1. Tổng vàng lưu thông (từ data_inventory JSON, cột đầu tiên là gold)
            totalGold = 0;
            totalGem = 0;
            totalRuby = 0;
            List<Long> allGolds = new ArrayList<>();

            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT data_inventory, gender FROM player");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String inv = rs.getString("data_inventory");
                    int gender = rs.getInt("gender");
                    if (inv != null && !inv.isEmpty()) {
                        try {
                            org.json.simple.JSONArray arr = (org.json.simple.JSONArray) org.json.simple.JSONValue.parse(inv);
                            if (arr != null && arr.size() > 0) {
                                long gold = Long.parseLong(String.valueOf(arr.get(0)));
                                totalGold += gold;
                                allGolds.add(gold);
                                if (gender >= 0 && gender <= 2) raceGold[gender] += gold;
                                if (arr.size() > 1) totalGem += Long.parseLong(String.valueOf(arr.get(1)));
                                if (arr.size() > 2) totalRuby += Long.parseLong(String.valueOf(arr.get(2)));
                            }
                        } catch (Exception ignored) {}
                    }
                    if (gender >= 0 && gender <= 2) racePlayers[gender]++;
                }
            }

            totalPlayers = allGolds.size();

            // 2. Tổng thỏi vàng
            try (PreparedStatement ps = con.prepareStatement("SELECT COALESCE(SUM(thoi_vang), 0) FROM account");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) totalGoldBar = rs.getLong(1);
            }

            // 3. Trung bình & Trung vị
            avgGold = totalPlayers > 0 ? totalGold / totalPlayers : 0;
            Collections.sort(allGolds);
            medianGold = totalPlayers > 0 ? allGolds.get(totalPlayers / 2) : 0;

            // 4. Hệ số Gini (đo bất bình đẳng)
            giniCoeff = calculateGini(allGolds);

            // 5. Top 10% chiếm bao nhiêu %
            long topTenPctGold = 0;
            int topTenCount = Math.max(1, totalPlayers / 10);
            for (int i = allGolds.size() - 1; i >= allGolds.size() - topTenCount && i >= 0; i--) {
                topTenPctGold += allGolds.get(i);
            }
            double topHoarderPct = totalGold > 0 ? (100.0 * topTenPctGold / totalGold) : 0;

            // 6. Wealth brackets
            wealthBrackets = new int[6];
            for (long g : allGolds) {
                if (g < 1_000_000L) wealthBrackets[0]++;
                else if (g < 100_000_000L) wealthBrackets[1]++;
                else if (g < 1_000_000_000L) wealthBrackets[2]++;
                else if (g < 100_000_000_000L) wealthBrackets[3]++;
                else if (g < 1_000_000_000_000L) wealthBrackets[4]++;
                else wealthBrackets[5]++;
            }

            // 7. Top 10 đại gia
            List<String[]> topList = new ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT p.name, p.gender, p.data_inventory, a.thoi_vang, p.power " +
                    "FROM player p LEFT JOIN account a ON p.account_id = a.id " +
                    "ORDER BY p.power DESC LIMIT 10");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("name");
                    int gender = rs.getInt("gender");
                    String inv = rs.getString("data_inventory");
                    int goldBar = rs.getInt("thoi_vang");
                    long power = rs.getLong("power");

                    long pGold = 0;
                    int pGem = 0;
                    if (inv != null) {
                        try {
                            org.json.simple.JSONArray arr = (org.json.simple.JSONArray) org.json.simple.JSONValue.parse(inv);
                            if (arr != null && arr.size() > 0) pGold = Long.parseLong(String.valueOf(arr.get(0)));
                            if (arr != null && arr.size() > 1) pGem = Integer.parseInt(String.valueOf(arr.get(1)));
                        } catch (Exception ignored) {}
                    }

                    String[] raceName = {"Trái Đất", "Namec", "Xayda"};
                    topList.add(new String[]{
                        name != null ? name : "?",
                        gender >= 0 && gender <= 2 ? raceName[gender] : "?",
                        formatBigNum(pGold),
                        String.valueOf(goldBar),
                        String.valueOf(pGem),
                        String.valueOf(power)
                    });
                }
            }
            topRich = topList.toArray(new String[0][]);

            // Update UI
            final double fTopHoarderPct = topHoarderPct;
            SwingUtilities.invokeLater(() -> {
                updateBigCard(lblTotalGoldCirculation, "Tổng Vàng Lưu Thông", formatBigNum(totalGold), new Color(255, 152, 0), "🪙");
                updateBigCard(lblTotalGoldBar, "Tổng Thỏi Vàng (ATM)", formatBigNum(totalGoldBar), new Color(220, 53, 69), "💎");
                updateBigCard(lblTotalRuby, "Tổng Ruby", formatBigNum(totalRuby), new Color(142, 68, 173), "💠");
                updateBigCard(lblTotalGem, "Tổng Ngọc Xanh", formatBigNum(totalGem), new Color(0, 120, 215), "🔷");

                updateHealthCard(lblAvgGoldPerPlayer, "Vàng TB / Người", formatBigNum(avgGold),
                    avgGold > 10_000_000_000L ? new Color(220, 53, 69) : new Color(40, 167, 69));

                String giniStr = String.format("%.3f", giniCoeff);
                Color giniColor = giniCoeff < 0.3 ? new Color(40, 167, 69) : giniCoeff < 0.5 ? new Color(255, 152, 0) : new Color(220, 53, 69);
                updateHealthCard(lblGiniIndex, "Hệ Số Gini (Bất Bình Đẳng)", giniStr, giniColor);

                updateHealthCard(lblTopHoarderPct, "Top 10% Giữ Bao Nhiêu %", String.format("%.1f%%", fTopHoarderPct),
                    fTopHoarderPct > 80 ? new Color(220, 53, 69) : fTopHoarderPct > 60 ? new Color(255, 152, 0) : new Color(40, 167, 69));

                String riskLabel;
                Color riskColor;
                if (giniCoeff < 0.3) { riskLabel = "🟢 AN TOÀN"; riskColor = new Color(40, 167, 69); }
                else if (giniCoeff < 0.5) { riskLabel = "🟡 CẢNH GIÁC"; riskColor = new Color(255, 193, 7); }
                else if (giniCoeff < 0.7) { riskLabel = "🟠 NGUY HIỂM"; riskColor = new Color(255, 87, 34); }
                else { riskLabel = "🔴 LẠM PHÁT!"; riskColor = new Color(220, 53, 69); }
                updateHealthCard(lblInflationRisk, "Mức Rủi Ro Lạm Phát", riskLabel, riskColor);

                // Update tables
                topRichModel.setRowCount(0);
                for (int i = 0; i < topRich.length; i++) {
                    topRichModel.addRow(new Object[]{i + 1, topRich[i][0], topRich[i][1], topRich[i][2], topRich[i][3], topRich[i][4], topRich[i][5]});
                }

                String[] bracketLabels = {"Nghèo (0 - 1M)", "Trung Bình (1M - 100M)", "Khá Giả (100M - 1B)", "Giàu (1B - 100B)", "Đại Gia (100B - 1T)", "Tỷ Phú (>1T)"};
                wealthBracketModel.setRowCount(0);
                for (int i = 0; i < 6; i++) {
                    double pct = totalPlayers > 0 ? (100.0 * wealthBrackets[i] / totalPlayers) : 0;
                    String bar = "█".repeat(Math.max(0, (int)(pct / 2)));
                    wealthBracketModel.addRow(new Object[]{bracketLabels[i], wealthBrackets[i], String.format("%.1f%%", pct), bar});
                }

                // Repaint charts
                if (chartWealthDistribution != null) chartWealthDistribution.repaint();
                if (chartGoldByRace != null) chartGoldByRace.repaint();
                if (chartInflationGauge != null) chartInflationGauge.repaint();

                lblLastScan.setText("Quét lúc: " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            });
        } catch (Exception e) {
            System.err.println("EconomyMonitor error: " + e.getMessage());
        }
    }

    // ====== UTILITY ======
    private double calculateGini(List<Long> sortedValues) {
        int n = sortedValues.size();
        if (n == 0) return 0;
        double sumOfDiffs = 0;
        double totalSum = 0;
        for (int i = 0; i < n; i++) {
            totalSum += sortedValues.get(i);
            sumOfDiffs += (2.0 * (i + 1) - n - 1) * sortedValues.get(i);
        }
        if (totalSum == 0) return 0;
        return sumOfDiffs / (n * totalSum);
    }

    private void updateBigCard(JLabel lbl, String title, String value, Color color, String icon) {
        String hex = toHex(color);
        lbl.setText("<html><div style='text-align:center;padding:10px;'>"
                + "<span style='font-size:22px;'>" + icon + "</span><br>"
                + "<span style='font-size:10px;color:#888;'>" + title + "</span><br>"
                + "<span style='font-size:16px;color:" + hex + ";font-weight:bold;'>" + value + "</span>"
                + "</div></html>");
    }

    private void updateHealthCard(JLabel lbl, String title, String value, Color color) {
        String hex = toHex(color);
        lbl.setText("<html><div style='text-align:center;padding:8px;'>"
                + "<span style='font-size:10px;color:#888;'>" + title + "</span><br>"
                + "<span style='font-size:15px;color:" + hex + ";font-weight:bold;'>" + value + "</span>"
                + "</div></html>");
    }

    private String formatBigNum(long num) {
        if (num >= 1_000_000_000_000L) return String.format("%.1fT", num / 1_000_000_000_000.0);
        if (num >= 1_000_000_000L) return String.format("%.1fB", num / 1_000_000_000.0);
        if (num >= 1_000_000L) return String.format("%.1fM", num / 1_000_000.0);
        if (num >= 1_000L) return String.format("%.1fK", num / 1_000.0);
        return String.valueOf(num);
    }

    private String toHex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }
}
