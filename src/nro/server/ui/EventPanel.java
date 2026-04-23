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
        btnDetail.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, EVENT_DESCS[index], "Chi tiết sự kiện - " + EVENT_NAMES[index], JOptionPane.INFORMATION_MESSAGE);
        });

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
}