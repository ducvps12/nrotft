package nro.server.ui;

import jdbc.DBConnecter;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;

/**
 * Panel quản lý & audit vật phẩm chưa có chức năng (UseItem handler).
 * - Tab 1: Audit items chưa xử lý (scan UseItem.java vs DB)
 * - Tab 2: Quick Admin Tools (gửi item, broadcast, kick)
 * - Tab 3: Thống kê vật phẩm theo type
 */
public class ItemAuditPanel extends JPanel {

    private DefaultTableModel unhandledModel;
    private DefaultTableModel statsModel;
    private JTable unhandledTable;
    private JLabel lblSummary;
    private JTextArea txtLog;

    public ItemAuditPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // Header
        JLabel header = new JLabel("📋 QUẢN LÝ VẬT PHẨM & CÔNG CỤ ADMIN");
        header.setFont(new Font("Segoe UI", Font.BOLD, 22));
        header.setForeground(new Color(0, 102, 204));
        header.setBorder(new EmptyBorder(0, 5, 10, 0));
        add(header, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabs.addTab("🔍 Audit Vật Phẩm", createAuditTab());
        tabs.addTab("⚡ Quick Tools", createQuickToolsTab());
        tabs.addTab("📊 Thống Kê Type", createStatsTab());
        add(tabs, BorderLayout.CENTER);
    }

