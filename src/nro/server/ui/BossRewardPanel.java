package nro.server.ui;

import boss.BossData;
import boss.BossID;
import boss.BossesData;
import models.Template.ItemTemplate;
import nro.server.Manager;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.List;

/**
 * Panel quản lý phần thưởng Boss - Hỗ trợ tỉ lệ rơi từng item
 * Format lưu: itemId-quantity-dropRate (VD: 992-1-3,1229-50-10)
 * Tương thích ngược: itemId-quantity (mặc định 30%)
 */
public class BossRewardPanel extends JPanel {

    private JComboBox<BossComboItem> cbBossList;
    private DefaultTableModel dropTableModel;
    private JTable dropTable;
    private JTextField txtSearchTable;
    private DefaultTableModel overviewModel;
    private JTable overviewTable;
    private JTextField txtSearchOverview;

    public BossRewardPanel() {
        setLayout(new BorderLayout(15, 15));
        setBackground(new Color(240, 242, 245));
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // --- PANEL TRÁI: CẤU HÌNH DROP ---
        JPanel leftPanel = new JPanel(new BorderLayout(0, 10));
        leftPanel.setOpaque(false);
        leftPanel.setPreferredSize(new Dimension(520, 0));

        // Header card
        JPanel headerCard = new JPanel(new GridBagLayout());
        headerCard.setBackground(Color.WHITE);
        headerCard.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(220, 220, 220), 1, true),
                new EmptyBorder(15, 15, 15, 15)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblTitle = new JLabel("⚔ CẤU HÌNH PHẦN THƯỞNG BOSS");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitle.setForeground(new Color(44, 62, 80));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 3;
        headerCard.add(lblTitle, gbc);

        // Chọn Boss
        gbc.gridy = 1; gbc.gridwidth = 1; gbc.weightx = 0;
        headerCard.add(createLabel("Boss:"), gbc);

        cbBossList = new JComboBox<>();
        cbBossList.setPreferredSize(new Dimension(0, 35));
        cbBossList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        loadBossListFromReflection();
        cbBossList.addActionListener(e -> loadDropsForSelectedBoss());
        gbc.gridx = 1; gbc.weightx = 1;
        headerCard.add(cbBossList, gbc);

        JButton btnRefresh = createStyledButton("Tải lại", new Color(108, 117, 125));
        btnRefresh.addActionListener(e -> {
            loadBossListFromReflection();
            loadDropsForSelectedBoss();
            refreshOverview();
        });
        gbc.gridx = 2; gbc.weightx = 0;
        headerCard.add(btnRefresh, gbc);

        leftPanel.add(headerCard, BorderLayout.NORTH);

