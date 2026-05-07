package nro.server.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.JTextComponent;
import javax.swing.undo.UndoManager;
import jdbc.DBConnecter;
import nro.server.Client;
import nro.server.io.MySession;
import services.func.ChangeMapService;
import nro.services.Service;
import utils.Util;

public class PlayersPanel extends JPanel {

    private static final String ICON_FOLDER = "data/icon/";

    private JTable table;
    private DefaultTableModel model;
    private JTextField txtSearch;
    private TableRowSorter<DefaultTableModel> sorter;
    private JButton btnQuickSave;

    // Pagination
    private int currentPage = 1;
    private int pageSize = 30;
    private int totalRows = 0;
    private JLabel lblPageInfo;
    private JComboBox<String> cbFilter;

    // Cache dữ liệu
    private final Map<Integer, String> itemTemplateMap = new HashMap<>();
    private final Map<Integer, Integer> itemIconMap = new HashMap<>();
    private final Map<Integer, String> clanNameMap = new HashMap<>();
    private final Map<Integer, String> optionTemplateMap = new HashMap<>();

    // Cache Template Badge
    private final Map<Integer, BadgeTemplate> badgeTemplateMap = new HashMap<>();

    // Cache template nhiệm vụ
    private final Map<Integer, String> taskMainTemplateMap = new HashMap<>();
    private final Map<Integer, String> taskMainDetailMap = new HashMap<>();
    private final Map<Integer, List<SubTaskTemplate>> taskSubTemplateMap = new HashMap<>();
    private final Map<Integer, String> sideTaskTemplateMap = new HashMap<>();
    private final Map<Integer, String> clanTaskTemplateMap = new HashMap<>();
    private final Map<Integer, String> kolTaskTemplateMap = new HashMap<>();

    // Cache Inventory JSON để phục vụ lưu nhanh
    private final Map<Integer, String> inventoryCache = new HashMap<>();
    private final Set<Integer> modifiedPlayerIds = new HashSet<>();

    private final List<ItemData> listAllItems = new ArrayList<>();

    private static class ItemData {
        int id;
        String name;
        int type;
        int gender;

