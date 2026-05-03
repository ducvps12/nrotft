package nro.server.ui;

import boss.BossData;
import boss.BossID;
import boss.BossesData;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import jdbc.DBConnecter;
import models.Template.ItemTemplate;
import nro.server.Manager;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
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

/**
 * Panel Quản Lý Drop Item — 3 Tabs:
 * 1. Cấu hình Drop Mob (DB: mob_drop_config)
 * 2. Boss Reward Viewer (Reflection từ BossesData)
 * 3. Tra cứu Vật Phẩm (item_template browser)
 */
public class DropItemPanel extends JPanel {

    // --- Constants ---
    private static final String ICON_FOLDER = "data/icon/";
    private static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font FONT_PLAIN = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Color COL_PRIMARY = new Color(0, 120, 215);
    private static final Color COL_BG = Color.WHITE;

    // --- Cache ---
    private final Map<Integer, String> itemTemplateMap = new HashMap<>();
    private final Map<Integer, Integer> itemIconMap = new HashMap<>();
    private final Map<Integer, ImageIcon> iconCache = new HashMap<>();
    private final Map<Integer, Boolean> noIconCache = new HashMap<>();
    private final List<ItemData> listAllItems = new ArrayList<>();

    // --- Tab 1: Mob Drop ---
    private DefaultTableModel mobDropModel;
    private JTable mobDropTable;

    // --- Tab 2: Boss Reward ---
    private DefaultTableModel bossRewardModel;

    // --- Tab 3: Item Catalog ---
    private DefaultTableModel itemCatalogModel;
    private TableRowSorter<DefaultTableModel> catalogSorter;

    private JTextArea txtLog;

    static class ItemData {
        int id;
        String name;
        int type;
        int gender;
        int iconId;