        // Drop table
        JPanel dropCard = new JPanel(new BorderLayout(0, 8));
        dropCard.setBackground(Color.WHITE);
        dropCard.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(220, 220, 220), 1, true),
                new EmptyBorder(10, 10, 10, 10)));

        JLabel lblDropTitle = new JLabel("Danh sách vật phẩm rơi:");
        lblDropTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblDropTitle.setForeground(new Color(52, 73, 94));

        String[] dropCols = {"ID Item", "Tên Item", "Số lượng", "Tỉ lệ (%)"};
        dropTableModel = new DefaultTableModel(dropCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return c == 2 || c == 3; // Chỉ cho sửa số lượng và tỉ lệ
            }
        };
        dropTable = new JTable(dropTableModel);
        styleTable(dropTable);
        dropTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        dropTable.getColumnModel().getColumn(0).setMaxWidth(80);
        dropTable.getColumnModel().getColumn(1).setPreferredWidth(200);
        dropTable.getColumnModel().getColumn(2).setPreferredWidth(70);
        dropTable.getColumnModel().getColumn(2).setMaxWidth(90);
        dropTable.getColumnModel().getColumn(3).setPreferredWidth(70);
        dropTable.getColumnModel().getColumn(3).setMaxWidth(90);

        // Render tỉ lệ với màu
        dropTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                lbl.setHorizontalAlignment(JLabel.CENTER);
                try {
                    int rate = Integer.parseInt(value.toString());
                    if (rate >= 50) lbl.setForeground(new Color(39, 174, 96));
                    else if (rate >= 20) lbl.setForeground(new Color(243, 156, 18));
                    else if (rate >= 5) lbl.setForeground(new Color(231, 76, 60));
                    else lbl.setForeground(new Color(155, 89, 182));
                    if (!isSelected) lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
                } catch (Exception e) {}
                return lbl;
            }
        });

        JScrollPane scrollDrop = new JScrollPane(dropTable);
        scrollDrop.getViewport().setBackground(Color.WHITE);

        // Nút điều khiển
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        btnRow.setOpaque(false);

        JButton btnAdd = createStyledButton("+ Thêm Item", new Color(52, 152, 219));
        btnAdd.addActionListener(e -> openItemSearchDialog());

        JButton btnDel = createStyledButton("- Xóa", new Color(220, 53, 69));
        btnDel.addActionListener(e -> {
            int row = dropTable.getSelectedRow();
            if (row >= 0) dropTableModel.removeRow(row);
        });

        JButton btnSave = createStyledButton("LƯU CẤU HÌNH", new Color(46, 204, 113));
        btnSave.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSave.addActionListener(e -> saveAction());

        btnRow.add(btnAdd);
        btnRow.add(btnDel);
        btnRow.add(Box.createHorizontalStrut(20));
        btnRow.add(btnSave);

        // Chú thích
        JLabel lblHint = new JLabel("<html><small>Tỉ lệ: <font color='#9b59b6'>1-4% Cực hiếm</font> | " +
                "<font color='#e74c3c'>5-19% Hiếm</font> | <font color='#f39c12'>20-49% TB</font> | " +
                "<font color='#27ae60'>50%+ Cao</font></small></html>");
        lblHint.setBorder(new EmptyBorder(5, 0, 0, 0));

        dropCard.add(lblDropTitle, BorderLayout.NORTH);
        dropCard.add(scrollDrop, BorderLayout.CENTER);
        JPanel bottomDrop = new JPanel(new BorderLayout());
        bottomDrop.setOpaque(false);
        bottomDrop.add(btnRow, BorderLayout.NORTH);
        bottomDrop.add(lblHint, BorderLayout.SOUTH);
        dropCard.add(bottomDrop, BorderLayout.SOUTH);

        leftPanel.add(dropCard, BorderLayout.CENTER);

        // --- PANEL PHẢI: TỔNG QUAN TẤT CẢ BOSS ---
        JPanel rightPanel = new JPanel(new BorderLayout(0, 10));
        rightPanel.setBackground(Color.WHITE);
        rightPanel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(220, 220, 220), 1, true),
                new EmptyBorder(15, 15, 15, 15)));

        JLabel lblOverview = new JLabel("TỔNG QUAN TẤT CẢ BOSS");
        lblOverview.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblOverview.setForeground(new Color(44, 62, 80));

        txtSearchOverview = new JTextField();
        txtSearchOverview.setPreferredSize(new Dimension(0, 30));
        txtSearchOverview.setBorder(BorderFactory.createTitledBorder("Lọc nhanh Boss..."));
        txtSearchOverview.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filterOverview(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filterOverview(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filterOverview(); }
        });

        JPanel topRight = new JPanel(new BorderLayout(10, 5));
        topRight.setOpaque(false);
        topRight.add(lblOverview, BorderLayout.WEST);
        topRight.add(txtSearchOverview, BorderLayout.CENTER);

        String[] overviewCols = {"Key", "Tên Boss", "Số Item", "Chi tiết Drop"};
        overviewModel = new DefaultTableModel(overviewCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        overviewTable = new JTable(overviewModel);
        styleTable(overviewTable);
        overviewTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        overviewTable.getColumnModel().getColumn(0).setMaxWidth(60);
        overviewTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        overviewTable.getColumnModel().getColumn(2).setPreferredWidth(60);
        overviewTable.getColumnModel().getColumn(2).setMaxWidth(80);

        // Double click để chọn boss
        overviewTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = overviewTable.convertRowIndexToModel(overviewTable.getSelectedRow());
                    String bossKey = overviewModel.getValueAt(row, 0).toString();
                    for (int i = 0; i < cbBossList.getItemCount(); i++) {
                        if (cbBossList.getItemAt(i).key.equals(bossKey)) {
                            cbBossList.setSelectedIndex(i);
                            break;
                        }
                    }
                }
            }
        });

        JScrollPane scrollOverview = new JScrollPane(overviewTable);
        scrollOverview.getViewport().setBackground(Color.WHITE);

        rightPanel.add(topRight, BorderLayout.NORTH);
        rightPanel.add(scrollOverview, BorderLayout.CENTER);

        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.CENTER);

        loadDropsForSelectedBoss();
        refreshOverview();
    }

    // ================ HELPER ================

    private JLabel createLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        return lbl;
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(8, 16, 8, 16));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        return btn;
    }

    private void styleTable(JTable table) {
        table.setRowHeight(28);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(236, 240, 241));
        table.setSelectionBackground(new Color(52, 152, 219, 50));
        table.setSelectionForeground(Color.BLACK);
        table.setShowGrid(true);
        table.setGridColor(new Color(235, 235, 235));
    }

    private String getItemName(int itemId) {
        if (Manager.ITEM_TEMPLATES != null) {
            for (ItemTemplate t : Manager.ITEM_TEMPLATES) {
                if (t.id == itemId) return t.name;
            }
        }
        return "ID=" + itemId;
    }

    // ================ DATA ================

    private void loadBossListFromReflection() {
        cbBossList.removeAllItems();
        try {
            Field[] fields = BossesData.class.getFields();
            Map<String, BossComboItem> sortedMap = new TreeMap<>();
            for (Field field : fields) {
                if (field.getType() == BossData.class) {
                    BossData data = (BossData) field.get(null);
                    if (data != null) {
                        int bossId = -1;
                        try {
                            Field idField = BossID.class.getField(field.getName());
                            bossId = idField.getInt(null);
                        } catch (Exception e) { bossId = 999; }
                        sortedMap.put(field.getName(), new BossComboItem(bossId, field.getName(), data.getName()));
                    }
                }
            }
            for (BossComboItem item : sortedMap.values()) cbBossList.addItem(item);
        } catch (Exception e) { e.printStackTrace(); }
    }

    /**
     * Load drops cho boss đang chọn từ Manager.BOSS_REWARD_PANEL
     * Format: itemId-qty-rate,itemId-qty-rate,...
     * Tương thích: itemId-qty (mặc định rate=30)
     */
    private void loadDropsForSelectedBoss() {
        dropTableModel.setRowCount(0);
        BossComboItem selected = (BossComboItem) cbBossList.getSelectedItem();
        if (selected == null) return;

        String items = Manager.BOSS_REWARD_PANEL.get(selected.key);
        if (items == null || items.isEmpty()) return;

        String[] entries = items.split(",");
        for (String entry : entries) {
            try {
                String[] parts = entry.trim().split("-");
                int itemId = Integer.parseInt(parts[0]);
                int qty = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                int rate = parts.length > 2 ? Integer.parseInt(parts[2]) : 30; // mặc định 30%
                dropTableModel.addRow(new Object[]{itemId, getItemName(itemId), qty, rate});
            } catch (Exception e) {
                System.err.println("Lỗi parse drop entry: " + entry);
            }
        }
    }

    private void saveAction() {
        BossComboItem selected = (BossComboItem) cbBossList.getSelectedItem();
        if (selected == null) return;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < dropTableModel.getRowCount(); i++) {
            int itemId = Integer.parseInt(dropTableModel.getValueAt(i, 0).toString());
            int qty = Integer.parseInt(dropTableModel.getValueAt(i, 2).toString());
            int rate = Integer.parseInt(dropTableModel.getValueAt(i, 3).toString());
            if (rate < 1) rate = 1;
            if (rate > 100) rate = 100;
            if (i > 0) sb.append(",");
            sb.append(itemId).append("-").append(qty).append("-").append(rate);
        }

        String result = sb.toString();
        if (result.isEmpty()) {
            Manager.BOSS_REWARD_PANEL.remove(selected.key);
        } else {
            Manager.BOSS_REWARD_PANEL.put(selected.key, result);
        }
        Manager.saveBossRewardConfig();
        refreshOverview();
        JOptionPane.showMessageDialog(this,
                "Đã lưu cấu hình drop cho: " + selected.name + "\n" +
                "Tổng " + dropTableModel.getRowCount() + " item(s)",
                "Thành công", JOptionPane.INFORMATION_MESSAGE);
    }

    private void refreshOverview() {
        overviewModel.setRowCount(0);
        for (Map.Entry<String, String> entry : Manager.BOSS_REWARD_PANEL.entrySet()) {
            String bossName = "?";
            String bossKey = entry.getKey();
            for (int i = 0; i < cbBossList.getItemCount(); i++) {
                if (cbBossList.getItemAt(i).key.equals(bossKey)) {
                    bossName = cbBossList.getItemAt(i).name;
                    break;
                }
            }

            String raw = entry.getValue();
            String[] parts = raw.split(",");
            int count = parts.length;

            // Build chi tiết
            StringBuilder detail = new StringBuilder();
            for (String p : parts) {
                try {
                    String[] s = p.trim().split("-");
                    int id = Integer.parseInt(s[0]);
                    int qty = s.length > 1 ? Integer.parseInt(s[1]) : 1;
                    int rate = s.length > 2 ? Integer.parseInt(s[2]) : 30;
                    String name = getItemName(id);
                    if (detail.length() > 0) detail.append(" | ");
                    detail.append(name).append(" x").append(qty).append(" (").append(rate).append("%)");
                } catch (Exception e) {}
            }

            overviewModel.addRow(new Object[]{bossKey, bossName, count, detail.toString()});
        }
    }

    private void filterOverview() {
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(overviewModel);
        overviewTable.setRowSorter(sorter);
        String text = txtSearchOverview.getText().trim();
        if (text.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
        }
    }

    // ================ DIALOG THÊM ITEM ================

    private void openItemSearchDialog() {
        JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Kho vật phẩm hệ thống", true);
        d.setSize(650, 700);
        d.setLocationRelativeTo(this);
        d.setLayout(new BorderLayout(0, 5));

        DefaultTableModel m = new DefaultTableModel(new String[]{"ID", "Tên Item"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable t = new JTable(m);
        styleTable(t);

        for (ItemTemplate it : Manager.ITEM_TEMPLATES) m.addRow(new Object[]{it.id, it.name});

        JTextField fSearch = new JTextField();
        fSearch.setBorder(BorderFactory.createTitledBorder("Tìm tên hoặc ID..."));
        TableRowSorter<DefaultTableModel> s = new TableRowSorter<>(m);
        t.setRowSorter(s);
        fSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { doFilter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { doFilter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { doFilter(); }
            void doFilter() { s.setRowFilter(RowFilter.regexFilter("(?i)" + fSearch.getText())); }
        });

        t.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = t.convertRowIndexToModel(t.getSelectedRow());
                    int id = (int) m.getValueAt(row, 0);
                    String name = m.getValueAt(row, 1).toString();

                    JTextField txtQty = new JTextField("1");
                    JTextField txtRate = new JTextField("30");
                    JPanel inputPanel = new JPanel(new GridLayout(2, 2, 5, 5));
                    inputPanel.add(new JLabel("Số lượng:"));
                    inputPanel.add(txtQty);
                    inputPanel.add(new JLabel("Tỉ lệ rơi (%):"));
                    inputPanel.add(txtRate);

                    int result = JOptionPane.showConfirmDialog(d, inputPanel,
                            "Thêm: " + name, JOptionPane.OK_CANCEL_OPTION);

                    if (result == JOptionPane.OK_OPTION) {
                        try {
                            int qty = Integer.parseInt(txtQty.getText().trim());
                            int rate = Integer.parseInt(txtRate.getText().trim());
                            if (qty < 1) qty = 1;
                            if (rate < 1) rate = 1;
                            if (rate > 100) rate = 100;
                            dropTableModel.addRow(new Object[]{id, name, qty, rate});
                            d.dispose();
                        } catch (NumberFormatException ex) {
                            JOptionPane.showMessageDialog(d, "Vui lòng nhập số hợp lệ!");
                        }
                    }
                }
            }
        });

        d.add(fSearch, BorderLayout.NORTH);
        d.add(new JScrollPane(t), BorderLayout.CENTER);

        JLabel hint = new JLabel("  Double click vào item để thêm. Nhập số lượng + tỉ lệ rơi.");
        hint.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        hint.setForeground(Color.GRAY);
        d.add(hint, BorderLayout.SOUTH);

        d.setVisible(true);
    }

    // ================ INNER CLASS ================

    private static class BossComboItem {
        int id; String key; String name;
        public BossComboItem(int id, String key, String name) { this.id = id; this.key = key; this.name = name; }
        @Override public String toString() { return "[" + id + "] " + name; }
    }
}