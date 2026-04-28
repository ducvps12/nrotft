package nro.server.ui;

import event.EventManager;
import nro.services.Service;
import utils.Logger;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.border.CompoundBorder;

public class EventPanel extends JPanel {

    private JTextArea txtHistory;
    private final List<JCheckBox> checkBoxes = new ArrayList<>();

    // Danh sách tên sự kiện
    private static final String[] EVENT_NAMES = {
            "Halloween", "8/3 Quốc Tế PN", "Giáng Sinh", "Tết Nguyên Đán",
            "Trung Thu", "Giỗ Tổ", "Top Up (Mặc định)", "Pokemon",
            "20/11", "Phở Anh Hai", "Sự Kiện Hè"
    };

    // Mô tả chi tiết cho từng sự kiện
    private static final String[] EVENT_DESCS = {
            "Săn kẹo ma quỷ, đổi trang phục Halloween đặc biệt",
            "Tặng quà, nhiệm vụ đặc biệt ngày Quốc tế Phụ nữ",
            "Tuyết rơi, quà Giáng Sinh, triệu hồi Santa Boss",
            "Lì xì may mắn, pháo hoa, Boss Tết đặc biệt",
            "Bánh trung thu, đèn lồng, Boss Thỏ Ngọc",
            "Nhiệm vụ lịch sử Hùng Vương, quà đặc biệt",
            "Nạp thẻ tích điểm, bảng xếp hạng nạp thẻ",
            "Bắt Pokemon, tiến hóa, đấu trường Pokemon",
            "Ngày Nhà giáo VN, nhiệm vụ tri ân thầy cô",
            "Nấu phở đặc biệt, thu thập nguyên liệu hiếm",
            "Thu thập vỏ sò, kem tươi, đổi quà mùa hè đặc biệt"
    };

    // Emoji cho mỗi sự kiện
    private static final String[] EVENT_ICONS = {
            "🎃", "💐", "🎄", "🧧", "🥮", "🏛", "💰", "⚡", "📚", "🍜", "🏖️"
    };

    // Màu cho mỗi sự kiện
    private static final Color[] EVENT_COLORS = {
            new Color(255, 140, 0),    // Halloween - cam
            new Color(255, 105, 180),  // 8/3 - hồng
            new Color(220, 53, 69),    // Giáng Sinh - đỏ
            new Color(255, 215, 0),    // Tết - vàng
            new Color(255, 193, 7),    // Trung Thu - vàng cam
            new Color(139, 90, 43),    // Giỗ Tổ - nâu
            new Color(0, 120, 215),    // Top Up - xanh
            new Color(255, 230, 0),    // Pokemon - vàng
            new Color(142, 68, 173),   // 20/11 - tím
            new Color(210, 105, 30),   // Phở - nâu cam
            new Color(0, 191, 255)     // Mùa Hè - xanh dương nhạt (DeepSkyBlue)
    };

    public EventPanel() {
        setLayout(new BorderLayout(15, 15));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        setBackground(Color.WHITE);
        initUI();
    }

