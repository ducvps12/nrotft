package nro.server.ui;

import com.google.gson.*;
import jdbc.DBConnecter;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.sql.*;
import java.util.HashMap;
import java.util.Vector;

public class RadarPanel extends JPanel {

    private JTable tableList;
    private DefaultTableModel listModel;

    private JTextField txtSearch;

    private JTextField txtId, txtName, txtIcon, txtRank, txtMax, txtType, txtAura;
    private JTextField txtHead, txtBody, txtLeg, txtBag;
    private JTextArea txtInfo;

    private JTable tableOptions;
    private DefaultTableModel optionModel;

    private JLabel lblIcon;
    private static final String ICON_FOLDER = "data/icon/";
    private HashMap<Integer, ImageIcon> iconCache = new HashMap<>();

    private boolean isCreating = false;

    public RadarPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        initUI();
        loadRadarList(null);
    }

    private Connection getConnection() throws SQLException {
        return DBConnecter.getConnectionServer();
    }

    private void initUI() {

        JPanel left = new JPanel(new BorderLayout(5, 5));
        left.setPreferredSize(new Dimension(300, 650));

        txtSearch = new JTextField("Nhập ID hoặc tên radar...");
        txtSearch.setForeground(Color.GRAY);
        txtSearch.setPreferredSize(new Dimension(200, 34));

        txtSearch.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                if (txtSearch.getText().equals("Nhập ID hoặc tên radar...")) {
                    txtSearch.setText("");
                    txtSearch.setForeground(Color.BLACK);
                }
            }

            public void focusLost(java.awt.event.FocusEvent evt) {
                if (txtSearch.getText().isEmpty()) {
                    txtSearch.setText("Nhập ID hoặc tên radar...");
                    txtSearch.setForeground(Color.GRAY);
                }
            }
        });

        txtSearch.addActionListener(e -> loadRadarList(txtSearch.getText()));

        JButton btnAdd = new JButton("Thêm Mới");
        btnAdd.setBackground(new Color(40, 167, 69));
        btnAdd.setForeground(Color.WHITE);
        btnAdd.setFocusPainted(false);

        JButton btnDelete = new JButton("Xóa");
        btnDelete.setBackground(new Color(220, 53, 69));
        btnDelete.setForeground(Color.WHITE);
        btnDelete.setFocusPainted(false);

        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        btnPanel.add(btnAdd);
        btnPanel.add(btnDelete);

        JPanel topLeft = new JPanel(new BorderLayout(5, 5));
        topLeft.add(txtSearch, BorderLayout.NORTH);
        topLeft.add(btnPanel, BorderLayout.SOUTH);

        listModel = new DefaultTableModel(new String[] { "Radar" }, 0) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tableList = new JTable(listModel);
        tableList.setRowHeight(42);
        tableList.setShowGrid(false);
        tableList.setDefaultRenderer(Object.class, new RadarRenderer());
        tableList.putClientProperty("terminateEditOnFocusLost", true);
        tableList.getSelectionModel().addListSelectionListener(e -> loadSelectedRadar());

        left.add(topLeft, BorderLayout.NORTH);
        left.add(new JScrollPane(tableList), BorderLayout.CENTER);
        add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new BorderLayout(10, 10));
        right.add(createInfoPanel(), BorderLayout.NORTH);
        right.add(createOptionPanel(), BorderLayout.CENTER);
        right.add(createBottomButtons(), BorderLayout.SOUTH);

        add(right, BorderLayout.CENTER);

        btnAdd.addActionListener(e -> openCreateDialog());

        btnDelete.addActionListener(e -> deleteRadar());
    }

    private JPanel createInfoPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(new TitledBorder("THÔNG TIN CHUNG"));

        JPanel grid = new JPanel(new GridLayout(4, 4, 10, 8));

        txtId = createField();
        txtName = createField();
        txtIcon = createField();
        txtRank = createField();
        txtMax = createField();
        txtType = createField();
        txtAura = createField();

        lblIcon = new JLabel();
        lblIcon.setPreferredSize(new Dimension(40, 40));

        grid.add(new JLabel("ID"));
        grid.add(txtId);
        grid.add(new JLabel("Tên Radar"));
        grid.add(txtName);

        grid.add(new JLabel("Icon ID"));
        grid.add(txtIcon);
        grid.add(new JLabel("Icon"));
        grid.add(lblIcon);

        grid.add(new JLabel("Rank"));
        grid.add(txtRank);
        grid.add(new JLabel("Max Amount"));
        grid.add(txtMax);

        grid.add(new JLabel("Type"));
        grid.add(txtType);
        grid.add(new JLabel("Aura ID"));
        grid.add(txtAura);

        JPanel bodyPanel = new JPanel(new GridLayout(1, 8, 10, 5));
        bodyPanel.setBorder(new TitledBorder("NGOẠI HÌNH"));

        txtHead = createField();
        txtBody = createField();
        txtLeg = createField();
        txtBag = createField();

        bodyPanel.add(new JLabel("Head"));
        bodyPanel.add(txtHead);
        bodyPanel.add(new JLabel("Body"));
        bodyPanel.add(txtBody);
        bodyPanel.add(new JLabel("Leg"));
        bodyPanel.add(txtLeg);
        bodyPanel.add(new JLabel("Bag"));
        bodyPanel.add(txtBag);

        txtInfo = new JTextArea(3, 20);
        JScrollPane infoScroll = new JScrollPane(txtInfo);
        infoScroll.setBorder(new TitledBorder("MÔ TẢ"));

        panel.add(grid, BorderLayout.NORTH);
        panel.add(bodyPanel, BorderLayout.CENTER);
        panel.add(infoScroll, BorderLayout.SOUTH);

        txtIcon.addActionListener(e -> updateIconPreview());

        return panel;
    }

    private JPanel createOptionPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("CHỈ SỐ"));

        optionModel = new DefaultTableModel(
                new String[] { "Option ID", "Param", "Active", "Mô tả" }, 0);

        tableOptions = new JTable(optionModel);
        tableOptions.setRowHeight(28);
        tableOptions.putClientProperty("terminateEditOnFocusLost", true);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton btnAddRow = new JButton("Thêm Dòng");
        JButton btnRemoveRow = new JButton("Xóa Dòng");

        btnAddRow.addActionListener(e -> optionModel.addRow(new Object[] { 0, 0, 0, "" }));

        btnRemoveRow.addActionListener(e -> {
            int r = tableOptions.getSelectedRow();
            if (r != -1)
                optionModel.removeRow(r);
        });

        top.add(btnAddRow);
        top.add(btnRemoveRow);

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(tableOptions), BorderLayout.CENTER);

        return panel;
    }

    private JPanel createBottomButtons() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton btnReload = new JButton("TẢI LẠI DB");
        JButton btnSave = new JButton("LƯU DỮ LIỆU");

        btnReload.addActionListener(e -> loadRadarList(null));
        btnSave.addActionListener(e -> saveRadar());

        panel.add(btnReload);
        panel.add(btnSave);

        return panel;
    }

    private JTextField createField() {
        JTextField f = new JTextField();
        f.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        return f;
    }

    private void loadRadarList(String keyword) {
        listModel.setRowCount(0);
        new Thread(() -> {
            try (Connection conn = getConnection()) {

                String sql = "SELECT id,name,iconid FROM radar";
                if (keyword != null && !keyword.contains("Nhập"))
                    sql += " WHERE id LIKE ? OR name LIKE ?";
                sql += " ORDER BY id ASC";

                PreparedStatement ps = conn.prepareStatement(sql);

                if (keyword != null && !keyword.contains("Nhập")) {
                    ps.setString(1, "%" + keyword + "%");
                    ps.setString(2, "%" + keyword + "%");
                }

                ResultSet rs = ps.executeQuery();

                while (rs.next()) {
                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    int iconId = rs.getInt("iconid");

                    Vector<Object> row = new Vector<>();
                    row.add(new RadarItem(id, name, iconId));

                    SwingUtilities.invokeLater(() -> listModel.addRow(row));
                }

            } catch (Exception ignored) {
            }
        }).start();
    }

    private void loadSelectedRadar() {
        int row = tableList.getSelectedRow();
        if (row == -1)
            return;

        RadarItem item = (RadarItem) tableList.getValueAt(row, 0);

        new Thread(() -> {
            try (Connection conn = getConnection();
                    PreparedStatement ps = conn.prepareStatement("SELECT * FROM radar WHERE id=?")) {

                ps.setInt(1, item.id);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    txtId.setText(rs.getString("id"));
                    txtName.setText(rs.getString("name"));

                    int iconId = rs.getInt("iconid");
                    txtIcon.setText(String.valueOf(iconId));
                    lblIcon.setIcon(getIcon(iconId));

                    txtRank.setText(rs.getString("rank"));
                    txtMax.setText(rs.getString("max"));
                    txtType.setText(rs.getString("type"));
                    txtAura.setText(rs.getString("aura_id"));
                    txtInfo.setText(rs.getString("info"));

                    parseBody(rs.getString("body"));
                    parseOptions(rs.getString("options"));
                }

            } catch (Exception ignored) {
            }
        }).start();
    }

    private void clearForm() {
        txtId.setText("");
        txtName.setText("");
        txtIcon.setText("");
        txtRank.setText("");
        txtMax.setText("");
        txtType.setText("");
        txtAura.setText("");
        txtHead.setText("");
        txtBody.setText("");
        txtLeg.setText("");
        txtBag.setText("");
        txtInfo.setText("");
        lblIcon.setIcon(null);
        optionModel.setRowCount(0);
    }

    private void confirmCreate() {
        if (!isCreating)
            return;
        isCreating = false;
        JOptionPane.showMessageDialog(this, "Đã xác nhận. Bấm LƯU DỮ LIỆU.");
    }

    private void deleteRadar() {
        if (txtId.getText().isEmpty())
            return;

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Bạn có chắc muốn xóa radar này?",
                "Xác nhận xóa",
                JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION)
            return;

        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement("DELETE FROM radar WHERE id=?")) {

            ps.setInt(1, Integer.parseInt(txtId.getText()));
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "Đã xóa!");
            loadRadarList(null);
            clearForm();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void saveRadar() {

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Bạn có chắc muốn lưu thay đổi radar này?",
                "Xác nhận lưu",
                JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION)
            return;

        int id = parseIntSafe(txtId.getText(), -1);
        if (id <= 0) {
            JOptionPane.showMessageDialog(this, "ID không hợp lệ!");
            return;
        }

        try (Connection conn = getConnection()) {

            boolean exists;

            try (PreparedStatement check = conn.prepareStatement(
                    "SELECT id FROM radar WHERE id=?")) {
                check.setInt(1, id);
                exists = check.executeQuery().next();
            }

            PreparedStatement ps;

            if (exists) {
                ps = conn.prepareStatement(
                        "UPDATE radar SET name=?,iconid=?,rank=?,max=?,type=?,aura_id=?,body=?,info=?,options=? WHERE id=?");
            } else {
                ps = conn.prepareStatement(
                        "INSERT INTO radar(id,name,iconid,rank,max,type,aura_id,body,info,options) VALUES(?,?,?,?,?,?,?,?,?,?)");
            }

            if (exists) {
                ps.setString(1, txtName.getText());
                ps.setInt(2, parseIntSafe(txtIcon.getText(), 0));
                ps.setInt(3, parseIntSafe(txtRank.getText(), 0));
                ps.setInt(4, parseIntSafe(txtMax.getText(), 0));
                ps.setInt(5, parseIntSafe(txtType.getText(), 0));
                ps.setInt(6, parseIntSafe(txtAura.getText(), 0));
                ps.setString(7, buildBodyJson());
                ps.setString(8, txtInfo.getText());
                ps.setString(9, buildOptionsJson());
                ps.setInt(10, id);
            } else {
                ps.setInt(1, id);
                ps.setString(2, txtName.getText());
                ps.setInt(3, parseIntSafe(txtIcon.getText(), 0));
                ps.setInt(4, parseIntSafe(txtRank.getText(), 0));
                ps.setInt(5, parseIntSafe(txtMax.getText(), 0));
                ps.setInt(6, parseIntSafe(txtType.getText(), 0));
                ps.setInt(7, parseIntSafe(txtAura.getText(), 0));
                ps.setString(8, buildBodyJson());
                ps.setString(9, txtInfo.getText());
                ps.setString(10, buildOptionsJson());
            }

            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, exists ? "Đã cập nhật!" : "Đã tạo radar!");
            loadRadarList(null);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private String buildBodyJson() {
        JsonObject o = new JsonObject();
        o.addProperty("head", txtHead.getText());
        o.addProperty("body", txtBody.getText());
        o.addProperty("leg", txtLeg.getText());
        o.addProperty("bag", txtBag.getText());
        JsonArray arr = new JsonArray();
        arr.add(o);
        return arr.toString();
    }

    private String buildOptionsJson() {
        JsonArray arr = new JsonArray();
        for (int i = 0; i < optionModel.getRowCount(); i++) {
            JsonObject o = new JsonObject();
            o.addProperty("id", Integer.parseInt(optionModel.getValueAt(i, 0).toString()));
            o.addProperty("param", Integer.parseInt(optionModel.getValueAt(i, 1).toString()));
            o.addProperty("activeCard", Integer.parseInt(optionModel.getValueAt(i, 2).toString()));
            arr.add(o);
        }
        return arr.toString();
    }

    private void parseBody(String json) {
        try {
            JsonArray arr = new JsonParser().parse(json).getAsJsonArray();
            JsonObject o = arr.get(0).getAsJsonObject();
            txtHead.setText(o.get("head").getAsString());
            txtBody.setText(o.get("body").getAsString());
            txtLeg.setText(o.get("leg").getAsString());
            txtBag.setText(o.get("bag").getAsString());
        } catch (Exception ignored) {
        }
    }

    private void parseOptions(String json) {
        optionModel.setRowCount(0);
        try {
            JsonArray arr = new JsonParser().parse(json).getAsJsonArray();
            for (JsonElement el : arr) {
                JsonObject o = el.getAsJsonObject();
                optionModel.addRow(new Object[] {
                        o.get("id").getAsInt(),
                        o.get("param").getAsInt(),
                        o.get("activeCard").getAsInt(),
                        ""
                });
            }
        } catch (Exception ignored) {
        }
    }

    private void updateIconPreview() {
        try {
            int id = Integer.parseInt(txtIcon.getText());
            lblIcon.setIcon(getIcon(id));
        } catch (Exception ignored) {
        }
    }

    private ImageIcon getIcon(int iconId) {
        if (iconCache.containsKey(iconId))
            return iconCache.get(iconId);
        try {
            String[] zoomLevels = { "x4", "x3", "x2", "x1" };
            for (String zoom : zoomLevels) {
                File f = new File(ICON_FOLDER + zoom + "/" + iconId + ".png");
                if (f.exists()) {
                    BufferedImage img = ImageIO.read(f);
                    Image dimg = img.getScaledInstance(28, 28, Image.SCALE_SMOOTH);
                    ImageIcon icon = new ImageIcon(dimg);
                    iconCache.put(iconId, icon);
                    return icon;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    class RadarRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(
                JTable table, Object value,
                boolean isSelected, boolean hasFocus,
                int row, int column) {

            JLabel label = new JLabel();
            label.setOpaque(true);
            label.setFont(new Font("Segoe UI", Font.PLAIN, 13));

            if (value instanceof RadarItem) {
                RadarItem item = (RadarItem) value;
                label.setText("[" + item.id + "] " + item.name);
                label.setIcon(RadarPanel.this.getIcon(item.iconId));
            }

            if (isSelected)
                label.setBackground(new Color(220, 240, 255));
            else
                label.setBackground(Color.WHITE);

            return label;
        }
    }

    class RadarItem {
        int id;
        String name;
        int iconId;

        RadarItem(int id, String name, int iconId) {
            this.id = id;
            this.name = name;
            this.iconId = iconId;
        }
    }

    private void openCreateDialog() {

        JDialog dialog = new JDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                "Tạo Radar Mới",
                true);

        dialog.setSize(600, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel form = new JPanel(new GridLayout(8, 2, 8, 8));

        JTextField id = new JTextField();
        JTextField name = new JTextField();
        JTextField icon = new JTextField();
        JTextField rank = new JTextField();
        JTextField max = new JTextField();
        JTextField type = new JTextField();
        JTextField aura = new JTextField();

        form.add(new JLabel("ID"));
        form.add(id);
        form.add(new JLabel("Tên"));
        form.add(name);
        form.add(new JLabel("Icon ID"));
        form.add(icon);
        form.add(new JLabel("Rank"));
        form.add(rank);
        form.add(new JLabel("Max"));
        form.add(max);
        form.add(new JLabel("Type"));
        form.add(type);
        form.add(new JLabel("Aura"));
        form.add(aura);

        JButton btnOK = new JButton("XÁC NHẬN TẠO");
        btnOK.setBackground(new Color(40, 167, 69));
        btnOK.setForeground(Color.WHITE);

        btnOK.addActionListener(e -> {
            txtId.setText(id.getText());
            txtName.setText(name.getText());
            txtIcon.setText(icon.getText());
            txtRank.setText(rank.getText());
            txtMax.setText(max.getText());
            txtType.setText(type.getText());
            txtAura.setText(aura.getText());

            isCreating = true;
            dialog.dispose();

            JOptionPane.showMessageDialog(this, "Đã xác nhận. Nhấn LƯU DỮ LIỆU.");
        });

        JPanel bottom = new JPanel();
        bottom.add(btnOK);

        dialog.add(form, BorderLayout.CENTER);
        dialog.add(bottom, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private int parseIntSafe(String text, int defaultValue) {
        if (text == null)
            return defaultValue;
        text = text.trim();
        if (text.isEmpty())
            return defaultValue;
        try {
            return Integer.parseInt(text);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}