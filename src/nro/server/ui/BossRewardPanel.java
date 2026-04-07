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
import java.lang.reflect.Field;
import java.util.*;
import java.util.List;

public class BossRewardPanel extends JPanel {

    private JComboBox<BossComboItem> cbBossList;
    private JTextArea txtItemList;
    private DefaultTableModel tableModel;
    private JTable rewardTable;
    private JTextField txtSearchTable;

    public BossRewardPanel() {
        setLayout(new BorderLayout(15, 15));
        setBackground(new Color(240, 242, 245)); // Màu nền xám nhạt hiện đại
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // --- PANEL TRÁI: KHU VỰC ĐIỀU KHIỂN ---
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BorderLayout(0, 15));
        leftPanel.setOpaque(false);
        leftPanel.setPreferredSize(new Dimension(420, 0));

        // Form nhập liệu
        JPanel cardPanel = new JPanel(new GridBagLayout());
        cardPanel.setBackground(Color.WHITE);
        cardPanel.setBorder(new LineBorder(new Color(220, 220, 220), 1, true));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 15, 10, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Tiêu đề nhỏ
        JLabel lblTitle = new JLabel("CẤU HÌNH PHẦN THƯỞNG");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitle.setForeground(new Color(52, 73, 94));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        cardPanel.add(lblTitle, gbc);

        // Chọn Boss
        gbc.gridy = 1; gbc.gridwidth = 1;
        cardPanel.add(new JLabel("Chọn Boss mục tiêu:"), gbc);
        
        cbBossList = new JComboBox<>();
        cbBossList.setPreferredSize(new Dimension(0, 35));
        cbBossList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        loadBossListFromReflection();
        cbBossList.addActionListener(e -> syncTextFromSelectedBoss());
        gbc.gridx = 1;
        cardPanel.add(cbBossList, gbc);

        // Danh sách Item
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        cardPanel.add(new JLabel("Danh sách ID vật phẩm rơi (ngăn cách bằng dấu phẩy):"), gbc);

        txtItemList = new JTextArea(8, 20);
        txtItemList.setLineWrap(true);
        txtItemList.setWrapStyleWord(true);
        txtItemList.setFont(new Font("Monospaced", Font.BOLD, 14));
        txtItemList.setBackground(new Color(248, 249, 250));
        txtItemList.setBorder(new EmptyBorder(5, 5, 5, 5));
        
        JScrollPane scrollArea = new JScrollPane(txtItemList);
        scrollArea.setBorder(new LineBorder(new Color(200, 200, 200)));
        gbc.gridy = 3;
        cardPanel.add(scrollArea, gbc);

        // Nút thêm Item
        JButton btnAddItem = createStyledButton("+ THÊM VẬT PHẨM TỪ KHO", new Color(52, 152, 219));
        btnAddItem.addActionListener(e -> openItemSearchDialog());
        gbc.gridy = 4;
        cardPanel.add(btnAddItem, gbc);

        // Nút Lưu
        JButton btnSave = createStyledButton("XÁC NHẬN LƯU CẤU HÌNH", new Color(46, 204, 113));
        btnSave.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSave.addActionListener(e -> saveAction());
        gbc.gridy = 5;
        cardPanel.add(btnSave, gbc);

        leftPanel.add(cardPanel, BorderLayout.NORTH);