    private void initUI() {
        // --- 1. Event Cards Panel ---
        JPanel cardsContainer = new JPanel(new GridLayout(0, 2, 12, 12));
        cardsContainer.setOpaque(false);

        TitledBorder titledBorder = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                " Chọn sự kiện chạy song song ",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 14), new Color(0, 102, 204));
        cardsContainer.setBorder(new CompoundBorder(titledBorder, new EmptyBorder(15, 15, 15, 15)));

        List<Integer> savedIds = loadEventConfig();

        for (int i = 0; i < EVENT_NAMES.length; i++) {
            JPanel card = createEventCard(i, savedIds.contains(i + 1));
            cardsContainer.add(card);
        }

        JScrollPane scrollCards = new JScrollPane(cardsContainer);
        scrollCards.setBorder(null);
        scrollCards.getVerticalScrollBar().setUnitIncrement(16);

        // --- 2. Button & Hint Panel ---
        JPanel actionPanel = new JPanel();
        actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.Y_AXIS));
        actionPanel.setOpaque(false);

        JLabel lblHint = new JLabel("(*Lưu ý: Hệ thống sẽ tự động Restart sau 10 giây sau khi lưu thành công)");
        lblHint.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblHint.setForeground(new Color(220, 53, 69));
        lblHint.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblHint.setBorder(new EmptyBorder(0, 0, 10, 0));

        JButton btnApply = ServerGuiUtils.createStyledButton("LƯU CẤU HÌNH & KÍCH HOẠT NGAY", new Color(0, 123, 255),
                Color.WHITE);
        btnApply.setPreferredSize(new Dimension(300, 50));
        btnApply.setMaximumSize(new Dimension(500, 50));
        btnApply.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnApply.setAlignmentX(Component.CENTER_ALIGNMENT);

        btnApply.addActionListener(e -> {
            List<Integer> selected = checkBoxes.stream()
                    .filter(JCheckBox::isSelected)
                    .map(chk -> (int) chk.getClientProperty("id"))
                    .collect(Collectors.toList());

            if (selected.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn ít nhất 1 sự kiện!", "Cảnh báo",
                        JOptionPane.WARNING_MESSAGE);
            } else {
                int confirm = JOptionPane.showConfirmDialog(this,
                        "Xác nhận lưu thay đổi?\nServer sẽ được khởi động lại sau 10 giây.",
                        "Xác nhận hệ thống", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

                if (confirm == JOptionPane.YES_OPTION) {
                    doiSuKienVaRestart(selected);
                }
            }
        });

        actionPanel.add(lblHint);
        actionPanel.add(btnApply);

        // --- 3. Log Area ---
        txtHistory = new JTextArea();
        txtHistory.setEditable(false);
        txtHistory.setFont(new Font("Consolas", Font.PLAIN, 12));
        txtHistory.setBackground(new Color(245, 245, 245));
        txtHistory.setMargin(new Insets(10, 10, 10, 10));

        JScrollPane scrollLog = new JScrollPane(txtHistory);
        TitledBorder logBorder = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                " Lịch sử hoạt động ");
        scrollLog.setBorder(logBorder);
        scrollLog.setPreferredSize(new Dimension(0, 150));

        // Bottom
        JPanel bottomContainer = new JPanel(new BorderLayout(0, 15));
        bottomContainer.setOpaque(false);
        bottomContainer.add(actionPanel, BorderLayout.NORTH);
        bottomContainer.add(scrollLog, BorderLayout.CENTER);

        add(scrollCards, BorderLayout.CENTER);
        add(bottomContainer, BorderLayout.SOUTH);

        log("Đã tải cấu hình sự kiện hiện tại: " + savedIds.toString());
    }

    private JPanel createEventCard(int index, boolean selected) {
        JPanel card = new JPanel(new BorderLayout(8, 0));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(selected ? EVENT_COLORS[index] : new Color(220, 220, 220), selected ? 2 : 1),
                new EmptyBorder(10, 12, 10, 12)));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Checkbox
        JCheckBox chk = new JCheckBox();
        chk.setOpaque(false);
        chk.setSelected(selected);
        chk.putClientProperty("id", index + 1);
        checkBoxes.add(chk);

        // Left icon
        JLabel lblIcon = new JLabel(EVENT_ICONS[index]);
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
        lblIcon.setPreferredSize(new Dimension(36, 36));
        lblIcon.setHorizontalAlignment(SwingConstants.CENTER);

        // Center info
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);

        JLabel lblName = new JLabel((index + 1) + ". " + EVENT_NAMES[index]);
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblName.setForeground(EVENT_COLORS[index]);

        JLabel lblDesc = new JLabel(EVENT_DESCS[index]);
        lblDesc.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblDesc.setForeground(new Color(120, 120, 120));

        infoPanel.add(lblName);
        infoPanel.add(Box.createVerticalStrut(2));
        infoPanel.add(lblDesc);

        // Left side: checkbox + icon
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        leftPanel.setOpaque(false);
        leftPanel.add(chk);
        leftPanel.add(lblIcon);

        // Right side: Detail button
        JButton btnDetail = new JButton("ℹ️ Chi tiết");
        btnDetail.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnDetail.setFocusPainted(false);
        btnDetail.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnDetail.setBackground(new Color(240, 248, 255));
        btnDetail.addActionListener(e -> showEventDetailDialog(index));

        card.add(leftPanel, BorderLayout.WEST);
        card.add(infoPanel, BorderLayout.CENTER);
        card.add(btnDetail, BorderLayout.EAST);

        // Toggle border color on check
        chk.addActionListener(e -> {
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(chk.isSelected() ? EVENT_COLORS[index] : new Color(220, 220, 220), chk.isSelected() ? 2 : 1),
                    new EmptyBorder(10, 12, 10, 12)));
        });

        // Click anywhere on card toggles checkbox
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                chk.setSelected(!chk.isSelected());
                chk.getActionListeners()[0].actionPerformed(null);
            }
        });

        return card;
    }

    public void doiSuKienVaRestart(List<Integer> eventIds) {
        log("Đang tiến hành lưu cấu hình vào active_event.txt...");
        saveEventConfig(eventIds);
        log("Cấu hình đã lưu. Chuẩn bị Restart...");
        triggerCustomRestart();
    }

    private void triggerCustomRestart() {
        int delaySeconds = 10;
        System.out.println(">>> [EventPanel] Requesting restart in " + delaySeconds + "s...");

        try {
            String currentDir = System.getProperty("user.dir");
            String osName = System.getProperty("os.name").toLowerCase();

            ProcessBuilder pb;
            if (osName.contains("win")) {
                pb = new ProcessBuilder("cmd", "/c", "start", "cmd", "/c",
                        "title Server Restarting... && echo Server se khoi dong lai sau 10s... && timeout /t "
                                + delaySeconds + " /nobreak && run.bat");
            } else {
                pb = new ProcessBuilder("bash", "-c", "sleep " + delaySeconds + "; ./run.sh &");
            }

            pb.directory(new File(currentDir));
            pb.start();

            ServerManagerUI.REQUEST_AUTO_RESTART = false;

            System.out.println(">>> Bye bye! Exiting Java...");
            System.exit(0);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi thực thi Restart: " + e.getMessage());
        }
    }

    private void saveEventConfig(List<Integer> eventIds) {
        try (PrintWriter pw = new PrintWriter(new FileWriter("active_event.txt"))) {
            String line = eventIds.stream().map(String::valueOf).collect(Collectors.joining("-"));
            pw.println(line);
            pw.flush();
        } catch (IOException e) {
            Logger.error("Lỗi lưu file active_event.txt: " + e.getMessage());
        }
    }

    public List<Integer> loadEventConfig() {
        List<Integer> ids = new ArrayList<>();
        File f = new File("active_event.txt");
        if (!f.exists()) {
            ids.add(7);
            return ids;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line = br.readLine();
            if (line != null && !line.isEmpty()) {
                for (String part : line.split("-")) {
                    try {
                        ids.add(Integer.parseInt(part.trim()));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        } catch (Exception e) {
        }
        if (ids.isEmpty())
            ids.add(7);
        return ids;
    }

    private void log(String msg) {
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        txtHistory.append("[" + time + "] " + msg + "\n");
        txtHistory.setCaretPosition(txtHistory.getDocument().getLength());
    }

    // ================================================================
    // CHI TIẾT SỰ KIỆN (DIALOG)
    // ================================================================
    private void showEventDetailDialog(int index) {
        JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "Chi tiết: " + EVENT_NAMES[index], true);
        d.setSize(700, 520);
        d.setLocationRelativeTo(this);
        d.setLayout(new BorderLayout());

        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(new EmptyBorder(15, 15, 15, 15));
        content.setBackground(Color.WHITE);

        // Header
        JLabel lblHeader = new JLabel(EVENT_ICONS[index] + "  " + EVENT_NAMES[index]);
        lblHeader.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblHeader.setForeground(EVENT_COLORS[index]);
        lblHeader.setBorder(new EmptyBorder(0, 0, 10, 0));
        content.add(lblHeader, BorderLayout.NORTH);

        // Body - tùy theo event
        JPanel body;
        switch (index) {
            case 5 -> body = buildHungVuongDetail();
            case 0 -> body = buildGenericDetail("Halloween", new String[][]{
                    {"Ma trơi", "500,000 HP", "Galick Lv7", "Map ngẫu nhiên", "10 phút"},
                    {"Dơi Nhí", "500,000 HP", "Galick Lv7", "Map ngẫu nhiên", "10 phút"},
                    {"Bí ma", "500,000 HP", "Galick Lv7", "Map ngẫu nhiên", "10 phút"},
                    {"Xương khô", "500,000 HP", "Galick Lv3+7", "Map ngẫu nhiên", "10 phút"},
                    {"Đracula", "1B HP", "Tái Tạo NL", "Map 112", "1 giây"},
                }, "Săn kẹo ma quỷ, đổi trang phục Halloween.\nBoss xuất hiện trên nhiều map ngẫu nhiên.\nĐánh boss để nhận vật phẩm sự kiện.");
            case 2 -> body = buildGenericDetail("Giáng Sinh", new String[][]{
                    {"Ông già Noel", "500 HP", "Tái Tạo NL Lv7", "Map ngẫu nhiên", "1 phút"},
                }, "Tuyết rơi, quà Giáng Sinh.\nĐánh boss Ông già Noel để nhận quà.\nSự kiện trang trí bản đồ chủ đề Noel.");
            case 3 -> body = buildGenericDetail("Tết Nguyên Đán", new String[][]{
                    {"Lân con", "5M HP", "Tái Tạo NL Lv7", "Map ngẫu nhiên", "1 phút"},
                }, "Lì xì may mắn, pháo hoa.\nĐánh boss Lân con nhận vật phẩm Tết.\nNhiệm vụ đặc biệt ngày Tết.");
            default -> body = buildSimpleDetail(index);
        }

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        content.add(scroll, BorderLayout.CENTER);

        // Close button
        JButton btnClose = new JButton("Đóng");
        btnClose.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnClose.addActionListener(e -> d.dispose());
        JPanel pBtn = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pBtn.setOpaque(false);
        pBtn.add(btnClose);
        content.add(pBtn, BorderLayout.SOUTH);

        d.add(content);
        d.setVisible(true);
    }

    /** Chi tiết Giỗ Tổ Hùng Vương - đầy đủ nhất */
    private JPanel buildHungVuongDetail() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.WHITE);

        // 1. Mô tả
        JTextArea desc = new JTextArea(
            "🏛 Sự kiện Giỗ Tổ Hùng Vương\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
            "Kỷ niệm ngày Quốc Tổ, triệu hồi Boss Thủy Tinh & Sơn Tinh.\n" +
            "NPC Hùng Vương (ID: 52) xuất hiện - mang lễ vật đổi quà đặc biệt.\n" +
            "Boss Rồng Thần 1-7 sao cũng xuất hiện trong sự kiện."
        );
        desc.setEditable(false);
        desc.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        desc.setBackground(new Color(255, 250, 240));
        desc.setBorder(new CompoundBorder(
            new TitledBorder(BorderFactory.createLineBorder(new Color(139, 90, 43)),
                " Mô Tả Sự Kiện ", TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12), new Color(139, 90, 43)),
            new EmptyBorder(8, 8, 8, 8)));
        desc.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        p.add(desc);
        p.add(Box.createVerticalStrut(10));

        // 2. Bảng Boss
        String[] bossCols = {"Tên Boss", "HP", "Sức Đánh", "Hồi Sinh", "Ghi Chú"};
        Object[][] bossData = {
            {"🌊 Thủy Tinh", "50,000,000", "Tự điều chỉnh", "15 phút", "Boss chính, kéo Sơn Tinh"},
            {"⛰ Sơn Tinh", "50,000,000", "Tự điều chỉnh", "Đi cùng", "Xuất hiện cùng Thủy Tinh"},
            {"🐉 Rồng 1 sao", "Theo cấu hình", "-", "5 phút", "Rồng Thần event"},
            {"🐉 Rồng 2 sao", "Theo cấu hình", "-", "5 phút", "Rồng Thần event"},
            {"🐉 Rồng 3 sao", "Theo cấu hình", "-", "5 phút", "Rồng Thần event"},
            {"🐉 Rồng 4 sao", "Theo cấu hình", "-", "5 phút", "Rồng Thần event"},
            {"🐉 Rồng 5 sao", "Theo cấu hình", "-", "5 phút", "Rồng Thần event"},
            {"🐉 Rồng 6 sao", "Theo cấu hình", "-", "5 phút", "Rồng Thần event"},
            {"🐉 Rồng 7 sao", "Theo cấu hình", "-", "5 phút", "Rồng Thần event"},
        };
        JTable tblBoss = createStyledTable(bossCols, bossData);
        JScrollPane scrollBoss = new JScrollPane(tblBoss);
        scrollBoss.setBorder(new TitledBorder(BorderFactory.createLineBorder(new Color(0, 102, 204)),
            " Danh Sách Boss ", TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 12), new Color(0, 102, 204)));
        scrollBoss.setPreferredSize(new Dimension(0, 200));
        scrollBoss.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));
        p.add(scrollBoss);
        p.add(Box.createVerticalStrut(10));

        // 3. Phần thưởng
        String[] rewardCols = {"Boss", "Drop Item", "Options chính"};
        Object[][] rewardData = {
            {"Thủy Tinh", "Item #422 (Trang bị)", "HP+20~30%, KI+20~30%, SD+20~30%, Chí Mạng+10~15%"},
            {"Sơn Tinh", "Item #421 (Trang bị)", "HP+20~30%, KI+20~30%, SD+20~30%, CM+12~15%, NeDon+12~15%"},
        };
        JTable tblReward = createStyledTable(rewardCols, rewardData);
        JScrollPane scrollReward = new JScrollPane(tblReward);
        scrollReward.setBorder(new TitledBorder(BorderFactory.createLineBorder(new Color(40, 167, 69)),
            " Phần Thưởng Drop ", TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 12), new Color(40, 167, 69)));
        scrollReward.setPreferredSize(new Dimension(0, 80));
        scrollReward.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        p.add(scrollReward);

        return p;
    }

    /** Cho event có boss list dạng bảng */
    private JPanel buildGenericDetail(String title, String[][] bossRows, String description) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.WHITE);

        JTextArea desc = new JTextArea(description);
        desc.setEditable(false);
        desc.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        desc.setBackground(new Color(248, 250, 255));
        desc.setBorder(new EmptyBorder(10, 10, 10, 10));
        desc.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        p.add(desc);
        p.add(Box.createVerticalStrut(10));

        if (bossRows.length > 0) {
            String[] cols = {"Tên Boss", "HP", "Skill", "Map", "Hồi Sinh"};
            JTable tbl = createStyledTable(cols, bossRows);
            JScrollPane scroll = new JScrollPane(tbl);
            scroll.setBorder(new TitledBorder("Boss Sự Kiện"));
            scroll.setPreferredSize(new Dimension(0, Math.min(bossRows.length * 30 + 50, 200)));
            scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));
            p.add(scroll);
        }
        return p;
    }

    /** Cho event chưa có data chi tiết */
    private JPanel buildSimpleDetail(int index) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        JTextArea ta = new JTextArea(EVENT_DESCS[index] + "\n\n(Chưa có dữ liệu chi tiết cho sự kiện này)");
        ta.setEditable(false);
        ta.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        ta.setBorder(new EmptyBorder(15, 15, 15, 15));
        p.add(ta);
        return p;
    }

    private JTable createStyledTable(String[] cols, Object[][] data) {
        JTable t = new JTable(data, cols) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        t.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        t.setRowHeight(28);
        t.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        t.getTableHeader().setBackground(new Color(0, 102, 204));
        t.getTableHeader().setForeground(Color.WHITE);
        t.setShowVerticalLines(false);
        return t;
    }
}