package nro.server.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import jdbc.DBConnecter;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.sql.*;
import java.util.*;

/**
 * Panel quản lý NPC - cho phép xem, sửa, thêm NPC template.
 */
public class NpcManagerPanel extends JPanel {

    private static final String ICON_FOLDER = "data/icon/";
    private static final Font FONT = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 13);

    private JTable table;
    private DefaultTableModel model;
    private JTextField txtSearch;
    private final Map<Integer, Integer> partIconMap = new HashMap<>();
    private final Map<Integer, ImageIcon> iconCache = new HashMap<>();

    public NpcManagerPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(15, 15, 15, 15));
        loadPartIcons();
        initUI();
    }

    private void loadPartIcons() {
        new Thread(() -> {
            try (Connection conn = DBConnecter.getConnectionServer();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id, data FROM part WHERE type = 0")) {
                while (rs.next()) {
                    try {
                        JsonArray arr = new JsonParser().parse(rs.getString("data")).getAsJsonArray();
                        if (arr.size() > 0) partIconMap.put(rs.getInt("id"), arr.get(0).getAsJsonArray().get(0).getAsInt());
                    } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
            SwingUtilities.invokeLater(this::loadData);
        }).start();
    }

    private ImageIcon getHeadIcon(int headPartId, int size) {
        if (headPartId <= 0) return null;
        if (iconCache.containsKey(headPartId)) return iconCache.get(headPartId);
        Integer iconId = partIconMap.get(headPartId);
        if (iconId == null) return null;
        try {
            for (String z : new String[]{"x4", "x3", "x2", "x1"}) {
                File f = new File(ICON_FOLDER + z + "/" + iconId + ".png");
                if (f.exists()) {
                    Image img = ImageIO.read(f).getScaledInstance(size, size, Image.SCALE_SMOOTH);
                    ImageIcon icon = new ImageIcon(img);
                    iconCache.put(headPartId, icon);
                    return icon;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void initUI() {
        // Header
        JLabel lblTitle = new JLabel("QUẢN LÝ NPC TEMPLATE");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(new Color(60, 60, 60));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        searchPanel.setOpaque(false);

        txtSearch = new JTextField(20);
        txtSearch.setFont(FONT);
        txtSearch.setPreferredSize(new Dimension(250, 35));
        txtSearch.putClientProperty("JTextField.placeholderText", "Tìm NPC theo tên hoặc ID...");

        JButton btnSearch = ServerGuiUtils.createStyledButton("Tìm", new Color(0, 120, 215), Color.WHITE);
        JButton btnReload = ServerGuiUtils.createStyledButton("Tải lại", new Color(40, 167, 69), Color.WHITE);
        JButton btnAdd = ServerGuiUtils.createStyledButton("+ Thêm NPC", new Color(255, 140, 0), Color.WHITE);

        btnSearch.addActionListener(e -> searchData());
        btnReload.addActionListener(e -> { txtSearch.setText(""); loadData(); });
        btnAdd.addActionListener(e -> openEditDialog(-1));

        searchPanel.add(txtSearch);
        searchPanel.add(btnSearch);
        searchPanel.add(btnReload);
        searchPanel.add(btnAdd);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(lblTitle, BorderLayout.NORTH);
        top.add(searchPanel, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        // Table
        String[] cols = {"Avatar", "ID", "Tên NPC", "Head", "Body", "Leg", "Avatar ID", "Shop"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) { return c == 0 ? ImageIcon.class : Object.class; }
        };

        table = new JTable(model);
        table.setRowHeight(45);
        table.setFont(FONT);
        table.setShowVerticalLines(false);

        JTableHeader header = table.getTableHeader();
        header.setFont(FONT_BOLD);
        header.setBackground(new Color(0, 120, 215));
        header.setForeground(Color.WHITE);
        header.setPreferredSize(new Dimension(0, 38));

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                comp.setBackground(sel ? new Color(220, 235, 255) : (r % 2 == 0 ? Color.WHITE : new Color(248, 250, 252)));
                setHorizontalAlignment(JLabel.CENTER);
                if (c == 2) { setFont(FONT_BOLD); setForeground(new Color(0, 102, 204)); }
                else { setFont(FONT); setForeground(Color.BLACK); }
                return comp;
            }
        });

        TableColumnModel cm = table.getColumnModel();
        cm.getColumn(0).setPreferredWidth(50);
        cm.getColumn(1).setPreferredWidth(50);
        cm.getColumn(2).setPreferredWidth(150);
        cm.getColumn(3).setPreferredWidth(60);
        cm.getColumn(4).setPreferredWidth(60);
        cm.getColumn(5).setPreferredWidth(60);
        cm.getColumn(6).setPreferredWidth(60);
        cm.getColumn(7).setPreferredWidth(120);

        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() != -1) {
                    int id = Integer.parseInt(table.getValueAt(table.getSelectedRow(), 1).toString());
                    openEditDialog(id);
                }
            }
        });

        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private void loadData() {
        model.setRowCount(0);
        new Thread(() -> {
            try (Connection conn = DBConnecter.getConnectionServer();
                 Statement s = conn.createStatement();
                 ResultSet rs = s.executeQuery(
                     "SELECT n.*, GROUP_CONCAT(s.tag_name SEPARATOR ', ') as shops " +
                     "FROM npc_template n LEFT JOIN shop s ON s.npc_id = n.id " +
                     "GROUP BY n.id ORDER BY n.id")) {
                while (rs.next()) {
                    addRow(rs);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void searchData() {
        String txt = txtSearch.getText().trim();
        if (txt.isEmpty()) { loadData(); return; }
        model.setRowCount(0);
        new Thread(() -> {
            try (Connection conn = DBConnecter.getConnectionServer();
                 PreparedStatement ps = conn.prepareStatement(
                     "SELECT n.*, GROUP_CONCAT(s.tag_name SEPARATOR ', ') as shops " +
                     "FROM npc_template n LEFT JOIN shop s ON s.npc_id = n.id " +
                     "WHERE n.id = ? OR n.NAME LIKE ? GROUP BY n.id ORDER BY n.id")) {
                try { ps.setInt(1, Integer.parseInt(txt)); } catch (Exception e) { ps.setInt(1, -1); }
                ps.setString(2, "%" + txt + "%");
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) addRow(rs);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void addRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("NAME");
        int head = rs.getInt("head");
        int body = rs.getInt("body");
        int leg = rs.getInt("leg");
        int avatar = rs.getInt("avatar");
        String shops = rs.getString("shops");

        ImageIcon icon = getHeadIcon(head, 32);
        SwingUtilities.invokeLater(() -> model.addRow(new Object[]{
            icon, id, name, head, body, leg, avatar,
            shops != null ? shops : "-"
        }));
    }

    // ==============================================
    // EDIT / ADD DIALOG
    // ==============================================
    private void openEditDialog(int npcId) {
        boolean isNew = (npcId < 0);
        JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                isNew ? "Thêm NPC mới" : "Sửa NPC #" + npcId, true);
        d.setSize(500, 350);
        d.setLocationRelativeTo(this);
        d.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(15, 20, 10, 20));
        form.setBackground(Color.WHITE);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 5, 6, 5);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;

        JTextField txtId = new JTextField(); txtId.setEditable(false);
        JTextField txtName = new JTextField();
        JTextField txtHead = new JTextField();
        JTextField txtBody = new JTextField();
        JTextField txtLeg = new JTextField();
        JTextField txtAvatar = new JTextField();

        // Load data if editing
        if (!isNew) {
            try (Connection conn = DBConnecter.getConnectionServer();
                 PreparedStatement ps = conn.prepareStatement("SELECT * FROM npc_template WHERE id = ?")) {
                ps.setInt(1, npcId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        txtId.setText(String.valueOf(rs.getInt("id")));
                        txtName.setText(rs.getString("NAME"));
                        txtHead.setText(String.valueOf(rs.getInt("head")));
                        txtBody.setText(String.valueOf(rs.getInt("body")));
                        txtLeg.setText(String.valueOf(rs.getInt("leg")));
                        txtAvatar.setText(String.valueOf(rs.getInt("avatar")));
                    }
                }
            } catch (Exception ex) { ex.printStackTrace(); }
        }

        int row = 0;
        addFormRow(form, g, row++, "ID:", txtId);
        addFormRow(form, g, row++, "Tên NPC:", txtName);
        addFormRow(form, g, row++, "Head Part ID:", txtHead);
        addFormRow(form, g, row++, "Body Part ID:", txtBody);
        addFormRow(form, g, row++, "Leg Part ID:", txtLeg);
        addFormRow(form, g, row++, "Avatar ID:", txtAvatar);

        // Buttons
        JPanel pBtn = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pBtn.setBackground(new Color(245, 245, 245));
        pBtn.setBorder(new MatteBorder(1, 0, 0, 0, new Color(220, 220, 220)));

        JButton btnSave = ServerGuiUtils.createStyledButton("LƯU", new Color(0, 120, 215), Color.WHITE);
        btnSave.setPreferredSize(new Dimension(120, 38));
        JButton btnDelete = ServerGuiUtils.createStyledButton("XÓA", new Color(220, 53, 69), Color.WHITE);
        btnDelete.setPreferredSize(new Dimension(100, 38));
        btnDelete.setVisible(!isNew);
        JButton btnCancel = new JButton("Đóng");
        btnCancel.addActionListener(e -> d.dispose());

        btnSave.addActionListener(e -> {
            String name = txtName.getText().trim();
            if (name.isEmpty()) { JOptionPane.showMessageDialog(d, "Tên NPC không được trống!"); return; }
            int head = safeInt(txtHead), body = safeInt(txtBody), leg = safeInt(txtLeg), avt = safeInt(txtAvatar);

            new Thread(() -> {
                try (Connection conn = DBConnecter.getConnectionServer()) {
                    if (isNew) {
                        // Tìm ID lớn nhất + 1
                        int newId = 0;
                        try (Statement st = conn.createStatement();
                             ResultSet rs = st.executeQuery("SELECT MAX(id) FROM npc_template")) {
                            if (rs.next()) newId = rs.getInt(1) + 1;
                        }
                        try (PreparedStatement ps = conn.prepareStatement(
                                "INSERT INTO npc_template (id, NAME, head, body, leg, avatar) VALUES (?,?,?,?,?,?)")) {
                            ps.setInt(1, newId); ps.setString(2, name);
                            ps.setInt(3, head); ps.setInt(4, body); ps.setInt(5, leg); ps.setInt(6, avt);
                            ps.executeUpdate();
                        }
                    } else {
                        try (PreparedStatement ps = conn.prepareStatement(
                                "UPDATE npc_template SET NAME=?, head=?, body=?, leg=?, avatar=? WHERE id=?")) {
                            ps.setString(1, name); ps.setInt(2, head); ps.setInt(3, body);
                            ps.setInt(4, leg); ps.setInt(5, avt); ps.setInt(6, npcId);
                            ps.executeUpdate();
                        }
                    }
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(d, "Lưu thành công!");
                        d.dispose();
                        loadData();
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(d, "Lỗi: " + ex.getMessage()));
                }
            }).start();
        });

        btnDelete.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(d, "Xóa NPC #" + npcId + "?", "Xác nhận",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
                new Thread(() -> {
                    try (Connection conn = DBConnecter.getConnectionServer();
                         PreparedStatement ps = conn.prepareStatement("DELETE FROM npc_template WHERE id = ?")) {
                        ps.setInt(1, npcId);
                        ps.executeUpdate();
                        SwingUtilities.invokeLater(() -> { d.dispose(); loadData(); });
                    } catch (Exception ex) {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(d, "Lỗi: " + ex.getMessage()));
                    }
                }).start();
            }
        });

        pBtn.add(btnDelete);
        pBtn.add(btnCancel);
        pBtn.add(btnSave);

        d.add(form, BorderLayout.CENTER);
        d.add(pBtn, BorderLayout.SOUTH);
        d.setVisible(true);
    }

    private void addFormRow(JPanel p, GridBagConstraints g, int row, String label, JComponent field) {
        g.gridx = 0; g.gridy = row; g.weightx = 0;
        JLabel lbl = new JLabel(label);
        lbl.setFont(FONT_BOLD);
        p.add(lbl, g);
        g.gridx = 1; g.weightx = 1;
        field.setFont(FONT);
        p.add(field, g);
    }

    private int safeInt(JTextField t) {
        try { return Integer.parseInt(t.getText().trim()); } catch (Exception e) { return 0; }
    }
}
