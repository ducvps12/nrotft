package nro.server.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import consts.ConstNpc;
import event.EventManager;
import jdbc.DBConnecter;
import nro.server.Manager;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.*;
import java.util.*;
import java.util.List;

public class NpcManagerPanel extends JPanel {

    private static final String ICON_FOLDER = "data/icon/";
    private static final Font FONT = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 13);

    private JTable table;
    private DefaultTableModel model;
    private JTextField txtSearch;
    private final Map<Integer, Integer> partIconMap = new HashMap<>();
    private final Map<Integer, ImageIcon> iconCache = new HashMap<>();

    // Cache: partId -> {iconId, type}
    private final List<int[]> allParts = new ArrayList<>(); // [id, iconId, type]

    // NPC handler mapping: npc_template.id -> ConstNpc field name
    private final Map<Integer, String> npcHandlerMap = new HashMap<>();
    // NPC map placement: npc_template.id -> list of map IDs
    private final Map<Integer, List<Integer>> npcMapPlacement = new HashMap<>();

    public NpcManagerPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(15, 15, 15, 15));
        buildNpcHandlerMap();
        loadPartIcons();
    }

    /**
     * Build mapping từ ConstNpc field names → NPC ID values
     * và mapping NPC ID → maps (từ game data Manager.MAPS)
     */
    private void buildNpcHandlerMap() {
        try {
            for (Field f : ConstNpc.class.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) && Modifier.isFinal(f.getModifiers())
                        && f.getType() == byte.class) {
                    int val = f.getByte(null) & 0xFF;
                    npcHandlerMap.put(val, f.getName());
                }
            }
        } catch (Exception ignored) {}

        // Load NPC placement from map data
        try {
            if (Manager.MAPS != null) {
                for (map.Map m : Manager.MAPS) {
                    if (m.npcs != null) {
                        for (var npc : m.npcs) {
                            npcMapPlacement.computeIfAbsent(npc.tempId, k -> new ArrayList<>()).add(m.mapId);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    /**
     * Lấy tên event liên kết với NPC ID (nếu có)
     */
    private String getEventForNpc(int npcConstId) {
        // Map ConstNpc ID -> Event name dựa trên logic trong EventManager
        return switch (npcConstId) {
            case ConstNpc.SANTA -> EventManager.CHRISTMAS ? "✅ Giáng Sinh" : "❌ Giáng Sinh";
            case ConstNpc.HUNG_VUONG -> EventManager.HUNG_VUONG ? "✅ Giỗ Tổ" : "❌ Giỗ Tổ";
            case ConstNpc.NPC_TRUNG_THU, ConstNpc.TRUNG_THU -> EventManager.TRUNG_THU ? "✅ Trung Thu" : "❌ Trung Thu";
            case ConstNpc.PHO_ANH_HAI -> EventManager.PHO_ANH_HAI ? "✅ Phở Anh Hai" : "❌ Phở Anh Hai";
            case ConstNpc.NPC_DIA_NGUC -> EventManager.DIA_NGUC ? "✅ Địa Ngục" : "❌ Địa Ngục";
            case ConstNpc.CAY_THONG, ConstNpc.CAY_NEU -> "Trang trí";
            case ConstNpc.HOA_HONG, ConstNpc.HAI_HOA_HONG -> "8/3";
            case ConstNpc.QUA_TRUNG, ConstNpc.DUA_HAU -> "Sự kiện mùa";
            case ConstNpc.NOI_BANH -> "Tết";
            case ConstNpc.ONG_GOHAN, ConstNpc.ONG_PARAGUS, ConstNpc.ONG_MOORI -> "NPC Làng";
            case ConstNpc.BUNMA, ConstNpc.DENDE, ConstNpc.APPULE -> "NPC Làng";
            case ConstNpc.DR_DRIEF, ConstNpc.CARGO, ConstNpc.CUI -> "NPC Làng";
            case ConstNpc.QUY_LAO_KAME -> "NPC Chính";
            case ConstNpc.THUONG_DE -> "NPC Chính";
            case ConstNpc.THAN_VU_TRU -> "NPC Chính";
            case ConstNpc.BA_HAT_MIT -> "NPC Chính";
            case ConstNpc.THAN_MEO_KARIN -> "NPC Chính";
            case ConstNpc.URON -> "Shop";
            case ConstNpc.RUONG_DO -> "Rương đồ";
            case ConstNpc.DAU_THAN -> "Cây Đậu Thần";
            case ConstNpc.CUA_HANG_KY_GUI -> "Ký gửi";
            case ConstNpc.BANG_DANH_VONG -> "Danh vọng";
            case ConstNpc.RUONG_SUU_TAP -> "Sưu tập";
            default -> "";
        };
    }

    private void loadPartIcons() {
        new Thread(() -> {
            try (Connection conn = DBConnecter.getConnectionServer();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id, type, data FROM part")) {
                while (rs.next()) {
                    try {
                        int id = rs.getInt("id");
                        int type = rs.getInt("type");
                        JsonArray arr = new JsonParser().parse(rs.getString("data")).getAsJsonArray();
                        if (arr.size() > 0) {
                            int iconId = arr.get(0).getAsJsonArray().get(0).getAsInt();
                            if (type == 0) partIconMap.put(id, iconId);
                            allParts.add(new int[]{id, iconId, type});
                        }
                    } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
            SwingUtilities.invokeLater(() -> { initUI(); loadData(); });
        }).start();
    }

    private ImageIcon getPartIcon(int partId, int size) {
        if (partId <= 0) return null;
        String cacheKey = partId + "_" + size;
        int hash = cacheKey.hashCode();
        if (iconCache.containsKey(hash)) return iconCache.get(hash);

        Integer iconId = null;
        for (int[] p : allParts) {
            if (p[0] == partId) { iconId = p[1]; break; }
        }
        if (iconId == null) iconId = partIconMap.get(partId);
        if (iconId == null) return null;

        try {
            for (String z : new String[]{"x4", "x3", "x2", "x1"}) {
                File f = new File(ICON_FOLDER + z + "/" + iconId + ".png");
                if (f.exists()) {
                    Image img = ImageIO.read(f).getScaledInstance(size, size, Image.SCALE_SMOOTH);
                    ImageIcon icon = new ImageIcon(img);
                    iconCache.put(hash, icon);
                    return icon;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void initUI() {
        JLabel lblTitle = new JLabel("QUẢN LÝ NPC TEMPLATE");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(new Color(60, 60, 60));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        searchPanel.setOpaque(false);

        txtSearch = new JTextField(20);
        txtSearch.setFont(FONT);
        txtSearch.setPreferredSize(new Dimension(250, 35));
        txtSearch.putClientProperty("JTextField.placeholderText", "Tìm NPC theo tên hoặc ID...");

        JButton btnSearch = ServerGuiUtils.createStyledButton("Tìm Kiếm", new Color(0, 120, 215), Color.WHITE);
        JButton btnReload = ServerGuiUtils.createStyledButton("Tải Lại", new Color(40, 167, 69), Color.WHITE);
        JButton btnAdd = ServerGuiUtils.createStyledButton("+ Thêm NPC Mới", new Color(255, 140, 0), Color.WHITE);

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

        // Table - thêm cột Maps và Event/Handler
        String[] cols = {"Avatar", "ID", "Tên NPC", "Head", "Body", "Leg", "Avatar ID", "Maps", "Handler", "Event/Loại", "Shop"};
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
                else if (c == 7) { setFont(FONT); setForeground(new Color(128, 0, 128)); } // Maps: tím
                else if (c == 8) { setFont(new Font("Consolas", Font.PLAIN, 11)); setForeground(new Color(100, 100, 100)); } // Handler: xám
                else if (c == 9) { // Event: color-coded
                    String val = v != null ? v.toString() : "";
                    if (val.startsWith("✅")) setForeground(new Color(40, 167, 69));
                    else if (val.startsWith("❌")) setForeground(new Color(220, 53, 69));
                    else if (val.contains("NPC")) setForeground(new Color(0, 102, 204));
                    else setForeground(new Color(150, 150, 150));
                    setFont(FONT_BOLD);
                }
                else { setFont(FONT); setForeground(Color.BLACK); }
                return comp;
            }
        });

        TableColumnModel cm = table.getColumnModel();
        cm.getColumn(0).setPreferredWidth(50);   // Avatar
        cm.getColumn(1).setPreferredWidth(40);   // ID
        cm.getColumn(2).setPreferredWidth(130);  // Tên NPC
        cm.getColumn(3).setPreferredWidth(50);   // Head
        cm.getColumn(4).setPreferredWidth(50);   // Body
        cm.getColumn(5).setPreferredWidth(50);   // Leg
        cm.getColumn(6).setPreferredWidth(50);   // Avatar ID
        cm.getColumn(7).setPreferredWidth(100);  // Maps
        cm.getColumn(8).setPreferredWidth(100);  // Handler
        cm.getColumn(9).setPreferredWidth(100);  // Event/Loại
        cm.getColumn(10).setPreferredWidth(100); // Shop

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
                while (rs.next()) addRow(rs);
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
                try (ResultSet rs = ps.executeQuery()) { while (rs.next()) addRow(rs); }
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
        ImageIcon icon = getPartIcon(head, 32);

        // Maps placement
        List<Integer> maps = npcMapPlacement.get(id);
        String mapsStr = (maps != null && !maps.isEmpty()) ? maps.toString() : "-";

        // Handler name from ConstNpc
        String handler = npcHandlerMap.getOrDefault(id, "-");

        // Event/Loại
        String eventType = getEventForNpc(id);

        SwingUtilities.invokeLater(() -> model.addRow(new Object[]{
            icon, id, name, head, body, leg, avatar, mapsStr, handler, eventType, shops != null ? shops : "-"
        }));
    }

    // ==============================================
    // EDIT / ADD DIALOG - với visual picker
    // ==============================================
    private void openEditDialog(int npcId) {
        boolean isNew = (npcId < 0);
        JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                isNew ? "Thêm NPC mới" : "Sửa NPC #" + npcId, true);
        d.setSize(600, 520);
        d.setLocationRelativeTo(this);
        d.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(15, 20, 10, 20));
        form.setBackground(Color.WHITE);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 5, 6, 5);
        g.fill = GridBagConstraints.HORIZONTAL;

        JTextField txtId = new JTextField(); txtId.setEditable(false);
        JTextField txtName = new JTextField();
        JTextField txtHead = new JTextField();
        JTextField txtBody = new JTextField();
        JTextField txtLeg = new JTextField();
        JTextField txtAvatar = new JTextField();

        // Preview label
        JLabel lblPreview = new JLabel();
        lblPreview.setHorizontalAlignment(SwingConstants.CENTER);
        lblPreview.setPreferredSize(new Dimension(64, 64));
        lblPreview.setBorder(BorderFactory.createLineBorder(new Color(200,200,200), 1));
        lblPreview.setToolTipText("Preview avatar");

        // Update preview khi head thay đổi
        Runnable updatePreview = () -> {
            try {
                int hid = Integer.parseInt(txtHead.getText().trim());
                ImageIcon ic = getPartIcon(hid, 56);
                lblPreview.setIcon(ic);
                lblPreview.setText(ic == null ? "?" : "");
            } catch (Exception ex) { lblPreview.setIcon(null); lblPreview.setText("?"); }
        };
        txtHead.addActionListener(e -> updatePreview.run());
        txtHead.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) { updatePreview.run(); }
        });

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
            updatePreview.run();
        }

        int row = 0;
        // Preview ở trên cùng
        g.gridx = 0; g.gridy = row; g.weightx = 0; g.gridwidth = 1;
        form.add(new JLabel("Preview:"), g);
        g.gridx = 1; g.weightx = 1;
        JPanel previewRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        previewRow.setOpaque(false);
        previewRow.add(lblPreview);
        form.add(previewRow, g);
        row++;

        g.gridwidth = 1;
        addFormRow(form, g, row++, "ID:", txtId);
        addFormRow(form, g, row++, "Tên NPC:", txtName);
        addFormRowWithPicker(form, g, row++, "Head Part:", txtHead, 0, d, updatePreview);
        addFormRowWithPicker(form, g, row++, "Body Part:", txtBody, 1, d, null);
        addFormRowWithPicker(form, g, row++, "Leg Part:", txtLeg, 2, d, null);
        addFormRowWithPicker(form, g, row++, "Avatar ID:", txtAvatar, -1, d, null);

        // Buttons
        JPanel pBtn = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
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
                        d.dispose(); loadData();
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

        pBtn.add(btnDelete); pBtn.add(btnCancel); pBtn.add(btnSave);
        d.add(form, BorderLayout.CENTER);
        d.add(pBtn, BorderLayout.SOUTH);
        d.setVisible(true);
    }

    /**
     * Thêm row có nút "Chọn" mở visual picker
     */
    private void addFormRowWithPicker(JPanel p, GridBagConstraints g, int row,
                                       String label, JTextField field, int partType,
                                       JDialog parent, Runnable onSelect) {
        g.gridx = 0; g.gridy = row; g.weightx = 0;
        JLabel lbl = new JLabel(label);
        lbl.setFont(FONT_BOLD);
        p.add(lbl, g);

        g.gridx = 1; g.weightx = 1;
        JPanel rowPanel = new JPanel(new BorderLayout(5, 0));
        rowPanel.setOpaque(false);
        field.setFont(FONT);
        rowPanel.add(field, BorderLayout.CENTER);

        if (partType >= 0) { // -1 = avatar (no picker)
            JButton btnPick = new JButton("Chọn...");
            btnPick.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            btnPick.setPreferredSize(new Dimension(70, 28));
            btnPick.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnPick.addActionListener(e -> openPartPicker(parent, field, partType, onSelect));
            rowPanel.add(btnPick, BorderLayout.EAST);
        }
        p.add(rowPanel, g);
    }

    /**
     * Mở dialog chọn Part với hình ảnh grid
     */
    private void openPartPicker(JDialog parent, JTextField targetField, int partType, Runnable onSelect) {
        String title;
        switch (partType) {
            case 0 -> title = "Chọn Head Part";
            case 1 -> title = "Chọn Body Part";
            case 2 -> title = "Chọn Leg Part";
            default -> title = "Chọn Part";
        }

        JDialog picker = new JDialog(parent, title, true);
        picker.setSize(700, 500);
        picker.setLocationRelativeTo(parent);
        picker.setLayout(new BorderLayout());

        // Filter bar
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        filterBar.setBackground(new Color(245, 245, 245));
        JTextField txtFilter = new JTextField(15);
        txtFilter.putClientProperty("JTextField.placeholderText", "Lọc theo ID...");
        JLabel lblCount = new JLabel();
        lblCount.setFont(FONT);
        filterBar.add(new JLabel("Lọc:"));
        filterBar.add(txtFilter);
        filterBar.add(lblCount);
        picker.add(filterBar, BorderLayout.NORTH);

        // Grid panel
        JPanel grid = new JPanel(new GridLayout(0, 6, 6, 6));
        grid.setBackground(Color.WHITE);
        grid.setBorder(new EmptyBorder(10, 10, 10, 10));
        JScrollPane scroll = new JScrollPane(grid);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        picker.add(scroll, BorderLayout.CENTER);

        // Lọc parts theo type
        List<int[]> filtered = new ArrayList<>();
        for (int[] pt : allParts) {
            if (pt[2] == partType) filtered.add(pt);
        }
        filtered.sort(Comparator.comparingInt(a -> a[0]));

        Runnable populateGrid = () -> {
            grid.removeAll();
            String filterText = txtFilter.getText().trim();
            int count = 0;
            for (int[] pt : filtered) {
                if (!filterText.isEmpty()) {
                    try {
                        if (!String.valueOf(pt[0]).contains(filterText)) continue;
                    } catch (Exception ignored) { continue; }
                }
                count++;
                int partId = pt[0];
                int iconId = pt[1];

                JPanel cell = new JPanel(new BorderLayout(2, 2));
                cell.setBackground(Color.WHITE);
                cell.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(220,220,220), 1),
                    new EmptyBorder(4, 4, 4, 4)));
                cell.setCursor(new Cursor(Cursor.HAND_CURSOR));
                cell.setPreferredSize(new Dimension(90, 80));

                // Icon
                JLabel lblIcon = new JLabel();
                lblIcon.setHorizontalAlignment(SwingConstants.CENTER);
                ImageIcon ic = getPartIcon(partId, 40);
                if (ic != null) lblIcon.setIcon(ic);
                else lblIcon.setText("?");
                cell.add(lblIcon, BorderLayout.CENTER);

                // ID label
                JLabel lblId = new JLabel("ID: " + partId, SwingConstants.CENTER);
                lblId.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                lblId.setForeground(new Color(100, 100, 100));
                cell.add(lblId, BorderLayout.SOUTH);

                // Click to select
                cell.addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) {
                        targetField.setText(String.valueOf(partId));
                        if (onSelect != null) onSelect.run();
                        picker.dispose();
                    }
                    public void mouseEntered(MouseEvent e) {
                        cell.setBackground(new Color(230, 242, 255));
                        cell.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(0, 120, 215), 2),
                            new EmptyBorder(3, 3, 3, 3)));
                    }
                    public void mouseExited(MouseEvent e) {
                        cell.setBackground(Color.WHITE);
                        cell.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(220,220,220), 1),
                            new EmptyBorder(4, 4, 4, 4)));
                    }
                });

                grid.add(cell);
            }
            lblCount.setText("Hiện " + count + " / " + filtered.size() + " parts");
            grid.revalidate();
            grid.repaint();
        };

        populateGrid.run();

        txtFilter.addActionListener(e -> populateGrid.run());
        txtFilter.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { populateGrid.run(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { populateGrid.run(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { populateGrid.run(); }
        });

        picker.setVisible(true);
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