        ItemData(int id, String name, int type, int gender, int iconId) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.gender = gender;
            this.iconId = iconId;
        }
    }

    public DropItemPanel() {
        setLayout(new BorderLayout(0, 10));
        setBackground(COL_BG);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        JLabel title = new JLabel("🎁 Quản Lý Drop & Vật Phẩm");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(new Color(50, 50, 50));
        JLabel subtitle = new JLabel("Cấu hình drop mob, xem reward boss, tra cứu vật phẩm");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(new Color(120, 120, 120));
        JPanel textPanel = new JPanel(new GridLayout(2, 1));
        textPanel.setOpaque(false);
        textPanel.add(title);
        textPanel.add(subtitle);
        headerPanel.add(textPanel, BorderLayout.WEST);
        add(headerPanel, BorderLayout.NORTH);

        // Tabs
        JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabs.setBackground(COL_BG);

        tabs.addTab("⚔ Drop Mob Config", createMobDropTab());
        tabs.addTab("👹 Boss Reward Viewer", createBossRewardTab());
        tabs.addTab("📦 Tra Cứu Vật Phẩm", createItemCatalogTab());
        tabs.addTab("📊 Hardcode Drop (Source)", createHardcodeDropTab());

        add(tabs, BorderLayout.CENTER);

        // Load cache in background only after all tabs (and their models) are created
        new Thread(this::loadCacheData).start();

        // Log Panel
        txtLog = new JTextArea(3, 50);
        txtLog.setEditable(false);
        txtLog.setFont(new Font("Monospaced", Font.PLAIN, 11));
        txtLog.setBackground(new Color(250, 250, 250));
        JScrollPane logScroll = new JScrollPane(txtLog);
        logScroll.setBorder(ServerGuiUtils.createSectionBorder("📝 Drop Manager Log"));
        logScroll.setPreferredSize(new Dimension(0, 80));
        add(logScroll, BorderLayout.SOUTH);

        log("Panel initialized. Loading item data...");
    }

    // ===================================================================
    // CACHE LOADING
    // ===================================================================
    private void loadCacheData() {
        try (Connection conn = DBConnecter.getConnectionServer();
             Statement stmt = conn.createStatement()) {

            try (ResultSet rs = stmt.executeQuery("SELECT id, name, icon_id, type, gender FROM item_template")) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    int iconId = rs.getInt("icon_id");
                    int type = rs.getInt("type");
                    int gender = rs.getInt("gender");
                    itemTemplateMap.put(id, name);
                    itemIconMap.put(id, iconId);
                    listAllItems.add(new ItemData(id, name, type, gender, iconId));
                }
            }
            log("Loaded " + listAllItems.size() + " item templates.");

            // After cache loaded, populate tabs
            SwingUtilities.invokeLater(() -> {
                loadMobDropData();
                loadBossRewardData();
                loadItemCatalogData();
            });

        } catch (Exception e) {
            log("ERROR loading cache: " + e.getMessage());
        }
    }

    private ImageIcon getItemIcon(int itemId, int size) {
        int cacheKey = itemId * 1000 + size;
        if (iconCache.containsKey(cacheKey)) return iconCache.get(cacheKey);
        if (noIconCache.containsKey(itemId)) return null;

        try {
            int iconId = itemIconMap.getOrDefault(itemId, -1);
            if (iconId == -1) {
                noIconCache.put(itemId, true);
                return null;
            }
            String[] zoomLevels = {"x4", "x3", "x2", "x1"};
            for (String zoom : zoomLevels) {
                File f = new File(ICON_FOLDER + zoom + "/" + iconId + ".png");
                if (f.exists()) {
                    BufferedImage img = ImageIO.read(f);
                    Image dimg = img.getScaledInstance(size, size, Image.SCALE_SMOOTH);
                    ImageIcon icon = new ImageIcon(dimg);
                    iconCache.put(cacheKey, icon);
                    return icon;
                }
            }
        } catch (Exception e) { /* ignore */ }
        noIconCache.put(itemId, true);
        return null;
    }

    private String getItemName(int id) {
        return itemTemplateMap.getOrDefault(id, "Unknown (" + id + ")");
    }

    // ===================================================================
    // TAB 1: MOB DROP CONFIG
    // ===================================================================
    private JPanel createMobDropTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Info banner
        JPanel infoBanner = new JPanel(new FlowLayout(FlowLayout.LEFT));
        infoBanner.setBackground(new Color(232, 245, 233));
        infoBanner.setBorder(new CompoundBorder(
            new LineBorder(new Color(76, 175, 80), 1, true),
            new EmptyBorder(8, 12, 8, 12)
        ));
        JLabel lblInfo = new JLabel("💡 Cấu hình drop item cho mob thường. Dữ liệu lưu trong bảng mob_drop_config (tự tạo nếu chưa có).");
        lblInfo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblInfo.setForeground(new Color(27, 94, 32));
        infoBanner.add(lblInfo);

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        toolbar.setOpaque(false);

        JButton btnAdd = ServerGuiUtils.createStyledButton("+ Thêm Rule", new Color(40, 167, 69), Color.WHITE);
        btnAdd.addActionListener(e -> openMobDropEditor(-1));

        JButton btnEdit = ServerGuiUtils.createStyledButton("✏ Sửa", new Color(255, 152, 0), Color.WHITE);
        btnEdit.addActionListener(e -> {
            int row = mobDropTable.getSelectedRow();
            if (row >= 0) {
                int modelRow = mobDropTable.convertRowIndexToModel(row);
                openMobDropEditor((int) mobDropModel.getValueAt(modelRow, 0));
            } else {
                JOptionPane.showMessageDialog(this, "Chọn một dòng để sửa!");
            }
        });

        JButton btnDelete = ServerGuiUtils.createStyledButton("🗑 Xóa", new Color(220, 53, 69), Color.WHITE);
        btnDelete.addActionListener(e -> deleteMobDropRule());

        JButton btnRefresh = ServerGuiUtils.createStyledButton("🔄 Làm mới", new Color(108, 117, 125), Color.WHITE);
        btnRefresh.addActionListener(e -> loadMobDropData());

        JButton btnInitTable = ServerGuiUtils.createStyledButton("📋 Tạo bảng DB", new Color(0, 123, 255), Color.WHITE);
        btnInitTable.addActionListener(e -> initMobDropTable());

        toolbar.add(btnAdd);
        toolbar.add(btnEdit);
        toolbar.add(btnDelete);
        toolbar.add(btnRefresh);
        toolbar.add(btnInitTable);

        // Table
        String[] cols = {"ID", "Map ID", "Mob ID", "Item ID", "Tên Item", "SL Min", "SL Max", "Tỷ lệ (%)", "Ghi chú"};
        mobDropModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        mobDropTable = new JTable(mobDropModel);
        mobDropTable.setFont(FONT_PLAIN);
        mobDropTable.setRowHeight(30);
        mobDropTable.setShowGrid(true);
        mobDropTable.setGridColor(new Color(230, 230, 230));
        mobDropTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JTableHeader header = mobDropTable.getTableHeader();
        header.setFont(FONT_BOLD);
        header.setBackground(new Color(63, 81, 181));
        header.setForeground(Color.WHITE);
        header.setPreferredSize(new Dimension(0, 35));

        // Column widths
        TableColumnModel cm = mobDropTable.getColumnModel();
        cm.getColumn(0).setPreferredWidth(50);
        cm.getColumn(1).setPreferredWidth(70);
        cm.getColumn(2).setPreferredWidth(70);
        cm.getColumn(3).setPreferredWidth(70);
        cm.getColumn(4).setPreferredWidth(180);
        cm.getColumn(5).setPreferredWidth(60);
        cm.getColumn(6).setPreferredWidth(60);
        cm.getColumn(7).setPreferredWidth(80);
        cm.getColumn(8).setPreferredWidth(150);

        // Alternating row colors
        mobDropTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                if (!sel) comp.setBackground(r % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                else comp.setBackground(new Color(220, 235, 255));
                if (c == 7) { // Tỷ lệ column
                    setForeground(new Color(220, 53, 69));
                    setFont(new Font("Consolas", Font.BOLD, 13));
                } else if (c == 4) {
                    setForeground(new Color(0, 102, 204));
                    setFont(FONT_BOLD);
                } else {
                    setForeground(Color.BLACK);
                    setFont(FONT_PLAIN);
                }
                setHorizontalAlignment(c <= 3 || c == 5 || c == 6 || c == 7 ? JLabel.CENTER : JLabel.LEFT);
                return comp;
            }
        });

        // Double click to edit
        mobDropTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && mobDropTable.getSelectedRow() >= 0) {
                    int modelRow = mobDropTable.convertRowIndexToModel(mobDropTable.getSelectedRow());
                    openMobDropEditor((int) mobDropModel.getValueAt(modelRow, 0));
                }
            }
        });

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(infoBanner, BorderLayout.NORTH);
        topPanel.add(toolbar, BorderLayout.SOUTH);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(mobDropTable), BorderLayout.CENTER);
        return panel;
    }

    private void initMobDropTable() {
        new Thread(() -> {
            try (Connection conn = DBConnecter.getConnectionServer();
                 Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS mob_drop_config (" +
                    "  id INT AUTO_INCREMENT PRIMARY KEY," +
                    "  map_id INT DEFAULT -1 COMMENT 'Map ID (-1 = all maps)'," +
                    "  mob_id INT DEFAULT -1 COMMENT 'Mob ID (-1 = all mobs)'," +
                    "  item_id INT NOT NULL COMMENT 'Item template ID'," +
                    "  quantity_min INT DEFAULT 1," +
                    "  quantity_max INT DEFAULT 1," +
                    "  drop_rate DOUBLE DEFAULT 10.0 COMMENT 'Tỷ lệ % (0-100)'," +
                    "  note VARCHAR(200) DEFAULT ''," +
                    "  active TINYINT DEFAULT 1," +
                    "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
                );
                log("✅ Bảng mob_drop_config đã được tạo/kiểm tra thành công.");
                SwingUtilities.invokeLater(this::loadMobDropData);
            } catch (Exception e) {
                log("❌ Lỗi tạo bảng: " + e.getMessage());
            }
        }).start();
    }

    private void loadMobDropData() {
        mobDropModel.setRowCount(0);
        new Thread(() -> {
            try (Connection conn = DBConnecter.getConnectionServer();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM mob_drop_config WHERE active = 1 ORDER BY id")) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    int mapId = rs.getInt("map_id");
                    int mobId = rs.getInt("mob_id");
                    int itemId = rs.getInt("item_id");
                    String itemName = getItemName(itemId);
                    int qMin = rs.getInt("quantity_min");
                    int qMax = rs.getInt("quantity_max");
                    double rate = rs.getDouble("drop_rate");
                    String note = rs.getString("note");

                    SwingUtilities.invokeLater(() ->
                        mobDropModel.addRow(new Object[]{
                            id,
                            mapId == -1 ? "Tất cả" : mapId,
                            mobId == -1 ? "Tất cả" : mobId,
                            itemId, itemName, qMin, qMax,
                            String.format("%.1f%%", rate),
                            note != null ? note : ""
                        })
                    );
                }
                log("Loaded mob drop rules.");
            } catch (Exception e) {
                log("⚠ Bảng mob_drop_config chưa tồn tại. Nhấn 'Tạo bảng DB' để tạo.");
            }
        }).start();
    }

    private void openMobDropEditor(int editId) {
        JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
            editId == -1 ? "Thêm Drop Rule" : "Sửa Drop Rule #" + editId, true);
        d.setSize(500, 400);
        d.setLocationRelativeTo(this);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(20, 20, 20, 20));
        form.setBackground(COL_BG);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 8, 6, 8);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;

        JTextField tfMapId = new JTextField("-1", 10);
        JTextField tfMobId = new JTextField("-1", 10);
        JTextField tfItemId = new JTextField("", 10);
        JLabel lblItemName = new JLabel("...");
        lblItemName.setFont(FONT_BOLD);
        lblItemName.setForeground(COL_PRIMARY);
        JTextField tfQtyMin = new JTextField("1", 6);
        JTextField tfQtyMax = new JTextField("1", 6);
        JTextField tfRate = new JTextField("10.0", 8);
        JTextField tfNote = new JTextField("", 20);

        // Auto-resolve item name when typing
        tfItemId.getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override
            public void update() {
                try {
                    int id = Integer.parseInt(tfItemId.getText().trim());
                    lblItemName.setText(getItemName(id));
                } catch (Exception ex) {
                    lblItemName.setText("...");
                }
            }
        });

        // Load existing data if editing
        if (editId > 0) {
            try (Connection conn = DBConnecter.getConnectionServer();
                 PreparedStatement ps = conn.prepareStatement("SELECT * FROM mob_drop_config WHERE id = ?")) {
                ps.setInt(1, editId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    tfMapId.setText(String.valueOf(rs.getInt("map_id")));
                    tfMobId.setText(String.valueOf(rs.getInt("mob_id")));
                    tfItemId.setText(String.valueOf(rs.getInt("item_id")));
                    lblItemName.setText(getItemName(rs.getInt("item_id")));
                    tfQtyMin.setText(String.valueOf(rs.getInt("quantity_min")));
                    tfQtyMax.setText(String.valueOf(rs.getInt("quantity_max")));
                    tfRate.setText(String.valueOf(rs.getDouble("drop_rate")));
                    tfNote.setText(rs.getString("note"));
                }
            } catch (Exception e) {
                log("Error loading rule: " + e.getMessage());
            }
        }

        // Layout
        int row = 0;
        g.gridx = 0; g.gridy = row; form.add(new JLabel("Map ID (-1 = tất cả):"), g);
        g.gridx = 1; form.add(tfMapId, g);

        row++;
        g.gridx = 0; g.gridy = row; form.add(new JLabel("Mob ID (-1 = tất cả):"), g);
        g.gridx = 1; form.add(tfMobId, g);

        row++;
        g.gridx = 0; g.gridy = row; form.add(new JLabel("Item ID:"), g);
        JPanel itemRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        itemRow.setOpaque(false);
        itemRow.add(tfItemId);
        JButton btnSearch = new JButton("🔍");
        btnSearch.addActionListener(e -> showQuickItemSearch(d, (id, name) -> {
            tfItemId.setText(String.valueOf(id));
            lblItemName.setText(name);
        }));
        itemRow.add(btnSearch);
        g.gridx = 1; form.add(itemRow, g);

        row++;
        g.gridx = 0; g.gridy = row; form.add(new JLabel("Tên item:"), g);
        g.gridx = 1; form.add(lblItemName, g);

        row++;
        g.gridx = 0; g.gridy = row; form.add(new JLabel("Số lượng (Min - Max):"), g);
        JPanel qtyRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        qtyRow.setOpaque(false);
        qtyRow.add(tfQtyMin);
        qtyRow.add(new JLabel(" - "));
        qtyRow.add(tfQtyMax);
        g.gridx = 1; form.add(qtyRow, g);

        row++;
        g.gridx = 0; g.gridy = row; form.add(new JLabel("Tỷ lệ drop (%):"), g);
        g.gridx = 1; form.add(tfRate, g);

        row++;
        g.gridx = 0; g.gridy = row; form.add(new JLabel("Ghi chú:"), g);
        g.gridx = 1; form.add(tfNote, g);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setBackground(new Color(245, 245, 245));
        btnPanel.setBorder(new MatteBorder(1, 0, 0, 0, new Color(220, 220, 220)));

        JButton btnSave = ServerGuiUtils.createStyledButton("💾 Lưu", new Color(40, 167, 69), Color.WHITE);
        btnSave.addActionListener(e -> {
            saveMobDropRule(editId, tfMapId, tfMobId, tfItemId, tfQtyMin, tfQtyMax, tfRate, tfNote);
            d.dispose();
        });

        JButton btnCancel = new JButton("Đóng");
        btnCancel.addActionListener(e -> d.dispose());

        btnPanel.add(btnCancel);
        btnPanel.add(btnSave);

        d.setLayout(new BorderLayout());
        d.add(form, BorderLayout.CENTER);
        d.add(btnPanel, BorderLayout.SOUTH);
        d.setVisible(true);
    }

    private void saveMobDropRule(int editId, JTextField mapId, JTextField mobId, JTextField itemId,
                                  JTextField qMin, JTextField qMax, JTextField rate, JTextField note) {
        new Thread(() -> {
            try (Connection conn = DBConnecter.getConnectionServer()) {
                if (editId <= 0) {
                    // INSERT
                    PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO mob_drop_config (map_id, mob_id, item_id, quantity_min, quantity_max, drop_rate, note) VALUES (?,?,?,?,?,?,?)");
                    ps.setInt(1, Integer.parseInt(mapId.getText().trim()));
                    ps.setInt(2, Integer.parseInt(mobId.getText().trim()));
                    ps.setInt(3, Integer.parseInt(itemId.getText().trim()));
                    ps.setInt(4, Integer.parseInt(qMin.getText().trim()));
                    ps.setInt(5, Integer.parseInt(qMax.getText().trim()));
                    ps.setDouble(6, Double.parseDouble(rate.getText().trim()));
                    ps.setString(7, note.getText().trim());
                    ps.executeUpdate();
                    log("✅ Đã thêm drop rule mới.");
                } else {
                    // UPDATE
                    PreparedStatement ps = conn.prepareStatement(
                        "UPDATE mob_drop_config SET map_id=?, mob_id=?, item_id=?, quantity_min=?, quantity_max=?, drop_rate=?, note=? WHERE id=?");
                    ps.setInt(1, Integer.parseInt(mapId.getText().trim()));
                    ps.setInt(2, Integer.parseInt(mobId.getText().trim()));
                    ps.setInt(3, Integer.parseInt(itemId.getText().trim()));
                    ps.setInt(4, Integer.parseInt(qMin.getText().trim()));
                    ps.setInt(5, Integer.parseInt(qMax.getText().trim()));
                    ps.setDouble(6, Double.parseDouble(rate.getText().trim()));
                    ps.setString(7, note.getText().trim());
                    ps.setInt(8, editId);
                    ps.executeUpdate();
                    log("✅ Đã cập nhật drop rule #" + editId);
                }
                SwingUtilities.invokeLater(this::loadMobDropData);
            } catch (Exception e) {
                log("❌ Lỗi lưu: " + e.getMessage());
                SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this, "Lỗi: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE));
            }
        }).start();
    }

    private void deleteMobDropRule() {
        int row = mobDropTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Chọn một dòng để xóa!");
            return;
        }
        int modelRow = mobDropTable.convertRowIndexToModel(row);
        int id = (int) mobDropModel.getValueAt(modelRow, 0);
        if (JOptionPane.showConfirmDialog(this, "Xóa drop rule #" + id + "?",
            "Xác nhận", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            new Thread(() -> {
                try (Connection conn = DBConnecter.getConnectionServer();
                     PreparedStatement ps = conn.prepareStatement("DELETE FROM mob_drop_config WHERE id = ?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                    log("✅ Đã xóa drop rule #" + id);
                    SwingUtilities.invokeLater(this::loadMobDropData);
                } catch (Exception e) {
                    log("❌ Lỗi xóa: " + e.getMessage());
                }
            }).start();
        }
    }

    // ===================================================================
    // TAB 2: BOSS REWARD VIEWER
    // ===================================================================
    private JPanel createBossRewardTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Search
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        searchPanel.setOpaque(false);
        JTextField txtSearch = new JTextField(25);
        txtSearch.putClientProperty("JTextField.placeholderText", "Tìm Boss theo tên hoặc ID...");
        txtSearch.setPreferredSize(new Dimension(300, 32));
        txtSearch.setFont(FONT_PLAIN);

        JButton btnRefresh = ServerGuiUtils.createStyledButton("🔄 Làm mới", new Color(108, 117, 125), Color.WHITE);
        btnRefresh.addActionListener(e -> loadBossRewardData());

        searchPanel.add(new JLabel("🔍 Tìm kiếm:"));
        searchPanel.add(txtSearch);
        searchPanel.add(btnRefresh);

        // Table
        String[] cols = {"Boss ID", "Key", "Tên Boss", "HP", "Damage", "Drop Items", "Tỷ lệ drop (%)"};
        bossRewardModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(bossRewardModel);
        table.setFont(FONT_PLAIN);
        table.setRowHeight(32);
        table.setShowGrid(true);
        table.setGridColor(new Color(230, 230, 230));

        JTableHeader header = table.getTableHeader();
        header.setFont(FONT_BOLD);
        header.setBackground(new Color(156, 39, 176));
        header.setForeground(Color.WHITE);
        header.setPreferredSize(new Dimension(0, 35));

        // Column widths
        TableColumnModel cm = table.getColumnModel();
        cm.getColumn(0).setPreferredWidth(60);
        cm.getColumn(1).setPreferredWidth(140);
        cm.getColumn(2).setPreferredWidth(150);
        cm.getColumn(3).setPreferredWidth(100);
        cm.getColumn(4).setPreferredWidth(100);
        cm.getColumn(5).setPreferredWidth(300);
        cm.getColumn(6).setPreferredWidth(80);

        // Row colors
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                if (!sel) comp.setBackground(r % 2 == 0 ? Color.WHITE : new Color(248, 245, 252));
                else comp.setBackground(new Color(230, 210, 255));
                if (c == 2) { setForeground(new Color(106, 27, 154)); setFont(FONT_BOLD); }
                else if (c == 5) { setForeground(new Color(0, 102, 204)); setFont(FONT_BOLD); }
                else { setForeground(Color.BLACK); setFont(FONT_PLAIN); }
                setHorizontalAlignment(c == 0 || c == 3 || c == 4 || c == 6 ? JLabel.CENTER : JLabel.LEFT);
                return comp;
            }
        });

        // Filter
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(bossRewardModel);
        table.setRowSorter(sorter);
        txtSearch.getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override
            public void update() {
                String text = txtSearch.getText().trim();
                if (text.isEmpty()) sorter.setRowFilter(null);
                else sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
            }
        });

        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private void loadBossRewardData() {
        bossRewardModel.setRowCount(0);
        new Thread(() -> {
            try {
                Field[] idFields = BossID.class.getFields();
                Map<String, Integer> idMap = new HashMap<>();
                for (Field f : idFields) {
                    if (Modifier.isStatic(f.getModifiers()) && f.getType() == int.class) {
                        idMap.put(f.getName(), f.getInt(null));
                    }
                }

                Field[] dataFields = BossesData.class.getFields();
                List<Object[]> rows = new ArrayList<>();

                for (Field f : dataFields) {
                    if (f.getType() == BossData.class && idMap.containsKey(f.getName())) {
                        BossData data = (BossData) f.get(null);
                        if (data == null) continue;
                        int bossId = idMap.get(f.getName());

                        // Get reward items from Manager config
                        String dropItems = "";
                        String dropRate = "";
                        if (Manager.BOSS_REWARD_PANEL != null) {
                            String rewardStr = Manager.BOSS_REWARD_PANEL.get(bossId);
                            if (rewardStr != null && !rewardStr.isEmpty()) {
                                // Parse "itemId-qty,itemId-qty" format
                                StringBuilder sb = new StringBuilder();
                                for (String part : rewardStr.split(",")) {
                                    String[] p = part.trim().split("-");
                                    if (p.length >= 1) {
                                        try {
                                            int iid = Integer.parseInt(p[0].trim());
                                            String qty = p.length > 1 ? p[1].trim() : "1";
                                            sb.append(getItemName(iid)).append(" x").append(qty).append(", ");
                                        } catch (Exception ex) {
                                            sb.append(part).append(", ");
                                        }
                                    }
                                }
                                dropItems = sb.length() > 2 ? sb.substring(0, sb.length() - 2) : sb.toString();
                            }
                        }

                        long bossHp = 0;
                        try {
                            Field hpField = BossData.class.getDeclaredField("hp");
                            hpField.setAccessible(true);
                            long[] hpArr = (long[]) hpField.get(data);
                            if (hpArr != null && hpArr.length > 0) bossHp = hpArr[0];

                            Field dameField = BossData.class.getDeclaredField("dame");
                            dameField.setAccessible(true);
                            long bossDame = dameField.getLong(data);

                            Field nameField = BossData.class.getDeclaredField("name");
                            nameField.setAccessible(true);
                            String bossName = (String) nameField.get(data);

                            rows.add(new Object[]{
                                bossId, f.getName(), bossName,
                                formatNumber(bossHp),
                                formatNumber(bossDame),
                                dropItems.isEmpty() ? "(Chưa cấu hình)" : dropItems,
                                dropRate.isEmpty() ? "-" : dropRate
                            });
                        } catch (Exception ex) {
                            rows.add(new Object[]{
                                bossId, f.getName(), f.getName(),
                                "?", "?",
                                dropItems.isEmpty() ? "(Chưa cấu hình)" : dropItems,
                                "-"
                            });
                        }
                    }
                }

                // Sort by boss ID
                rows.sort(Comparator.comparingInt(a -> (int) a[0]));

                SwingUtilities.invokeLater(() -> {
                    for (Object[] row : rows) bossRewardModel.addRow(row);
                    log("Loaded " + rows.size() + " boss entries.");
                });

            } catch (Exception e) {
                log("Error loading boss data: " + e.getMessage());
            }
        }).start();
    }

    // ===================================================================
    // TAB 3: ITEM CATALOG
    // ===================================================================
    private JPanel createItemCatalogTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Filter panel
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filterPanel.setOpaque(false);

        JTextField txtSearch = new JTextField(20);
        txtSearch.putClientProperty("JTextField.placeholderText", "Nhập Tên hoặc ID item...");
        txtSearch.setPreferredSize(new Dimension(250, 32));
        txtSearch.setFont(FONT_PLAIN);

        String[] types = {"- Tất cả Loại -", "0 - Áo", "1 - Quần", "2 - Găng", "3 - Giày", "4 - Rada",
            "5 - Cải trang/Tóc", "6 - Đậu thần", "12 - Ngọc rồng", "27 - Vật phẩm", "29 - Capsule/Bánh", "32 - Giáp tập"};
        JComboBox<String> cbType = new JComboBox<>(types);
        cbType.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        String[] genders = {"- Tất cả Hệ -", "0 - Trái Đất", "1 - Namếc", "2 - Xayda", "3 - Chung"};
        JComboBox<String> cbGender = new JComboBox<>(genders);
        cbGender.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JLabel lblCount = new JLabel("Tổng: 0");
        lblCount.setFont(FONT_BOLD);
        lblCount.setForeground(COL_PRIMARY);

        filterPanel.add(new JLabel("🔍"));
        filterPanel.add(txtSearch);
        filterPanel.add(new JLabel(" Loại:"));
        filterPanel.add(cbType);
        filterPanel.add(new JLabel(" Hệ:"));
        filterPanel.add(cbGender);
        filterPanel.add(Box.createHorizontalStrut(20));
        filterPanel.add(lblCount);

        // Table
        String[] cols = {"ID", "Icon", "Tên Item", "Type", "Gender", "Icon ID"};
        itemCatalogModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
            @Override
            public Class<?> getColumnClass(int c) { return c == 1 ? ImageIcon.class : Object.class; }
        };
        JTable table = new JTable(itemCatalogModel);
        table.setFont(FONT_PLAIN);
        table.setRowHeight(35);
        table.setShowGrid(true);
        table.setGridColor(new Color(230, 230, 230));

        JTableHeader header = table.getTableHeader();
        header.setFont(FONT_BOLD);
        header.setBackground(new Color(255, 152, 0));
        header.setForeground(Color.WHITE);
        header.setPreferredSize(new Dimension(0, 35));

        TableColumnModel cm = table.getColumnModel();
        cm.getColumn(0).setPreferredWidth(60);
        cm.getColumn(1).setPreferredWidth(50);
        cm.getColumn(2).setPreferredWidth(300);
        cm.getColumn(3).setPreferredWidth(80);
        cm.getColumn(4).setPreferredWidth(80);
        cm.getColumn(5).setPreferredWidth(70);

        catalogSorter = new TableRowSorter<>(itemCatalogModel);
        table.setRowSorter(catalogSorter);

        // Filter logic
        Runnable doFilter = () -> {
            String text = txtSearch.getText().trim();
            int typeIdx = cbType.getSelectedIndex();
            int genderIdx = cbGender.getSelectedIndex();

            List<RowFilter<Object, Object>> filters = new ArrayList<>();

            if (!text.isEmpty()) {
                try {
                    int idVal = Integer.parseInt(text);
                    List<RowFilter<Object, Object>> orFilters = new ArrayList<>();
                    orFilters.add(RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL, idVal, 0));
                    orFilters.add(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(text), 2));
                    filters.add(RowFilter.orFilter(orFilters));
                } catch (NumberFormatException e) {
                    filters.add(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(text), 2));
                }
            }

            if (typeIdx > 0) {
                try {
                    int val = Integer.parseInt(cbType.getSelectedItem().toString().split(" - ")[0].trim());
                    filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL, val, 3));
                } catch (Exception e) { /* ignore */ }
            }

            if (genderIdx > 0) {
                try {
                    int val = Integer.parseInt(cbGender.getSelectedItem().toString().split(" - ")[0].trim());
                    filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL, val, 4));
                } catch (Exception e) { /* ignore */ }
            }

            if (filters.isEmpty()) catalogSorter.setRowFilter(null);
            else catalogSorter.setRowFilter(RowFilter.andFilter(filters));

            lblCount.setText("Hiển thị: " + table.getRowCount() + " / " + itemCatalogModel.getRowCount());
        };

        txtSearch.getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override public void update() { doFilter.run(); }
        });
        cbType.addActionListener(e -> doFilter.run());
        cbGender.addActionListener(e -> doFilter.run());

        panel.add(filterPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private void loadItemCatalogData() {
        itemCatalogModel.setRowCount(0);
        for (ItemData item : listAllItems) {
            ImageIcon icon = getItemIcon(item.id, 28);
            itemCatalogModel.addRow(new Object[]{
                item.id, icon, item.name, item.type, item.gender, item.iconId
            });
        }
        log("Loaded " + listAllItems.size() + " items to catalog.");
    }

    // ===================================================================
    // HELPERS
    // ===================================================================
    private interface ItemCallback {
        void onSelect(int id, String name);
    }

    private void showQuickItemSearch(Window parent, ItemCallback callback) {
        JDialog d = new JDialog((Dialog) null, "Tìm Vật Phẩm", true);
        d.setSize(600, 500);
        d.setLocationRelativeTo(parent);
        d.setLayout(new BorderLayout(5, 5));

        JTextField txtSearch = new JTextField();
        txtSearch.putClientProperty("JTextField.placeholderText", "Nhập tên hoặc ID...");
        txtSearch.setBorder(BorderFactory.createTitledBorder("Tìm kiếm nhanh"));

        DefaultTableModel m = new DefaultTableModel(new String[]{"ID", "Icon", "Tên"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
            @Override
            public Class<?> getColumnClass(int c) { return c == 1 ? ImageIcon.class : Object.class; }
        };

        for (ItemData item : listAllItems) {
            m.addRow(new Object[]{item.id, getItemIcon(item.id, 22), item.name});
        }

        JTable t = new JTable(m);
        t.setRowHeight(30);
        t.setFont(FONT_PLAIN);
        t.getColumnModel().getColumn(0).setPreferredWidth(60);
        t.getColumnModel().getColumn(1).setPreferredWidth(40);
        t.getColumnModel().getColumn(2).setPreferredWidth(350);

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(m);
        t.setRowSorter(sorter);

        txtSearch.getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override
            public void update() {
                String text = txtSearch.getText().trim();
                if (text.isEmpty()) sorter.setRowFilter(null);
                else {
                    try {
                        int id = Integer.parseInt(text);
                        List<RowFilter<Object, Object>> or = new ArrayList<>();
                        or.add(RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL, id, 0));
                        or.add(RowFilter.regexFilter("(?i)" + text, 2));
                        sorter.setRowFilter(RowFilter.orFilter(or));
                    } catch (NumberFormatException e) {
                        sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text, 2));
                    }
                }
            }
        });

        t.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && t.getSelectedRow() >= 0) {
                    int modelRow = t.convertRowIndexToModel(t.getSelectedRow());
                    callback.onSelect((int) m.getValueAt(modelRow, 0), (String) m.getValueAt(modelRow, 2));
                    d.dispose();
                }
            }
        });

        d.add(txtSearch, BorderLayout.NORTH);
        d.add(new JScrollPane(t), BorderLayout.CENTER);
        d.setVisible(true);
    }

    // ===================================================================
    // TAB 4: HARDCODE DROP VIEWER (parsed from Mob.java source)
    // ===================================================================
    private JPanel createHardcodeDropTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Info
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        infoPanel.setBackground(new Color(255, 243, 224));
        infoPanel.setBorder(new CompoundBorder(
            new LineBorder(new Color(255, 152, 0), 1, true),
            new EmptyBorder(8, 12, 8, 12)));
        JLabel lblInfo = new JLabel("📊 Drop hardcode trong Mob.java — Quét tự động từ source code. Chỉ đọc, không sửa DB.");
        lblInfo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblInfo.setForeground(new Color(230, 81, 0));
        infoPanel.add(lblInfo);

        // Search
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        searchPanel.setOpaque(false);
        JTextField txtSearch = new JTextField(20);
        txtSearch.setFont(FONT_PLAIN);
        String[] filterOpts = {"- Tất cả -", "Sự kiện", "Map cụ thể", "General", "Đồ thần linh", "Hùng Vương", "Pokémon",
            "Địa Ngục", "Godzilla", "Juventus", "Thần Thú", "Kỷ Băng Hà"};
        JComboBox<String> cbFilter = new JComboBox<>(filterOpts);
        cbFilter.setFont(FONT_PLAIN);
        JLabel lblTotal = new JLabel("Tổng: 0");
        lblTotal.setFont(FONT_BOLD);
        searchPanel.add(new JLabel("🔍"));
        searchPanel.add(txtSearch);
        searchPanel.add(new JLabel(" Nhóm:"));
        searchPanel.add(cbFilter);
        searchPanel.add(Box.createHorizontalStrut(10));
        searchPanel.add(lblTotal);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(infoPanel, BorderLayout.NORTH);
        topPanel.add(searchPanel, BorderLayout.SOUTH);

        // Table
        String[] cols = {"Nhóm/Sự kiện", "Map", "Mob Condition", "Item ID", "Tên Item", "SL", "Tỷ lệ", "Ghi chú"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setFont(FONT_PLAIN);
        table.setRowHeight(28);
        table.setShowGrid(true);
        table.setGridColor(new Color(230, 230, 230));

        JTableHeader header = table.getTableHeader();
        header.setFont(FONT_BOLD);
        header.setBackground(new Color(255, 152, 0));
        header.setForeground(Color.WHITE);

        // Color-code by group
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                if (!sel) {
                    int mr = t.convertRowIndexToModel(r);
                    String group = String.valueOf(model.getValueAt(mr, 0));
                    if (group.contains("Địa Ngục")) setBackground(new Color(255, 235, 238));
                    else if (group.contains("Godzilla")) setBackground(new Color(232, 245, 233));
                    else if (group.contains("Juventus")) setBackground(new Color(232, 234, 246));
                    else if (group.contains("Thần Thú")) setBackground(new Color(255, 248, 225));
                    else if (group.contains("Kỷ Băng")) setBackground(new Color(225, 245, 254));
                    else if (group.contains("Pokémon")) setBackground(new Color(243, 229, 245));
                    else if (group.contains("Hùng Vương")) setBackground(new Color(255, 243, 224));
                    else setBackground(r % 2 == 0 ? Color.WHITE : new Color(248, 248, 248));
                } else setBackground(new Color(220, 235, 255));
                if (c == 4) { setForeground(new Color(0, 102, 204)); setFont(FONT_BOLD); }
                else if (c == 6) { setForeground(new Color(220, 53, 69)); setFont(new Font("Consolas", Font.BOLD, 12)); }
                else { setForeground(Color.BLACK); setFont(FONT_PLAIN); }
                setHorizontalAlignment(c == 1 || c == 3 || c == 5 || c == 6 ? JLabel.CENTER : JLabel.LEFT);
                return this;
            }
        });

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        // Filter logic
        Runnable applyFilter = () -> {
            List<RowFilter<Object, Object>> filters = new ArrayList<>();
            String searchText = txtSearch.getText().trim();
            if (!searchText.isEmpty()) {
                filters.add(RowFilter.regexFilter("(?i)" + searchText));
            }
            int filterIdx = cbFilter.getSelectedIndex();
            if (filterIdx > 0) {
                String filterKey = cbFilter.getSelectedItem().toString();
                filters.add(RowFilter.regexFilter("(?i)" + filterKey, 0));
            }
            sorter.setRowFilter(filters.isEmpty() ? null : RowFilter.andFilter(filters));
            lblTotal.setText("Hiện: " + table.getRowCount() + "/" + model.getRowCount());
        };

        txtSearch.getDocument().addDocumentListener(new SimpleDocumentListener() {
            public void update() { applyFilter.run(); }
        });
        cbFilter.addActionListener(e -> applyFilter.run());

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        // Populate data in background
        new Thread(() -> {
            populateHardcodeDropData(model);
            SwingUtilities.invokeLater(() -> {
                lblTotal.setText("Tổng: " + model.getRowCount());
                log("Loaded " + model.getRowCount() + " hardcoded drop rules from Mob.java.");
            });
        }).start();

        return panel;
    }

    private void populateHardcodeDropData(DefaultTableModel model) {
        // All hardcoded drop rules extracted from Mob.java
        // Format: {Group, Map, MobCondition, ItemID, ItemName, Quantity, Rate, Note}
        Object[][] data = {
            // ========== HÙNG VƯƠNG ==========
            {"Hùng Vương", "Tất cả", "Tất cả mob", 1847, "", "1-2", "1/40 (2.5%)", "Mảnh Đinh Ba"},
            {"Hùng Vương", "Tất cả", "Tất cả mob", 1848, "", "1-2", "1/40 (2.5%)", "Mảnh Cung Tên"},

            // ========== POKÉMON ==========
            {"Pokémon", "Tất cả", "Tất cả mob", 1664, "", "1-2", "8%", "Vỏ Sên - đổi Bóng Poké tại ChiChi"},
            {"Pokémon", "Tất cả", "Tất cả mob", 1665, "", "1", "3%", "Túi Dựng - đổi Bóng Ultra/Master"},

            // ========== ĐỊA NGỤC ĐẢO LỘN ==========
            {"Địa Ngục", "174,179,180", "Mob Quỷ", 457, "", "1-3", "15%", "Thỏi Vàng (Hồn Quỷ)"},
            {"Địa Ngục", "174,179,180", "Mob Quỷ", 935, "", "1", "8%", "Đá Xanh Lam"},
            {"Địa Ngục", "180", "Mob Quỷ Map 180", 934, "", "1-2", "5%", "Mảnh hồn bông tai - chỉ ĐN3"},
            {"Địa Ngục", "174,179,180", "Mob Quỷ", 190, "", "100K-300K", "20%", "Vàng cao"},
            {"Địa Ngục", "174,179,180", "Mob Quỷ", 861, "", "50-200", "3%", "Hồng Ngọc"},

            // ========== GODZILLA VS KONG ==========
            {"Godzilla", "175", "Mob Vampa", 1634, "", "1-3", "12%", "Mảnh Titan (Thiên Sứ)"},
            {"Godzilla", "175", "Mob Vampa", 192, "", "1", "1%", "Capsule dây chuyền"},
            {"Godzilla", "175", "Mob Vampa", 190, "", "150K-500K", "25%", "Vàng cao"},
            {"Godzilla", "175", "Mob Vampa", 1855, "", "1-5", "5%", "Mảnh vỡ BTC3"},

            // ========== JUVENTUS TOURNAMENT ==========
            {"Juventus", "183", "Mob PVP Arena", 190, "", "200K-600K", "30%", "Vàng cao"},
            {"Juventus", "183", "Mob PVP Arena", 861, "", "100-500", "10%", "Hồng Ngọc"},
            {"Juventus", "183", "Mob PVP Arena", 457, "", "1", "3%", "Thỏi Vàng"},
            {"Juventus", "183", "Mob PVP Arena", -1, "", "1", "0.5%", "Set kích hoạt ngẫu nhiên"},

            // ========== THẦN THÚ CỔ ĐẠI ==========
            {"Thần Thú", "176", "Mob Voi/Gà", 1634, "", "1-2", "8%", "Linh Phù Địa (Map 176)"},
            {"Thần Thú", "178", "Mob Ngựa/Rồng", 1634, "", "1-2", "8%", "Linh Phù Thiên (Map 178)"},
            {"Thần Thú", "176,178", "Mob Thần Thú", 935, "", "1", "5%", "Đá Xanh Lam"},
            {"Thần Thú", "176,178", "Mob Thần Thú", 934, "", "1", "6%", "Mảnh hồn bông tai"},
            {"Thần Thú", "176,178", "Mob Thần Thú", 190, "", "100K-400K", "20%", "Vàng cao"},

            // ========== KỶ BĂNG HÀ ==========
            {"Kỷ Băng Hà", "195-197", "Mob Băng", 1855, "", "1-3", "15%", "Mảnh vỡ BTC3 — nguồn chính"},
            {"Kỷ Băng Hà", "195-197", "Mob Băng", 1855, "", "1-5", "10%", "Mảnh vỡ BTC3 (tầng cao)"},
            {"Kỷ Băng Hà", "195-197", "Mob Băng", 935, "", "1-2", "8%", "Đá Xanh Lam"},
            {"Kỷ Băng Hà", "195-197", "Mob Băng", 190, "", "200K-500K", "25%", "Vàng cao"},
            {"Kỷ Băng Hà", "195-197", "Mob Băng", 861, "", "100-300", "15%", "Hồng Ngọc"},
            {"Kỷ Băng Hà", "197", "Mob Elite Map 197", 1855, "", "3-5", "0.3%", "BTC3 rare (1/1000)"},

            // ========== GENERAL DROPS ==========
            {"General", "Doanh Trại", "Mob Doanh Trại", -2, "", "1-5", "10%", "Đá nâng cấp ngẫu nhiên"},
            {"General", "3 hành tinh", "Mob thường", -2, "", "1-5", "10%", "Đá nâng cấp ngẫu nhiên"},
            {"General", "Tất cả", "Mob > lv80", 190, "", "Dựa lv", "15%+", "Máy dò vàng (isUseMayDo)"},
            {"General", "Tất cả", "Mob 80-81", -2, "", "1", "15%", "Máy dò đồ (isUseMayDo2)"},
            {"General", "Tất cả", "Mob bất kỳ", -3, "", "1", "0.0001%", "SKH ngẫu nhiên (1/1M)"},
        };

        SwingUtilities.invokeLater(() -> {
            for (Object[] row : data) {
                int itemId = (int) row[3];
                String itemName;
                if (itemId > 0) {
                    itemName = getItemName(itemId);
                } else if (itemId == -1) {
                    itemName = "Set kích hoạt (random)";
                } else if (itemId == -2) {
                    itemName = "Đá nâng cấp (random)";
                } else {
                    itemName = "SKH ngẫu nhiên";
                }
                model.addRow(new Object[]{
                    row[0], row[1], row[2],
                    itemId > 0 ? itemId : "N/A",
                    itemName, row[5], row[6], row[7]
                });
            }
        });
    }

    private String formatNumber(long num) {
        if (num >= 1_000_000_000) return String.format("%.1fB", num / 1_000_000_000.0);
        if (num >= 1_000_000) return String.format("%.1fM", num / 1_000_000.0);
        if (num >= 1_000) return String.format("%.1fK", num / 1_000.0);
        return String.valueOf(num);
    }

    private void log(String msg) {
        if (txtLog == null) return;
        SwingUtilities.invokeLater(() -> {
            String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            txtLog.append("[" + time + "] " + msg + "\n");
            txtLog.setCaretPosition(txtLog.getDocument().getLength());
        });
    }
}