    // ============ TAB 1: AUDIT ============
    private JPanel createAuditTab() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(10, 5, 5, 5));

        // Top bar
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        topBar.setOpaque(false);

        JButton btnScan = new JButton("🔄 Quét UseItem.java vs DB");
        btnScan.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnScan.setBackground(new Color(0, 120, 215));
        btnScan.setForeground(Color.WHITE);
        btnScan.setFocusPainted(false);
        btnScan.addActionListener(e -> performAudit());

        JComboBox<String> cbFilter = new JComboBox<>(new String[]{
            "Tất cả", "Chỉ Type 27 (Sự kiện)", "Chỉ Type 12 (Ngọc Rồng)", "Chỉ Type 99 (Đặc biệt)"
        });
        cbFilter.addActionListener(e -> filterTable(cbFilter.getSelectedIndex()));

        lblSummary = new JLabel("Chưa quét. Nhấn nút để bắt đầu.");
        lblSummary.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblSummary.setForeground(Color.GRAY);

        topBar.add(btnScan);
        topBar.add(new JLabel("Lọc:"));
        topBar.add(cbFilter);
        topBar.add(lblSummary);

        p.add(topBar, BorderLayout.NORTH);

        // Table
        String[] cols = {"ID", "Tên Item", "Type", "Giới Tính", "Level", "Trạng Thái", "Ghi Chú"};
        unhandledModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        unhandledTable = new JTable(unhandledModel);
        unhandledTable.setRowHeight(28);
        unhandledTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        unhandledTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));

        // Color renderer
        unhandledTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                if (!sel) {
                    String status = (String) t.getValueAt(r, 5);
                    if ("✅ Đã xử lý".equals(status)) {
                        comp.setBackground(new Color(220, 255, 220));
                    } else if ("❌ Chưa xử lý".equals(status)) {
                        comp.setBackground(new Color(255, 230, 230));
                    } else {
                        comp.setBackground(Color.WHITE);
                    }
                }
                return comp;
            }
        });

        JScrollPane scroll = new JScrollPane(unhandledTable);
        p.add(scroll, BorderLayout.CENTER);

        return p;
    }

    private void performAudit() {
        new Thread(() -> {
            try {
                // 1. Scan UseItem.java for all case IDs
                Set<Integer> handledIds = scanUseItemCases();

                // 2. Load all items from DB
                List<Object[]> allItems = loadAllItemsFromDB();

                // 3. Cross-reference
                int handled = 0, unhandled = 0;
                SwingUtilities.invokeLater(() -> unhandledModel.setRowCount(0));

                for (Object[] item : allItems) {
                    int id = (int) item[0];
                    String name = (String) item[1];
                    int type = (int) item[2];
                    String gender = (String) item[3];
                    int level = (int) item[4];

                    boolean isHandled = handledIds.contains(id);
                    String status = isHandled ? "✅ Đã xử lý" : "❌ Chưa xử lý";
                    String note = "";

                    // Only show event/special items that SHOULD have handlers
                    if (type == 27 || type == 99 || type == 12 || type == 32) {
                        if (isHandled) handled++; else unhandled++;
                        final String fStatus = status;
                        final String fNote = note;
                        SwingUtilities.invokeLater(() ->
                            unhandledModel.addRow(new Object[]{id, name, type, gender, level, fStatus, fNote})
                        );
                    }
                }

                final int fHandled = handled, fUnhandled = unhandled;
                SwingUtilities.invokeLater(() ->
                    lblSummary.setText("✅ " + fHandled + " đã xử lý | ❌ " + fUnhandled + " chưa xử lý | Tổng: " + (fHandled + fUnhandled))
                );
            } catch (Exception e) {
                SwingUtilities.invokeLater(() ->
                    lblSummary.setText("Lỗi: " + e.getMessage())
                );
                e.printStackTrace();
            }
        }).start();
    }

    private Set<Integer> scanUseItemCases() {
        Set<Integer> ids = new HashSet<>();
        try {
            String path = "src/services/func/UseItem.java";
            String content = Files.readString(Path.of(path));
            // Match "case XXXX:" patterns inside the default switch(item.template.id)
            Pattern p = Pattern.compile("case\\s+(\\d+)\\s*:");
            Matcher m = p.matcher(content);
            while (m.find()) {
                ids.add(Integer.parseInt(m.group(1)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ids;
    }

    private List<Object[]> loadAllItemsFromDB() {
        List<Object[]> items = new ArrayList<>();
        try (Connection conn = DBConnecter.getConnectionServer();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT id, name, type, gender, level FROM item_template ORDER BY id")) {
            while (rs.next()) {
                String gender;
                switch (rs.getInt("gender")) {
                    case 0: gender = "Trái Đất"; break;
                    case 1: gender = "Namec"; break;
                    case 2: gender = "Xayda"; break;
                    default: gender = "All"; break;
                }
                items.add(new Object[]{
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getInt("type"),
                    gender,
                    rs.getInt("level")
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return items;
    }

    private void filterTable(int filterIndex) {
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(unhandledModel);
        unhandledTable.setRowSorter(sorter);
        switch (filterIndex) {
            case 1: sorter.setRowFilter(RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL, 27, 2)); break;
            case 2: sorter.setRowFilter(RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL, 12, 2)); break;
            case 3: sorter.setRowFilter(RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL, 99, 2)); break;
            default: sorter.setRowFilter(null); break;
        }
    }

    // ============ TAB 2: QUICK TOOLS ============
    private JPanel createQuickToolsTab() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel tools = new JPanel(new GridLayout(0, 1, 0, 8));
        tools.setOpaque(false);

        // Tool 1: Broadcast Message
        tools.add(createToolCard("📢 Thông Báo Toàn Server", "Gửi thông báo đến tất cả người chơi", () -> {
            String msg = JOptionPane.showInputDialog(this, "Nội dung thông báo:", "Broadcast", JOptionPane.PLAIN_MESSAGE);
            if (msg != null && !msg.trim().isEmpty()) {
                try {
                    nro.services.Service.gI().sendThongBaoAllPlayer(msg);
                    addLog("📢 Broadcast: " + msg);
                } catch (Exception e) { addLog("Lỗi: " + e.getMessage()); }
            }
        }));

        // Tool 2: Gửi Item cho Player
        tools.add(createToolCard("🎁 Gửi Vật Phẩm", "Gửi item theo ID đến người chơi đang online", () -> {
            JPanel inputPanel = new JPanel(new GridLayout(3, 2, 5, 5));
            JTextField tfPlayer = new JTextField();
            JTextField tfItemId = new JTextField();
            JTextField tfQuantity = new JTextField("1");
            inputPanel.add(new JLabel("Tên người chơi:"));
            inputPanel.add(tfPlayer);
            inputPanel.add(new JLabel("Item ID:"));
            inputPanel.add(tfItemId);
            inputPanel.add(new JLabel("Số lượng:"));
            inputPanel.add(tfQuantity);
            int result = JOptionPane.showConfirmDialog(this, inputPanel, "Gửi Vật Phẩm", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                try {
                    String playerName = tfPlayer.getText().trim();
                    int itemId = Integer.parseInt(tfItemId.getText().trim());
                    int qty = Integer.parseInt(tfQuantity.getText().trim());
                    // Find player and send
                    nro.player.Player pl = nro.server.Client.gI().getPlayer(playerName);
                    if (pl != null) {
                        for (int i = 0; i < qty; i++) {
                            item.Item it = nro.services.ItemService.gI().createNewItem((short) itemId);
                            nro.services.InventoryService.gI().addItemBag(pl, it);
                        }
                        nro.services.InventoryService.gI().sendItemBag(pl);
                        nro.services.Service.gI().sendThongBao(pl, "Admin đã gửi cho bạn " + qty + "x item [" + itemId + "]");
                        addLog("✅ Gửi " + qty + "x item " + itemId + " cho " + playerName);
                    } else {
                        addLog("❌ Không tìm thấy player: " + playerName);
                        JOptionPane.showMessageDialog(this, "Player không online!");
                    }
                } catch (Exception e) {
                    addLog("Lỗi: " + e.getMessage());
                    JOptionPane.showMessageDialog(this, "Lỗi: " + e.getMessage());
                }
            }
        }));

        // Tool 3: Gửi Vàng/Ngọc
        tools.add(createToolCard("💰 Gửi Vàng/Ngọc", "Cộng vàng hoặc ngọc cho người chơi online", () -> {
            JPanel inputPanel = new JPanel(new GridLayout(3, 2, 5, 5));
            JTextField tfPlayer = new JTextField();
            JTextField tfGold = new JTextField("0");
            JTextField tfGem = new JTextField("0");
            inputPanel.add(new JLabel("Tên người chơi:"));
            inputPanel.add(tfPlayer);
            inputPanel.add(new JLabel("Vàng (+):"));
            inputPanel.add(tfGold);
            inputPanel.add(new JLabel("Ngọc (+):"));
            inputPanel.add(tfGem);
            int result = JOptionPane.showConfirmDialog(this, inputPanel, "Gửi Vàng/Ngọc", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                try {
                    String pName = tfPlayer.getText().trim();
                    long gold = Long.parseLong(tfGold.getText().trim());
                    int gem = Integer.parseInt(tfGem.getText().trim());
                    nro.player.Player pl = nro.server.Client.gI().getPlayer(pName);
                    if (pl != null) {
                        pl.inventory.addGoldSafe(gold);
                        pl.inventory.gem += gem;
                        nro.services.PlayerService.gI().sendInfoHpMpMoney(pl);
                        nro.services.Service.gI().sendThongBao(pl, "Admin cộng: +" + gold + " vàng, +" + gem + " ngọc");
                        addLog("✅ Gửi " + gold + " vàng + " + gem + " ngọc cho " + pName);
                    } else {
                        JOptionPane.showMessageDialog(this, "Player không online!");
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "Lỗi: " + e.getMessage());
                }
            }
        }));

        // Tool 4: Kick Player
        tools.add(createToolCard("🚫 Kick Người Chơi", "Đá người chơi ra khỏi server", () -> {
            String name = JOptionPane.showInputDialog(this, "Tên người chơi cần kick:");
            if (name != null && !name.trim().isEmpty()) {
                nro.player.Player pl = nro.server.Client.gI().getPlayer(name.trim());
                if (pl != null) {
                    pl.getSession().disconnect();
                    addLog("🚫 Kicked: " + name);
                } else {
                    JOptionPane.showMessageDialog(this, "Player không online!");
                }
            }
        }));

        // Tool 5: Set EXP Rate
        tools.add(createToolCard("⚡ Đổi Tỷ Lệ EXP", "Thay đổi hệ số EXP toàn server", () -> {
            String input = JOptionPane.showInputDialog(this, "Hệ số EXP mới (VD: 2.0):",
                String.valueOf(nro.server.Manager.RATE_EXP_SERVER));
            if (input != null) {
                try {
                    double rate = Double.parseDouble(input.trim());
                    nro.server.Manager.RATE_EXP_SERVER = rate;
                    nro.services.Service.gI().sendThongBaoAllPlayer("EXP Rate: x" + rate);
                    addLog("⚡ EXP Rate → x" + rate);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "Số không hợp lệ!");
                }
            }
        }));

        JScrollPane scroll = new JScrollPane(tools);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        p.add(scroll, BorderLayout.CENTER);

        // Log area
        txtLog = new JTextArea(5, 0);
        txtLog.setEditable(false);
        txtLog.setFont(new Font("Consolas", Font.PLAIN, 12));
        txtLog.setBackground(new Color(30, 30, 30));
        txtLog.setForeground(new Color(0, 255, 100));
        JScrollPane logScroll = new JScrollPane(txtLog);
        logScroll.setBorder(BorderFactory.createTitledBorder("Admin Log"));
        p.add(logScroll, BorderLayout.SOUTH);

        return p;
    }

    private JPanel createToolCard(String title, String desc, Runnable action) {
        JPanel card = new JPanel(new BorderLayout(10, 0));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
            new EmptyBorder(12, 15, 12, 15)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));

        JPanel textPanel = new JPanel(new GridLayout(2, 1));
        textPanel.setOpaque(false);
        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        JLabel lblDesc = new JLabel(desc);
        lblDesc.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblDesc.setForeground(Color.GRAY);
        textPanel.add(lblTitle);
        textPanel.add(lblDesc);

        JButton btn = new JButton("Thực hiện");
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(new Color(0, 120, 215));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(100, 35));
        btn.addActionListener(e -> action.run());
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(new Color(0, 90, 180)); }
            public void mouseExited(MouseEvent e) { btn.setBackground(new Color(0, 120, 215)); }
        });

        card.add(textPanel, BorderLayout.CENTER);
        card.add(btn, BorderLayout.EAST);
        return card;
    }

    // ============ TAB 3: STATS ============
    private JPanel createStatsTab() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(10, 5, 5, 5));

        JButton btnRefresh = new JButton("🔄 Tải Thống Kê");
        btnRefresh.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnRefresh.setBackground(new Color(40, 167, 69));
        btnRefresh.setForeground(Color.WHITE);
        btnRefresh.setFocusPainted(false);
        btnRefresh.addActionListener(e -> loadStats());
        p.add(btnRefresh, BorderLayout.NORTH);

        String[] cols = {"Type", "Tên Loại", "Số Lượng Item", "Có Handler", "Chưa Handler"};
        statsModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tbl = new JTable(statsModel);
        tbl.setRowHeight(28);
        tbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tbl.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));

        p.add(new JScrollPane(tbl), BorderLayout.CENTER);
        return p;
    }

    private void loadStats() {
        new Thread(() -> {
            try {
                Set<Integer> handledIds = scanUseItemCases();
                Map<Integer, String> typeNames = new LinkedHashMap<>();
                typeNames.put(0, "Áo"); typeNames.put(1, "Quần"); typeNames.put(2, "Giày");
                typeNames.put(3, "Găng"); typeNames.put(4, "Rada"); typeNames.put(5, "Mũ");
                typeNames.put(6, "Thức ăn"); typeNames.put(7, "Sách Skill");
                typeNames.put(11, "Bag Item"); typeNames.put(12, "Ngọc Rồng");
                typeNames.put(21, "Bông tai"); typeNames.put(23, "Thú cưỡi mới");
                typeNames.put(27, "Vật phẩm sự kiện"); typeNames.put(32, "Trang sức");
                typeNames.put(33, "Card"); typeNames.put(72, "Mặt nạ"); typeNames.put(99, "Đặc biệt");

                Map<Integer, int[]> stats = new TreeMap<>(); // type -> [total, handled, unhandled]

                try (Connection conn = DBConnecter.getConnectionServer();
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT id, type FROM item_template")) {
                    while (rs.next()) {
                        int id = rs.getInt("id");
                        int type = rs.getInt("type");
                        stats.computeIfAbsent(type, k -> new int[3]);
                        stats.get(type)[0]++;
                        if (handledIds.contains(id)) stats.get(type)[1]++;
                        else stats.get(type)[2]++;
                    }
                }

                SwingUtilities.invokeLater(() -> {
                    statsModel.setRowCount(0);
                    for (var entry : stats.entrySet()) {
                        int type = entry.getKey();
                        int[] s = entry.getValue();
                        String typeName = typeNames.getOrDefault(type, "Type " + type);
                        statsModel.addRow(new Object[]{type, typeName, s[0], s[1], s[2]});
                    }
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void addLog(String msg) {
        if (txtLog != null) {
            SwingUtilities.invokeLater(() -> {
                txtLog.append("[" + java.time.LocalTime.now().toString().substring(0, 8) + "] " + msg + "\n");
                txtLog.setCaretPosition(txtLog.getDocument().getLength());
            });
        }
    }
}