        public ItemData(int id, String name, int type, int gender) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.gender = gender;
        }
    }

    private static class BadgeTemplate {
        int id;
        String name;
        String optionsJson;
        int iconId; // ID icon lấy từ item_template

        public BadgeTemplate(int id, String name, String optionsJson, int iconId) {
            this.id = id;
            this.name = name;
            this.optionsJson = optionsJson;
            this.iconId = iconId;
        }
    }

    private static class SubTaskTemplate {
        String name;
        short maxCount;
        String notify;
        byte npcId;
        short mapId;
    }

    private final Map<Integer, Integer> partHeadIconMap = new HashMap<>();
    private final Map<Integer, ImageIcon> iconCache = new HashMap<>();
    private final Map<Integer, Boolean> noIconCache = new HashMap<>();
    private final Map<Integer, ImageIcon> headCache = new HashMap<>();
    private final Map<Integer, ImageIcon> rawIconCache = new HashMap<>(); // Cache cho loadIconRaw

    // --- MÀU SẮC ---
    private final Color COLOR_PRIMARY = new Color(0, 120, 215);
    private final Color COLOR_SUCCESS = new Color(40, 167, 69);
    private final Color COLOR_INFO = new Color(23, 162, 184);
    private final Color COLOR_BG_HEADER = new Color(230, 240, 255);
    private final Color COLOR_ALT_ROW = new Color(245, 245, 245);
    private final Color COLOR_EDITABLE = new Color(0, 50, 150);

    public PlayersPanel() {
        setLayout(new BorderLayout(15, 15));
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(15, 15, 15, 15));

        initStaticData();
        loadCacheData();
        loadPartsHead();

        initTopControls();
        initTable();
        setupGlobalShortcuts();
    }

    private void setupGlobalShortcuts() {
        InputMap im = this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = this.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()),
                "quickSave");
        am.put("quickSave", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (btnQuickSave.isEnabled()) {
                    saveModifiedRows();
                }
            }
        });
    }

    private void addUndoRedo(JTextComponent textComponent) {
        UndoManager undoManager = new UndoManager();
        textComponent.getDocument().addUndoableEditListener(e -> undoManager.addEdit(e.getEdit()));

        InputMap im = textComponent.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = textComponent.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "Undo");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "Redo");

        am.put("Undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (undoManager.canUndo())
                    undoManager.undo();
            }
        });
        am.put("Redo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (undoManager.canRedo())
                    undoManager.redo();
            }
        });
    }

    private Connection getConnection() throws SQLException {
        return DBConnecter.getConnectionServer();
    }

    private void loadCacheData() {
        new Thread(() -> {
            listAllItems.clear();
            try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
                // Load item templates
                try (ResultSet rs = stmt.executeQuery("SELECT id, name, icon_id, type, gender FROM item_template")) {
                    while (rs.next()) {
                        listAllItems.add(new ItemData(rs.getInt("id"), rs.getString("name"), rs.getInt("type"),
                                rs.getInt("gender")));
                        itemTemplateMap.put(rs.getInt("id"), rs.getString("name"));
                        itemIconMap.put(rs.getInt("id"), rs.getInt("icon_id"));
                    }
                }

                // Load clan names
                try (ResultSet rs = stmt.executeQuery("SELECT id, name FROM clan")) {
                    while (rs.next())
                        clanNameMap.put(rs.getInt("id"), rs.getString("name"));
                }

                // Load option templates
                try (ResultSet rs = stmt.executeQuery("SELECT id, name FROM item_option_template")) {
                    while (rs.next())
                        optionTemplateMap.put(rs.getInt("id"), rs.getString("name"));
                }

                try (ResultSet rs = stmt.executeQuery(
                        "SELECT b.id, b.idEffect, b.name, b.Options, i.icon_id FROM data_badges b LEFT JOIN item_template i ON b.iditem = i.id")) {
                    while (rs.next()) {
                        int idKey = rs.getInt("idEffect");

                        String name = rs.getString("name");
                        String options = rs.getString("Options");
                        int iconId = rs.getInt("icon_id");

                        badgeTemplateMap.put(idKey, new BadgeTemplate(idKey, name, options, iconId));
                    }
                }

                // Load task main templates
                try (ResultSet rs = stmt.executeQuery("SELECT id, name, detail FROM task_main_template")) {
                    while (rs.next()) {
                        int id = rs.getInt("id");
                        taskMainTemplateMap.put(id, rs.getString("name"));
                        taskMainDetailMap.put(id, rs.getString("detail"));
                    }
                }

                // Load task sub templates
                try (ResultSet rs = stmt.executeQuery(
                        "SELECT task_main_id, name, max_count, notify, npc_id, map FROM task_sub_template ORDER BY task_main_id")) {
                    while (rs.next()) {
                        int taskMainId = rs.getInt("task_main_id");
                        SubTaskTemplate subTask = new SubTaskTemplate();
                        subTask.name = rs.getString("name");
                        subTask.maxCount = rs.getShort("max_count");
                        subTask.notify = rs.getString("notify");
                        subTask.npcId = rs.getByte("npc_id");
                        subTask.mapId = rs.getShort("map");

                        taskSubTemplateMap.computeIfAbsent(taskMainId, k -> new ArrayList<>()).add(subTask);
                    }
                }

                // Load side task templates
                try (ResultSet rs = stmt.executeQuery("SELECT id, name FROM side_task_template")) {
                    while (rs.next()) {
                        sideTaskTemplateMap.put(rs.getInt("id"), rs.getString("name"));
                    }
                }

                // Load clan task templates
                try (ResultSet rs = stmt.executeQuery("SELECT id, name FROM clan_task_template")) {
                    while (rs.next()) {
                        clanTaskTemplateMap.put(rs.getInt("id"), rs.getString("name"));
                    }
                }

                // Load KOL task templates
                try (ResultSet rs = stmt.executeQuery("SELECT id, info FROM task_kol_template")) {
                    while (rs.next()) {
                        kolTaskTemplateMap.put(rs.getInt("id"), rs.getString("info"));
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void loadPartsHead() {
        new Thread(() -> {
            try (Connection conn = getConnection();
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT id, data FROM part WHERE type = 0")) {
                while (rs.next()) {
                    try {
                        JsonArray arr = new JsonParser().parse(rs.getString("data")).getAsJsonArray();
                        if (arr.size() > 0)
                            partHeadIconMap.put(rs.getInt("id"), arr.get(0).getAsJsonArray().get(0).getAsInt());
                    } catch (Exception ignored) {
                    }
                }
            } catch (SQLException ignored) {
            }
            SwingUtilities.invokeLater(() -> loadPlayersFromDB(""));
        }).start();
    }

    private ImageIcon drawHeadIcon(int headPartId) {
        if (headPartId <= 0)
            return null;
        if (headCache.containsKey(headPartId))
            return headCache.get(headPartId);
        Integer iconId = partHeadIconMap.get(headPartId);
        if (iconId != null) {
            try {
                String[] zoomLevels = { "x4", "x3", "x2", "x1" };
                for (String zoom : zoomLevels) {
                    File f = new File(ICON_FOLDER + zoom + "/" + iconId + ".png");
                    if (f.exists()) {
                        Image dimg = ImageIO.read(f).getScaledInstance(28, 28, Image.SCALE_SMOOTH);
                        ImageIcon icon = new ImageIcon(dimg);
                        headCache.put(headPartId, icon);
                        return icon;
                    }
                }
            } catch (Exception e) {
            }
        }
        return null;
    }

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
            return loadIconRaw(iconId);
        } catch (Exception e) {
        }
        noIconCache.put(itemId, true);
        return null;
    }

    // Hàm mới để load icon trực tiếp từ icon_id
    private ImageIcon loadIconRaw(int iconId) {
        if (iconId <= 0)
            return null;
        if (rawIconCache.containsKey(iconId))
            return rawIconCache.get(iconId);
        try {
            String[] zoomLevels = { "x4", "x3", "x2", "x1" };
            File f = null;
            for (String zoom : zoomLevels) {
                f = new File(ICON_FOLDER + zoom + "/" + iconId + ".png");
                if (f.exists())
                    break;
            }
            if (f != null && f.exists()) {
                Image dimg = ImageIO.read(f).getScaledInstance(20, 20, Image.SCALE_SMOOTH);
                ImageIcon icon = new ImageIcon(dimg);
                rawIconCache.put(iconId, icon);
                return icon;
            }
        } catch (Exception e) {
        }
        return null;
    }

    private void initStaticData() {
        String raw = "0,Tấn công +#;50,Sức đánh +#%;77,HP +#%;103,KI +#%;14,Chí mạng +#%;30,Khóa giao dịch;93,Hạn sử dụng # ngày;73,Không thể bán;9,Hiệu lực # phút";
        for (String s : raw.split(";")) {
            String[] p = s.split(",");
            if (p.length == 2)
                optionTemplateMap.put(Integer.parseInt(p[0]), p[1]);
        }
    }

    private String getItemName(int id) {
        return itemTemplateMap.getOrDefault(id, "Unknown [" + id + "]");
    }

    private String getClanName(int id) {
        return id == -1 ? "Không có" : clanNameMap.getOrDefault(id, "Clan [" + id + "]");
    }

    private String getOptionName(int id) {
        return optionTemplateMap.getOrDefault(id, "Option " + id);
    }

    private String formatOption(int id, int param) {
        return getOptionName(id).replace("#", String.valueOf(param));
    }

    private void initTopControls() {
        JPanel topWrapper = new JPanel();
        topWrapper.setLayout(new BoxLayout(topWrapper, BoxLayout.Y_AXIS));
        topWrapper.setOpaque(false);
        topWrapper.setBorder(new EmptyBorder(0, 0, 5, 0));

        // Row 1: Search + Actions
        JPanel row1 = new JPanel(new BorderLayout(10, 0));
        row1.setOpaque(false);
        JPanel searchP = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        searchP.setOpaque(false);

        txtSearch = new JTextField(18);
        txtSearch.putClientProperty("JTextField.placeholderText", "Tên nhân vật...");
        txtSearch.setPreferredSize(new Dimension(180, 32));
        txtSearch.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtSearch.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) { currentPage = 1; loadPlayersFromDB(txtSearch.getText().trim()); }
            }
        });
        addUndoRedo(txtSearch);

        // Filter dropdown
        cbFilter = new JComboBox<>(new String[]{
            "Tất cả", "Đã kích hoạt", "Chưa kích hoạt", "Bị Ban",
            "───────────",
            "🏆 Top Sức Mạnh", "💰 Top Nạp", "💎 Top VNĐ"
        });
        cbFilter.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cbFilter.setPreferredSize(new Dimension(160, 32));
        cbFilter.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                String s = value != null ? value.toString() : "";
                if (s.startsWith("─")) {
                    lbl.setEnabled(false);
                    lbl.setFont(new Font("Segoe UI", Font.PLAIN, 8));
                } else if (s.contains("Top") || s.contains("🏆") || s.contains("💰") || s.contains("💎")) {
                    lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    if (!isSelected) lbl.setForeground(new Color(180, 100, 0));
                } else {
                    lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                }
                return lbl;
            }
        });
        cbFilter.addActionListener(e -> {
            String sel = (String) cbFilter.getSelectedItem();
            if (sel != null && sel.startsWith("─")) { cbFilter.setSelectedIndex(0); return; }
            currentPage = 1; loadPlayersFromDB(txtSearch.getText().trim());
        });

        JButton btnSearch = createStyledButton("Tìm kiếm", COLOR_PRIMARY, Color.WHITE);
        btnSearch.addActionListener(e -> { currentPage = 1; loadPlayersFromDB(txtSearch.getText().trim()); });

        btnQuickSave = createStyledButton("Lưu thay đổi (Ctrl+S)", Color.GRAY, Color.WHITE);
        btnQuickSave.setEnabled(false);
        btnQuickSave.addActionListener(e -> saveModifiedRows());

        JButton btnReload = createStyledButton("Tải lại DB", new Color(100, 100, 100), Color.WHITE);
        btnReload.addActionListener(e -> loadPartsHead());

        searchP.add(txtSearch);
        searchP.add(new JLabel("Lọc:"));
        searchP.add(cbFilter);
        searchP.add(btnSearch);
        searchP.add(btnQuickSave);
        searchP.add(btnReload);

        JPanel actionP = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actionP.setOpaque(false);

        JButton btnKick = createStyledButton("Kick Online", new Color(255, 140, 0), Color.WHITE);
        btnKick.addActionListener(e -> kickSelectedPlayer());
        JButton btnBan = createStyledButton("Ban/Unban", new Color(220, 53, 69), Color.WHITE);
        btnBan.addActionListener(e -> toggleBanSelectedPlayer());
        JButton btnBuff = createStyledButton("Buff Item", new Color(40, 167, 69), Color.WHITE);
        btnBuff.addActionListener(e -> {
            int r = table.getSelectedRow();
            if (r != -1) {
                int modelRow = table.convertRowIndexToModel(r);
                openBuffItemDialog(Integer.parseInt(model.getValueAt(modelRow, 1).toString()),
                        model.getValueAt(modelRow, 3).toString());
            } else JOptionPane.showMessageDialog(this, "Chọn 1 người chơi trước!");
        });
        JButton btnBatchBan = createStyledButton("Ban chọn", new Color(180, 40, 55), Color.WHITE);
        btnBatchBan.addActionListener(e -> batchToggleBan());
        JButton btnUnstuck = createStyledButton("🏠 Giải Kẹt", new Color(0, 150, 136), Color.WHITE);
        btnUnstuck.setToolTipText("Chuyển người chơi bị kẹt về nhà (map làng)");
        btnUnstuck.addActionListener(e -> unstuckSelectedPlayer());

        actionP.add(btnKick); actionP.add(btnBan); actionP.add(btnBuff); actionP.add(btnUnstuck); actionP.add(btnBatchBan);
        row1.add(searchP, BorderLayout.WEST);
        row1.add(actionP, BorderLayout.EAST);

        // Row 2: Pagination
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        row2.setOpaque(false);
        JButton btnFirst = new JButton("|<"); btnFirst.addActionListener(e -> { currentPage = 1; loadPlayersFromDB(txtSearch.getText().trim()); });
        JButton btnPrev = new JButton("<"); btnPrev.addActionListener(e -> { if (currentPage > 1) { currentPage--; loadPlayersFromDB(txtSearch.getText().trim()); } });
        JButton btnNext = new JButton(">"); btnNext.addActionListener(e -> { int maxPage = Math.max(1, (totalRows + pageSize - 1) / pageSize); if (currentPage < maxPage) { currentPage++; loadPlayersFromDB(txtSearch.getText().trim()); } });
        JButton btnLast = new JButton(">|"); btnLast.addActionListener(e -> { currentPage = Math.max(1, (totalRows + pageSize - 1) / pageSize); loadPlayersFromDB(txtSearch.getText().trim()); });
        lblPageInfo = new JLabel("Trang 1");
        lblPageInfo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        JComboBox<String> cbPageSize = new JComboBox<>(new String[]{"30", "50", "100", "200"});
        cbPageSize.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cbPageSize.addActionListener(e -> { pageSize = Integer.parseInt((String) cbPageSize.getSelectedItem()); currentPage = 1; loadPlayersFromDB(txtSearch.getText().trim()); });
        JCheckBox cbSelectAll = new JCheckBox("Chọn tất cả");
        cbSelectAll.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cbSelectAll.addActionListener(e -> { for (int i = 0; i < model.getRowCount(); i++) model.setValueAt(cbSelectAll.isSelected(), i, 0); });

        row2.add(cbSelectAll); row2.add(Box.createHorizontalStrut(10));
        row2.add(btnFirst); row2.add(btnPrev); row2.add(lblPageInfo); row2.add(btnNext); row2.add(btnLast);
        row2.add(Box.createHorizontalStrut(10)); row2.add(new JLabel("Hiển thị:")); row2.add(cbPageSize);

        topWrapper.add(row1); topWrapper.add(row2);
        add(topWrapper, BorderLayout.NORTH);
    }

    private void showGuide() {
        String html = "<html><body style='width: 300px'>"
                + "<h3>Hướng dẫn quản lý Người chơi</h3>"
                + "<ul>"
                + "<li><b>Sửa nhanh:</b> Click đúp vào các cột <font color='blue'>Vàng, Ngọc, VNĐ, Tổng Nạp</font> để sửa trực tiếp trên bảng.</li>"
                + "<li><b>Chi tiết:</b> Click đúp vào các cột còn lại (Tên, ID...) để mở cửa sổ chỉnh sửa đầy đủ (Item, Đệ tử...).</li>"
                + "<li><b>Lưu:</b> Sau khi sửa nhanh trên bảng, nhấn nút <b>'Lưu thay đổi'</b> hoặc phím tắt <b>Ctrl + S</b>.</li>"
                + "<li><b>Tiện ích:</b>"
                + "<ul>"
                + "<li><b>Ctrl + Z:</b> Hoàn tác (Undo) khi nhập liệu text.</li>"
                + "<li><b>Ctrl + Y:</b> Làm lại (Redo) khi nhập liệu text.</li>"
                + "<li><b>Chuột phải:</b> Mở menu chức năng phụ (Buff item...).</li>"
                + "</ul></li>"
                + "</ul>"
                + "<p><i>Lưu ý: Cột 'Tình trạng' màu đỏ nghĩa là tài khoản đang bị khóa (Ban).</i></p>"
                + "</body></html>";
        JOptionPane.showMessageDialog(this, new JLabel(html), "Hướng dẫn sử dụng", JOptionPane.INFORMATION_MESSAGE);
    }

    private void initTable() {
        String[] cols = { "", "ID", "Head", "Tên nhân vật", "Sức Mạnh", "Clan", "Vàng", "Ngọc", "Thỏi Vàng", "VNĐ",
                "Tổng Nạp", "Trạng thái", "Tình trạng" };
        model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0 || column == 6 || column == 7 || column == 9 || column == 10;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) return Boolean.class;
                if (columnIndex == 2) return ImageIcon.class;
                if (columnIndex == 1 || columnIndex == 8 || columnIndex == 9 || columnIndex == 10)
                    return Long.class;
                return super.getColumnClass(columnIndex);
            }
        };

        model.addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE) {
                int row = e.getFirstRow();
                int col = e.getColumn();
                if (row >= 0 && col > 0) {
                    try {
                        int playerId = Integer.parseInt(model.getValueAt(row, 1).toString());
                        modifiedPlayerIds.add(playerId);
                        btnQuickSave.setEnabled(true);
                        btnQuickSave.setBackground(new Color(255, 69, 0));
                        btnQuickSave.setText("Lưu thay đổi (" + modifiedPlayerIds.size() + ")");
                    } catch (Exception ex) {
                    }
                }
            }
        });

        table = new JTable(model);
        table.setRowHeight(40);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setShowVerticalLines(false);
        table.setSelectionBackground(new Color(232, 242, 252));
        table.setSelectionForeground(Color.BLACK);

        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setBackground(COLOR_BG_HEADER);
        table.getTableHeader().setPreferredSize(new Dimension(0, 40));

        table.getColumnModel().getColumn(0).setPreferredWidth(30);
        table.getColumnModel().getColumn(0).setMaxWidth(35);

        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        table.getColumnModel().getColumn(1).setPreferredWidth(50);
        table.getColumnModel().getColumn(2).setPreferredWidth(50);
        table.getColumnModel().getColumn(2).setPreferredWidth(150);

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBorder(new EmptyBorder(0, 10, 0, 10));
                if (!isSelected)
                    setBackground(row % 2 == 0 ? Color.WHITE : COLOR_ALT_ROW);

                if (column == 6 || column == 7 || column == 9 || column == 10) {
                    setFont(new Font("Segoe UI", Font.BOLD, 13));
                    setForeground(COLOR_EDITABLE);
                    if (isSelected)
                        setForeground(Color.BLUE);
                } else if (column == 11) {
                    setFont(new Font("Segoe UI", Font.BOLD, 12));
                    setForeground("Đã kích hoạt".equals(value) ? new Color(0, 128, 0) : Color.RED);
                } else if (column == 12) {
                    setFont(new Font("Segoe UI", Font.BOLD, 12));
                    if ("Bị chặn (Block)".equals(value))
                        setForeground(Color.RED);
                    else
                        setForeground(new Color(0, 128, 0));
                } else {
                    setForeground(Color.BLACK);
                }
                return this;
            }
        });

        createContextMenu();

        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int viewRow = table.getSelectedRow();
                    int viewCol = table.getSelectedColumn();
                    if (viewRow != -1) {
                        int modelCol = table.convertColumnIndexToModel(viewCol);
                        if (modelCol == 0 || modelCol == 6 || modelCol == 7 || modelCol == 9 || modelCol == 10) {
                            return;
                        }
                        int modelRow = table.convertRowIndexToModel(viewRow);
                        openPlayerEditorDB(Integer.parseInt(model.getValueAt(modelRow, 1).toString()));
                    }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new LineBorder(new Color(220, 220, 220)));
        scroll.getViewport().setBackground(Color.WHITE);
        add(scroll, BorderLayout.CENTER);
    }

    private void createContextMenu() {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem mKick = new JMenuItem("⚡ Kick Online");
        mKick.setFont(new Font("Segoe UI", Font.BOLD, 12));
        mKick.addActionListener(e -> kickSelectedPlayer());
        menu.add(mKick);

        JMenuItem mBan = new JMenuItem("🚫 Ban / Unban");
        mBan.setFont(new Font("Segoe UI", Font.BOLD, 12));
        mBan.addActionListener(e -> toggleBanSelectedPlayer());
        menu.add(mBan);

        menu.addSeparator();

        JMenuItem mBuffItem = new JMenuItem("Buff Item");
        mBuffItem.setFont(new Font("Segoe UI", Font.BOLD, 12));
        mBuffItem.addActionListener(e -> {
            int r = table.getSelectedRow();
            if (r != -1)
                openBuffItemDialog(Integer.parseInt(model.getValueAt(table.convertRowIndexToModel(r), 1).toString()),
                        model.getValueAt(table.convertRowIndexToModel(r), 3).toString());
        });
        menu.add(mBuffItem);

        menu.addSeparator();

        JMenuItem mUnstuck = new JMenuItem("🏠 Giải Kẹt Map");
        mUnstuck.setFont(new Font("Segoe UI", Font.BOLD, 12));
        mUnstuck.addActionListener(e -> unstuckSelectedPlayer());
        menu.add(mUnstuck);

        table.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger())
                    showMenu(e);
            }

            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger())
                    showMenu(e);
            }

            private void showMenu(MouseEvent e) {
                int r = table.rowAtPoint(e.getPoint());
                if (r >= 0 && r < table.getRowCount()) {
                    table.setRowSelectionInterval(r, r);
                    menu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }

    private void batchToggleBan() {
        List<Integer> ids = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            Object chk = model.getValueAt(i, 0);
            if (Boolean.TRUE.equals(chk)) {
                ids.add(Integer.parseInt(model.getValueAt(i, 1).toString()));
                names.add(model.getValueAt(i, 3).toString());
            }
        }
        if (ids.isEmpty()) { JOptionPane.showMessageDialog(this, "Chưa chọn người chơi nào!"); return; }
        int confirm = JOptionPane.showConfirmDialog(this, "Ban " + ids.size() + " người chơi?\n" + String.join(", ", names), "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        new Thread(() -> {
            try (Connection conn = getConnection()) {
                try (PreparedStatement ps = conn.prepareStatement("UPDATE account SET ban = 1 WHERE id = (SELECT account_id FROM player WHERE id = ?)")) {
                    for (int pid : ids) { ps.setInt(1, pid); ps.addBatch(); }
                    ps.executeBatch();
                }
                SwingUtilities.invokeLater(() -> { JOptionPane.showMessageDialog(this, "Đã ban " + ids.size() + " người chơi!"); loadPlayersFromDB(txtSearch.getText().trim()); });
            } catch (Exception ex) { ex.printStackTrace(); }
        }).start();
    }

    // === ACTION METHODS ===

    private void kickSelectedPlayer() {
        int r = table.getSelectedRow();
        if (r == -1) {
            JOptionPane.showMessageDialog(this, "Chọn 1 người chơi trước!", "Chưa chọn", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = table.convertRowIndexToModel(r);
        String playerName = model.getValueAt(modelRow, 3).toString();
        int confirm = JOptionPane.showConfirmDialog(this,
                "Kick \"" + playerName + "\" khỏi server?", "Xác nhận Kick",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            nro.player.Player pl = Client.gI().getPlayer(playerName);
            if (pl != null && pl.getSession() != null) {
                Client.gI().kickSession((MySession) pl.getSession());
                JOptionPane.showMessageDialog(this, "Đã kick \"" + playerName + "\" thành công!",
                        "Thành công", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Người chơi \"" + playerName + "\" không online!",
                        "Không tìm thấy", JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi kick: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void unstuckSelectedPlayer() {
        int r = table.getSelectedRow();
        if (r == -1) {
            JOptionPane.showMessageDialog(this, "Chọn 1 người chơi trước!", "Chưa chọn", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = table.convertRowIndexToModel(r);
        String playerName = model.getValueAt(modelRow, 3).toString();
        int confirm = JOptionPane.showConfirmDialog(this,
                "Giải kẹt \"" + playerName + "\"?\n\n"
                + "Thao tác này sẽ:\n"
                + "• Hồi sinh nếu đang chết\n"
                + "• Chuyển về map Làng (21/22/23)\n"
                + "• Hồi đầy HP/MP",
                "🏠 Giải Kẹt Map",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            nro.player.Player pl = Client.gI().getPlayer(playerName);
            if (pl != null && pl.getSession() != null) {
                // Xác định map nhà dựa theo giới tính
                int homeMapId;
                switch (pl.gender) {
                    case 0 -> homeMapId = 21;  // Làng Aru (Trái Đất)
                    case 1 -> homeMapId = 22;  // Làng Moori (Namếc)
                    case 2 -> homeMapId = 23;  // Làng Kakarot (Xayda)
                    default -> homeMapId = 21;
                }

                // Hồi sinh nếu đang chết
                if (pl.isDie()) {
                    pl.nPoint.setHp(Util.maxIntValue(pl.nPoint.hpMax));
                    pl.nPoint.setMp(Util.maxIntValue(pl.nPoint.mpMax));
                    Service.gI().hsChar(pl, Util.maxIntValue(pl.nPoint.hpMax),
                            Util.maxIntValue(pl.nPoint.mpMax));
                }

                // Lấy map hiện tại trước khi chuyển
                String fromMap = (pl.zone != null && pl.zone.map != null)
                        ? pl.zone.map.mapName + " (ID: " + pl.zone.map.mapId + ")"
                        : "Unknown";

                // Chuyển map về nhà
                ChangeMapService.gI().changeMap(pl, homeMapId, -1, Util.nextInt(200, 400), 336);

                JOptionPane.showMessageDialog(this,
                        "✅ Giải kẹt \"" + playerName + "\" thành công!\n\n"
                        + "Từ: " + fromMap + "\n"
                        + "Về: Làng (Map " + homeMapId + ")\n"
                        + "HP/MP: Đã hồi đầy",
                        "Thành công", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Người chơi \"" + playerName + "\" không online!\n"
                        + "Chỉ có thể giải kẹt player đang online.",
                        "Không tìm thấy", JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi giải kẹt: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void toggleBanSelectedPlayer() {
        int r = table.getSelectedRow();
        if (r == -1) {
            JOptionPane.showMessageDialog(this, "Chọn 1 người chơi trước!", "Chưa chọn", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = table.convertRowIndexToModel(r);
        int playerId = Integer.parseInt(model.getValueAt(modelRow, 1).toString());
        String playerName = model.getValueAt(modelRow, 3).toString();
        String currentStatus = model.getValueAt(modelRow, 12).toString();
        boolean isBanned = currentStatus.contains("Block") || currentStatus.contains("chặn");

        String action = isBanned ? "Unban" : "Ban";
        int confirm = JOptionPane.showConfirmDialog(this,
                action + " người chơi \"" + playerName + "\"?", "Xác nhận " + action,
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection conn = getConnection()) {
            // Find account_id from player
            int accountId = -1;
            try (PreparedStatement ps = conn.prepareStatement("SELECT account_id FROM player WHERE id = ?")) {
                ps.setInt(1, playerId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) accountId = rs.getInt("account_id");
            }

            if (accountId == -1) {
                JOptionPane.showMessageDialog(this, "Không tìm thấy tài khoản!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int newBanValue = isBanned ? 0 : 1;
            try (PreparedStatement ps = conn.prepareStatement("UPDATE account SET ban = ? WHERE id = ?")) {
                ps.setInt(1, newBanValue);
                ps.setInt(2, accountId);
                ps.executeUpdate();
            }

            // Update table display
            model.setValueAt(isBanned ? "Bình thường" : "Bị chặn (Block)", modelRow, 12);

            // If banning, also kick online
            if (!isBanned) {
                try {
                    nro.player.Player pl = Client.gI().getPlayer(playerName);
                    if (pl != null && pl.getSession() != null) {
                        Client.gI().kickSession((MySession) pl.getSession());
                    }
                } catch (Exception ignored) {}
            }

            JOptionPane.showMessageDialog(this,
                    action + " \"" + playerName + "\" thành công!",
                    "Thành công", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi " + action + ": " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openBuffItemDialog(int playerId, String playerName) {
        JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "⚡ Buff Item Nhanh — " + playerName + " (ID: " + playerId + ")", true);
        d.setSize(950, 650);
        d.setLocationRelativeTo(null);
        d.setLayout(new BorderLayout(10, 10));

        // ===== TOP: Filters =====
        JPanel pFilter = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        pFilter.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(COLOR_PRIMARY), " 🔍 Tìm Item ",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12), COLOR_PRIMARY));

        JTextField txtSearchItem = new JTextField(14);
        txtSearchItem.setPreferredSize(new Dimension(140, 30));
        addUndoRedo(txtSearchItem);

        String[] types = {"- Tất cả Loại -", "0 - Áo", "1 - Quần", "2 - Găng", "3 - Giày", "4 - Rada",
                "5 - Cải trang/Tóc", "6 - Đậu thần", "12 - Ngọc rồng", "27 - Vật phẩm", "29 - Capsule/Bánh",
                "32 - Giáp tập"};
        JComboBox<String> cbFilterType = new JComboBox<>(types);
        String[] genders = {"- Tất cả Hệ -", "0 - Trái Đất", "1 - Namếc", "2 - Xayda"};
        JComboBox<String> cbFilterGender = new JComboBox<>(genders);

        pFilter.add(new JLabel("Tên/ID:"));
        pFilter.add(txtSearchItem);
        pFilter.add(new JLabel("Loại:"));
        pFilter.add(cbFilterType);
        pFilter.add(new JLabel("Hệ:"));
        pFilter.add(cbFilterGender);

        // ===== CENTER: Item search table =====
        DefaultTableModel searchModel = new DefaultTableModel(
                new String[]{"ID", "Icon", "Tên Item", "Type", "Gender"}, 0) {
            @Override
            public Class<?> getColumnClass(int c) {
                return c == 1 ? ImageIcon.class : Object.class;
            }
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        for (ItemData item : listAllItems) {
            searchModel.addRow(new Object[]{item.id, getItemIcon(item.id), item.name, item.type, item.gender});
        }

        JTable tSearch = new JTable(searchModel);
        tSearch.setRowHeight(30);
        tSearch.getColumnModel().getColumn(0).setPreferredWidth(50);
        tSearch.getColumnModel().getColumn(1).setPreferredWidth(40);
        tSearch.getColumnModel().getColumn(2).setPreferredWidth(300);
        tSearch.getColumnModel().getColumn(3).setMinWidth(0);
        tSearch.getColumnModel().getColumn(3).setMaxWidth(0);
        tSearch.getColumnModel().getColumn(4).setMinWidth(0);
        tSearch.getColumnModel().getColumn(4).setMaxWidth(0);
        tSearch.setSelectionBackground(new Color(200, 230, 255));

        TableRowSorter<DefaultTableModel> sSorter = new TableRowSorter<>(searchModel);
        tSearch.setRowSorter(sSorter);

        Runnable applyFilter = () -> {
            String text = txtSearchItem.getText().trim();
            int typeIdx = cbFilterType.getSelectedIndex();
            int genderIdx = cbFilterGender.getSelectedIndex();
            List<RowFilter<Object, Object>> filters = new ArrayList<>();
            if (!text.isEmpty()) {
                try {
                    int idVal = Integer.parseInt(text);
                    List<RowFilter<Object, Object>> orFilters = new ArrayList<>();
                    orFilters.add(RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL, idVal, 0));
                    orFilters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(text), 2));
                    filters.add(RowFilter.orFilter(orFilters));
                } catch (NumberFormatException ex) {
                    filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(text), 2));
                }
            }
            if (typeIdx > 0) {
                try {
                    int val = Integer.parseInt(cbFilterType.getSelectedItem().toString().split(" - ")[0]);
                    filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL, val, 3));
                } catch (Exception ex) {}
            }
            if (genderIdx > 0) {
                try {
                    int val = Integer.parseInt(cbFilterGender.getSelectedItem().toString().split(" - ")[0]);
                    filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL, val, 4));
                } catch (Exception ex) {}
            }
            sSorter.setRowFilter(filters.isEmpty() ? null : RowFilter.andFilter(filters));
        };
        txtSearchItem.getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override
            public void update() {
                applyFilter.run();
            }
        });
        cbFilterType.addActionListener(e -> applyFilter.run());
        cbFilterGender.addActionListener(e -> applyFilter.run());

        // ===== BOTTOM: Buff controls =====
        JPanel pBottom = new JPanel();
        pBottom.setLayout(new BoxLayout(pBottom, BoxLayout.Y_AXIS));
        pBottom.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(COLOR_SUCCESS), " 🎁 Buff Item ",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12), COLOR_SUCCESS));

        JPanel pSelected = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        JLabel lblSelected = new JLabel("Chưa chọn item");
        lblSelected.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblSelected.setForeground(Color.GRAY);
        pSelected.add(new JLabel("Item đã chọn:"));
        pSelected.add(lblSelected);

        JPanel pInputs = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        JTextField txtQty = new JTextField("1", 5);
        txtQty.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        addUndoRedo(txtQty);
        JTextField txtOpt = new JTextField("[]", 20);
        txtOpt.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtOpt.setToolTipText("Option JSON, ví dụ: [[50,15],[77,20]] = SD+15%, HP+20%");
        addUndoRedo(txtOpt);
        pInputs.add(new JLabel("SL:"));
        pInputs.add(txtQty);
        pInputs.add(new JLabel("Options:"));
        pInputs.add(txtOpt);

        JPanel pButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 8));
        JButton btnBuff = createStyledButton("⚡ BUFF VÀO HÀNH TRANG", COLOR_SUCCESS, Color.WHITE);
        btnBuff.setPreferredSize(new Dimension(220, 38));
        btnBuff.setEnabled(false);
        JButton btnClose = createStyledButton("Đóng", new Color(108, 117, 125), Color.WHITE);
        pButtons.add(btnBuff);
        pButtons.add(btnClose);

        pBottom.add(pSelected);
        pBottom.add(pInputs);
        pBottom.add(pButtons);

        // ===== HINT =====
        JLabel lblHint = new JLabel("  💡 Click đúp vào item để chọn, rồi nhấn BUFF. Option mẫu: [[0,500]] = Tấn công +500");
        lblHint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblHint.setForeground(new Color(100, 100, 100));

        // Track selected item
        final int[] selectedItemId = {-1};

        tSearch.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 || e.getClickCount() == 1) {
                    int r = tSearch.getSelectedRow();
                    if (r != -1) {
                        int modelRow = tSearch.convertRowIndexToModel(r);
                        selectedItemId[0] = (int) searchModel.getValueAt(modelRow, 0);
                        String name = (String) searchModel.getValueAt(modelRow, 2);
                        lblSelected.setText("[" + selectedItemId[0] + "] " + name);
                        lblSelected.setForeground(new Color(0, 128, 0));
                        btnBuff.setEnabled(true);
                    }
                }
            }
        });

        btnClose.addActionListener(e -> d.dispose());

        btnBuff.addActionListener(e -> {
            if (selectedItemId[0] == -1) {
                JOptionPane.showMessageDialog(d, "Chưa chọn item!", "Lỗi", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int qty;
            try {
                qty = Integer.parseInt(txtQty.getText().trim());
                if (qty <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(d, "Số lượng không hợp lệ!", "Lỗi", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String optJson = txtOpt.getText().trim();
            if (optJson.isEmpty()) optJson = "[]";

            String itemName = getItemName(selectedItemId[0]);
            int confirm = JOptionPane.showConfirmDialog(d,
                    "Buff cho " + playerName + ":\n"
                    + "• Item: [" + selectedItemId[0] + "] " + itemName + "\n"
                    + "• Số lượng: " + qty + "\n"
                    + "• Options: " + optJson + "\n\n"
                    + "Xác nhận buff?",
                    "Xác nhận Buff", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) return;

            // Execute buff
            final String finalOptJson = optJson;
            final int finalQty = qty;
            new Thread(() -> {
                try (Connection conn = getConnection()) {
                    // 1. Load items_bag hiện tại
                    String loadSql = "SELECT items_bag FROM player WHERE id = ?";
                    String itemsBagJson;
                    try (PreparedStatement ps = conn.prepareStatement(loadSql)) {
                        ps.setInt(1, playerId);
                        ResultSet rs = ps.executeQuery();
                        if (!rs.next()) {
                            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(d,
                                    "Không tìm thấy nhân vật!", "Lỗi", JOptionPane.ERROR_MESSAGE));
                            return;
                        }
                        itemsBagJson = rs.getString("items_bag");
                    }

                    // 2. Parse và tìm ô trống
                    JsonArray bagArr;
                    try {
                        bagArr = new JsonParser().parse(itemsBagJson).getAsJsonArray();
                    } catch (Exception ex) {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(d,
                                "Lỗi parse items_bag!", "Lỗi", JOptionPane.ERROR_MESSAGE));
                        return;
                    }

                    int emptySlot = -1;
                    for (int i = 0; i < bagArr.size(); i++) {
                        try {
                            String innerStr = bagArr.get(i).getAsString();
                            JsonArray itemData = new JsonParser().parse(innerStr).getAsJsonArray();
                            if (itemData.get(0).getAsInt() == -1) {
                                emptySlot = i;
                                break;
                            }
                        } catch (Exception ex) {
                            emptySlot = i;
                            break;
                        }
                    }

                    if (emptySlot == -1) {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(d,
                                "Hành trang đã đầy! Không còn ô trống.", "Lỗi", JOptionPane.ERROR_MESSAGE));
                        return;
                    }

                    // 3. Tạo item mới
                    JsonArray newItem = new JsonArray();
                    newItem.add(selectedItemId[0]);
                    newItem.add(finalQty);
                    newItem.add(finalOptJson);
                    newItem.add(System.currentTimeMillis());
                    bagArr.set(emptySlot, new JsonPrimitive(newItem.toString()));

                    // 4. Update DB
                    String updateSql = "UPDATE player SET items_bag = ? WHERE id = ?";
                    try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                        ps.setString(1, bagArr.toString());
                        ps.setInt(2, playerId);
                        ps.executeUpdate();
                    }

                    final int slotIndex = emptySlot;
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(d,
                                "✅ Buff thành công!\n\n"
                                + "• Item: [" + selectedItemId[0] + "] " + getItemName(selectedItemId[0]) + "\n"
                                + "• Số lượng: " + finalQty + "\n"
                                + "• Ô hành trang: #" + (slotIndex + 1) + "\n\n"
                                + "⚠️ Nhân vật cần re-login để nhận đồ.",
                                "Buff Thành Công", JOptionPane.INFORMATION_MESSAGE);
                    });

                } catch (Exception ex) {
                    ex.printStackTrace();
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(d,
                            "Lỗi: " + ex.getMessage(), "Buff Thất Bại", JOptionPane.ERROR_MESSAGE));
                }
            }).start();
        });

        // ===== Layout =====
        d.add(pFilter, BorderLayout.NORTH);
        d.add(new JScrollPane(tSearch), BorderLayout.CENTER);
        JPanel south = new JPanel(new BorderLayout());
        south.add(lblHint, BorderLayout.NORTH);
        south.add(pBottom, BorderLayout.CENTER);
        d.add(south, BorderLayout.SOUTH);

        d.setVisible(true);
    }

    private long countItemTotal(String... jsonLists) {
        long total = 0;
        for (String json : jsonLists) {
            try {
                if (json == null || json.isEmpty())
                    continue;
                JsonElement parsed = new JsonParser().parse(json);
                if (!parsed.isJsonArray())
                    continue;
                JsonArray arr = parsed.getAsJsonArray();
                for (JsonElement e : arr) {
                    JsonArray item;
                    if (e.isJsonPrimitive())
                        item = new JsonParser().parse(e.getAsString()).getAsJsonArray();
                    else
                        item = e.getAsJsonArray();
                    if (item.size() >= 2 && item.get(0).getAsInt() == 457)
                        total += item.get(1).getAsLong();
                }
            } catch (Exception e) {
            }
        }
        return total;
    }

    private void loadPlayersFromDB(String keyword) {
        new Thread(() -> {
            // Count total
            String filterIdx = cbFilter != null ? (String) cbFilter.getSelectedItem() : "Tất cả";
            String countSql = "SELECT COUNT(*) FROM player p LEFT JOIN account a ON p.account_id = a.id WHERE 1=1";
            String dataSql = "SELECT p.id, p.head, p.name, p.power, p.clan_id, p.data_inventory, p.items_bag, p.items_box, a.cash, a.danap, a.active, a.ban FROM player p LEFT JOIN account a ON p.account_id = a.id WHERE 1=1";
            String where = "";
            if (!keyword.isEmpty()) where += " AND p.name LIKE '%" + keyword + "%'";
            if ("Đã kích hoạt".equals(filterIdx)) where += " AND a.active = 1";
            else if ("Chưa kích hoạt".equals(filterIdx)) where += " AND (a.active = 0 OR a.active IS NULL)";
            else if ("Bị Ban".equals(filterIdx)) where += " AND a.ban = 1";
            countSql += where;

            // Determine ORDER BY based on ranking filter
            String orderBy;
            if (filterIdx != null && filterIdx.contains("Top Sức Mạnh")) {
                orderBy = " ORDER BY p.power DESC";
            } else if (filterIdx != null && filterIdx.contains("Top Nạp")) {
                orderBy = " ORDER BY a.danap DESC";
            } else if (filterIdx != null && filterIdx.contains("Top VNĐ")) {
                orderBy = " ORDER BY a.cash DESC";
            } else {
                orderBy = " ORDER BY p.id DESC";
            }
            dataSql += where + orderBy + " LIMIT " + pageSize + " OFFSET " + ((currentPage - 1) * pageSize);

            try (Connection conn = getConnection()) {
                try (Statement st = conn.createStatement(); ResultSet crs = st.executeQuery(countSql)) {
                    if (crs.next()) totalRows = crs.getInt(1);
                }
            } catch (Exception e) { e.printStackTrace(); }

            int maxPage = Math.max(1, (totalRows + pageSize - 1) / pageSize);
            if (currentPage > maxPage) currentPage = maxPage;

            SwingUtilities.invokeLater(() -> {
                model.setRowCount(0);
                inventoryCache.clear();
                modifiedPlayerIds.clear();
                btnQuickSave.setEnabled(false);
                btnQuickSave.setText("Lưu thay đổi (Ctrl+S)");
                btnQuickSave.setBackground(Color.GRAY);
                lblPageInfo.setText("Trang " + currentPage + " / " + maxPage + " (" + totalRows + " người chơi)");
            });

            try (Connection conn = getConnection();
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(dataSql)) {
                while (rs.next()) {
                    int pid = rs.getInt("id");
                    Vector<Object> row = new Vector<>();
                    row.add(false); // checkbox
                    row.add((long) pid);
                    row.add(drawHeadIcon(rs.getInt("head")));
                    row.add(rs.getString("name"));
                    row.add(String.format("%,d", rs.getLong("power")));
                    row.add(getClanName(rs.getInt("clan_id")));

                    String rawInv = rs.getString("data_inventory");
                    inventoryCache.put(pid, rawInv);

                    try {
                        JsonArray inv = new JsonParser().parse(rawInv).getAsJsonArray();
                        row.add(inv.get(0).getAsLong());
                        row.add(inv.get(1).getAsLong());
                    } catch (Exception e) {
                        row.add(0L);
                        row.add(0L);
                    }

                    row.add(countItemTotal(rs.getString("items_bag"), rs.getString("items_box")));
                    row.add(rs.getLong("cash"));
                    row.add(rs.getLong("danap"));
                    row.add(rs.getInt("active") == 1 ? "Đã kích hoạt" : "Chưa kích hoạt");
                    row.add(rs.getInt("ban") == 1 ? "Bị chặn (Block)" : "Bình thường");

                    SwingUtilities.invokeLater(() -> model.addRow(row));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void saveModifiedRows() {
        if (modifiedPlayerIds.isEmpty())
            return;
        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn lưu " + modifiedPlayerIds.size() + " tài khoản đã sửa?", "Xác nhận",
                JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION)
            return;

        new Thread(() -> {
            try (Connection conn = getConnection()) {
                conn.setAutoCommit(false);
                String updatePlayerSql = "UPDATE player SET data_inventory = ? WHERE id = ?";
                String updateAccountSql = "UPDATE account SET cash = ?, vnd = ?, danap = ? WHERE id = (SELECT account_id FROM player WHERE id = ?)";

                try (PreparedStatement psPlayer = conn.prepareStatement(updatePlayerSql);
                        PreparedStatement psAccount = conn.prepareStatement(updateAccountSql)) {

                    for (int i = 0; i < model.getRowCount(); i++) {
                        int pid = Integer.parseInt(model.getValueAt(i, 1).toString());
                        if (modifiedPlayerIds.contains(pid)) {
                            long newGold = Long
                                    .parseLong(model.getValueAt(i, 6).toString().replace(",", "").replace(".", ""));
                            long newGem = Long
                                    .parseLong(model.getValueAt(i, 7).toString().replace(",", "").replace(".", ""));
                            long newCash = Long
                                    .parseLong(model.getValueAt(i, 9).toString().replace(",", "").replace(".", ""));
                            long newDaNap = Long
                                    .parseLong(model.getValueAt(i, 10).toString().replace(",", "").replace(".", ""));

                            String rawInv = inventoryCache.get(pid);
                            JsonArray invArr;
                            try {
                                invArr = new JsonParser().parse(rawInv).getAsJsonArray();
                            } catch (Exception ex) {
                                invArr = new JsonArray();
                                invArr.add(0);
                                invArr.add(0);
                                invArr.add(0);
                            }
                            while (invArr.size() < 3)
                                invArr.add(0);

                            invArr.set(0, new JsonPrimitive(newGold));
                            invArr.set(1, new JsonPrimitive(newGem));

                            psPlayer.setString(1, invArr.toString());
                            psPlayer.setInt(2, pid);
                            psPlayer.addBatch();

                            psAccount.setLong(1, newCash);
                            psAccount.setLong(2, newCash); // vnd sync with cash
                            psAccount.setLong(3, newDaNap);
                            psAccount.setInt(4, pid);
                            psAccount.addBatch();
                        }
                    }
                    psPlayer.executeBatch();
                    psAccount.executeBatch();
                    conn.commit();

                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "Đã lưu thành công!");
                        modifiedPlayerIds.clear();
                        btnQuickSave.setEnabled(false);
                        btnQuickSave.setBackground(Color.GRAY);
                        btnQuickSave.setText("Lưu thay đổi (Ctrl+S)");
                        loadPlayersFromDB(txtSearch.getText().trim());
                    });
                } catch (Exception ex) {
                    conn.rollback();
                    ex.printStackTrace();
                    SwingUtilities
                            .invokeLater(() -> JOptionPane.showMessageDialog(this, "Lỗi khi lưu: " + ex.getMessage()));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void openPlayerEditorDB(int playerId) {
        JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Chỉnh sửa Chi Tiết - ID: " + playerId,
                true);
        d.setSize(1200, 800);
        d.setLocationRelativeTo(null);
        d.setLayout(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));

        Map<String, Component> inputs = new HashMap<>();
        Map<String, String> originalData = new HashMap<>();

        JRootPane rootPane = d.getRootPane();
        InputMap im = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = rootPane.getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()),
                "saveDetail");

        new Thread(() -> {
            // Thay dataTaskBadges bằng dataBadges
            String query = "SELECT p.*, a.cash, a.danap, a.active, a.ban FROM player p LEFT JOIN account a ON p.account_id = a.id WHERE p.id = "
                    + playerId;
            try (Connection conn = getConnection();
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(query)) {
                if (rs.next()) {
                    int accountId = rs.getInt("account_id");
                    originalData.put("data_inventory", rs.getString("data_inventory"));
                    originalData.put("data_point", rs.getString("data_point"));
                    originalData.put("items_body", rs.getString("items_body"));
                    originalData.put("items_bag", rs.getString("items_bag"));
                    originalData.put("items_box", rs.getString("items_box"));
                    originalData.put("pet", rs.getString("pet"));

                    // Load dữ liệu nhiệm vụ
                    originalData.put("data_task", rs.getString("data_task"));
                    originalData.put("data_side_task", rs.getString("data_side_task"));
                    originalData.put("data_clan_task", rs.getString("data_clan_task"));
                    originalData.put("data_kol_task", rs.getString("data_kol_task"));

                    // NEW: dataBadges
                    originalData.put("dataBadges", rs.getString("dataBadges"));

                    JPanel pMainInfo = new JPanel(new GridLayout(2, 2, 15, 15));
                    pMainInfo.setBorder(new EmptyBorder(15, 15, 15, 15));

                    JPanel pAcc = createSectionPanel("Thông tin Tài khoản");
                    addLabelInput(pAcc, "VND:", rs.getString("cash"), "cash", inputs);
                    addLabelInput(pAcc, "Tổng Nạp:", rs.getString("danap"), "danap", inputs);

                    JComboBox<String> cbActive = new JComboBox<>(
                            new String[] { "0 - Chưa kích hoạt", "1 - Đã kích hoạt" });
                    cbActive.setSelectedIndex(rs.getInt("active") == 1 ? 1 : 0);
                    inputs.put("active_box", cbActive);
                    JPanel pAct = new JPanel(new BorderLayout());
                    pAct.add(new JLabel("Active:"), BorderLayout.WEST);
                    pAct.add(cbActive);
                    pAcc.add(pAct);

                    JComboBox<String> cbBan = new JComboBox<>(new String[] { "0 - Disabled", "1 - Blocked" });
                    cbBan.setSelectedIndex(rs.getInt("ban") == 1 ? 1 : 0);
                    inputs.put("ban_box", cbBan);
                    JPanel pBan = new JPanel(new BorderLayout());
                    pBan.add(new JLabel("Ban:"), BorderLayout.WEST);
                    pBan.add(cbBan);
                    pAcc.add(pBan);

                    JPanel pChar = createSectionPanel("Thông tin Nhân vật");
                    addLabelInput(pChar, "Tên:", rs.getString("name"), "name", inputs);
                    addLabelInput(pChar, "Sức mạnh:", rs.getString("power"), "power", inputs);
                    addLabelInput(pChar, "Head Part ID:", String.valueOf(rs.getInt("head")), "head", inputs);

                    JPanel pPoint = createSectionPanel("Chỉ số & Tiềm năng");
                    pPoint.setLayout(new GridLayout(0, 2, 5, 5));
                    JsonArray point = new JsonParser().parse(originalData.get("data_point")).getAsJsonArray();
                    addLabelInputGrid(pPoint, "Tiềm năng:", getJsonVal(point, 2), "tiemnang", inputs);
                    addLabelInputGrid(pPoint, "HP Gốc:", getJsonVal(point, 5), "hpg", inputs);
                    addLabelInputGrid(pPoint, "KI Gốc:", getJsonVal(point, 6), "mpg", inputs);
                    addLabelInputGrid(pPoint, "Sức đánh:", getJsonVal(point, 7), "dameg", inputs);
                    addLabelInputGrid(pPoint, "Giáp:", getJsonVal(point, 8), "defg", inputs);
                    addLabelInputGrid(pPoint, "Chí mạng:", getJsonVal(point, 9), "critg", inputs);

                    JPanel pAsset = createSectionPanel("Tài sản");
                    JsonArray inv = new JsonParser().parse(originalData.get("data_inventory")).getAsJsonArray();
                    addLabelInput(pAsset, "Vàng:", inv.get(0).getAsString(), "gold", inputs);
                    addLabelInput(pAsset, "Ngọc xanh:", inv.get(1).getAsString(), "gem", inputs);
                    addLabelInput(pAsset, "Hồng ngọc:", inv.size() > 2 ? inv.get(2).getAsString() : "0", "ruby", inputs);

                    pMainInfo.add(pAcc);
                    pMainInfo.add(pPoint);
                    pMainInfo.add(pChar);
                    pMainInfo.add(pAsset);


                    JTabbedPane tabItems = new JTabbedPane();
                    DefaultTableModel mBody = createItemModel();
                    DefaultTableModel mBag = createItemModel();
                    DefaultTableModel mBox = createItemModel();

                    loadItemsToModel(originalData.get("items_body"), mBody);
                    loadItemsToModel(originalData.get("items_bag"), mBag);
                    loadItemsToModel(originalData.get("items_box"), mBox);

                    tabItems.addTab("Đồ đang mặc", createItemPanel(mBody, d, true));
                    tabItems.addTab("Hành trang", createItemPanel(mBag, d, false));
                    tabItems.addTab("Rương đồ", createItemPanel(mBox, d, false));

                    JPanel pPet = new JPanel(new BorderLayout());
                    JPanel pPetContent = new JPanel(new GridBagLayout());
                    JScrollPane petScroll = new JScrollPane(pPetContent);
                    pPet.add(petScroll, BorderLayout.CENTER);

                    String petStr = rs.getString("pet");
                    if (petStr != null && !petStr.equals("[]") && !petStr.isEmpty()) {
                        try {
                            JsonArray petArr = new JsonParser().parse(petStr).getAsJsonArray();
                            if (petArr.size() > 1) {
                                String infoStr = petArr.get(0).getAsString();
                                JsonArray infoArr = new JsonParser().parse(infoStr).getAsJsonArray();
                                String pointStr = petArr.get(1).getAsString();
                                JsonArray pointArr = new JsonParser().parse(pointStr).getAsJsonArray();

                                GridBagConstraints gp = new GridBagConstraints();
                                gp.fill = GridBagConstraints.HORIZONTAL;
                                gp.weightx = 1.0;
                                gp.insets = new Insets(5, 5, 5, 5);
                                gp.gridx = 0;
                                gp.gridy = 0;

                                JPanel pPetInfo = createSectionPanel("Thông tin cơ bản");
                                pPetInfo.setLayout(new GridLayout(0, 2, 10, 10));

                                JComboBox<String> cbPetType = new JComboBox<>(
                                        new String[] { "0 - Đệ tử (thường)", "1 - Mabư", "2 - Black Goku", "3 - Cell/Pic", "4 - Berus", "5 - Tuyệt Thế" });
                                cbPetType.setEditable(true);
                                int currentType = infoArr.get(0).getAsInt();
                                if (currentType >= 0 && currentType <= 5) {
                                    cbPetType.setSelectedIndex(currentType);
                                } else {
                                    cbPetType.setSelectedItem(infoArr.get(0).getAsString());
                                }
                                inputs.put("pet_type", cbPetType);
                                pPetInfo.add(new JLabel("Loại Đệ:"));
                                pPetInfo.add(cbPetType);

                                JComboBox<String> cbPetGender = new JComboBox<>(
                                        new String[] { "0 - Trái đất", "1 - Namếc", "2 - Xayda" });
                                cbPetGender.setSelectedIndex(infoArr.get(1).getAsInt());
                                inputs.put("pet_gender", cbPetGender);
                                pPetInfo.add(new JLabel("Hệ:"));
                                pPetInfo.add(cbPetGender);

                                addLabelInputGrid(pPetInfo, "Tên Đệ tử:", infoArr.get(2).getAsString(), "pet_name", inputs);

                                JComboBox<String> cbPetStatus = new JComboBox<>(new String[] { "0 - Đi theo",
                                        "1 - Bảo vệ", "2 - Tấn công", "3 - Về nhà", "4 - Hợp thể" });
                                try {
                                    cbPetStatus.setSelectedIndex(infoArr.get(5).getAsInt());
                                } catch (Exception ex) {
                                }
                                inputs.put("pet_status", cbPetStatus);
                                pPetInfo.add(new JLabel("Trạng thái:"));
                                pPetInfo.add(cbPetStatus);
                                
                                pPetContent.add(pPetInfo, gp);

                                // === QUICK PET CHANGE BUTTONS ===
                                gp.gridy = 1;
                                JPanel pQuickChange = createSectionPanel("⚡ Đổi Loại Đệ Nhanh (Admin Test)");
                                pQuickChange.setLayout(new GridLayout(1, 6, 8, 8));

                                String[][] petPresets = {
                                    {"Thường", "0", "$NormalPet", "2000", "1500", "1500", "30", "15", "3"},
                                    {"Mabu", "1", "$MabuPet", "1500000", "80000", "80000", "80", "25", "8"},
                                    {"B.Goku", "2", "$BlackGoku", "1500000", "90000", "90000", "100", "30", "10"},
                                    {"Cell", "3", "$CellPet", "1500000", "90000", "90000", "100", "30", "10"},
                                    {"Berus", "4", "$BerusPet", "1500000", "100000", "100000", "120", "40", "12"},
                                    {"T.Thế", "5", "$TuyetThe", "100000000000", "900000", "900000", "40000", "100", "12"},
                                };

                                for (String[] preset : petPresets) {
                                    JButton btnPreset = new JButton(preset[0]);
                                    btnPreset.setFont(new Font("Segoe UI", Font.BOLD, 11));
                                    btnPreset.setFocusPainted(false);
                                    btnPreset.setCursor(new Cursor(Cursor.HAND_CURSOR));
                                    int typeIdx = Integer.parseInt(preset[1]);
                                    if (typeIdx == currentType) {
                                        btnPreset.setBackground(new Color(46, 204, 113));
                                        btnPreset.setForeground(Color.WHITE);
                                    } else {
                                        btnPreset.setBackground(new Color(52, 152, 219));
                                        btnPreset.setForeground(Color.WHITE);
                                    }
                                    btnPreset.addActionListener(ev -> {
                                        cbPetType.setSelectedIndex(typeIdx);
                                        ((JTextField) inputs.get("pet_name")).setText(preset[2]);
                                        ((JTextField) inputs.get("pet_power")).setText(preset[3]);
                                        ((JTextField) inputs.get("pet_hpg")).setText(preset[4]);
                                        ((JTextField) inputs.get("pet_mpg")).setText(preset[5]);
                                        ((JTextField) inputs.get("pet_dameg")).setText(preset[6]);
                                        ((JTextField) inputs.get("pet_defg")).setText(preset[7]);
                                        ((JTextField) inputs.get("pet_critg")).setText(preset[8]);
                                        JOptionPane.showMessageDialog(null,
                                                "✅ Đã set nhanh → " + preset[0] + "\n"
                                                + "• Type: " + preset[1] + "\n"
                                                + "• SM: " + preset[3] + "\n"
                                                + "Nhấn LƯU DỮ LIỆU để apply!\n"
                                                + "⚠️ Player cần re-login.",
                                                "Quick Set Pet", JOptionPane.INFORMATION_MESSAGE);
                                    });
                                    pQuickChange.add(btnPreset);
                                }

                                pPetContent.add(pQuickChange, gp);

                                gp.gridy = 2;
                                JPanel pPetStats = createSectionPanel("Chỉ số Sức Mạnh (Point)");
                                pPetStats.setLayout(new GridLayout(0, 2, 10, 10));

                                addLabelInputGrid(pPetStats, "Sức mạnh:", getJsonVal(pointArr, 1), "pet_power", inputs);
                                addLabelInputGrid(pPetStats, "Tiềm năng:", getJsonVal(pointArr, 2), "pet_tiemnang",
                                        inputs);
                                addLabelInputGrid(pPetStats, "HP Gốc:", getJsonVal(pointArr, 5), "pet_hpg", inputs);
                                addLabelInputGrid(pPetStats, "KI Gốc:", getJsonVal(pointArr, 6), "pet_mpg", inputs);
                                addLabelInputGrid(pPetStats, "Sức đánh:", getJsonVal(pointArr, 7), "pet_dameg", inputs);
                                addLabelInputGrid(pPetStats, "Giáp:", getJsonVal(pointArr, 8), "pet_defg", inputs);
                                addLabelInputGrid(pPetStats, "Chí mạng:", getJsonVal(pointArr, 9), "pet_critg", inputs);

                                pPetContent.add(pPetStats, gp);
                            }
                        } catch (Exception ex) {
                            pPet.add(new JLabel("Lỗi đọc đệ tử: " + ex.getMessage()));
                        }
                    } else {
                        pPet.add(new JLabel("Không có đệ tử.", SwingConstants.CENTER));
                    }

                    // Tab nhiệm vụ
                    JPanel pTasks = createTaskPanel(originalData, inputs);

                    // NEW: Tab Badges (Thay thế TaskBadges cũ)
                    DefaultTableModel mBadges = new DefaultTableModel(
                            new String[] { "ID Badges", "Icon", "Tên Danh Hiệu", "Chỉ số (Options)",
                                    "Thời gian hết hạn (Long)", "Ngày còn lại", "Đang dùng" },
                            0) {
                        @Override
                        public boolean isCellEditable(int row, int column) {
                            return column == 0 || column == 4 || column == 6;
                        }

                        @Override
                        public Class<?> getColumnClass(int columnIndex) {
                            if (columnIndex == 1)
                                return ImageIcon.class; // Icon
                            if (columnIndex == 6)
                                return Boolean.class; // Checkbox
                            return Object.class;
                        }
                    };
                    JPanel pBadges = createBadgesPanel(originalData.get("dataBadges"), mBadges, d);

                    tabs.addTab("Thông tin chung", new JScrollPane(pMainInfo));
                    tabs.addTab("Vật phẩm", tabItems);
                    tabs.addTab("Đệ tử", pPet);
                    tabs.addTab("Nhiệm vụ", new JScrollPane(pTasks));
                    tabs.addTab("Danh hiệu (Badges)", pBadges);
                    tabs.addTab("Truy vết", createPlayerAuditPanel(accountId, rs.getString("name"), originalData));

                    d.add(tabs, BorderLayout.CENTER);

                    JPanel pBtn = new JPanel();
                    pBtn.setBorder(new EmptyBorder(10, 0, 10, 0));
                    JButton btnSave = createStyledButton("LƯU DỮ LIỆU", COLOR_SUCCESS, Color.WHITE);
                    btnSave.setFont(new Font("Segoe UI", Font.BOLD, 14));
                    btnSave.setPreferredSize(new Dimension(200, 45));
                    btnSave.addActionListener(ev -> savePlayerDB(playerId, accountId, inputs, mBody, mBag, mBox,
                            mBadges, originalData, d));
                    pBtn.add(btnSave);
                    d.add(pBtn, BorderLayout.SOUTH);

                    am.put("saveDetail", new AbstractAction() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            btnSave.doClick();
                        }
                    });

                    SwingUtilities.invokeLater(() -> d.setVisible(true));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private JPanel createPlayerAuditPanel(int accountId, String playerName, Map<String, String> originalData) {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setBackground(Color.WHITE);

        // === TOP: Summary Cards ===
        JPanel cards = new JPanel(new GridLayout(1, 5, 8, 0));
        cards.setOpaque(false);
        cards.setBorder(new EmptyBorder(0, 0, 8, 0));

        long gold = 0, gem = 0, ruby = 0, cash = 0, danap = 0;
        try {
            JsonArray inv = new JsonParser().parse(originalData.get("data_inventory")).getAsJsonArray();
            gold = inv.size() > 0 ? inv.get(0).getAsLong() : 0;
            gem = inv.size() > 1 ? inv.get(1).getAsLong() : 0;
            ruby = inv.size() > 2 ? inv.get(2).getAsLong() : 0;
        } catch (Exception ignored) {}
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT cash, danap FROM account WHERE id = ?")) {
            ps.setInt(1, accountId);
            ResultSet crs = ps.executeQuery();
            if (crs.next()) { cash = crs.getLong("cash"); danap = crs.getLong("danap"); }
        } catch (Exception ignored) {}

        cards.add(buildCard("Vang", String.format("%,d", gold), new Color(255, 193, 7)));
        cards.add(buildCard("Ngoc Xanh", String.format("%,d", gem), new Color(40, 167, 69)));
        cards.add(buildCard("Hong Ngoc", String.format("%,d", ruby), new Color(220, 53, 69)));
        cards.add(buildCard("So du VND", String.format("%,d", cash), new Color(0, 120, 215)));
        cards.add(buildCard("Tong Nap", String.format("%,d", danap), new Color(102, 51, 204)));
        panel.add(cards, BorderLayout.NORTH);

        // === CENTER: Audit Tabs ===
        JTabbedPane auditTabs = new JTabbedPane();
        auditTabs.setFont(new Font("Segoe UI", Font.BOLD, 12));

        auditTabs.addTab("VND/Cash", createQueryTablePanel(
                "SELECT created_at AS time, source, amount, balance_before, balance_after, detail "
                + "FROM cash_audit_log WHERE account_id = ? ORDER BY created_at DESC LIMIT 200",
                accountId,
                new String[] {"time", "source", "amount", "balance_before", "balance_after", "detail"},
                "Chua co log VND."));
        auditTabs.addTab("Nap Tien (Recharge)", createQueryTablePanel(
                "SELECT r.id, r.trans_id, r.amount, r.description, r.status, r.created_at "
                + "FROM recharge_log r WHERE r.description LIKE CONCAT('%', (SELECT CAST(p.id AS CHAR) FROM player p WHERE p.account_id = ? LIMIT 1), '%') ORDER BY r.id DESC LIMIT 100",
                accountId,
                new String[] {"id", "trans_id", "amount", "description", "status", "created_at"},
                "Chua co giao dich nap tien."));
        auditTabs.addTab("Phan Tich Vang", createGoldRelationPanel(accountId, playerName));
        auditTabs.addTab("Giao dich P2P", createTradeHistoryPanel(playerName));
        auditTabs.addTab("Tai Khoan", createAccountInfoPanel(accountId));
        panel.add(auditTabs, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Panel lịch sử giao dịch P2P của một player
     */
    private JPanel createTradeHistoryPanel(String playerName) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setBackground(Color.WHITE);

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        toolbar.setOpaque(false);
        JLabel lblInfo = new JLabel("📦 Lịch sử giao dịch giữa người chơi (Trade P2P)");
        lblInfo.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblInfo.setForeground(new Color(0, 120, 215));
        toolbar.add(lblInfo);

        JButton btnReload = createStyledButton("🔄 Tải lại", COLOR_PRIMARY, Color.WHITE);
        toolbar.add(btnReload);

        JButton btnAllTrades = createStyledButton("🌐 Tất cả GD Server", new Color(102, 51, 176), Color.WHITE);
        toolbar.add(btnAllTrades);

        panel.add(toolbar, BorderLayout.NORTH);

        // Table
        DefaultTableModel tradeModel = new DefaultTableModel(
                new String[] { "ID", "Thời gian", "Người 1", "Người 2",
                        "Đồ P1 gửi", "Đồ P2 gửi",
                        "Vàng P1 trước", "Vàng P1 sau", "Vàng P2 trước", "Vàng P2 sau" },
                0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tradeTable = new JTable(tradeModel);
        tradeTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tradeTable.setRowHeight(28);
        tradeTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tradeTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // Set column widths
        int[] widths = {40, 140, 120, 120, 250, 250, 100, 100, 100, 100};
        for (int i = 0; i < widths.length && i < tradeTable.getColumnCount(); i++) {
            tradeTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        JScrollPane scrollTrade = new JScrollPane(tradeTable);
        panel.add(scrollTrade, BorderLayout.CENTER);

        // Detail panel bottom
        JTextArea txtDetail = new JTextArea(6, 80);
        txtDetail.setEditable(false);
        txtDetail.setFont(new Font("Consolas", Font.PLAIN, 12));
        txtDetail.setBackground(new Color(250, 250, 250));
        txtDetail.setLineWrap(true);
        txtDetail.setWrapStyleWord(true);
        JScrollPane scrollDetail = new JScrollPane(txtDetail);
        scrollDetail.setBorder(BorderFactory.createTitledBorder("Chi tiết giao dịch (click vào dòng để xem)"));
        panel.add(scrollDetail, BorderLayout.SOUTH);

        // Click row to show detail
        tradeTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = tradeTable.getSelectedRow();
                if (row < 0) return;
                int id = Integer.parseInt(tradeModel.getValueAt(row, 0).toString());
                new Thread(() -> {
                    try (Connection conn = getConnection();
                         PreparedStatement ps = conn.prepareStatement(
                             "SELECT * FROM history_transaction WHERE id = ?")) {
                        ps.setInt(1, id);
                        ResultSet rs = ps.executeQuery();
                        if (rs.next()) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("═══ GIAO DỊCH #").append(id).append(" ═══\n");
                            sb.append("Thời gian: ").append(rs.getTimestamp("time_tran")).append("\n");
                            sb.append("Người 1: ").append(rs.getString("player_1")).append("\n");
                            sb.append("Người 2: ").append(rs.getString("player_2")).append("\n\n");
                            sb.append("── P1 gửi ──\n").append(rs.getString("item_player_1")).append("\n\n");
                            sb.append("── P2 gửi ──\n").append(rs.getString("item_player_2")).append("\n\n");
                            sb.append("── Vàng ──\n");
                            sb.append("P1: ").append(rs.getLong("gold_1_before")).append(" → ").append(rs.getLong("gold_1_after")).append("\n");
                            sb.append("P2: ").append(rs.getLong("gold_2_before")).append(" → ").append(rs.getLong("gold_2_after")).append("\n\n");
                            sb.append("── Bag P1 trước ──\n").append(rs.getString("bag_1_before_tran")).append("\n\n");
                            sb.append("── Bag P1 sau ──\n").append(rs.getString("bag_1_after_tran")).append("\n");
                            SwingUtilities.invokeLater(() -> txtDetail.setText(sb.toString()));
                        }
                    } catch (Exception ex) {
                        SwingUtilities.invokeLater(() -> txtDetail.setText("Lỗi: " + ex.getMessage()));
                    }
                }).start();
            }
        });

        // Load data for this player
        Runnable loadPlayerTrades = () -> {
            tradeModel.setRowCount(0);
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, time_tran, player_1, player_2, item_player_1, item_player_2, "
                     + "gold_1_before, gold_1_after, gold_2_before, gold_2_after "
                     + "FROM history_transaction WHERE player_1 LIKE ? OR player_2 LIKE ? "
                     + "ORDER BY id DESC LIMIT 200")) {
                String pattern = "%" + playerName + "%";
                ps.setString(1, pattern);
                ps.setString(2, pattern);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    String items1 = rs.getString("item_player_1");
                    String items2 = rs.getString("item_player_2");
                    // Truncate for display
                    if (items1 != null && items1.length() > 80) items1 = items1.substring(0, 80) + "...";
                    if (items2 != null && items2.length() > 80) items2 = items2.substring(0, 80) + "...";
                    tradeModel.addRow(new Object[] {
                        rs.getInt("id"),
                        rs.getTimestamp("time_tran"),
                        rs.getString("player_1"),
                        rs.getString("player_2"),
                        items1,
                        items2,
                        String.format("%,d", rs.getLong("gold_1_before")),
                        String.format("%,d", rs.getLong("gold_1_after")),
                        String.format("%,d", rs.getLong("gold_2_before")),
                        String.format("%,d", rs.getLong("gold_2_after"))
                    });
                }
                if (tradeModel.getRowCount() == 0) {
                    SwingUtilities.invokeLater(() -> txtDetail.setText("Không tìm thấy giao dịch nào của " + playerName));
                }
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> txtDetail.setText("Lỗi truy vấn: " + ex.getMessage()));
            }
        };
        btnReload.addActionListener(e -> new Thread(loadPlayerTrades).start());
        new Thread(loadPlayerTrades).start();

        // All server trades button
        btnAllTrades.addActionListener(e -> {
            new Thread(() -> {
                tradeModel.setRowCount(0);
                try (Connection conn = getConnection();
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(
                         "SELECT id, time_tran, player_1, player_2, item_player_1, item_player_2, "
                         + "gold_1_before, gold_1_after, gold_2_before, gold_2_after "
                         + "FROM history_transaction ORDER BY id DESC LIMIT 500")) {
                    while (rs.next()) {
                        String items1 = rs.getString("item_player_1");
                        String items2 = rs.getString("item_player_2");
                        if (items1 != null && items1.length() > 80) items1 = items1.substring(0, 80) + "...";
                        if (items2 != null && items2.length() > 80) items2 = items2.substring(0, 80) + "...";
                        tradeModel.addRow(new Object[] {
                            rs.getInt("id"),
                            rs.getTimestamp("time_tran"),
                            rs.getString("player_1"),
                            rs.getString("player_2"),
                            items1,
                            items2,
                            String.format("%,d", rs.getLong("gold_1_before")),
                            String.format("%,d", rs.getLong("gold_1_after")),
                            String.format("%,d", rs.getLong("gold_2_before")),
                            String.format("%,d", rs.getLong("gold_2_after"))
                        });
                    }
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> txtDetail.setText("Lỗi: " + ex.getMessage()));
                }
            }).start();
        });

        return panel;
    }

    private JPanel buildCard(String title, String value, Color color) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(color);
        card.setBorder(new EmptyBorder(8, 12, 8, 12));
        JLabel lblT = new JLabel(title);
        lblT.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblT.setForeground(new Color(255, 255, 255, 200));
        lblT.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lblV = new JLabel(value);
        lblV.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblV.setForeground(Color.WHITE);
        lblV.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(lblT); card.add(Box.createVerticalStrut(2)); card.add(lblV);
        return card;
    }

    private JPanel createAccountInfoPanel(int accountId) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        JTextArea txt = new JTextArea();
        txt.setEditable(false);
        txt.setFont(new Font("Consolas", Font.PLAIN, 13));
        txt.setBackground(new Color(250, 250, 250));
        panel.add(new JScrollPane(txt), BorderLayout.CENTER);
        JButton btnReload = createStyledButton("Tai lai", COLOR_PRIMARY, Color.WHITE);
        panel.add(btnReload, BorderLayout.NORTH);
        Runnable load = () -> {
            StringBuilder sb = new StringBuilder();
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "SELECT a.*, p.name AS char_name, p.power, p.clan_id FROM account a "
                     + "LEFT JOIN player p ON p.account_id = a.id WHERE a.id = ?")) {
                ps.setInt(1, accountId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    sb.append("=== THONG TIN TAI KHOAN ===\n\n");
                    sb.append("Account ID: ").append(accountId).append("\n");
                    sb.append("Username: ").append(rs.getString("username")).append("\n");
                    sb.append("Nhan vat: ").append(rs.getString("char_name")).append("\n");
                    sb.append("Suc manh: ").append(String.format("%,d", rs.getLong("power"))).append("\n");
                    sb.append("Clan: ").append(getClanName(rs.getInt("clan_id"))).append("\n\n");
                    sb.append("=== TAI CHINH ===\n");
                    sb.append("Cash (VND): ").append(String.format("%,d", rs.getLong("cash"))).append("\n");
                    sb.append("Tong nap: ").append(String.format("%,d", rs.getLong("danap"))).append("\n");
                    try { sb.append("Thoi vang: ").append(String.format("%,d", rs.getLong("thoi_vang"))).append("\n"); } catch (Exception ignored) {}
                    sb.append("\n=== TRANG THAI ===\n");
                    sb.append("Active: ").append(rs.getInt("active") == 1 ? "Da kich hoat" : "Chua kich hoat").append("\n");
                    sb.append("Ban: ").append(rs.getInt("ban") == 1 ? "BI BAN" : "Binh thuong").append("\n");
                    try { sb.append("Last login: ").append(rs.getTimestamp("last_login")).append("\n"); } catch (Exception ignored) {}
                    try { sb.append("IP cuoi: ").append(rs.getString("last_ip")).append("\n"); } catch (Exception ignored) {}
                }
            } catch (Exception ex) { sb.append("Loi: ").append(ex.getMessage()); }
            SwingUtilities.invokeLater(() -> txt.setText(sb.toString()));
        };
        btnReload.addActionListener(e -> new Thread(load).start());
        new Thread(load).start();
        return panel;
    }

    private String buildAuditSummary(int accountId, String playerName, Map<String, String> originalData) {
        StringBuilder sb = new StringBuilder();
        sb.append("TÀI KHOẢN: ").append(playerName).append(" | account_id=").append(accountId).append('\n');
        try {
            JsonArray inv = new JsonParser().parse(originalData.get("data_inventory")).getAsJsonArray();
            long gold = inv.size() > 0 ? inv.get(0).getAsLong() : 0;
            long gem = inv.size() > 1 ? inv.get(1).getAsLong() : 0;
            long ruby = inv.size() > 2 ? inv.get(2).getAsLong() : 0;
            sb.append("Vàng hiện tại: ").append(gold).append(" | Ngọc xanh: ").append(gem).append(" | Hồng ngọc: ").append(ruby).append('\n');
            if (gold >= 1_000_000_000L) {
                sb.append("CẢNH BÁO: Vàng từ 1 tỷ trở lên, nên đối chiếu tab VND/Cash + Nạp bank + Liên đới vàng.\n");
            }
        } catch (Exception e) {
            sb.append("Không đọc được data_inventory: ").append(e.getMessage()).append('\n');
        }
        sb.append("Gợi ý đọc log: BANK_ATM/BANK_AUTO là nạp ATM hoặc cộng thưởng; DOI_THOI_VANG là đổi VNĐ sang thỏi vàng; ADMIN_* là thao tác admin.\n");
        sb.append("Server không dùng nạp thẻ. Muốn truy ra map/NPC phát sinh vàng chi tiết cần bật logger runtime cho các điểm cộng/trừ vàng và giao dịch.");
        return sb.toString();
    }

    private JPanel createQueryTablePanel(String sql, Object param, String[] columns, String emptyMessage) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        DefaultTableModel auditModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable auditTable = new JTable(auditModel);
        auditTable.setRowHeight(28);
        auditTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        auditTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        panel.add(new JScrollPane(auditTable), BorderLayout.CENTER);

        JButton btnReload = createStyledButton("Tải lại truy vấn", COLOR_PRIMARY, Color.WHITE);
        JLabel lblStatus = new JLabel(" ");
        JPanel top = new JPanel(new BorderLayout());
        top.add(btnReload, BorderLayout.WEST);
        top.add(lblStatus, BorderLayout.CENTER);
        panel.add(top, BorderLayout.NORTH);

        Runnable load = () -> {
            auditModel.setRowCount(0);
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                if (param instanceof Integer) {
                    ps.setInt(1, (Integer) param);
                } else {
                    ps.setString(1, String.valueOf(param));
                }
                try (ResultSet rs = ps.executeQuery()) {
                    int count = 0;
                    while (rs.next()) {
                        Vector<Object> row = new Vector<>();
                        for (String col : columns) {
                            row.add(rs.getObject(col));
                        }
                        auditModel.addRow(row);
                        count++;
                    }
                    lblStatus.setText(count == 0 ? emptyMessage : "Tìm thấy " + count + " dòng.");
                }
            } catch (Exception ex) {
                lblStatus.setText(emptyMessage + " (" + ex.getMessage() + ")");
            }
        };
        btnReload.addActionListener(e -> new Thread(load).start());
        new Thread(load).start();
        return panel;
    }

    private JPanel createGoldRelationPanel(int accountId, String playerName) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        JTextArea txt = new JTextArea();
        txt.setEditable(false);
        txt.setFont(new Font("Consolas", Font.PLAIN, 13));
        txt.setText("Đang phân tích dữ liệu vàng hiện có...\n");
        panel.add(new JScrollPane(txt), BorderLayout.CENTER);

        JButton btnReload = createStyledButton("Phân tích lại", COLOR_PRIMARY, Color.WHITE);
        panel.add(btnReload, BorderLayout.NORTH);
        Runnable load = () -> {
            StringBuilder sb = new StringBuilder();
            sb.append("PHÂN TÍCH LIÊN ĐỚI VÀNG - ").append(playerName).append("\n\n");
            try (Connection conn = getConnection()) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT p.name, JSON_UNQUOTE(JSON_EXTRACT(p.data_inventory, '$[0]')) AS gold, "
                        + "a.cash, a.vnd, a.danap, a.thoi_vang, a.active, a.ban "
                        + "FROM player p LEFT JOIN account a ON p.account_id = a.id WHERE a.id = ?")) {
                    ps.setInt(1, accountId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            sb.append("Snapshot DB:\n");
                            sb.append("- Gold: ").append(rs.getString("gold")).append('\n');
                            sb.append("- Cash/VND/Danap: ").append(rs.getLong("cash")).append("/")
                                    .append(rs.getLong("vnd")).append("/").append(rs.getLong("danap")).append('\n');
                            sb.append("- Thỏi vàng giữ hộ: ").append(rs.getLong("thoi_vang")).append('\n');
                            sb.append("- Active/Ban: ").append(rs.getInt("active")).append("/").append(rs.getInt("ban")).append("\n\n");
                        }
                    }
                }
                sb.append("Các nguồn hiện truy được:\n");
                sb.append("1) cash_audit_log: biết VND/Cash đến từ ATM, thưởng, admin hay đổi vật phẩm.\n");
                sb.append("2) recharge_log: biết lịch sử ATM/phần thưởng đã tạo giao dịch.\n");
                sb.append("3) data_inventory: biết lượng vàng hiện tại.\n\n");
                sb.append("Chưa có log map/NPC/trade vàng lịch sử nếu trước đây server chưa ghi.\n");
                sb.append("Bước tiếp theo nên triển khai: tạo bảng gold_audit_log và hook vào các điểm: giao dịch người chơi, bán thỏi vàng, NPC thưởng, nhặt item/vàng, admin buff.\n");
            } catch (Exception ex) {
                sb.append("Lỗi phân tích: ").append(ex.getMessage()).append('\n');
            }
            SwingUtilities.invokeLater(() -> txt.setText(sb.toString()));
        };
        btnReload.addActionListener(e -> new Thread(load).start());
        new Thread(load).start();
        return panel;
    }

    private JPanel createBadgesPanel(String jsonBadges, DefaultTableModel model, JDialog parent) {
        JPanel p = new JPanel(new BorderLayout(5, 5));
        p.setBorder(new EmptyBorder(10, 10, 10, 10));

        model.setRowCount(0); // Reset bảng

        if (jsonBadges != null && !jsonBadges.isEmpty() && !jsonBadges.equals("[]")) {
            try {
                JsonArray arr = new JsonParser().parse(jsonBadges).getAsJsonArray();
                for (JsonElement e : arr) {
                    JsonObject obj = e.getAsJsonObject();

                    // Lấy ID từ JSON của người chơi
                    int id = obj.get("idBadGes").getAsInt();
                    long time = obj.get("timeofUseBadges").getAsLong();
                    boolean isUse = obj.get("isUse").getAsBoolean();

                    long timeLeft = time - System.currentTimeMillis();
                    long daysLeft = timeLeft / (24 * 60 * 60 * 1000L);

                    // Tìm thông tin Badge trong cache
                    BadgeTemplate temp = badgeTemplateMap.get(id);

                    String name;
                    String optionsReadable;
                    ImageIcon icon;

                    if (temp != null) {
                        name = temp.name;
                        optionsReadable = parseBadgeOptions(temp.optionsJson);
                        // Lấy icon từ template (đã load từ loadCacheData)
                        icon = loadIconRaw(temp.iconId);
                    } else {
                        name = "Unknown [" + id + "]";
                        optionsReadable = "";
                        icon = null;
                    }

                    model.addRow(new Object[] { id, icon, name, optionsReadable, time,
                            (daysLeft > 0 ? daysLeft + " ngày" : "Hết hạn"), isUse });
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        // Cấu hình bảng hiển thị
        JTable t = new JTable(model);
        t.setRowHeight(30); // Tăng độ cao dòng để chứa icon
        t.getColumnModel().getColumn(0).setPreferredWidth(60);
        t.getColumnModel().getColumn(1).setPreferredWidth(40); // Cột Icon
        t.getColumnModel().getColumn(2).setPreferredWidth(150);
        t.getColumnModel().getColumn(3).setPreferredWidth(250);

        JPanel pTool = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnAdd = new JButton("Thêm Badge");
        JButton btnDel = new JButton("Xóa Badge");

        btnAdd.addActionListener(e -> openBadgeAddDialog(model, parent));
        btnDel.addActionListener(e -> {
            int r = t.getSelectedRow();
            if (r != -1)
                model.removeRow(r);
        });

        pTool.add(btnAdd);
        pTool.add(btnDel);
        p.add(pTool, BorderLayout.NORTH);
        p.add(new JScrollPane(t), BorderLayout.CENTER);
        return p;
    }

    private void openBadgeAddDialog(DefaultTableModel model, JDialog parent) {
        JDialog d = new JDialog(parent, "Thêm Danh Hiệu", true);
        d.setSize(600, 500);
        d.setLayout(new BorderLayout());
        d.setLocationRelativeTo(parent);

        JPanel pTop = new JPanel(new BorderLayout(5, 5));
        pTop.setBorder(new EmptyBorder(5, 5, 5, 5));
        JTextField txtSearch = new JTextField();
        txtSearch.putClientProperty("JTextField.placeholderText", "Nhập tên hoặc ID danh hiệu...");
        pTop.add(new JLabel("Tìm kiếm: "), BorderLayout.WEST);
        pTop.add(txtSearch, BorderLayout.CENTER);

        DefaultTableModel searchModel = new DefaultTableModel(new String[] { "ID", "Icon", "Tên Danh Hiệu", "Options" },
                0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 1)
                    return ImageIcon.class;
                return Object.class;
            }
        };

        for (BadgeTemplate b : badgeTemplateMap.values()) {
            searchModel.addRow(new Object[] { b.id, loadIconRaw(b.iconId), b.name, parseBadgeOptions(b.optionsJson) });
        }

        JTable t = new JTable(searchModel);
        t.setRowHeight(25);
        t.getColumnModel().getColumn(0).setPreferredWidth(50);
        t.getColumnModel().getColumn(1).setPreferredWidth(40);
        t.getColumnModel().getColumn(2).setPreferredWidth(150);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(searchModel);
        t.setRowSorter(sorter);

        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                filter();
            }

            public void removeUpdate(DocumentEvent e) {
                filter();
            }

            public void changedUpdate(DocumentEvent e) {
                filter();
            }

            void filter() {
                String text = txtSearch.getText().trim();
                if (text.isEmpty())
                    sorter.setRowFilter(null);
                else
                    sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
            }
        });

        t.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int r = t.getSelectedRow();
                    if (r != -1) {
                        int modelRow = t.convertRowIndexToModel(r);
                        int id = (int) searchModel.getValueAt(modelRow, 0);
                        ImageIcon icon = (ImageIcon) searchModel.getValueAt(modelRow, 1);
                        String name = (String) searchModel.getValueAt(modelRow, 2);
                        String opts = (String) searchModel.getValueAt(modelRow, 3);

                        String dayStr = JOptionPane.showInputDialog(d, "Nhập số ngày sử dụng:", "30");
                        if (dayStr != null) {
                            try {
                                int days = Integer.parseInt(dayStr);
                                long time = System.currentTimeMillis() + (long) days * 24 * 60 * 60 * 1000L;
                                model.addRow(new Object[] { id, icon, name, opts, time, days + " ngày", false });
                                d.dispose();
                            } catch (Exception ex) {
                                JOptionPane.showMessageDialog(d, "Lỗi nhập ngày!");
                            }
                        }
                    }
                }
            }
        });

        d.add(pTop, BorderLayout.NORTH);
        d.add(new JScrollPane(t), BorderLayout.CENTER);
        d.setVisible(true);
    }

    private String parseBadgeOptions(String jsonOpt) {
        if (jsonOpt == null || jsonOpt.isEmpty())
            return "";
        StringBuilder sb = new StringBuilder();
        try {
            JsonArray arr = new JsonParser().parse(jsonOpt).getAsJsonArray();
            for (JsonElement e : arr) {
                JsonObject obj = e.getAsJsonObject();
                int id = obj.get("id").getAsInt();
                int param = obj.get("param").getAsInt();
                sb.append(formatOption(id, param)).append("; ");
            }
        } catch (Exception e) {
            return jsonOpt;
        }
        return sb.toString();
    }

    private JPanel createTaskPanel(Map<String, String> originalData, Map<String, Component> inputs) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Nhiệm vụ chính
        JPanel mainTaskPanel = createSectionPanel("Nhiệm vụ chính tuyến");
        mainTaskPanel.setLayout(new GridLayout(0, 2, 5, 5));

        JsonArray dataTask = new JsonArray();
        try {
            String taskStr = originalData.get("data_task");
            if (taskStr != null && !taskStr.isEmpty()) {
                dataTask = new JsonParser().parse(taskStr).getAsJsonArray();
            }
        } catch (Exception e) {
            dataTask = new JsonArray();
        }
        while (dataTask.size() < 4)
            dataTask.add(0);

        JLabel lblMainTaskId = new JLabel("ID Nhiệm vụ:");
        JComboBox<String> cbMainTaskId = new JComboBox<>();
        cbMainTaskId.addItem("-1 - Không có");
        for (Map.Entry<Integer, String> entry : taskMainTemplateMap.entrySet()) {
            cbMainTaskId.addItem(entry.getKey() + " - " + entry.getValue());
        }
        int currentMainTaskId = dataTask.get(0).getAsInt();
        for (int i = 0; i < cbMainTaskId.getItemCount(); i++) {
            String item = cbMainTaskId.getItemAt(i);
            if (item.startsWith(currentMainTaskId + " - ")) {
                cbMainTaskId.setSelectedIndex(i);
                break;
            }
        }
        inputs.put("mainTaskId", cbMainTaskId);

        JLabel lblMainTaskIndex = new JLabel("Index:");
        JTextField txtMainTaskIndex = new JTextField(dataTask.get(1).getAsString());
        addUndoRedo(txtMainTaskIndex);
        inputs.put("mainTaskIndex", txtMainTaskIndex);

        JLabel lblMainTaskCount = new JLabel("Count:");
        JTextField txtMainTaskCount = new JTextField(dataTask.get(2).getAsString());
        addUndoRedo(txtMainTaskCount);
        inputs.put("mainTaskCount", txtMainTaskCount);

        JLabel lblMainTaskLastTime = new JLabel("Last Time:");
        JTextField txtMainTaskLastTime = new JTextField(dataTask.get(3).getAsString());
        addUndoRedo(txtMainTaskLastTime);
        inputs.put("mainTaskLastTime", txtMainTaskLastTime);

        mainTaskPanel.add(lblMainTaskId);
        mainTaskPanel.add(cbMainTaskId);
        mainTaskPanel.add(lblMainTaskIndex);
        mainTaskPanel.add(txtMainTaskIndex);
        mainTaskPanel.add(lblMainTaskCount);
        mainTaskPanel.add(txtMainTaskCount);
        mainTaskPanel.add(lblMainTaskLastTime);
        mainTaskPanel.add(txtMainTaskLastTime);

        // Nhiệm vụ hàng ngày
        JPanel sideTaskPanel = createSectionPanel("Nhiệm vụ hàng ngày");
        sideTaskPanel.setLayout(new GridLayout(0, 4, 5, 5));

        JsonArray sideTask = new JsonArray();
        try {
            String sideStr = originalData.get("data_side_task");
            if (sideStr != null && !sideStr.isEmpty()) {
                sideTask = new JsonParser().parse(sideStr).getAsJsonArray();
            }
        } catch (Exception e) {
            sideTask = new JsonArray();
        }
        while (sideTask.size() < 6)
            sideTask.add(0);

        for (int i = 0; i < 3; i++) {
            int idx = i * 2;
            JLabel lblSideId = new JLabel("Nhiệm vụ " + (i + 1) + " ID:");
            JComboBox<String> cbSideId = new JComboBox<>();
            cbSideId.addItem("-1 - Không có");
            for (Map.Entry<Integer, String> entry : sideTaskTemplateMap.entrySet()) {
                cbSideId.addItem(entry.getKey() + " - " + entry.getValue());
            }
            int currentSideId = sideTask.get(idx).getAsInt();
            for (int j = 0; j < cbSideId.getItemCount(); j++) {
                String item = cbSideId.getItemAt(j);
                if (item.startsWith(currentSideId + " - ")) {
                    cbSideId.setSelectedIndex(j);
                    break;
                }
            }
            inputs.put("sideTaskId_" + i, cbSideId);

            JLabel lblSideCount = new JLabel("Count:");
            JTextField txtSideCount = new JTextField(sideTask.get(idx + 1).getAsString());
            addUndoRedo(txtSideCount);
            inputs.put("sideTaskCount_" + i, txtSideCount);

            sideTaskPanel.add(lblSideId);
            sideTaskPanel.add(cbSideId);
            sideTaskPanel.add(lblSideCount);
            sideTaskPanel.add(txtSideCount);
        }

        // Nhiệm vụ clan
        JPanel clanTaskPanel = createSectionPanel("Nhiệm vụ clan");
        clanTaskPanel.setLayout(new GridLayout(0, 4, 5, 5));

        JsonArray clanTask = new JsonArray();
        try {
            String clanStr = originalData.get("data_clan_task");
            if (clanStr != null && !clanStr.isEmpty()) {
                clanTask = new JsonParser().parse(clanStr).getAsJsonArray();
            }
        } catch (Exception e) {
            clanTask = new JsonArray();
        }
        while (clanTask.size() < 6)
            clanTask.add(0);

        for (int i = 0; i < 3; i++) {
            int idx = i * 2;
            JLabel lblClanId = new JLabel("Nhiệm vụ " + (i + 1) + " ID:");
            JComboBox<String> cbClanId = new JComboBox<>();
            cbClanId.addItem("-1 - Không có");
            for (Map.Entry<Integer, String> entry : clanTaskTemplateMap.entrySet()) {
                cbClanId.addItem(entry.getKey() + " - " + entry.getValue());
            }
            int currentClanId = clanTask.get(idx).getAsInt();
            for (int j = 0; j < cbClanId.getItemCount(); j++) {
                String item = cbClanId.getItemAt(j);
                if (item.startsWith(currentClanId + " - ")) {
                    cbClanId.setSelectedIndex(j);
                    break;
                }
            }
            inputs.put("clanTaskId_" + i, cbClanId);

            JLabel lblClanCount = new JLabel("Count:");
            JTextField txtClanCount = new JTextField(clanTask.get(idx + 1).getAsString());
            addUndoRedo(txtClanCount);
            inputs.put("clanTaskCount_" + i, txtClanCount);

            clanTaskPanel.add(lblClanId);
            clanTaskPanel.add(cbClanId);
            clanTaskPanel.add(lblClanCount);
            clanTaskPanel.add(txtClanCount);
        }

        // Nhiệm vụ KOL
        JPanel kolTaskPanel = createSectionPanel("Nhiệm vụ KOL");
        kolTaskPanel.setLayout(new GridLayout(0, 2, 5, 5));

        JsonArray kolTask = new JsonArray();
        try {
            String kolStr = originalData.get("data_kol_task");
            if (kolStr != null && !kolStr.isEmpty()) {
                kolTask = new JsonParser().parse(kolStr).getAsJsonArray();
            }
        } catch (Exception e) {
            kolTask = new JsonArray();
        }
        while (kolTask.size() < 2)
            kolTask.add(0);

        JLabel lblKolTaskId = new JLabel("ID Nhiệm vụ KOL:");
        JComboBox<String> cbKolTaskId = new JComboBox<>();
        cbKolTaskId.addItem("-1 - Không có");
        for (Map.Entry<Integer, String> entry : kolTaskTemplateMap.entrySet()) {
            cbKolTaskId.addItem(entry.getKey() + " - " + entry.getValue());
        }
        int currentKolId = kolTask.get(0).getAsInt();
        for (int i = 0; i < cbKolTaskId.getItemCount(); i++) {
            String item = cbKolTaskId.getItemAt(i);
            if (item.startsWith(currentKolId + " - ")) {
                cbKolTaskId.setSelectedIndex(i);
                break;
            }
        }
        inputs.put("kolTaskId", cbKolTaskId);

        JLabel lblKolTaskCount = new JLabel("Count:");
        JTextField txtKolTaskCount = new JTextField(kolTask.get(1).getAsString());
        addUndoRedo(txtKolTaskCount);
        inputs.put("kolTaskCount", txtKolTaskCount);

        kolTaskPanel.add(lblKolTaskId);
        kolTaskPanel.add(cbKolTaskId);
        kolTaskPanel.add(lblKolTaskCount);
        kolTaskPanel.add(txtKolTaskCount);

        panel.add(mainTaskPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(sideTaskPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(clanTaskPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(kolTaskPanel);

        // Đã bỏ badgesTaskPanel ở đây

        return panel;
    }

    private String getJsonVal(JsonArray arr, int index) {
        if (index < arr.size())
            return arr.get(index).getAsString();
        return "0";
    }

    private JPanel createSectionPanel(String title) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createTitledBorder(
                new LineBorder(new Color(200, 200, 200)), title,
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12), Color.DARK_GRAY));
        return p;
    }

    private void addLabelInput(JPanel p, String label, String value, String key, Map<String, Component> map) {
        JPanel row = new JPanel(new BorderLayout(10, 5));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(5, 5, 5, 5));
        JLabel lbl = new JLabel(label);
        lbl.setPreferredSize(new Dimension(100, 25));
        JTextField txt = new JTextField(value);
        txt.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        addUndoRedo(txt);
        row.add(lbl, BorderLayout.WEST);
        row.add(txt, BorderLayout.CENTER);
        p.add(row);
        map.put(key, txt);
    }

    private void addLabelInputGrid(JPanel p, String label, String value, String key, Map<String, Component> map) {
        JPanel row = new JPanel(new BorderLayout(5, 0));
        JLabel lbl = new JLabel(label);
        JTextField txt = new JTextField(value);
        addUndoRedo(txt);
        row.add(lbl, BorderLayout.NORTH);
        row.add(txt, BorderLayout.CENTER);
        p.add(row);
        map.put(key, txt);
    }

    private JPanel createItemPanel(DefaultTableModel model, JDialog parent, boolean isEquipTab) {
        JPanel p = new JPanel(new BorderLayout());
        JTable t = new JTable(model);
        t.setRowHeight(30);
        t.getColumnModel().getColumn(0).setPreferredWidth(50);
        t.getColumnModel().getColumn(1).setPreferredWidth(40);
        t.getColumnModel().getColumn(2).setPreferredWidth(150);
        t.getColumnModel().getColumn(4).setPreferredWidth(300);

        t.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int r = t.getSelectedRow();
                    if (r != -1)
                        openItemDetailEditor(model, r, parent);
                }
            }
        });

        JPanel tool = new JPanel(new FlowLayout(FlowLayout.LEFT));
        tool.setOpaque(false);

        JButton btnAdd = createStyledButton("Thêm Item", COLOR_PRIMARY, Color.WHITE);
        JButton btnDel = createStyledButton("Xóa Item", Color.RED, Color.WHITE);

        btnAdd.addActionListener(e -> {
            if (isEquipTab) {
                JOptionPane.showMessageDialog(
                        parent,
                        "Không thể thêm item tại đây!\nVui lòng thêm item bên Hành trang hoặc Rương.",
                        "Thông báo",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            openItemAddDialog(model, parent);
        });

        btnDel.addActionListener(e -> {
            if (t.getSelectedRow() != -1)
                model.removeRow(t.getSelectedRow());
        });

        tool.add(btnAdd);
        tool.add(btnDel);

        p.add(tool, BorderLayout.NORTH);
        p.add(new JScrollPane(t), BorderLayout.CENTER);
        return p;
    }

    private void openItemDetailEditor(DefaultTableModel model, int row, JDialog parent) {
        JDialog d = new JDialog(parent, "Chỉnh sửa Vật phẩm", true);
        d.setSize(600, 500);
        d.setLocationRelativeTo(parent);
        d.setLayout(new BorderLayout());

        JPanel pTop = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 5, 5, 5);
        g.fill = GridBagConstraints.HORIZONTAL;

        int currId = Integer.parseInt(model.getValueAt(row, 0).toString());
        String currName = model.getValueAt(row, 2).toString();
        int currQty = Integer.parseInt(model.getValueAt(row, 3).toString());
        String currOpt = model.getValueAt(row, 5).toString();

        JLabel lblIcon = new JLabel(getItemIcon(currId));
        JTextField txtId = new JTextField(String.valueOf(currId), 10);
        addUndoRedo(txtId);
        JLabel lblName = new JLabel(currName);
        lblName.setForeground(Color.BLUE);
        JTextField txtQty = new JTextField(String.valueOf(currQty), 10);
        addUndoRedo(txtQty);
        JButton btnFind = new JButton("🔍");

        g.gridx = 0;
        g.gridy = 0;
        pTop.add(new JLabel("ID Item:"), g);
        g.gridx = 1;
        pTop.add(txtId, g);
        g.gridx = 2;
        pTop.add(btnFind, g);

        g.gridx = 0;
        g.gridy = 1;
        pTop.add(new JLabel("Info:"), g);
        JPanel pInfo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pInfo.add(lblIcon);
        pInfo.add(lblName);
        g.gridx = 1;
        g.gridwidth = 2;
        pTop.add(pInfo, g);
        g.gridwidth = 1;

        g.gridx = 0;
        g.gridy = 2;
        pTop.add(new JLabel("Số lượng:"), g);
        g.gridx = 1;
        pTop.add(txtQty, g);

        txtId.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                u();
            }

            public void removeUpdate(DocumentEvent e) {
                u();
            }

            public void changedUpdate(DocumentEvent e) {
                u();
            }

            void u() {
                try {
                    int id = Integer.parseInt(txtId.getText());
                    lblName.setText(getItemName(id));
                    lblIcon.setIcon(getItemIcon(id));
                } catch (Exception ex) {
                }
            }
        });

        btnFind.addActionListener(e -> {
            JDialog sd = new JDialog(d, "Tìm Item", true);
            sd.setSize(900, 600);
            sd.setLocationRelativeTo(d);
            sd.setLayout(new BorderLayout());
            JPanel pFilter = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JTextField st = new JTextField(15);
            JComboBox<String> cbType = new JComboBox<>(new String[] { "- Tất cả -", "0 - Áo", "1 - Quần", "2 - Găng",
                    "3 - Giày", "4 - Rada", "5 - Cải trang", "12 - Ngọc rồng", "27 - Vật phẩm" });
            JComboBox<String> cbGender = new JComboBox<>(
                    new String[] { "- Tất cả -", "0 - Trái đất", "1 - Namếc", "2 - Xayda" });
            pFilter.add(new JLabel("Tên/ID:"));
            pFilter.add(st);
            pFilter.add(new JLabel("Loại:"));
            pFilter.add(cbType);
            pFilter.add(new JLabel("Hệ:"));
            pFilter.add(cbGender);

            DefaultTableModel sm = new DefaultTableModel(new String[] { "ID", "Icon", "Name", "Type", "Gender" }, 0) {
                public Class<?> getColumnClass(int c) {
                    return c == 1 ? ImageIcon.class : Object.class;
                }

                @Override
                public boolean isCellEditable(int r, int c) {
                    return false;
                }
            };
            for (ItemData i : listAllItems)
                sm.addRow(new Object[] { i.id, getItemIcon(i.id), i.name, i.type, i.gender });
            JTable stab = new JTable(sm);
            stab.setRowHeight(30);
            stab.getColumnModel().getColumn(0).setPreferredWidth(50);
            stab.getColumnModel().getColumn(1).setPreferredWidth(40);
            stab.getColumnModel().getColumn(3).setMinWidth(0);
            stab.getColumnModel().getColumn(3).setMaxWidth(0);
            stab.getColumnModel().getColumn(4).setMinWidth(0);
            stab.getColumnModel().getColumn(4).setMaxWidth(0);

            TableRowSorter<DefaultTableModel> ss = new TableRowSorter<>(sm);
            stab.setRowSorter(ss);

            Runnable doFilter = () -> {
                String text = st.getText().trim();
                List<RowFilter<Object, Object>> filters = new ArrayList<>();
                // TÌM KIẾM BẰNG ID HOẶC TÊN
                if (!text.isEmpty()) {
                    try {
                        int id = Integer.parseInt(text);
                        List<RowFilter<Object, Object>> orFilters = new ArrayList<>();
                        orFilters.add(RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL, id, 0)); // ID
                        orFilters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(text), 2)); // Tên
                        filters.add(RowFilter.orFilter(orFilters));
                    } catch (NumberFormatException ex) {
                        filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(text), 2));
                    }
                }
                if (cbType.getSelectedIndex() > 0)
                    try {
                        filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL,
                                Integer.parseInt(cbType.getSelectedItem().toString().split(" - ")[0]), 3));
                    } catch (Exception ex) {
                    }
                if (cbGender.getSelectedIndex() > 0)
                    try {
                        filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL,
                                Integer.parseInt(cbGender.getSelectedItem().toString().split(" - ")[0]), 4));
                    } catch (Exception ex) {
                    }
                if (filters.isEmpty())
                    ss.setRowFilter(null);
                else
                    ss.setRowFilter(RowFilter.andFilter(filters));
            };
            st.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) {
                    doFilter.run();
                }

                public void removeUpdate(DocumentEvent e) {
                    doFilter.run();
                }

                public void changedUpdate(DocumentEvent e) {
                    doFilter.run();
                }
            });
            cbType.addActionListener(e1 -> doFilter.run());
            cbGender.addActionListener(e1 -> doFilter.run());

            stab.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        int viewRow = stab.getSelectedRow();
                        if (viewRow != -1) {
                            int modelRow = stab.convertRowIndexToModel(viewRow);
                            int mid = (int) sm.getValueAt(modelRow, 0);
                            txtId.setText(String.valueOf(mid));
                            sd.dispose();
                        }
                    }
                }
            });
            sd.add(pFilter, BorderLayout.NORTH);
            sd.add(new JScrollPane(stab), BorderLayout.CENTER);
            sd.setVisible(true);
        });

        String[] optCols = { "ID Option", "Param", "Mô tả" };
        DefaultTableModel optModel = new DefaultTableModel(optCols, 0);
        try {
            JsonArray arr = new JsonParser().parse(currOpt).getAsJsonArray();
            for (JsonElement el : arr) {
                JsonArray o = el.getAsJsonArray();
                int oid = o.get(0).getAsInt();
                int op = o.get(1).getAsInt();
                optModel.addRow(new Object[] { oid, op, formatOption(oid, op) });
            }
        } catch (Exception ex) {
        }

        JTable optTable = new JTable(optModel);
        optTable.setRowHeight(25);
        optModel.addTableModelListener(e -> {
            int r = e.getFirstRow();
            int c = e.getColumn();
            if (r >= 0 && r < optModel.getRowCount() && (c == 0 || c == 1)) {
                SwingUtilities.invokeLater(() -> {
                    try {
                        int oid = Integer.parseInt(optModel.getValueAt(r, 0).toString());
                        int op = Integer.parseInt(optModel.getValueAt(r, 1).toString());
                        optModel.setValueAt(formatOption(oid, op), r, 2);
                    } catch (Exception ex) {
                    }
                });
            }
        });

        JPanel pOptTool = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnAddOpt = new JButton("Thêm Opt");
        JButton btnFindOpt = new JButton("Tìm Opt");
        JButton btnDelOpt = new JButton("Xóa Opt");

        btnAddOpt.addActionListener(e -> optModel.addRow(new Object[] { 0, 0, getOptionName(0) }));

        btnFindOpt.addActionListener(e -> {
            JDialog fod = new JDialog(d, "Tìm Option", true);
            fod.setSize(400, 500);
            fod.setLocationRelativeTo(d);
            fod.setLayout(new BorderLayout());
            JTextField tf = new JTextField();

            DefaultTableModel om = new DefaultTableModel(new String[] { "ID", "Name" }, 0) {
                @Override
                public boolean isCellEditable(int r, int c) {
                    return false;
                }
            };
            optionTemplateMap.forEach((k, v) -> om.addRow(new Object[] { k, v }));
            JTable ot = new JTable(om);
            TableRowSorter<DefaultTableModel> os = new TableRowSorter<>(om);
            ot.setRowSorter(os);
            tf.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) {
                    f();
                }

                public void removeUpdate(DocumentEvent e) {
                    f();
                }

                public void changedUpdate(DocumentEvent e) {
                    f();
                }

                void f() {
                    String tx = tf.getText();
                    if (tx.isEmpty())
                        os.setRowFilter(null);
                    else
                        os.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(tx)));
                }
            });
            ot.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        int viewRow = ot.getSelectedRow();
                        if (viewRow != -1) {
                            int modelRow = ot.convertRowIndexToModel(viewRow);
                            int oid = (int) om.getValueAt(modelRow, 0);
                            optModel.addRow(new Object[] { oid, 0, getOptionName(oid).replace("#", "0") });
                            fod.dispose();
                        }
                    }
                }
            });
            fod.add(tf, BorderLayout.NORTH);
            fod.add(new JScrollPane(ot), BorderLayout.CENTER);
            fod.setVisible(true);
        });

        btnDelOpt.addActionListener(e -> {
            if (optTable.getSelectedRow() != -1)
                optModel.removeRow(optTable.getSelectedRow());
        });
        pOptTool.add(btnAddOpt);
        pOptTool.add(btnFindOpt);
        pOptTool.add(btnDelOpt);

        JPanel pCenter = new JPanel(new BorderLayout());
        pCenter.setBorder(new TitledBorder("Options"));
        pCenter.add(pOptTool, BorderLayout.NORTH);
        pCenter.add(new JScrollPane(optTable), BorderLayout.CENTER);

        JButton btnSave = createStyledButton("Lưu thay đổi", COLOR_SUCCESS, Color.WHITE);
        btnSave.addActionListener(e -> {
            JsonArray newArr = new JsonArray();
            for (int i = 0; i < optModel.getRowCount(); i++) {
                JsonArray o = new JsonArray();
                o.add(Integer.parseInt(optModel.getValueAt(i, 0).toString()));
                o.add(Integer.parseInt(optModel.getValueAt(i, 1).toString()));
                newArr.add(o);
            }
            int newId = Integer.parseInt(txtId.getText());
            model.setValueAt(newId, row, 0);
            model.setValueAt(getItemIcon(newId), row, 1);
            model.setValueAt(lblName.getText(), row, 2);
            model.setValueAt(Integer.parseInt(txtQty.getText()), row, 3);
            model.setValueAt(parseOptionReadable(newArr.toString()), row, 4);
            model.setValueAt(newArr.toString(), row, 5);
            d.dispose();
        });

        d.add(pTop, BorderLayout.NORTH);
        d.add(pCenter, BorderLayout.CENTER);
        d.add(btnSave, BorderLayout.SOUTH);
        d.setVisible(true);
    }

    private DefaultTableModel createItemModel() {
        return new DefaultTableModel(
                new String[] { "ID", "Icon", "Tên Item", "SL", "Options (Readable)", "Raw Options" }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 1)
                    return ImageIcon.class;
                return Object.class;
            }
        };
    }

    private void loadItemsToModel(String jsonArrayStr, DefaultTableModel model) {
        try {
            JsonArray arr = new JsonParser().parse(jsonArrayStr).getAsJsonArray();
            for (int idx = 0; idx < arr.size(); idx++) {
                JsonElement e = arr.get(idx);
                String innerStr = e.getAsString();
                JsonArray itemData = new JsonParser().parse(innerStr).getAsJsonArray();
                int id = itemData.get(0).getAsInt();
                if (id == -1) {
                    // GIỮ NGUYÊN VỊ TRÍ SLOT — không skip để tránh xô lệch index
                    model.addRow(new Object[] { -1, null, "(Trống - Slot " + idx + ")", 0, "", "[]" });
                    continue;
                }
                int qty = itemData.get(1).getAsInt();
                String rawOpt = (itemData.size() > 2) ? itemData.get(2).getAsString() : "[]";
                model.addRow(new Object[] { id, getItemIcon(id), getItemName(id), qty, parseOptionReadable(rawOpt),
                        rawOpt });
            }
        } catch (Exception e) {
        }
    }

    private String parseOptionReadable(String jsonOpt) {
        try {
            StringBuilder sb = new StringBuilder();
            JsonArray arr = new JsonParser().parse(jsonOpt).getAsJsonArray();
            for (JsonElement e : arr) {
                JsonArray opt = e.getAsJsonArray();
                int id = opt.get(0).getAsInt();
                int param = opt.get(1).getAsInt();
                sb.append(formatOption(id, param)).append(", ");
            }
            if (sb.length() > 2)
                return sb.substring(0, sb.length() - 2);
        } catch (Exception e) {
            return jsonOpt;
        }
        return "";
    }

    private void openItemAddDialog(DefaultTableModel model, JDialog parent) {
        JDialog d = new JDialog(parent, "Thêm Vật Phẩm", true);
        d.setSize(900, 600);
        d.setLayout(new BorderLayout());
        d.setLocationRelativeTo(parent);

        JPanel pFilter = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pFilter.setBorder(BorderFactory.createTitledBorder("Bộ Lọc"));
        JTextField txtSearch = new JTextField(15);
        String[] types = { "- Tất cả Loại -", "0 - Áo", "1 - Quần", "2 - Găng", "3 - Giày", "4 - Rada",
                "5 - Cải trang/Tóc", "6 - Đậu thần", "12 - Ngọc rồng", "27 - Vật phẩm", "29 - Capsule/Bánh",
                "32 - Giáp tập" };
        JComboBox<String> cbType = new JComboBox<>(types);
        String[] genders = { "- Tất cả Hệ -", "0 - Trái Đất", "1 - Namếc", "2 - Xayda", "3 - Chung/Tất cả" };
        JComboBox<String> cbGender = new JComboBox<>(genders);
        pFilter.add(new JLabel("Tên/ID:"));
        pFilter.add(txtSearch);
        pFilter.add(new JLabel(" | Loại:"));
        pFilter.add(cbType);
        pFilter.add(new JLabel(" | Hệ:"));
        pFilter.add(cbGender);

        DefaultTableModel searchModel = new DefaultTableModel(
                new String[] { "ID", "Icon", "Tên Item", "Type", "Gender" }, 0) {
            @Override
            public Class<?> getColumnClass(int c) {
                return c == 1 ? ImageIcon.class : Object.class;
            }

            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        for (ItemData item : listAllItems)
            searchModel.addRow(new Object[] { item.id, getItemIcon(item.id), item.name, item.type, item.gender });

        JTable t = new JTable(searchModel);
        t.setRowHeight(30);
        t.getColumnModel().getColumn(0).setPreferredWidth(50);
        t.getColumnModel().getColumn(1).setPreferredWidth(40);
        t.getColumnModel().getColumn(2).setPreferredWidth(350);
        t.getColumnModel().getColumn(3).setMinWidth(0);
        t.getColumnModel().getColumn(3).setMaxWidth(0);
        t.getColumnModel().getColumn(4).setMinWidth(0);
        t.getColumnModel().getColumn(4).setMaxWidth(0);

        TableRowSorter<DefaultTableModel> s = new TableRowSorter<>(searchModel);
        t.setRowSorter(s);

        // LOGIC LỌC ĐƯỢC CHỈNH SỬA Ở ĐÂY
        Runnable doFilter = () -> {
            String text = txtSearch.getText().trim();
            int typeIdx = cbType.getSelectedIndex();
            int genderIdx = cbGender.getSelectedIndex();
            List<RowFilter<Object, Object>> filters = new ArrayList<>();

            // XỬ LÝ TÌM BẰNG ID HOẶC TÊN
            if (!text.isEmpty()) {
                try {
                    int id = Integer.parseInt(text);
                    // Nếu parse thành công, tạo bộ lọc OR (ID hoặc Tên)
                    List<RowFilter<Object, Object>> orFilters = new ArrayList<>();
                    orFilters.add(RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL, id, 0)); // ID cột 0
                    orFilters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(text), 2)); // Tên cột 2
                    filters.add(RowFilter.orFilter(orFilters));
                } catch (NumberFormatException e) {
                    // Nếu không phải số, chỉ tìm theo tên
                    filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(text), 2));
                }
            }

            if (typeIdx > 0) {
                try {
                    int val = Integer.parseInt(cbType.getSelectedItem().toString().split(" - ")[0]);
                    filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL, val, 3));
                } catch (Exception e) {
                }
            }
            if (genderIdx > 0) {
                try {
                    int val = Integer.parseInt(cbGender.getSelectedItem().toString().split(" - ")[0]);
                    filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL, val, 4));
                } catch (Exception e) {
                }
            }
            if (filters.isEmpty())
                s.setRowFilter(null);
            else
                s.setRowFilter(RowFilter.andFilter(filters));
        };

        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                doFilter.run();
            }

            public void removeUpdate(DocumentEvent e) {
                doFilter.run();
            }

            public void changedUpdate(DocumentEvent e) {
                doFilter.run();
            }
        });
        cbType.addActionListener(e -> doFilter.run());
        cbGender.addActionListener(e -> doFilter.run());

        t.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int r = t.getSelectedRow();
                    if (r != -1) {
                        int modelRow = t.convertRowIndexToModel(r);
                        int id = (int) searchModel.getValueAt(modelRow, 0);
                        ImageIcon icon = (ImageIcon) searchModel.getValueAt(modelRow, 1);
                        String name = (String) searchModel.getValueAt(modelRow, 2);
                        model.addRow(new Object[] { id, icon, name, 1, "", "[]" });
                        d.dispose();
                    }
                }
            }
        });

        d.add(pFilter, BorderLayout.NORTH);
        d.add(new JScrollPane(t), BorderLayout.CENTER);
        d.setVisible(true);
    }

    private void savePlayerDB(int pid, int accountId, Map<String, Component> inputs,
            DefaultTableModel mBody, DefaultTableModel mBag, DefaultTableModel mBox,
            DefaultTableModel mBadges,
            Map<String, String> originalData, JDialog d) {
        new Thread(() -> {
            try {
                // 1. CHUẨN BỊ DỮ LIỆU INVENTORY & POINT
                JsonArray inv = new JsonArray();
                inv.add(getLongVal(inputs, "gold"));
                inv.add(getLongVal(inputs, "gem"));
                inv.add(getLongVal(inputs, "ruby"));
                // Giữ lại các chỉ số sau nếu có (coupon, event...)
                JsonArray oldInv = new JsonParser().parse(originalData.get("data_inventory")).getAsJsonArray();
                for (int i = 3; i < oldInv.size(); i++) {
                    inv.add(oldInv.get(i));
                }

                JsonArray point = new JsonParser().parse(originalData.get("data_point")).getAsJsonArray();
                setVal(point, 1, getText(inputs, "power"));
                setVal(point, 2, getText(inputs, "tiemnang"));
                setVal(point, 5, getText(inputs, "hpg"));
                setVal(point, 6, getText(inputs, "mpg"));
                setVal(point, 7, getText(inputs, "dameg"));
                setVal(point, 8, getText(inputs, "defg"));
                setVal(point, 9, getText(inputs, "critg"));

                // 2. XỬ LÝ ITEM
                String jsonBody, jsonBag, jsonBox;
                try {
                    jsonBody = mergeItemToDB(mBody, originalData.get("items_body"), "Body/Đồ mặc");
                    jsonBag = mergeItemToDB(mBag, originalData.get("items_bag"), "Hành trang");
                    jsonBox = mergeItemToDB(mBox, originalData.get("items_box"), "Rương đồ");
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(d, ex.getMessage(), "Lỗi đây rồi",
                            JOptionPane.ERROR_MESSAGE));
                    return;
                }

                // 3. XỬ LÝ PET
                String petJson = originalData.get("pet");
                if (petJson != null && !petJson.equals("[]") && inputs.containsKey("pet_name")) {
                    JsonArray petArr = new JsonParser().parse(petJson).getAsJsonArray();
                    if (petArr.size() > 1) {
                        JsonArray infoArr = new JsonParser().parse(petArr.get(0).getAsString()).getAsJsonArray();
                        String typeStr = ((JComboBox) inputs.get("pet_type")).getSelectedItem().toString();
                        if (typeStr.contains(" - "))
                            typeStr = typeStr.split(" - ")[0];
                        setVal(infoArr, 0, typeStr);
                        setVal(infoArr, 1, String.valueOf(((JComboBox) inputs.get("pet_gender")).getSelectedIndex()));
                        setVal(infoArr, 2, getText(inputs, "pet_name"));
                        setVal(infoArr, 5, String.valueOf(((JComboBox) inputs.get("pet_status")).getSelectedIndex()));
                        petArr.set(0, new JsonPrimitive(infoArr.toString()));

                        JsonArray pointArr = new JsonParser().parse(petArr.get(1).getAsString()).getAsJsonArray();
                        setVal(pointArr, 1, getText(inputs, "pet_power"));
                        setVal(pointArr, 2, getText(inputs, "pet_tiemnang"));
                        setVal(pointArr, 5, getText(inputs, "pet_hpg"));
                        setVal(pointArr, 6, getText(inputs, "pet_mpg"));
                        setVal(pointArr, 7, getText(inputs, "pet_dameg"));
                        setVal(pointArr, 8, getText(inputs, "pet_defg"));
                        setVal(pointArr, 9, getText(inputs, "pet_critg"));
                        petArr.set(1, new JsonPrimitive(pointArr.toString()));
                        petJson = petArr.toString();
                    }
                }

                // 4. XỬ LÝ NHIỆM VỤ
                String dataTaskJson = createTaskJson(inputs, "mainTaskId", "mainTaskIndex", "mainTaskCount",
                        "mainTaskLastTime", 4);
                String sideTaskJson = createMultiTaskJson(inputs, "sideTaskId_", "sideTaskCount_", 3);
                String clanTaskJson = createMultiTaskJson(inputs, "clanTaskId_", "clanTaskCount_", 3);
                String kolTaskJson = createTaskJson(inputs, "kolTaskId", null, "kolTaskCount", null, 2);

                // 5. XỬ LÝ BADGES (NEW)
                JsonArray badgesArr = new JsonArray();
                for (int i = 0; i < mBadges.getRowCount(); i++) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("idBadGes", Integer.parseInt(mBadges.getValueAt(i, 0).toString()));
                    obj.addProperty("timeofUseBadges", Long.parseLong(mBadges.getValueAt(i, 4).toString()));
                    obj.addProperty("isUse", Boolean.parseBoolean(mBadges.getValueAt(i, 6).toString()));
                    badgesArr.add(obj);
                }
                String dataBadgesJson = badgesArr.toString();

                // 6. UPDATE SQL
                // Thay dataTaskBadges bằng dataBadges
                String sqlPlayer = "UPDATE player SET name=?, power=?, head=?, data_inventory=?, data_point=?, items_body=?, items_bag=?, items_box=?, pet=?, data_task=?, data_side_task=?, data_clan_task=?, data_kol_task=?, dataBadges=? WHERE id=?";
                try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sqlPlayer)) {
                    ps.setString(1, getText(inputs, "name"));
                    ps.setLong(2, Long.parseLong(getText(inputs, "power")));
                    ps.setInt(3, Integer.parseInt(getText(inputs, "head")));
                    ps.setString(4, inv.toString());
                    ps.setString(5, point.toString());
                    ps.setString(6, jsonBody);
                    ps.setString(7, jsonBag);
                    ps.setString(8, jsonBox);
                    ps.setString(9, petJson);
                    ps.setString(10, dataTaskJson);
                    ps.setString(11, sideTaskJson);
                    ps.setString(12, clanTaskJson);
                    ps.setString(13, kolTaskJson);
                    ps.setString(14, dataBadgesJson);
                    ps.setInt(15, pid);
                    ps.executeUpdate();
                }

                String sqlAccount = "UPDATE account SET cash=?, vnd=?, danap=?, active=?, ban=? WHERE id=?";
                try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sqlAccount)) {
                    long cashVal = getLongVal(inputs, "cash");
                    ps.setLong(1, cashVal);
                    ps.setLong(2, cashVal); // vnd sync with cash
                    ps.setLong(3, getLongVal(inputs, "danap"));
                    ps.setInt(4, ((JComboBox) inputs.get("active_box")).getSelectedIndex());
                    ps.setInt(5, ((JComboBox) inputs.get("ban_box")).getSelectedIndex());
                    ps.setInt(6, accountId);
                    ps.executeUpdate();
                }

                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(d, "Lưu thành công!");
                    d.dispose();
                    loadPlayersFromDB("");
                });
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(d, "Lỗi lưu: " + e.getMessage()));
            }
        }).start();
    }

    private String createTaskJson(Map<String, Component> inputs, String idKey, String indexKey, String countKey,
            String timeKey, int size) {
        JsonArray arr = new JsonArray();

        // Lấy ID từ ComboBox
        int id = -1;
        Component idComp = inputs.get(idKey);
        if (idComp instanceof JComboBox) {
            String selected = ((JComboBox) idComp).getSelectedItem().toString();
            if (selected.contains(" - ")) {
                try {
                    id = Integer.parseInt(selected.split(" - ")[0]);
                } catch (NumberFormatException e) {
                    id = -1;
                }
            }
        }
        arr.add(id);

        // Thêm index nếu có
        if (indexKey != null && inputs.containsKey(indexKey)) {
            try {
                int index = Integer.parseInt(((JTextField) inputs.get(indexKey)).getText());
                arr.add(index);
            } catch (Exception e) {
                arr.add(0);
            }
        } else if (size > 1) {
            arr.add(0);
        }

        // Thêm count
        if (countKey != null && inputs.containsKey(countKey)) {
            try {
                int count = Integer.parseInt(((JTextField) inputs.get(countKey)).getText());
                arr.add(count);
            } catch (Exception e) {
                arr.add(0);
            }
        } else if (size > 2) {
            arr.add(0);
        }

        // Thêm last time nếu có
        if (timeKey != null && inputs.containsKey(timeKey)) {
            try {
                long lastTime = Long.parseLong(((JTextField) inputs.get(timeKey)).getText());
                arr.add(lastTime);
            } catch (Exception e) {
                arr.add(System.currentTimeMillis());
            }
        } else if (size > 3) {
            arr.add(System.currentTimeMillis());
        }

        // Đảm bảo đủ size
        while (arr.size() < size) {
            arr.add(0);
        }

        return arr.toString();
    }

    private String createMultiTaskJson(Map<String, Component> inputs, String idPrefix, String countPrefix, int count) {
        JsonArray arr = new JsonArray();

        for (int i = 0; i < count; i++) {
            String idKey = idPrefix + i;
            String countKey = countPrefix + i;

            // ID
            int id = -1;
            Component idComp = inputs.get(idKey);
            if (idComp instanceof JComboBox) {
                String selected = ((JComboBox) idComp).getSelectedItem().toString();
                if (selected.contains(" - ")) {
                    try {
                        id = Integer.parseInt(selected.split(" - ")[0]);
                    } catch (NumberFormatException e) {
                        id = -1;
                    }
                }
            }
            arr.add(id);

            // Count
            int taskCount = 0;
            Component countComp = inputs.get(countKey);
            if (countComp instanceof JTextField) {
                try {
                    taskCount = Integer.parseInt(((JTextField) countComp).getText());
                } catch (Exception e) {
                    taskCount = 0;
                }
            }
            arr.add(taskCount);
        }

        return arr.toString();
    }

    private String mergeItemToDB(DefaultTableModel model, String originalJson, String typeName) throws Exception {
        JsonArray dbArr;
        try {
            if (originalJson == null || originalJson.isEmpty())
                dbArr = new JsonArray();
            else
                dbArr = new JsonParser().parse(originalJson).getAsJsonArray();
        } catch (Exception e) {
            dbArr = new JsonArray();
        }

        int maxSlots = dbArr.size();
        int itemsInTable = model.getRowCount();

        if (itemsInTable > maxSlots) {
            throw new Exception("Lỗi: " + typeName + " đã bị ĐẦY!\nSố lượng hiện tại: " + itemsInTable
                    + "\nSức chứa tối đa: " + maxSlots + "\nVui lòng xóa bớt item trước khi lưu.");
        }

        for (int i = 0; i < maxSlots; i++) {
            if (i < itemsInTable) {
                try {
                    int id = Integer.parseInt(model.getValueAt(i, 0).toString());
                    int qty = Integer.parseInt(model.getValueAt(i, 3).toString());
                    String rawOpt = model.getValueAt(i, 5).toString();

                    JsonArray itemNode = new JsonArray();
                    itemNode.add(id);
                    itemNode.add(qty);
                    itemNode.add(rawOpt);
                    itemNode.add(System.currentTimeMillis());

                    dbArr.set(i, new JsonPrimitive(itemNode.toString()));
                } catch (Exception e) {
                    dbArr.set(i, new JsonPrimitive(createEmptyItem()));
                }
            } else {
                dbArr.set(i, new JsonPrimitive(createEmptyItem()));
            }
        }
        return dbArr.toString();
    }

    private String createEmptyItem() {
        JsonArray emptyNode = new JsonArray();
        emptyNode.add(-1);
        emptyNode.add(0);
        emptyNode.add("[]");
        emptyNode.add(System.currentTimeMillis());
        return emptyNode.toString();
    }

    private String getText(Map<String, Component> inputs, String key) {
        Component c = inputs.get(key);
        if (c instanceof JTextField)
            return ((JTextField) c).getText();
        return "0";
    }

    private long getLongVal(Map<String, Component> inputs, String key) {
        try {
            String txt = ((JTextField) inputs.get(key)).getText();
            return Long.parseLong(txt.replaceAll("[^0-9-]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    private void setVal(JsonArray arr, int index, String val) {
        while (arr.size() <= index) {
            arr.add(new JsonPrimitive(0));
        }
        try {
            String cleanVal = val.replaceAll("[^0-9-]", "");
            long v = Long.parseLong(cleanVal);
            arr.set(index, new JsonPrimitive(v));
        } catch (Exception e) {
            arr.set(index, new JsonPrimitive(val));
        }
    }

    private static JButton createStyledButton(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFocusPainted(false);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }
}