        // --- PANEL PHẢI: BẢNG DỮ LIỆU ---
        JPanel rightPanel = new JPanel(new BorderLayout(0, 10));
        rightPanel.setBackground(Color.WHITE);
        rightPanel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(220, 220, 220), 1, true),
                new EmptyBorder(15, 15, 15, 15)
        ));

        // Thanh tìm kiếm trong bảng
        JPanel searchPanel = new JPanel(new BorderLayout(10, 0));
        searchPanel.setOpaque(false);
        txtSearchTable = new JTextField();
        txtSearchTable.setPreferredSize(new Dimension(0, 30));
        txtSearchTable.setBorder(BorderFactory.createTitledBorder("Lọc nhanh Boss..."));
        searchPanel.add(txtSearchTable, BorderLayout.CENTER);
        
        JButton btnReload = new JButton("Làm mới bộ nhớ");
        btnReload.addActionListener(e -> refreshTable());
        searchPanel.add(btnReload, BorderLayout.EAST);

        // Thiết lập bảng
        String[] columns = {"ID Boss", "Tên Boss", "Vật Phẩm Rơi"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        rewardTable = new JTable(tableModel);
        styleTable(rewardTable);
        
        JScrollPane scrollTable = new JScrollPane(rewardTable);
        scrollTable.getViewport().setBackground(Color.WHITE);

        rightPanel.add(searchPanel, BorderLayout.NORTH);
        rightPanel.add(scrollTable, BorderLayout.CENTER);

        // Filter bảng
        txtSearchTable.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filterTable(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filterTable(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filterTable(); }
        });

        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.CENTER);
        
        refreshTable();
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(10, 20, 10, 20));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        return btn;
    }

    private void styleTable(JTable table) {
        table.setRowHeight(30);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setBackground(new Color(236, 240, 241));
        table.setSelectionBackground(new Color(52, 152, 219, 50));
        table.setSelectionForeground(Color.BLACK);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        
        // Căn giữa ID
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
    }

    private void syncTextFromSelectedBoss() {
        BossComboItem selected = (BossComboItem) cbBossList.getSelectedItem();
        if (selected != null) {
            String items = Manager.BOSS_REWARD_PANEL.get(selected.id);
            txtItemList.setText(items != null ? items : "");
        }
    }

    private void filterTable() {
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        rewardTable.setRowSorter(sorter);
        sorter.setRowFilter(RowFilter.regexFilter("(?i)" + txtSearchTable.getText()));
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (Map.Entry<Integer, String> entry : Manager.BOSS_REWARD_PANEL.entrySet()) {
            String bossName = "Không xác định";
            // Tìm tên từ combo box
            for (int i = 0; i < cbBossList.getItemCount(); i++) {
                if (cbBossList.getItemAt(i).id == entry.getKey()) {
                    bossName = cbBossList.getItemAt(i).name;
                    break;
                }
            }
            tableModel.addRow(new Object[]{entry.getKey(), bossName, entry.getValue()});
        }
    }

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

    private void saveAction() {
        BossComboItem selected = (BossComboItem) cbBossList.getSelectedItem();
        if (selected == null) return;
        try {
            int bossId = selected.id;
            String items = txtItemList.getText().trim();
            Manager.BOSS_REWARD_PANEL.put(bossId, items);
            Manager.saveBossRewardConfig();
            refreshTable();
            JOptionPane.showMessageDialog(this, "Đã cập nhật thưởng cho: " + selected.name, "Thành công", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + e.getMessage());
        }
    }

    private void openItemSearchDialog() {
        JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Kho vật phẩm hệ thống", true);
        d.setSize(600, 700);
        d.setLocationRelativeTo(this);
        d.setLayout(new BorderLayout());

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
        fSearch.addActionListener(e -> s.setRowFilter(RowFilter.regexFilter("(?i)" + fSearch.getText())));

        t.addMouseListener(new java.awt.event.MouseAdapter() {
    public void mouseClicked(java.awt.event.MouseEvent e) {
        if (e.getClickCount() == 2) {
            int row = t.convertRowIndexToModel(t.getSelectedRow());
            String id = m.getValueAt(row, 0).toString();
            String itemName = m.getValueAt(row, 1).toString();

            // Hiển thị hộp thoại nhập số lượng nhanh
            String quantity = JOptionPane.showInputDialog(d, 
                    "Nhập số lượng cho: " + itemName, 
                    "Số lượng", 
                    JOptionPane.QUESTION_MESSAGE);

            // Kiểm tra nếu người dùng không cancel và nhập số hợp lệ
            if (quantity != null && !quantity.trim().isEmpty()) {
                try {
                    int q = Integer.parseInt(quantity.trim());
                    if (q <= 0) q = 1; // Đảm bảo số lượng ít nhất là 1

                    String curr = txtItemList.getText().trim();
                    String newItem = id + "-" + q; // Định dạng ID-SốLượng
                    
                    txtItemList.setText(curr.isEmpty() ? newItem : curr + "," + newItem);
                    d.dispose(); // Đóng kho sau khi chọn xong
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(d, "Vui lòng chỉ nhập số nguyên!");
                }
            }
        }
    }
});

        d.add(fSearch, BorderLayout.NORTH);
        d.add(new JScrollPane(t), BorderLayout.CENTER);
        d.setVisible(true);
    }

    private static class BossComboItem {
        int id; String key; String name;
        public BossComboItem(int id, String key, String name) { this.id = id; this.key = key; this.name = name; }
        @Override public String toString() { return "[" + id + "] " + name; }
    }
}