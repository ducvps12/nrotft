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
            "20/11", "Phở Anh Hai", "Địa Ngục Đảo Lộn",
            "Godzilla vs Kong", "Juventus Tournament",
            "Thần Thú Cổ Đại", "Kỷ Băng Hà", "Sự Kiện Hè"
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
            "Farm Hồn Quỷ tại 3 tầng Địa Ngục, Boss Janemba mỗi 2h",
            "World Boss Godzilla (12h) & Kong (20h), hạ cả 2 spawn MechaGodzilla",
            "PVP Tournament hàng tuần, 16 người, Top 1 = 50 Thỏi Vàng",
            "Thu thập 3 Linh Phù từ Thần Thú → triệu hồi Boss ẩn Thần Long",
            "Map tuyết cấp 2 mở, farm BTC3 rate 10%, Boss Ice Shenron mỗi 4h",
            "Boss Mặt Trời, Nước Mía, Đổi Quà Biển, KM x2 Nạp ATM"
    };

    // Emoji cho mỗi sự kiện
    private static final String[] EVENT_ICONS = {
            "🎃", "💐", "🎄", "🧧", "🥮", "🏛", "💰", "⚡", "📚", "🍜", "🔥",
            "🦎", "🏟️", "🐘", "❄️", "🏖️"
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
            new Color(139, 0, 0),      // Địa Ngục - đỏ đậm
            new Color(34, 139, 34),    // Godzilla - xanh lá
            new Color(70, 130, 180),   // Juventus - steel blue
            new Color(218, 165, 32),   // Thần Thú - vàng đậm
            new Color(100, 149, 237),  // Kỷ Băng Hà - cornflower blue
            new Color(0, 191, 255)     // Sự Kiện Hè - xanh dương nhạt
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
            case 7 -> body = buildPokemonDetail();
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
            case 10 -> body = buildDiaNgucDetail();
            case 11 -> body = buildGodzillaVsKongDetail();
            case 12 -> body = buildJuventusDetail();
            case 13 -> body = buildThanThuDetail();
            case 14 -> body = buildKyBangHaDetail();
            case 15 -> body = buildSuKienHeDetail();
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

    /** Chi tiết Sự kiện Pokémon 30/4 - 1/5 */
    private JPanel buildPokemonDetail() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.WHITE);

        // 1. Mô tả
        JTextArea desc = new JTextArea(
            "⚡ Sự kiện Pokémon - Mừng 30/4 & 1/5\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
            "Pokémon hoang dã đã xuất hiện tại các làng!\n" +
            "Đánh Boss Pokémon → Nhặt Trứng → Mở ra Pet Pokémon!\n" +
            "Boss KHÔNG gây sát thương (ai cũng đánh được).\n" +
            "Cần có Bóng Poke/Ultra/Master để nhặt trứng tương ứng.\n" +
            "Quái thường drop vật phẩm sự kiện (695-698) với tỉ lệ 10%."
        );
        desc.setEditable(false);
        desc.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        desc.setBackground(new Color(255, 255, 240));
        desc.setBorder(new CompoundBorder(
            new TitledBorder(BorderFactory.createLineBorder(new Color(255, 230, 0)),
                " Mô Tả Sự Kiện ", TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12), new Color(200, 170, 0)),
            new EmptyBorder(8, 8, 8, 8)));
        desc.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));
        p.add(desc);
        p.add(Box.createVerticalStrut(10));

        // 2. Bảng Boss
        String[] bossCols = {"Tên Boss", "HP", "Dame/Hit", "Vị Trí", "Hồi Sinh"};
        Object[][] bossData = {
            {"⚡ Pikachu Hoang Dã", "5,000", "-1 HP/hit", "Làng Kakaro (map 2)", "30 phút"},
            {"🔥 Charmander Hoang Dã", "5,000", "-1 HP/hit", "Làng Aru (map 9)", "30 phút"},
            {"💧 Squirtle Hoang Dã", "5,000", "-1 HP/hit", "Làng Mori (map 16)", "30 phút"},
        };
        JTable tblBoss = createStyledTable(bossCols, bossData);
        JScrollPane scrollBoss = new JScrollPane(tblBoss);
        scrollBoss.setBorder(new TitledBorder(BorderFactory.createLineBorder(new Color(0, 102, 204)),
            " Boss Pokémon ", TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 12), new Color(0, 102, 204)));
        scrollBoss.setPreferredSize(new Dimension(0, 120));
        scrollBoss.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        p.add(scrollBoss);
        p.add(Box.createVerticalStrut(10));

        // 3. Bảng Trứng
        String[] eggCols = {"Loại Trứng", "Item ID", "Tỉ Lệ Drop", "Cần Bóng"};
        Object[][] eggData = {
            {"🟢 Trứng Thường", "1873", "60%", "Bóng Poke (1873)"},
            {"🔵 Trứng Ultra", "1874", "30%", "Bóng Ultra (1874)"},
            {"🟣 Trứng Master", "1875", "10%", "Bóng Master (1875)"},
        };
        JTable tblEgg = createStyledTable(eggCols, eggData);
        JScrollPane scrollEgg = new JScrollPane(tblEgg);
        scrollEgg.setBorder(new TitledBorder(BorderFactory.createLineBorder(new Color(40, 167, 69)),
            " Trứng Pokémon (Drop 2-4 trứng/boss) ", TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 12), new Color(40, 167, 69)));
        scrollEgg.setPreferredSize(new Dimension(0, 110));
        scrollEgg.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        p.add(scrollEgg);
        p.add(Box.createVerticalStrut(10));

        // 4. Bảng Pet
        String[] petCols = {"Loại Trứng", "Chỉ Số", "Vĩnh Viễn", "Hạn Thuê"};
        Object[][] petData = {
            {"🟢 Trứng Thường", "+5~10% HP/KI/SD", "40%", "1-4 ngày"},
            {"🔵 Trứng Ultra", "+11~16% HP/KI/SD", "60%", "1-4 ngày"},
            {"🟣 Trứng Master", "+15~25% HP/KI/SD", "100%", "Vĩnh viễn"},
        };
        JTable tblPet = createStyledTable(petCols, petData);
        JScrollPane scrollPet = new JScrollPane(tblPet);
        scrollPet.setBorder(new TitledBorder(BorderFactory.createLineBorder(new Color(142, 68, 173)),
            " Mở Trứng → Pet (ID 1865-1868) ", TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 12), new Color(142, 68, 173)));
        scrollPet.setPreferredSize(new Dimension(0, 110));
        scrollPet.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        p.add(scrollPet);

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

    // ======== CHI TIẾT 5 EVENT MỚI ========

    private JPanel buildDiaNgucDetail() {
        return buildFullDetail("🔥 Địa Ngục Đảo Lộn", new Color(139, 0, 0),
            "━━━ CÁCH CHƠI ━━━\n"
            + "1. Vào map Địa Ngục (174) qua NPC tại Thần Điện hoặc lệnh cm 174\n"
            + "2. Giết mob Quỷ nguyên tố (đỏ/xanh/xanh lá/vàng) để farm vật phẩm\n"
            + "3. Map 174 → 179 → 180 (Địa Ngục 3 = khó nhất, drop tốt nhất)\n"
            + "4. Boss Janemba spawn mỗi 2h tại Địa Ngục 3 (map 180)\n"
            + "5. Gom Thỏi vàng + Hồng Ngọc → đổi item VIP",
            new String[]{"Mob/Boss", "Map", "HP", "Drop chính", "Tỉ lệ"},
            new Object[][]{
                {"🔴 Quỷ Đỏ (88)", "174/179/180", "Scale", "Thỏi Vàng (457)", "15%"},
                {"🔵 Quỷ Xanh (89)", "174/179/180", "Scale", "Đá Xanh Lam (935)", "8%"},
                {"🟢 Quỷ Xanh Lá (90)", "174/179/180", "Scale", "Vàng 100K-300K", "20%"},
                {"🟡 Quỷ Vàng (91)", "174/179/180", "Scale", "Hồng Ngọc 50-200", "3%"},
                {"👹 Janemba (Boss)", "180", "5 Tỉ", "Mảnh Hồn BT (934)", "5%"},
            },
            new String[]{"Vật Phẩm", "Nguồn", "Công Dụng"},
            new Object[][]{
                {"Thỏi Vàng (457)", "Mob Quỷ 15%", "Đổi item, giao dịch"},
                {"Đá Xanh Lam (935)", "Mob Quỷ 8%", "Nâng cấp trang bị"},
                {"Mảnh Hồn BT (934)", "Địa Ngục 3 chỉ 5%", "Nâng cấp Bông Tai"},
                {"Hồng Ngọc (861)", "Mob Quỷ 3%", "Mua item shop HN"},
            });
    }

    private JPanel buildGodzillaVsKongDetail() {
        return buildFullDetail("🦎 Godzilla vs Kong", new Color(34, 139, 34),
            "━━━ CÁCH CHƠI ━━━\n"
            + "1. Đến Hành Tinh Vampa (map 175) qua Trạm tàu hoặc cm 175\n"
            + "2. Farm mob thường → drop Mảnh Titan + BTC3 + Vàng\n"
            + "3. Boss Godzilla spawn lúc 12h TRƯA — đánh xa, HP 5 Tỉ\n"
            + "4. Boss Kong spawn lúc 20h TỐI — đánh gần, HP 5 Tỉ\n"
            + "5. ★ HẠ CẢ 2 CÙNG NGÀY → spawn MechaGodzilla (HP 20 Tỉ)\n"
            + "6. MechaGodzilla drop Ngọc Rồng 5-7★, Capsule VIP, 50 Thỏi Vàng",
            new String[]{"Boss", "Spawn", "HP", "Drop đặc biệt", "Tỉ lệ"},
            new Object[][]{
                {"🦎 Godzilla", "12h Trưa", "5 Tỉ", "Set Godzilla (Cải trang)", "10%"},
                {"🦍 Kong", "20h Tối", "5 Tỉ", "Set Kong (Cải trang)", "10%"},
                {"🤖 MechaGodzilla", "Hạ cả 2", "20 Tỉ", "NR 5-7★ + 50 Thỏi Vàng", "100%"},
                {"Mob thường", "Liên tục", "Scale", "Mảnh Titan (1634)", "12%"},
                {"Mob thường", "Liên tục", "Scale", "BTC3 (1855)", "5%"},
            },
            new String[]{"Vật Phẩm", "Nguồn", "Công Dụng"},
            new Object[][]{
                {"Mảnh Titan (1634)", "Mob 12%", "Gom 50 → đổi Capsule VIP"},
                {"Capsule DC (192)", "Mob 1%", "Mở ra Dây Chuyền ngẫu nhiên"},
                {"BTC3 (1855)", "Mob 5%", "Farm nâng cấp Bông Tai cấp 3"},
                {"Vàng 150K-500K", "Mob 25%", "Thu nhập chính"},
            });
    }

    private JPanel buildJuventusDetail() {
        return buildFullDetail("🏟️ Juventus Tournament", new Color(70, 130, 180),
            "━━━ CÁCH CHƠI ━━━\n"
            + "1. Đến map Juventus (183) qua NPC tại ĐHVT hoặc cm 183\n"
            + "2. CHẾ ĐỘ 1 — FARM: Giết mob để farm vàng + HN + Thỏi vàng\n"
            + "3. CHẾ ĐỘ 2 — PVP: Đăng ký giải đấu hàng tuần (16 người)\n"
            + "4. Format: Single Elimination, Best of 1\n"
            + "5. Phí tham gia: 1 Thỏi Vàng\n"
            + "6. DB column 'juventus' tự đếm số lần tham gia\n"
            + "━━━ PHẦN THƯỞNG PVP ━━━\n"
            + "Top 1: 50 Thỏi Vàng + Danh hiệu 'Vô Địch Juventus'\n"
            + "Top 2: 30 Thỏi Vàng\n"
            + "Top 3-4: 15 Thỏi Vàng\n"
            + "Top 5-8: 5 Thỏi Vàng",
            new String[]{"Drop Mob", "Map", "Tỉ Lệ", "Số Lượng", "Ghi Chú"},
            new Object[][]{
                {"Vàng", "183", "30%", "200K-600K", "Thu nhập chính"},
                {"Hồng Ngọc (861)", "183", "10%", "100-500", "Shop Hồng Ngọc"},
                {"Thỏi Vàng (457)", "183", "3%", "1", "Giao dịch/Phí PVP"},
                {"Sét Kích Hoạt", "183", "0.5%", "1", "Full option ngẫu nhiên"},
            },
            null, null);
    }

    private JPanel buildThanThuDetail() {
        return buildFullDetail("🐘 Thần Thú Cổ Đại", new Color(218, 165, 32),
            "━━━ CÁCH CHƠI ━━━\n"
            + "1. Đến Cung Trăng (176) hoặc Vùng Đất Huyền Thoại (178)\n"
            + "2. Giết mob → drop 3 loại Linh Phù (Voi/Gà/Ngựa)\n"
            + "3. Gom mỗi loại x1 (3 cái khác nhau)\n"
            + "4. Mang 3 Linh Phù đến NPC → Triệu hồi BOSS ẨN: Thần Long Cổ Đại\n"
            + "5. Boss ẩn drop item cực hiếm!\n"
            + "━━━ THÚ VỊ ━━━\n"
            + "• Voi Chín Ngà, Gà Chín Cửa, Ngựa Chín Hồng Mao là 3 Thần Thú từ truyền thuyết\n"
            + "• Mỗi Thần Thú chỉ drop Linh Phù riêng ở map riêng\n"
            + "• Boss ẩn Thần Long chỉ xuất hiện khi ghép đủ 3 Linh Phù",
            new String[]{"Thần Thú/Boss", "Map", "Drop", "Tỉ Lệ", "Ghi Chú"},
            new Object[][]{
                {"🐘 Voi Chín Ngà", "176", "Linh Phù Voi (663)", "8%", "Chỉ ở Cung Trăng"},
                {"🐓 Gà Chín Cửa", "178", "Linh Phù Gà (664)", "8%", "Chỉ ở Vùng Đất HT"},
                {"🐴 Ngựa Chín HM", "176+178", "Linh Phù Ngựa (665)", "5%", "Cả 2 map"},
                {"🐉 Thần Long (Boss ẩn)", "176", "BTC3 + Capsule VIP", "Ghép 3 LP", "HP 10 Tỉ"},
                {"Mob thường", "176/178", "Mảnh BT (934)", "6%", "Nâng cấp Bông Tai"},
            },
            new String[]{"Vật Phẩm", "Cách Lấy", "Công Dụng"},
            new Object[][]{
                {"Linh Phù Voi (663)", "Mob Cung Trăng 8%", "1/3 để triệu hồi Boss"},
                {"Linh Phù Gà (664)", "Mob Vùng Đất HT 8%", "1/3 để triệu hồi Boss"},
                {"Linh Phù Ngựa (665)", "Cả 2 map 5%", "1/3 để triệu hồi Boss"},
                {"Mảnh BT (934)", "Mob 6%", "Nâng cấp Bông Tai Porata"},
            });
    }

    private JPanel buildKyBangHaDetail() {
        return buildFullDetail("❄️ Kỷ Băng Hà", new Color(100, 149, 237),
            "━━━ CÁCH CHƠI ━━━\n"
            + "1. Đến Cánh Đồng Tuyết 2 (195) qua NPC hoặc cm 195\n"
            + "2. 3 map: 195 (Cánh đồng) → 196 (Rừng tuyết) → 197 (Hang băng)\n"
            + "3. Mob mạnh hơn map tuyết gốc 5x — cần sức mạnh cao\n"
            + "4. ★ FARM BTC3 RATE 10% — nguồn farm tốt nhất server!\n"
            + "5. Boss Ice Shenron spawn mỗi 4h tại Hang Băng 2 (map 197)\n"
            + "6. Gom 99 Tinh Thể Băng → đổi Vũ Khí Băng (cải trang)\n"
            + "━━━ ĐẶC BIỆT ━━━\n"
            + "• Map 197 (Hang Băng 2) có drop Capsule DC 0.5% + Sách TK 0.3%\n"
            + "• Đây là nguồn farm BTC3 tốt nhất khi event mở",
            new String[]{"Mob/Boss", "Map", "Drop", "Tỉ Lệ", "Số Lượng"},
            new Object[][]{
                {"❄ Frostbite (105)", "195/196/197", "Tinh Thể Băng", "15%", "1-3"},
                {"🍊 Snowy (106)", "195/196/197", "BTC3 (1855)", "10%", "1-5"},
                {"🦖 Deinonychus (107)", "195/196/197", "Đá Xanh Lam", "8%", "1"},
                {"🐍 Snake (108)", "195/196/197", "Vàng 150K-350K", "25%", "-"},
                {"🐲 Ice Shenron", "197", "Capsule DC + Sách TK", "Boss", "Mỗi 4h"},
            },
            new String[]{"Vật Phẩm", "Tỉ Lệ", "Công Dụng"},
            new Object[][]{
                {"Tinh Thể Băng (1530)", "15%", "Gom 99 → đổi Vũ Khí Băng"},
                {"BTC3 (1855)", "10%", "Farm nâng cấp Bông Tai cấp 3"},
                {"Đá Xanh Lam (935)", "8%", "Nâng cấp trang bị"},
                {"Capsule DC (192)", "0.5% (map 197)", "Mở Dây Chuyền"},
                {"Sách TK (456)", "0.3% (map 197)", "Học Tiềm Năng"},
                {"Thức ăn", "15%", "Hồi HP/KI"},
            });
    }

    /** Builder chung cho event detail có 2 bảng */
    private JPanel buildFullDetail(String title, Color color, String desc,
            String[] bossCols, Object[][] bossData,
            String[] rewardCols, Object[][] rewardData) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.WHITE);

        // Mô tả
        JTextArea ta = new JTextArea(title + "\n" + desc);
        ta.setEditable(false);
        ta.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        ta.setBackground(new Color(248, 250, 255));
        ta.setBorder(new CompoundBorder(
            new TitledBorder(BorderFactory.createLineBorder(color),
                " Hướng Dẫn Chơi ", TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12), color),
            new EmptyBorder(8, 8, 8, 8)));
        ta.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));
        p.add(ta);
        p.add(Box.createVerticalStrut(10));

        // Bảng Boss/Mob
        if (bossCols != null && bossData != null) {
            JTable tbl = createStyledTable(bossCols, bossData);
            JScrollPane sc = new JScrollPane(tbl);
            sc.setBorder(new TitledBorder(BorderFactory.createLineBorder(new Color(0, 102, 204)),
                " Mob & Boss ", TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12), new Color(0, 102, 204)));
            sc.setPreferredSize(new Dimension(0, Math.min(bossData.length * 30 + 50, 200)));
            sc.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));
            p.add(sc);
            p.add(Box.createVerticalStrut(10));
        }

        // Bảng Reward
        if (rewardCols != null && rewardData != null) {
            JTable tbl2 = createStyledTable(rewardCols, rewardData);
            JScrollPane sc2 = new JScrollPane(tbl2);
            sc2.setBorder(new TitledBorder(BorderFactory.createLineBorder(new Color(40, 167, 69)),
                " Phần Thưởng & Vật Phẩm ", TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12), new Color(40, 167, 69)));
            sc2.setPreferredSize(new Dimension(0, Math.min(rewardData.length * 30 + 50, 180)));
            sc2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
            p.add(sc2);
        }
        return p;
    }

    /** Chi tiết Sự Kiện Hè */
    private JPanel buildSuKienHeDetail() {
        return buildFullDetail("\ud83c\udfd6\ufe0f S\u1ef1 Ki\u1ec7n H\u00e8 - M\u00f9a H\u00e8 R\u1ef1c R\u1ee1", new Color(0, 191, 255),
            "\u2501\u2501\u2501 C\u00c1CH CH\u01a0I \u2501\u2501\u2501\n"
            + "1. \u0110\u00e1nh quai t\u1ea1i map 161-163 (Nguy\u00ean Th\u1ee7y) \u2192 drop V\u1ecf \u1ed0c, V\u1ecf S\u00f2, Con Cua, Sao Bi\u1ec3n\n"
            + "2. Thu th\u1eadp N\u01b0\u1edbc \u0110\u00e1 + Kh\u00fac M\u00eda \u2192 \u0111\u1ebfn NPC Qu\u1ea7y N\u01b0\u1edbc M\u00eda \u2192 nh\u1eadn N\u01b0\u1edbc M\u00eda\n"
            + "3. Tr\u1ea3 N\u01b0\u1edbc M\u00eda cho NPC \u2192 t\u00edch \u0111i\u1ec3m + g\u00f3p ly buff server\n"
            + "4. Gom \u0111\u1ee7 v\u1eadt ph\u1ea9m bi\u1ec3n \u2192 \u0111\u1ed5i H\u1ed9p Qu\u00e0 SK t\u1ea1i Quy L\u00e3o Kame\n"
            + "5. Boss M\u1eb7t Tr\u1eddi spawn ng\u1eabu nhi\u00ean t\u1ea1i c\u00e1c l\u00e0ng\n"
            + "\u2501\u2501\u2501 KHUY\u1ebeN M\u00c3I \u2501\u2501\u2501\n"
            + "\u2605 N\u1ea0P ATM/BANK \u0110\u01af\u1ee2C x2 NGỌC su\u1ed1t th\u1eddi gian s\u1ef1 ki\u1ec7n!\n"
            + "\u2605 \u0110\u1ee7 999 ly n\u01b0\u1edbc m\u00eda \u2192 Buff +20% s\u1ee9c \u0111\u00e1nh to\u00e0n server 60 ph\u00fat!",
            new String[]{"Ho\u1ea1t \u0110\u1ed9ng", "Y\u00eau C\u1ea7u", "Ph\u1ea7n Th\u01b0\u1edfng", "T\u1ec9 L\u1ec7", "Ghi Ch\u00fa"},
            new Object[][]{
                {"\u2600 Boss M\u1eb7t Tr\u1eddi", "500 HP (\u0111\u00e1nh 1 dame/hit)", "C\u1edd M\u1eb7t Tr\u1eddi + Th\u1ecfi V\u00e0ng", "50%/30%", "Spawn m\u1ed7i 10p"},
                {"\ud83e\uddca Gi\u1ea3i Nhi\u1ec7t", "30 N\u01b0\u1edbc \u0110\u00e1 + 30 M\u00eda + 100tr", "N\u01b0\u1edbc M\u00eda ng\u1eabu nhi\u00ean", "15%/30%/55%", "+2 \u0111i\u1ec3m SK"},
                {"\ud83c\udf79 Tr\u1ea3 H\u00e0ng", "Ly N\u01b0\u1edbc M\u00eda", "\u0110i\u1ec3m SK + Buff server", "100%", "999 ly = buff"},
                {"\ud83c\udf81 \u0110\u1ed5i H\u1ed9p Qu\u00e0", "10 m\u1ed7i lo\u1ea1i v\u1ecf + 5 \u0111\u00e1 + 50tr", "C\u1ea3i Trang/Th\u1ecfi V\u00e0ng/\u0110\u00e1", "50% TC", "+1 \u0111i\u1ec3m SK"},
                {"\ud83d\udcb0 KM x2 N\u1ea1p", "N\u1ea1p ATM/Bank", "x2 Ng\u1ecdc nh\u1eadn \u0111\u01b0\u1ee3c", "100%", "Su\u1ed1t event"},
            },
            new String[]{"V\u1eadt Ph\u1ea9m", "Item ID", "Ngu\u1ed3n", "C\u00f4ng D\u1ee5ng"},
            new Object[][]{
                {"V\u1ecf \u1ed0c", "695", "Mob map 161-163 (10%)", "\u0110\u1ed5i H\u1ed9p Qu\u00e0 SK"},
                {"V\u1ecf S\u00f2", "696", "Mob map 161-163 (10%)", "\u0110\u1ed5i H\u1ed9p Qu\u00e0 SK"},
                {"Con Cua", "697", "Mob map 161-163 (10%)", "\u0110\u1ed5i H\u1ed9p Qu\u00e0 SK"},
                {"Sao Bi\u1ec3n", "698", "Mob map 161-163 (10%)", "\u0110\u1ed5i H\u1ed9p Qu\u00e0 SK"},
                {"\u0110\u00e1 Ng\u0169 S\u1eafc", "674", "Boss M\u1eb7t Tr\u1eddi 20%", "\u0110\u1ed5i H\u1ed9p Qu\u00e0 SK"},
                {"N\u01b0\u1edbc \u0110\u00e1", "1613", "Boss MT 10% / Mua", "Gi\u1ea3i Nhi\u1ec7t"},
                {"Kh\u00fac M\u00eda", "1612", "Boss MT 10% / Mua", "Gi\u1ea3i Nhi\u1ec7t"},
                {"C\u1edd M\u1eb7t Tr\u1eddi", "1562", "Boss M\u1eb7t Tr\u1eddi 50%", "Trang b\u1ecb \u0111\u1eb7c bi\u1ec7t"},
            });
    }
}