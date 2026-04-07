package nro.server.ui;

import com.google.gson.*;
import jdbc.DBConnecter;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class BadgesPanel extends JPanel {

    private JTable table;
    private DefaultTableModel model;
    private TableRowSorter<DefaultTableModel> sorter;
    private JTextField txtSearch;
    private final Map<Integer, String> optionNameMap = new HashMap<>();
    private final Map<Integer, String> rawOptionsMap = new HashMap<>();

    // --- ICON CONFIG ---
    private static final String ICON_FOLDER = "data/icon/";
    private final Map<Integer, Integer> itemIconMap = new HashMap<>();
    private final Map<Integer, ImageIcon> iconCache = new HashMap<>();
    private final Map<Integer, Boolean> noIconCache = new HashMap<>();

    public BadgesPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(245, 245, 245));
        setBorder(new EmptyBorder(15, 15, 15, 15));

        initUI();
        loadItemTemplateCache();
        loadOptionTemplateCache();
        loadDataFromDB();
    }

    private Connection getConnection() throws SQLException {
        return DBConnecter.getConnectionServer();
    }

    // ================= UI =================

    private void initUI() {

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        top.setOpaque(false);

        txtSearch = new JTextField(25);
        txtSearch.setPreferredSize(new Dimension(240, 32));

        JButton btnSearch = createButton("Tìm kiếm", new Color(0, 123, 255));
        JButton btnAdd = createButton("Thêm Danh Hiệu", new Color(40, 167, 69));
        JButton btnReload = createButton("Tải lại", new Color(108, 117, 125));
        JButton btnDelete = createButton("Xóa", new Color(220, 53, 69));

        btnSearch.addActionListener(e -> search());
        btnReload.addActionListener(e -> loadDataFromDB());
        btnDelete.addActionListener(e -> deleteSelectedBadge());
        btnAdd.addActionListener(e -> openBadgesEditor(null));

        top.add(txtSearch);
        top.add(btnSearch);
        top.add(btnAdd);
        top.add(btnReload);
        top.add(btnDelete);

        add(top, BorderLayout.NORTH);

        String[] cols = { "ID", "Icon", "Tên Danh Hiệu", "ID Effect", "ID Item", "Chỉ số (hiển thị)" };

        model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }

            public Class<?> getColumnClass(int c) {
                return c == 1 ? ImageIcon.class : Object.class;
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

        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(JLabel.CENTER);

        DefaultTableCellRenderer left = new DefaultTableCellRenderer();
        left.setHorizontalAlignment(JLabel.LEFT);

        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(0).setMaxWidth(70);
        table.getColumnModel().getColumn(0).setCellRenderer(center);

        DefaultTableCellRenderer iconCenter = new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object value) {
                setText("");
                setHorizontalAlignment(JLabel.CENTER);
                setIcon(value instanceof ImageIcon ? (ImageIcon) value : null);
            }
        };

        table.getColumnModel().getColumn(1).setPreferredWidth(60);
        table.getColumnModel().getColumn(1).setMaxWidth(70);
        table.getColumnModel().getColumn(1).setCellRenderer(iconCenter);

        table.getColumnModel().getColumn(2).setPreferredWidth(220);
        table.getColumnModel().getColumn(2).setCellRenderer(left);

        table.getColumnModel().getColumn(3).setPreferredWidth(90);
        table.getColumnModel().getColumn(3).setMaxWidth(110);
        table.getColumnModel().getColumn(3).setCellRenderer(center);

        table.getColumnModel().getColumn(4).setPreferredWidth(90);
        table.getColumnModel().getColumn(4).setMaxWidth(110);
        table.getColumnModel().getColumn(4).setCellRenderer(center);

        table.getColumnModel().getColumn(5).setPreferredWidth(400);
        table.getColumnModel().getColumn(5).setCellRenderer(left);

        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.getSelectedRow();
                    if (row != -1)
                        openBadgesEditor(table.convertRowIndexToModel(row));
                }
            }
        });

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));

        add(sp, BorderLayout.CENTER);
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

    private void search() {
        String key = txtSearch.getText().trim().toLowerCase();

        if (key.isEmpty()) {
            sorter.setRowFilter(null);
            return;
        }

        sorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {

                for (int i = 0; i < entry.getValueCount(); i++) {
                    Object v = entry.getValue(i);
                    if (v != null && v.toString().toLowerCase().contains(key)) {
                        return true;
                    }
                }
                return false;
            }
        });
    }

    // ================= LOAD DATA =================

    private void loadItemTemplateCache() {
        try (Connection conn = getConnection();
                Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery("SELECT id,icon_id FROM item_template")) {
            while (rs.next()) {
                itemIconMap.put(rs.getInt("id"), rs.getInt("icon_id"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadDataFromDB() {
        model.setRowCount(0);

        new Thread(() -> {
            try (Connection conn = getConnection();
                    Statement st = conn.createStatement();
                    ResultSet rs = st.executeQuery("SELECT * FROM data_badges ORDER BY id ASC")) {

                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    row.add(rs.getInt("id"));
                    row.add(getItemIcon(rs.getInt("idItem")));
                    row.add(rs.getString("NAME"));
                    row.add(rs.getInt("idEffect"));
                    row.add(rs.getInt("idItem"));
                    String rawJson = rs.getString("Options");
                    rawOptionsMap.put(rs.getInt("id"), rawJson);
                    row.add(formatOptions(rawJson));
                    SwingUtilities.invokeLater(() -> model.addRow(row));
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // ================= ICON =================

    private ImageIcon getItemIcon(int itemId) {
        if (iconCache.containsKey(itemId))
            return iconCache.get(itemId);
        if (noIconCache.containsKey(itemId))
            return null;

        try {
            int iconId = itemIconMap.getOrDefault(itemId, -1);
            if (iconId == -1) {
                noIconCache.put(itemId, true);
                return null;
            }
            String[] zoomLevels = { "x4", "x3", "x2", "x1" };
            File f = null;
            for (String zoom : zoomLevels) {
                f = new File(ICON_FOLDER + zoom + "/" + iconId + ".png");
                if (f.exists())
                    break;
            }
            if (f != null && f.exists()) {
                BufferedImage img = ImageIO.read(f);
                Image dimg = img.getScaledInstance(22, 22, Image.SCALE_SMOOTH);
                ImageIcon icon = new ImageIcon(dimg);
                iconCache.put(itemId, icon);
                return icon;
            }
        } catch (Exception ignored) {
        }
        noIconCache.put(itemId, true);
        return null;
    }

    // ================= OPTIONS FORMAT =================

    private String formatOptions(String json) {
        if (json == null || json.isEmpty())
            return "";

        try {
            JsonParser parser = new JsonParser();
            JsonArray arr = parser.parse(json).getAsJsonArray();

            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < arr.size(); i++) {
                JsonObject o = arr.get(i).getAsJsonObject();

                int param = o.get("param").getAsInt();
                int optionId = o.get("id").getAsInt();

                String template = optionNameMap.getOrDefault(optionId, "Option " + optionId);

                String text = template
                        .replace("#%", param + "%")
                        .replace("#", String.valueOf(param));

                sb.append(text);

                if (i < arr.size() - 1)
                    sb.append(", ");
            }

            return sb.toString();

        } catch (Exception e) {
            return json;
        }
    }

    // ================= DELETE =================

    private void deleteSelectedBadge() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Chọn dòng cần xóa!");
            return;
        }
        int id = (int) model.getValueAt(table.convertRowIndexToModel(row), 0);

        if (JOptionPane.showConfirmDialog(this, "Xóa danh hiệu ID " + id + " ?", "Xác nhận",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
            return;

        new Thread(() -> {
            try (Connection conn = getConnection();
                    PreparedStatement ps = conn.prepareStatement("DELETE FROM data_badges WHERE id=?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
                SwingUtilities.invokeLater(this::loadDataFromDB);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // ================= EDITOR =================

    private void openBadgesEditor(Integer rowIndex) {

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Badges Editor", true);
        dialog.setSize(500, 350);
        dialog.setLayout(new GridLayout(6, 2, 10, 10));
        dialog.setLocationRelativeTo(this);

        JTextField txtId = new JTextField(rowIndex != null ? model.getValueAt(rowIndex, 0).toString() : "");
        JTextField txtName = new JTextField(rowIndex != null ? model.getValueAt(rowIndex, 2).toString() : "");
        JTextField txtEffect = new JTextField(rowIndex != null ? model.getValueAt(rowIndex, 3).toString() : "");
        JTextField txtItem = new JTextField(rowIndex != null ? model.getValueAt(rowIndex, 4).toString() : "");
        String rawJson = "[]";
        if (rowIndex != null) {
            int id = Integer.parseInt(model.getValueAt(rowIndex, 0).toString());
            rawJson = rawOptionsMap.getOrDefault(id, "[]");
        }

        JTextArea txtOptions = new JTextArea(extractParams(rawJson));

        dialog.add(new JLabel("ID"));
        dialog.add(txtId);
        dialog.add(new JLabel("Tên"));
        dialog.add(txtName);
        dialog.add(new JLabel("ID Effect"));
        dialog.add(txtEffect);
        dialog.add(new JLabel("ID Item"));
        dialog.add(txtItem);
        dialog.add(new JLabel("Options JSON"));
        dialog.add(new JScrollPane(txtOptions));

        JButton btnSave = new JButton("Lưu");

        btnSave.addActionListener(e -> {
            try (Connection conn = getConnection()) {
                if (rowIndex == null) {
                    PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO data_badges (id,NAME,idEffect,idItem,Options) VALUES (?,?,?,?,?)");
                    ps.setInt(1, Integer.parseInt(txtId.getText()));
                    ps.setString(2, txtName.getText());
                    ps.setInt(3, Integer.parseInt(txtEffect.getText()));
                    ps.setInt(4, Integer.parseInt(txtItem.getText()));
                    ps.setString(5, txtOptions.getText());
                    ps.executeUpdate();
                } else {
                    PreparedStatement ps = conn.prepareStatement(
                            "UPDATE data_badges SET NAME=?, idEffect=?, idItem=?, Options=? WHERE id=?");
                    ps.setString(1, txtName.getText());
                    ps.setInt(2, Integer.parseInt(txtEffect.getText()));
                    ps.setInt(3, Integer.parseInt(txtItem.getText()));
                    String oldJson = rawOptionsMap.get(Integer.parseInt(txtId.getText()));
                    String newJson = rebuildParams(oldJson, txtOptions.getText());
                    ps.setString(4, newJson);
                    ps.setInt(5, Integer.parseInt(txtId.getText()));
                    ps.executeUpdate();
                }
                dialog.dispose();
                loadDataFromDB();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        dialog.add(new JLabel());
        dialog.add(btnSave);
        dialog.setVisible(true);
    }

    private void loadOptionTemplateCache() {
        try (Connection conn = getConnection();
                Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery("SELECT id, name FROM item_option_template")) {

            while (rs.next()) {
                optionNameMap.put(rs.getInt("id"), rs.getString("name"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String extractParams(String json) {
        if (json == null || json.isEmpty())
            return "";

        try {
            JsonArray arr = new JsonParser().parse(json).getAsJsonArray();
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < arr.size(); i++) {
                sb.append(arr.get(i).getAsJsonObject().get("param").getAsInt());
                if (i < arr.size() - 1)
                    sb.append(",");
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private String rebuildParams(String oldJson, String paramText) {
        try {
            JsonArray arr = new JsonParser().parse(oldJson).getAsJsonArray();
            String[] params = paramText.split(",");

            for (int i = 0; i < arr.size() && i < params.length; i++) {
                JsonObject o = arr.get(i).getAsJsonObject();
                o.addProperty("param", Integer.parseInt(params[i].trim()));
            }
            return arr.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return oldJson;
        }
    }
}