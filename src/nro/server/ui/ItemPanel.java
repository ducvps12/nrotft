package nro.server.ui;

import jdbc.DBConnecter;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.List;

public class ItemPanel extends JPanel {

    private JTable table;
    private DefaultTableModel model;
    private TableRowSorter<DefaultTableModel> sorter;
    private JTextField txtSearch;
    private JComboBox<String> cbType;

    private final Map<Integer, ImageIcon> iconCache = new HashMap<>();
    private final Map<Integer, Boolean> noIconCache = new HashMap<>();

    private static final String ICON_FOLDER = "data/icon/";

    private static final int COL_ID = 0;
    private static final int COL_NAME = 2;
    private static final int COL_TYPE = 3;

    public ItemPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(245, 245, 245));
        setBorder(new EmptyBorder(15, 15, 15, 15));
        initUI();
        loadTypesFromDB();
        loadDataFromDB();
    }

    private Connection getConnection() throws SQLException {
        return DBConnecter.getConnectionServer();
    }

    private void initUI() {

        JLabel title = new JLabel("QUẢN LÝ ITEM & DATA");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(new Color(0, 102, 204));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        left.add(title);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        cbType = new JComboBox<>();
        cbType.setPreferredSize(new Dimension(140, 32));

        txtSearch = new JTextField(12);
        txtSearch.setPreferredSize(new Dimension(120, 32));

        JButton btnTool = createButton("Công Cụ Mở Rộng", new Color(0, 123, 255));
        JButton btnAdd = createButton("Thêm Item", new Color(40, 167, 69));
        JButton btnReload = createButton("Làm Mới", new Color(108, 117, 125));

        btnReload.addActionListener(e -> loadDataFromDB());
        btnAdd.addActionListener(e -> openAddItemDialog());
        btnTool.addActionListener(e -> openToolDialog());

        cbType.addActionListener(e -> search());

        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                search();
            }

            public void removeUpdate(DocumentEvent e) {
                search();
            }

            public void changedUpdate(DocumentEvent e) {
                search();
            }
        });

        right.add(new JLabel("Lọc Type"));
        right.add(cbType);
        right.add(txtSearch);
        right.add(btnTool);
        right.add(btnAdd);
        right.add(btnReload);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(left, BorderLayout.WEST);
        top.add(right, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);

        String[] cols = {
                "ID",
                "Icon",
                "Tên Item",
                "Loại",
                "Giới Tính",
                "Level",
                "Part",
                "Mô Tả"
        };

        model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }

            public Class<?> getColumnClass(int c) {
                switch (c) {
                    case 0:
                        return Integer.class;
                    case 1:
                        return ImageIcon.class;
                    case 3:
                        return Integer.class;
                    case 5:
                        return Integer.class;
                    case 6:
                        return Integer.class;
                    default:
                        return String.class;
                }
            }
        };

        table = new JTable(model);
        table.setRowHeight(36);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.setShowGrid(true);
        table.setGridColor(new Color(220, 220, 220));
        table.setIntercellSpacing(new Dimension(1, 1));
        table.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));

        table.getTableHeader().setBackground(new Color(235, 245, 255));
        table.getTableHeader().setReorderingAllowed(false);

        table.setSelectionBackground(new Color(230, 240, 255));
        table.setSelectionForeground(Color.BLACK);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(JLabel.CENTER);

        DefaultTableCellRenderer iconCenter = new DefaultTableCellRenderer() {
            protected void setValue(Object value) {
                setText("");
                setIcon(value instanceof ImageIcon ? (ImageIcon) value : null);
                setHorizontalAlignment(JLabel.CENTER);
            }
        };

        table.getColumnModel().getColumn(0).setMaxWidth(60);
        table.getColumnModel().getColumn(0).setCellRenderer(center);
        table.getColumnModel().getColumn(1).setMaxWidth(60);
        table.getColumnModel().getColumn(1).setCellRenderer(iconCenter);

        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.getSelectedRow();
                    if (row != -1)
                        openEditor(table.convertRowIndexToModel(row));
                }
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));

        JPanel tableWrapper = new JPanel(new BorderLayout());
        tableWrapper.setOpaque(true);
        tableWrapper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                new EmptyBorder(10, 10, 10, 10)));

        tableWrapper.add(scroll, BorderLayout.CENTER);

        add(tableWrapper, BorderLayout.CENTER);
    }

    private JButton createButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void loadTypesFromDB() {
        try (Connection conn = getConnection();
                Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery("SELECT DISTINCT type FROM item_template ORDER BY type")) {

            cbType.removeAllItems();
            cbType.addItem("Tất cả Type");
            while (rs.next())
                cbType.addItem(String.valueOf(rs.getInt(1)));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadDataFromDB() {
        model.setRowCount(0);
        new Thread(() -> {
            try (Connection conn = getConnection();
                    Statement st = conn.createStatement();
                    ResultSet rs = st.executeQuery("SELECT * FROM item_template ORDER BY id ASC")) {

                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    row.add(rs.getInt("id"));
                    row.add(getIcon(rs.getInt("icon_id")));
                    row.add(rs.getString("name"));
                    row.add(rs.getInt("type"));
                    row.add(getGender(rs.getInt("gender")));
                    row.add(rs.getInt("level"));
                    row.add(rs.getInt("part"));
                    row.add(rs.getString("description"));
                    SwingUtilities.invokeLater(() -> model.addRow(row));
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void openToolDialog() {
        JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Công Cụ Mở Rộng", true);
        d.setSize(350, 200);
        d.setLocationRelativeTo(this);
        d.setLayout(new GridLayout(3, 1, 10, 10));

        JButton b1 = createButton("Quản lý Part", new Color(40, 167, 69));
        JButton b2 = createButton("Quản lý Head Avatar", new Color(0, 123, 255));
        JButton b3 = createButton("Quản lý Head Frames", new Color(255, 193, 7));

        b1.addActionListener(e -> openPartManager());
        b2.addActionListener(e -> openHeadAvatarManager());
        b3.addActionListener(e -> openHeadFrameManager());

        d.add(b1);
        d.add(b2);
        d.add(b3);
        d.setVisible(true);
    }

    private void openPartManager() {
        JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Quản lý Part", true);

        DefaultTableModel m = new DefaultTableModel(
                new String[] { "ID", "Type", "Data JSON" }, 0) {
            public boolean isCellEditable(int r, int c) {
                return c != 0;
            }
        };

        JTable t = new JTable(m);
        t.setRowHeight(28);

        try (Connection c = getConnection();
                Statement s = c.createStatement();
                ResultSet rs = s.executeQuery("SELECT * FROM part ORDER BY id ASC")) {

            while (rs.next()) {
                m.addRow(new Object[] {
                        rs.getInt("id"),
                        rs.getInt("type"),
                        rs.getString("data")
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        JButton add = new JButton("Thêm");
        JButton del = new JButton("Xóa");
        JButton save = new JButton("Lưu tất cả");

        add.addActionListener(e -> m.addRow(new Object[] { 0, 0, "[]" }));

        del.addActionListener(e -> {
            int r = t.getSelectedRow();
            if (r != -1)
                m.removeRow(r);
        });

        save.addActionListener(e -> {
            try (Connection c = getConnection()) {
                c.setAutoCommit(false);

                try (Statement clear = c.createStatement()) {
                    clear.executeUpdate("DELETE FROM part");
                }

                try (PreparedStatement insert = c.prepareStatement(
                        "INSERT INTO part(id,type,data) VALUES(?,?,?)")) {

                    for (int i = 0; i < m.getRowCount(); i++) {
                        insert.setInt(1, Integer.parseInt(m.getValueAt(i, 0).toString().trim()));
                        insert.setInt(2, Integer.parseInt(m.getValueAt(i, 1).toString().trim()));
                        insert.setString(3, m.getValueAt(i, 2) == null ? "" : m.getValueAt(i, 2).toString());
                        insert.addBatch();
                    }

                    insert.executeBatch();
                }

                c.commit();
                JOptionPane.showMessageDialog(d, "Đã lưu toàn bộ dữ liệu!");

            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(d, "Lưu thất bại!");
            }
        });

        d.setLayout(new BorderLayout());
        d.add(new JScrollPane(t), BorderLayout.CENTER);

        JPanel p = new JPanel();
        p.add(add);
        p.add(del);
        p.add(save);

        d.add(p, BorderLayout.SOUTH);
        d.setSize(750, 500);
        d.setLocationRelativeTo(this);
        d.setVisible(true);
    }

    private void openHeadFrameManager() {
        openSimpleJsonManager(
                "SELECT id,data FROM array_head_2_frames ORDER BY id ASC",
                "DELETE FROM array_head_2_frames",
                "INSERT INTO array_head_2_frames(id,data) VALUES(?,?)",
                new String[] { "ID", "Data Array" });
    }

    private void openSimpleJsonManager(String select, String deleteAll, String insert, String[] cols) {

        JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Manager", true);

        DefaultTableModel m = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) {
                return true;
            }
        };

        JTable t = new JTable(m);

        try (Connection c = getConnection();
                Statement s = c.createStatement();
                ResultSet rs = s.executeQuery(select)) {

            while (rs.next()) {
                Vector<Object> v = new Vector<>();
                for (int i = 1; i <= cols.length; i++)
                    v.add(rs.getObject(i));
                m.addRow(v);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        JButton add = new JButton("Thêm");
        JButton del = new JButton("Xóa");
        JButton save = new JButton("Lưu tất cả");

        add.addActionListener(e -> m.addRow(new Object[] { 0, "[]" }));

        del.addActionListener(e -> {
            int r = t.getSelectedRow();
            if (r != -1)
                m.removeRow(r);
        });

        save.addActionListener(e -> {
            try (Connection c = getConnection()) {
                c.setAutoCommit(false);

                try (Statement clear = c.createStatement()) {
                    clear.executeUpdate(deleteAll);
                }

                try (PreparedStatement ps = c.prepareStatement(insert)) {
                    for (int i = 0; i < m.getRowCount(); i++) {
                        ps.setInt(1, Integer.parseInt(m.getValueAt(i, 0).toString().trim()));
                        ps.setString(2, m.getValueAt(i, 1) == null ? "" : m.getValueAt(i, 1).toString());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                c.commit();
                JOptionPane.showMessageDialog(d, "Đã lưu!");

            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(d, "Lưu thất bại!");
            }
        });

        d.setLayout(new BorderLayout());
        d.add(new JScrollPane(t), BorderLayout.CENTER);

        JPanel p = new JPanel();
        p.add(add);
        p.add(del);
        p.add(save);

        d.add(p, BorderLayout.SOUTH);
        d.setSize(700, 500);
        d.setLocationRelativeTo(this);
        d.setVisible(true);
    }

    private void openHeadAvatarManager() {
        JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Head Avatar", true);

        DefaultTableModel m = new DefaultTableModel(
                new String[] { "Head ID", "Avatar Icon", "Preview" }, 0) {
            public boolean isCellEditable(int r, int c) {
                return c == 1;
            }

            public Class<?> getColumnClass(int c) {
                return c == 2 ? ImageIcon.class : Object.class;
            }
        };

        JTable t = new JTable(m);
        t.setRowHeight(40);

        try (Connection c = getConnection();
                Statement s = c.createStatement();
                ResultSet rs = s.executeQuery("SELECT head_id,avatar_id FROM head_avatar ORDER BY head_id ASC")) {

            while (rs.next()) {
                int id = rs.getInt(1);
                int icon = rs.getInt(2);
                m.addRow(new Object[] { id, icon, getIcon(icon) });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        JButton add = new JButton("Thêm");
        JButton del = new JButton("Xóa");
        JButton save = new JButton("Lưu tất cả");

        add.addActionListener(e -> m.addRow(new Object[] { 0, 0, null }));

        del.addActionListener(e -> {
            int r = t.getSelectedRow();
            if (r != -1)
                m.removeRow(r);
        });

        save.addActionListener(e -> {
            try (Connection c = getConnection()) {
                c.setAutoCommit(false);

                try (Statement clear = c.createStatement()) {
                    clear.executeUpdate("DELETE FROM head_avatar");
                }

                try (PreparedStatement insert = c.prepareStatement(
                        "INSERT INTO head_avatar(head_id,avatar_id) VALUES(?,?)")) {

                    for (int i = 0; i < m.getRowCount(); i++) {
                        int id = Integer.parseInt(m.getValueAt(i, 0).toString().trim());
                        int icon = Integer.parseInt(m.getValueAt(i, 1).toString().trim());
                        insert.setInt(1, id);
                        insert.setInt(2, icon);
                        insert.addBatch();
                        m.setValueAt(getIcon(icon), i, 2);
                    }

                    insert.executeBatch();
                }

                c.commit();
                JOptionPane.showMessageDialog(d, "Đã lưu!");

            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(d, "Lưu thất bại!");
            }
        });

        d.setLayout(new BorderLayout());
        d.add(new JScrollPane(t), BorderLayout.CENTER);

        JPanel p = new JPanel();
        p.add(add);
        p.add(del);
        p.add(save);

        d.add(p, BorderLayout.SOUTH);
        d.setSize(600, 450);
        d.setLocationRelativeTo(this);
        d.setVisible(true);
    }

    private void openEditor(Integer rowIndex) {

        if (rowIndex == null)
            return;

        JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Chỉnh Sửa Item", true);
        d.setLayout(new BorderLayout());
        d.setSize(820, 620);
        d.setLocationRelativeTo(this);

        Font inputFont = new Font("Segoe UI", Font.PLAIN, 13);

        JPanel main = new JPanel(new GridBagLayout());
        main.setBorder(BorderFactory.createEmptyBorder(20, 30, 10, 30));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.fill = GridBagConstraints.HORIZONTAL;

        // ===== LOAD DATA FROM TABLE =====
        JTextField id = new JTextField(model.getValueAt(rowIndex, 0).toString());
        id.setEditable(false);

        JTextField name = new JTextField(model.getValueAt(rowIndex, 2).toString());
        JTextArea description = new JTextArea(model.getValueAt(rowIndex, 7).toString(), 4, 20);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);

        JTextField type = new JTextField(model.getValueAt(rowIndex, 3).toString());
        JTextField level = new JTextField(model.getValueAt(rowIndex, 5).toString());
        JTextField part = new JTextField(model.getValueAt(rowIndex, 6).toString());

        // ===== ICON AUTO LOAD =====
        JTextField icon = new JTextField();
        icon.setEditable(false);

        // ===== GENDER DROPDOWN =====
        String[] genderNames = { "Trái đất", "Namek", "Xayda", "Tất cả" };
        JComboBox<String> genderBox = new JComboBox<>(genderNames);

        String genderText = model.getValueAt(rowIndex, 4).toString().toLowerCase();
        int genderValue = 0;

        if (genderText.contains("trái"))
            genderValue = 0;
        else if (genderText.contains("namek"))
            genderValue = 1;
        else if (genderText.contains("xayda"))
            genderValue = 2;

        genderBox.setSelectedIndex(genderValue);

        JTextField power = new JTextField("0");
        JTextField gold = new JTextField("0");
        JTextField gem = new JTextField("0");
        JTextField head = new JTextField("-1");
        JTextField body = new JTextField("-1");
        JTextField leg = new JTextField("-1");

        JCheckBox isUpToUp = new JCheckBox("Cộng dồn (UpToUp)");
        JCheckBox canTrade = new JCheckBox("Có thể giao dịch");
        JCheckBox isUpToUp99 = new JCheckBox("Up to up > 99");

        JTextField[] fields = {
                id, name, type, level, part, icon,
                power, gold, gem, head, body, leg
        };

        for (JTextField f : fields) {
            f.setFont(inputFont);
            f.setPreferredSize(new Dimension(120, 32));
        }

        genderBox.setFont(inputFont);
        genderBox.setPreferredSize(new Dimension(120, 32));
        description.setFont(inputFont);

        // ===== LOAD ICON FROM DB =====
        try (Connection conn = getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT icon_id, gender, is_up_to_up, can_trade, is_up_to_up_over_99 " +
                            "FROM item_template WHERE id=?");

            ps.setInt(1, Integer.parseInt(id.getText()));
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                icon.setText(String.valueOf(rs.getInt("icon_id")));

                // gender từ DB -> dropdown
                int gDb = rs.getInt("gender");
if (gDb >= 0 && gDb < genderBox.getItemCount()) {
    genderBox.setSelectedIndex(gDb);
} else {
    genderBox.setSelectedIndex(0);
}

                // checkbox theo DB
                isUpToUp.setSelected(rs.getInt("is_up_to_up") == 1);
                canTrade.setSelected(rs.getInt("can_trade") == 1);
                isUpToUp99.setSelected(rs.getInt("is_up_to_up_over_99") == 1);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        int row = 0;

        g.gridx = 0;
        g.gridy = row;
        main.add(new JLabel("ID:", SwingConstants.RIGHT), g);
        g.gridx = 1;
        g.gridwidth = 3;
        main.add(id, g);
        g.gridwidth = 1;
        row++;

        g.gridx = 0;
        g.gridy = row;
        main.add(new JLabel("Tên Item:", SwingConstants.RIGHT), g);
        g.gridx = 1;
        g.gridwidth = 3;
        main.add(name, g);
        g.gridwidth = 1;
        row++;

        g.gridx = 0;
        g.gridy = row;
        main.add(new JLabel("Mô tả:", SwingConstants.RIGHT), g);
        g.gridx = 1;
        g.gridwidth = 3;
        main.add(new JScrollPane(description), g);
        g.gridwidth = 1;
        row++;

        g.gridx = 0;
        g.gridy = row;
        main.add(new JLabel("Icon ID:", SwingConstants.RIGHT), g);
        g.gridx = 1;
        g.gridwidth = 3;
        main.add(icon, g);
        g.gridwidth = 1;
        row++;

        g.gridx = 0;
        g.gridy = row;
        main.add(new JLabel("Type:", SwingConstants.RIGHT), g);
        g.gridx = 1;
        main.add(type, g);
        g.gridx = 2;
        main.add(new JLabel("Gender:", SwingConstants.RIGHT), g);
        g.gridx = 3;
        main.add(genderBox, g);
        row++;

        g.gridx = 0;
        g.gridy = row;
        main.add(new JLabel("Level:", SwingConstants.RIGHT), g);
        g.gridx = 1;
        main.add(level, g);
        g.gridx = 2;
        main.add(new JLabel("Part:", SwingConstants.RIGHT), g);
        g.gridx = 3;
        main.add(part, g);
        row++;

        g.gridx = 0;
        g.gridy = row;
        main.add(new JLabel("Power:", SwingConstants.RIGHT), g);
        g.gridx = 1;
        main.add(power, g);
        g.gridx = 2;
        main.add(new JLabel("Gold:", SwingConstants.RIGHT), g);
        g.gridx = 3;
        main.add(gold, g);
        row++;

        g.gridx = 0;
        g.gridy = row;
        main.add(new JLabel("Gem:", SwingConstants.RIGHT), g);
        g.gridx = 1;
        main.add(gem, g);
        row++;

        JPanel checkPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        checkPanel.add(isUpToUp);
        checkPanel.add(canTrade);
        checkPanel.add(isUpToUp99);

        g.gridx = 0;
        g.gridy = row;
        g.gridwidth = 4;
        main.add(checkPanel, g);
        g.gridwidth = 1;
        row++;

        JButton save = new JButton("CẬP NHẬT");
        save.setBackground(new Color(0, 123, 255));
        save.setForeground(Color.WHITE);
        save.setFont(new Font("Segoe UI", Font.BOLD, 14));
        save.setPreferredSize(new Dimension(200, 40));

        JPanel bottom = new JPanel();
        bottom.add(save);

        save.addActionListener(e -> {
            new Thread(() -> {
                try (Connection conn = getConnection()) {

                    PreparedStatement ps = conn.prepareStatement(
                            "UPDATE item_template SET " +
                                    "name=?, type=?, gender=?, level=?, part=?, icon_id=?, description=?, " +
                                    "power_require=?, gold=?, gem=?, head=?, body=?, leg=?, " +
                                    "is_up_to_up=?, can_trade=?, is_up_to_up_over_99=? " +
                                    "WHERE id=?");

                    ps.setString(1, name.getText());
                    ps.setInt(2, parseIntSafe(type.getText()));
                    ps.setInt(3, genderBox.getSelectedIndex()); // lưu 0 1 2
                    ps.setInt(4, parseIntSafe(level.getText()));
                    ps.setInt(5, parseIntSafe(part.getText()));
                    ps.setInt(6, parseIntSafe(icon.getText()));
                    ps.setString(7, description.getText());
                    ps.setInt(8, parseIntSafe(power.getText()));
                    ps.setInt(9, parseIntSafe(gold.getText()));
                    ps.setInt(10, parseIntSafe(gem.getText()));
                    ps.setInt(11, parseIntSafe(head.getText()));
                    ps.setInt(12, parseIntSafe(body.getText()));
                    ps.setInt(13, parseIntSafe(leg.getText()));
                    ps.setInt(14, isUpToUp.isSelected() ? 1 : 0);
                    ps.setInt(15, canTrade.isSelected() ? 1 : 0);
                    ps.setInt(16, isUpToUp99.isSelected() ? 1 : 0);
                    ps.setInt(17, parseIntSafe(id.getText()));

                    ps.executeUpdate();

                    SwingUtilities.invokeLater(() -> {
                        loadDataFromDB();
                        d.dispose();
                    });

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }).start();
        });

        d.add(main, BorderLayout.CENTER);
        d.add(bottom, BorderLayout.SOUTH);
        d.setVisible(true);
    }

    private ImageIcon getIcon(int iconId) {
        if (iconCache.containsKey(iconId))
            return iconCache.get(iconId);
        if (noIconCache.containsKey(iconId))
            return null;
        try {
            String[] zoom = { "x4", "x3", "x2", "x1" };
            for (String z : zoom) {
                File f = new File(ICON_FOLDER + z + "/" + iconId + ".png");
                if (f.exists()) {
                    BufferedImage img = ImageIO.read(f);
                    Image d = img.getScaledInstance(26, 26, Image.SCALE_SMOOTH);
                    ImageIcon ic = new ImageIcon(d);
                    iconCache.put(iconId, ic);
                    return ic;
                }
            }
        } catch (Exception ignored) {
        }
        noIconCache.put(iconId, true);
        return null;
    }

    private String getGender(int g) {
        if (g == 0)
            return "Trái Đất";
        if (g == 1)
            return "Namec";
        if (g == 2)
            return "Xayda";
        return "All";
    }

    private void search() {
        List<RowFilter<Object, Object>> filters = new ArrayList<>();

        String key = txtSearch.getText().trim();
        if (!key.isEmpty()) {
            RowFilter<Object, Object> f1 = RowFilter.regexFilter("(?i)" + key, COL_ID);
            RowFilter<Object, Object> f2 = RowFilter.regexFilter("(?i)" + key, COL_NAME);
            filters.add(RowFilter.orFilter(Arrays.asList(f1, f2)));
        }

        if (cbType.getSelectedIndex() > 0) {
            int t = Integer.parseInt(cbType.getSelectedItem().toString());
            filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL, t, COL_TYPE));
        }

        sorter.setRowFilter(filters.isEmpty() ? null : RowFilter.andFilter(filters));
    }

    private int parseIntSafe(String s) {
        if (s == null || s.trim().isEmpty())
            return 0;
        return Integer.parseInt(s.trim());
    }

    private void openAddItemDialog() {

        JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Thêm Item Mới", true);
        d.setLayout(new BorderLayout());
        d.setSize(820, 620);
        d.setLocationRelativeTo(this);

        Font labelFont = new Font("Segoe UI", Font.PLAIN, 13);
        Font inputFont = new Font("Segoe UI", Font.PLAIN, 13);

        JPanel main = new JPanel(new GridBagLayout());
        main.setBorder(BorderFactory.createEmptyBorder(20, 30, 10, 30));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.fill = GridBagConstraints.HORIZONTAL;

        JTextField idStart = new JTextField("0");
        JTextField amount = new JTextField("1");

        JTextField name = new JTextField();
        JTextArea description = new JTextArea(4, 20);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);

        JTextField icon = new JTextField("0");
        JTextField part = new JTextField("-1");
        JTextField type = new JTextField("0");

        JComboBox<String> gender = new JComboBox<>(new String[] {
                "0 - Trái Đất",
                "1 - Namec",
                "2 - Xayda",
                "3 - Tất cả"
        });

        JTextField level = new JTextField("0");
        JTextField power = new JTextField("0");
        JTextField gold = new JTextField("0");
        JTextField gem = new JTextField("0");

        JTextField head = new JTextField("-1");
        JTextField body = new JTextField("-1");
        JTextField leg = new JTextField("-1");

        JCheckBox isUpToUp = new JCheckBox("Cộng dồn (UpToUp)");
        JCheckBox canTrade = new JCheckBox("Có thể giao dịch");
        JCheckBox isUpToUp99 = new JCheckBox("Up to up > 99");

        JTextField[] fields = {
                idStart, amount, name, icon, part, type,
                level, power, gold, gem, head, body, leg
        };

        for (JTextField f : fields) {
            f.setFont(inputFont);
            f.setPreferredSize(new Dimension(120, 32));
        }

        gender.setFont(inputFont);
        description.setFont(inputFont);

        int row = 0;

        g.gridx = 0;
        g.gridy = row;
        main.add(new JLabel("ID Bắt đầu:", SwingConstants.RIGHT), g);
        g.gridx = 1;
        main.add(idStart, g);
        g.gridx = 2;
        main.add(new JLabel("Số lượng tạo:", SwingConstants.RIGHT), g);
        g.gridx = 3;
        main.add(amount, g);
        row++;

        g.gridx = 0;
        g.gridy = row;
        main.add(new JLabel("Tên Item (NAME):", SwingConstants.RIGHT), g);
        g.gridx = 1;
        g.gridwidth = 3;
        main.add(name, g);
        g.gridwidth = 1;
        row++;

        g.gridx = 0;
        g.gridy = row;
        main.add(new JLabel("Mô tả (Description):", SwingConstants.RIGHT), g);
        g.gridx = 1;
        g.gridwidth = 3;
        main.add(new JScrollPane(description), g);
        g.gridwidth = 1;
        row++;

        g.gridx = 0;
        g.gridy = row;
        main.add(new JLabel("Icon ID & View:", SwingConstants.RIGHT), g);
        g.gridx = 1;
        g.gridwidth = 2;
        main.add(icon, g);
        g.gridwidth = 1;
        g.gridx = 3;
        main.add(new JLabel(), g);
        row++;

        g.gridx = 0;
        g.gridy = row;
        main.add(new JLabel("Part (Vẽ hình):", SwingConstants.RIGHT), g);
        g.gridx = 1;
        g.gridwidth = 3;
        main.add(part, g);
        g.gridwidth = 1;
        row++;

        g.gridx = 0;
        g.gridy = row;
        main.add(new JLabel("Loại (TYPE):", SwingConstants.RIGHT), g);
        g.gridx = 1;
        g.gridwidth = 3;
        main.add(type, g);
        g.gridwidth = 1;
        row++;

        g.gridx = 0;
        g.gridy = row;
        main.add(new JLabel("Giới tính:", SwingConstants.RIGHT), g);
        g.gridx = 1;
        g.gridwidth = 3;
        main.add(gender, g);
        g.gridwidth = 1;
        row++;

        g.gridx = 0;
        g.gridy = row;
        main.add(new JLabel("Level:", SwingConstants.RIGHT), g);
        g.gridx = 1;
        main.add(level, g);
        g.gridx = 2;
        main.add(new JLabel("Power Require:", SwingConstants.RIGHT), g);
        g.gridx = 3;
        main.add(power, g);
        row++;

        g.gridx = 0;
        g.gridy = row;
        main.add(new JLabel("Vàng (Gold):", SwingConstants.RIGHT), g);
        g.gridx = 1;
        main.add(gold, g);
        g.gridx = 2;
        main.add(new JLabel("Ngọc (Gem):", SwingConstants.RIGHT), g);
        g.gridx = 3;
        main.add(gem, g);
        row++;

        g.gridx = 0;
        g.gridy = row;
        main.add(new JLabel("Head (Đầu):", SwingConstants.RIGHT), g);
        g.gridx = 1;
        main.add(head, g);
        g.gridx = 2;
        main.add(new JLabel("Body (Thân):", SwingConstants.RIGHT), g);
        g.gridx = 3;
        main.add(body, g);
        row++;

        g.gridx = 0;
        g.gridy = row;
        main.add(new JLabel("Leg (Chân):", SwingConstants.RIGHT), g);
        g.gridx = 1;
        main.add(leg, g);
        row++;

        JPanel checkPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        checkPanel.add(isUpToUp);
        checkPanel.add(canTrade);
        checkPanel.add(isUpToUp99);

        g.gridx = 0;
        g.gridy = row;
        g.gridwidth = 4;
        main.add(checkPanel, g);
        g.gridwidth = 1;
        row++;

        JButton create = new JButton("THÊM MỚI");
        create.setBackground(new Color(40, 167, 69));
        create.setForeground(Color.WHITE);
        create.setFont(new Font("Segoe UI", Font.BOLD, 14));
        create.setPreferredSize(new Dimension(200, 40));

        JPanel bottom = new JPanel();
        bottom.add(create);

        create.addActionListener(e -> {
            try (Connection c = getConnection()) {

                int startId = parseIntSafe(idStart.getText());
                int total = parseIntSafe(amount.getText());

                PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO item_template(" +
                                "id,type,gender,name,description,level,icon_id,part," +
                                "is_up_to_up,power_require,gold,gem,head,body,leg," +
                                "is_up_to_up_over_99,can_trade) " +
                                "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

                for (int i = 0; i < total; i++) {

                    int id = startId + i;

                    ps.setInt(1, id);
                    ps.setInt(2, parseIntSafe(type.getText()));
                    ps.setInt(3, gender.getSelectedIndex());
                    ps.setString(4, name.getText());
                    ps.setString(5, description.getText());
                    ps.setInt(6, parseIntSafe(level.getText()));
                    ps.setInt(7, parseIntSafe(icon.getText()));
                    ps.setInt(8, parseIntSafe(part.getText()));
                    ps.setInt(9, isUpToUp.isSelected() ? 1 : 0);
                    ps.setInt(10, parseIntSafe(power.getText()));
                    ps.setInt(11, parseIntSafe(gold.getText()));
                    ps.setInt(12, parseIntSafe(gem.getText()));
                    ps.setInt(13, parseIntSafe(head.getText()));
                    ps.setInt(14, parseIntSafe(body.getText()));
                    ps.setInt(15, parseIntSafe(leg.getText()));
                    ps.setInt(16, isUpToUp99.isSelected() ? 1 : 0);
                    ps.setInt(17, canTrade.isSelected() ? 1 : 0);

                    ps.addBatch();
                }

                ps.executeBatch();
                JOptionPane.showMessageDialog(d, "Tạo item thành công!");
                d.dispose();

            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(d, "Tạo item thất bại!");
            }
        });

        d.add(main, BorderLayout.CENTER);
        d.add(bottom, BorderLayout.SOUTH);
        d.setVisible(true);
    }
}