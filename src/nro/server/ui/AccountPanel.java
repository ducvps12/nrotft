package nro.server.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import javax.imageio.ImageIO;
import jdbc.DBConnecter;

public class AccountPanel extends JPanel {

    // ========================================================================
    // 1. CẤU HÌNH GIAO DIỆN
    // ========================================================================
    private static final String ICON_FOLDER = "data/icon/";
    
    // Fonts
    private static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font FONT_DATA = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 12);
    private static final Font FONT_NUM = new Font("Consolas", Font.BOLD, 14); // Font số

    // Colors
    private static final Color COL_PRIMARY = new Color(0, 120, 215);
    private static final Color COL_BG = Color.WHITE;
    private static final Color COL_SECTION_BG = new Color(250, 252, 255);
    private static final Color COL_BORDER = new Color(220, 220, 220);
    private static final Color COL_TEXT_GRAY = new Color(100, 100, 100);

    // Format Date
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd/MM/yyyy");

    // Data Cache
    private final Map<Integer, Integer> partHeadIconMap = new HashMap<>();
    private final Map<Integer, ImageIcon> headCache = new HashMap<>();
    
    // Components
    private JTable table;
    private DefaultTableModel model;
    private JTextField txtSearch;
    private JPanel bulkActionBar;
    private JLabel lblSelected;

    public AccountPanel() {
        setLayout(new BorderLayout(15, 15));
        setBackground(COL_BG);
        setBorder(new EmptyBorder(15, 15, 15, 15));

        initUI();
        loadHeadPartCache(); // Cache icons first
        ensureLoanTinColumn(); // Tự động thêm cột loantin nếu chưa có
    }

    // ========================================================================
    // 2. GIAO DIỆN CHÍNH (MAIN LIST)
    // ========================================================================
    private void initUI() {
        // --- Header ---
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(COL_BG);

        JLabel lblTitle = new JLabel("QUẢN LÝ TÀI KHOẢN");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTitle.setForeground(new Color(60, 60, 60));
        lblTitle.setBorder(new EmptyBorder(0, 0, 15, 0));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        searchPanel.setBackground(COL_BG);

        txtSearch = new JTextField(25);
        txtSearch.putClientProperty("JTextField.placeholderText", "Tìm ID, Username, Tên nhân vật...");
        txtSearch.setPreferredSize(new Dimension(300, 40));
        txtSearch.setFont(FONT_DATA);
        txtSearch.putClientProperty("JTextField.showClearButton", true);

        JButton btnSearch = createButton("Tìm kiếm", COL_PRIMARY);
        JButton btnReload = createButton("Làm mới", new Color(40, 167, 69));
        JButton btnPurge = createButton("🗑 Xóa TK chưa tạo NV", new Color(220, 53, 69));

        btnSearch.addActionListener(e -> searchData(txtSearch.getText()));
        btnReload.addActionListener(e -> { txtSearch.setText(""); loadData(); });
        btnPurge.addActionListener(e -> purgeEmptyAccounts());

        searchPanel.add(txtSearch);
        searchPanel.add(btnSearch);
        searchPanel.add(btnReload);
        searchPanel.add(btnPurge);

        topPanel.add(lblTitle, BorderLayout.NORTH);
        topPanel.add(searchPanel, BorderLayout.CENTER);

        // --- Bulk Action Bar ---
        bulkActionBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        bulkActionBar.setBackground(new Color(255, 248, 230));
        bulkActionBar.setBorder(new MatteBorder(1, 0, 1, 0, new Color(255, 200, 100)));
        bulkActionBar.setVisible(false);

        lblSelected = new JLabel("Đã chọn: 0");
        lblSelected.setFont(FONT_BOLD);
        lblSelected.setForeground(COL_PRIMARY);

        JButton btnBanAll = createButton("\uD83D\uDEAB Ban tất cả", new Color(220, 53, 69));
        JButton btnUnbanAll = createButton("\u2705 Mở khóa tất cả", new Color(40, 167, 69));
        JButton btnDeleteAll = createButton("\uD83D\uDDD1 Xóa tất cả", new Color(108, 117, 125));
        JButton btnDeselectAll = createButton("Bỏ chọn", new Color(150, 150, 150));

        btnBanAll.addActionListener(e -> bulkBan(true));
        btnUnbanAll.addActionListener(e -> bulkBan(false));
        btnDeleteAll.addActionListener(e -> bulkDelete());
        btnDeselectAll.addActionListener(e -> deselectAll());

        bulkActionBar.add(lblSelected);
        bulkActionBar.add(Box.createHorizontalStrut(10));
        bulkActionBar.add(btnBanAll);
        bulkActionBar.add(btnUnbanAll);
        bulkActionBar.add(btnDeleteAll);
        bulkActionBar.add(Box.createHorizontalStrut(20));
        bulkActionBar.add(btnDeselectAll);

        JPanel northContainer = new JPanel(new BorderLayout());
        northContainer.setBackground(COL_BG);
        northContainer.add(topPanel, BorderLayout.NORTH);
        northContainer.add(bulkActionBar, BorderLayout.SOUTH);
        add(northContainer, BorderLayout.NORTH);

        // --- Table ---
        // Col 0 = checkbox, col 1 = head, col 2 = ID, ...
        String[] columns = {"✓", "Head", "ID", "Tài khoản", "Tên NV", "Mật khẩu", "Trạng thái", "VIP", "VND", "Đã Nạp", "Ngày tạo"};
        
        model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int col) { return col == 0; }
            @Override public Class<?> getColumnClass(int col) {
                if (col == 0) return Boolean.class;
                if (col == 1) return ImageIcon.class;
                return Object.class;
            }
        };

        table = new JTable(model);
        setupTableStyle();

        // Double-click to edit (skip checkbox column)
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() != -1) {
                    int col = table.columnAtPoint(e.getPoint());
                    if (col != 0) { // not checkbox
                        int id = Integer.parseInt(table.getValueAt(table.getSelectedRow(), 2).toString());
                        openEditDialog(id);
                    }
                }
            }
        });

        // Listen for checkbox changes to update bulk bar
        model.addTableModelListener(e -> {
            if (e.getColumn() == 0) updateBulkBar();
        });

        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private void setupTableStyle() {
        table.setFont(FONT_DATA);
        table.setRowHeight(55);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setBackground(COL_PRIMARY);
        header.setForeground(Color.WHITE);
        header.setPreferredSize(new Dimension(0, 40));
        ((DefaultTableCellRenderer) header.getDefaultRenderer()).setHorizontalAlignment(JLabel.CENTER);

        // Click header col 0 = Select All
        header.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int col = table.columnAtPoint(e.getPoint());
                if (col == 0) toggleSelectAll();
            }
        });

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                else c.setBackground(new Color(220, 235, 255));

                setHorizontalAlignment(JLabel.CENTER);
                setForeground(Color.BLACK);

                // Column indices shifted +1 due to checkbox column
                if (column == 4) { setFont(FONT_BOLD); setForeground(new Color(0, 102, 204)); }
                else if (column == 6) {
                    setFont(FONT_BOLD);
                    String s = value != null ? value.toString() : "";
                    if (s.contains("BAN")) setForeground(Color.RED);
                    else if (s.contains("Active")) setForeground(new Color(0, 150, 0));
                    else setForeground(Color.GRAY);
                } else if (column == 8 || column == 9) {
                    setFont(FONT_NUM); setForeground(new Color(153, 0, 153));
                } else {
                    setFont(FONT_DATA);
                }
                
                if (c instanceof JComponent) ((JComponent) c).setBorder(new MatteBorder(0, 0, 1, 0, new Color(230, 230, 230)));
                return c;
            }
        });
        
        // Widths
        TableColumnModel cm = table.getColumnModel();
        cm.getColumn(0).setPreferredWidth(35);  // checkbox
        cm.getColumn(0).setMaxWidth(40);
        cm.getColumn(1).setPreferredWidth(60);  // head
        cm.getColumn(2).setPreferredWidth(50);  // ID
        cm.getColumn(3).setPreferredWidth(120); // username
        cm.getColumn(4).setPreferredWidth(120); // charname
        cm.getColumn(5).setPreferredWidth(80);  // password
    }

    // ========================================================================
    // 3. EDIT DIALOG (FIXED LAYOUT)
    // ========================================================================
    // ================================================================
    // BULK ACTIONS
    // ================================================================
    private List<Integer> getCheckedIds() {
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            Boolean checked = (Boolean) model.getValueAt(i, 0);
            if (checked != null && checked) {
                ids.add(Integer.parseInt(model.getValueAt(i, 2).toString()));
            }
        }
        return ids;
    }

    private void updateBulkBar() {
        List<Integer> ids = getCheckedIds();
        bulkActionBar.setVisible(!ids.isEmpty());
        lblSelected.setText("Đã chọn: " + ids.size());
    }

    private void toggleSelectAll() {
        boolean hasUnchecked = false;
        for (int i = 0; i < model.getRowCount(); i++) {
            Boolean v = (Boolean) model.getValueAt(i, 0);
            if (v == null || !v) { hasUnchecked = true; break; }
        }
        for (int i = 0; i < model.getRowCount(); i++) {
            model.setValueAt(hasUnchecked, i, 0);
        }
        updateBulkBar();
    }

    private void deselectAll() {
        for (int i = 0; i < model.getRowCount(); i++) model.setValueAt(false, i, 0);
        updateBulkBar();
    }

    private void bulkBan(boolean ban) {
        List<Integer> ids = getCheckedIds();
        if (ids.isEmpty()) return;
        String action = ban ? "BAN" : "MỞ KHÓA";
        int confirm = JOptionPane.showConfirmDialog(this,
                action + " " + ids.size() + " tài khoản?\nIDs: " + ids,
                "Xác nhận " + action, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        new Thread(() -> {
            try (Connection conn = DBConnecter.getConnectionServer()) {
                StringBuilder sql = new StringBuilder("UPDATE account SET ban = " + (ban ? 1 : 0) + " WHERE id IN (");
                for (int i = 0; i < ids.size(); i++) {
                    if (i > 0) sql.append(",");
                    sql.append(ids.get(i));
                }
                sql.append(")");
                conn.createStatement().executeUpdate(sql.toString());
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Đã " + action + " " + ids.size() + " tài khoản!");
                    loadData();
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage()));
            }
        }).start();
    }

    private void bulkDelete() {
        List<Integer> ids = getCheckedIds();
        if (ids.isEmpty()) return;
        int confirm = JOptionPane.showConfirmDialog(this,
                "⚠ XÓA VĨNH VIỄN " + ids.size() + " tài khoản?\nIDs: " + ids + "\n\nHành động này KHÔNG THỂ hoàn tác!",
                "Xác nhận XÓA", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        // Double confirm
        int doubleConfirm = JOptionPane.showConfirmDialog(this,
                "BẠN CHẮC CHẮN MUỐN XÓA " + ids.size() + " TÀI KHOẢN?\nNhập YES để xác nhận.",
                "Xác nhận lần 2", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
        if (doubleConfirm != JOptionPane.YES_OPTION) return;

        new Thread(() -> {
            try (Connection conn = DBConnecter.getConnectionServer()) {
                // Delete player first (foreign key)
                StringBuilder sqlPlayer = new StringBuilder("DELETE FROM player WHERE account_id IN (");
                StringBuilder sqlAccount = new StringBuilder("DELETE FROM account WHERE id IN (");
                for (int i = 0; i < ids.size(); i++) {
                    if (i > 0) { sqlPlayer.append(","); sqlAccount.append(","); }
                    sqlPlayer.append(ids.get(i));
                    sqlAccount.append(ids.get(i));
                }
                sqlPlayer.append(")");
                sqlAccount.append(")");
                conn.createStatement().executeUpdate(sqlPlayer.toString());
                conn.createStatement().executeUpdate(sqlAccount.toString());
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Đã xóa " + ids.size() + " tài khoản!");
                    loadData();
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage()));
            }
        }).start();
    }

    // ========================================================================
    // 3. EDIT DIALOG (FIXED LAYOUT)
    // ========================================================================
    private void openEditDialog(int accountId) {
        JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Chỉnh sửa ID: " + accountId, true);
        d.setSize(950, 680);
        d.setLocationRelativeTo(null);
        d.setLayout(new BorderLayout());
        d.setBackground(COL_BG);

        // --- PREPARE FIELDS ---
        JTextField txtUser = createField(true); // Cho phép sửa username
        JTextField txtPass = createField(true);
        JTextField txtEmail = createField(true);
        
        // Lưu giá trị gốc cho password/email (dùng để toggle)
        final String[] realPass = {""};
        final String[] realEmail = {""};
        final boolean[] passVisible = {false};
        final boolean[] emailVisible = {false};
        
        JCheckBox chkActive = new JCheckBox("Kích hoạt");
        JCheckBox chkBan = new JCheckBox("Khóa (Ban)"); chkBan.setForeground(Color.RED);
        JCheckBox chkAdmin = new JCheckBox("Admin");
        JCheckBox chkLoanTin = new JCheckBox("📢 Loan Tin"); chkLoanTin.setForeground(new Color(156, 39, 176));
        
        JTextField txtRole = createField(true);
        JTextField txtVip = createField(true);
        JTextField txtServer = createField(true);

        JTextField txtVnd = createField(true); txtVnd.setFont(FONT_NUM); txtVnd.setForeground(Color.RED);
        JTextField txtDanap = createField(true); txtDanap.setFont(FONT_NUM); txtDanap.setForeground(Color.BLUE);
        JTextField txtGold = createField(true); txtGold.setFont(FONT_NUM);
        JTextField txtPoint = createField(true); txtPoint.setFont(FONT_NUM);

        // Lưu VND cũ để so sánh khi save
        final long[] oldCash = {0};
        final long[] oldDanap = {0};

        Map<String, JTextField> eventMap = new HashMap<>();
        String[] eventCols = {
            "DiemDanh", "diemboss", "bong_master", "hopquathang9", "hopquathang9vip",
            "hopquatrungthuvip", "longdentreo", "hoptrahoacuc", "hopkeomaquy",
            "capsuvip", "thiepchucvip", "halloween_master", "keo_halloween",
            "diemnoel", "vongquayvang", "phaobong", "lixi", "luotquay", "event_point"
        };
        for (String col : eventCols) eventMap.put(col, createField(true));

        final int[] headInfo = {-1}; // wrapper for head id

        // --- LOAD DATA ---
        loadAccountData(accountId, txtUser, txtPass, txtEmail, chkActive, chkBan, chkAdmin, chkLoanTin,
                        txtRole, txtVip, txtServer, txtVnd, txtDanap, txtGold, txtPoint, eventMap, headInfo);

        // Sau khi load, lưu giá trị gốc và mask
        SwingUtilities.invokeLater(() -> {
            realPass[0] = txtPass.getText();
            realEmail[0] = txtEmail.getText();
            oldCash[0] = safeLong(txtVnd);
            oldDanap[0] = safeLong(txtDanap);
            txtPass.setText("●●●●●●●●");
            txtPass.setEditable(false);
            txtEmail.setText(maskEmail(realEmail[0]));
            txtEmail.setEditable(false);
        });

        // --- BUILD UI ---
        JPanel pMain = new JPanel(new GridBagLayout());
        pMain.setBackground(COL_BG);
        pMain.setBorder(new EmptyBorder(15, 20, 15, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 15, 0); // Spacing bottom
        gbc.gridx = 0; gbc.weightx = 1.0;

        // 1. PROFILE SECTION
        JPanel pProfile = createSectionPanel("THÔNG TIN TÀI KHOẢN");
        pProfile.setLayout(new GridBagLayout()); // Use GridBag for flexible layout inside
        
        GridBagConstraints gp = new GridBagConstraints();
        gp.insets = new Insets(5, 5, 5, 10);
        gp.fill = GridBagConstraints.HORIZONTAL;
        
        // Avatar (Left)
        JLabel lblAvt = new JLabel(getAvatar(headInfo[0], txtUser.getText(), 80));
        gp.gridx = 0; gp.gridy = 0; gp.gridheight = 3; gp.weightx = 0; 
        pProfile.add(lblAvt, gp);

        // Reset gridheight
        gp.gridheight = 1; gp.weightx = 1.0;

        // Row 1: User, Pass (with eye toggle), Email (with eye toggle)
        JPanel pRow1 = new JPanel(new GridLayout(1, 3, 15, 0));
        pRow1.setOpaque(false);
        pRow1.add(createInputGroup("Tài khoản:", txtUser));
        pRow1.add(createInputGroupWithEye("Mật khẩu:", txtPass, passVisible, realPass));
        pRow1.add(createInputGroupWithEye("Email:", txtEmail, emailVisible, realEmail));
        
        gp.gridx = 1; gp.gridy = 0; 
        pProfile.add(pRow1, gp);

        // Row 2: Role, VIP, Server (Fixed width for these small fields)
        JPanel pRow2 = new JPanel(new GridLayout(1, 3, 15, 0));
        pRow2.setOpaque(false);
        pRow2.add(createInputGroup("Role (-1):", txtRole));
        pRow2.add(createInputGroup("VIP:", txtVip));
        pRow2.add(createInputGroup("Server Login:", txtServer));
        
        gp.gridx = 1; gp.gridy = 1;
        pProfile.add(pRow2, gp);

        // Row 3: Checkboxes
        JPanel pRow3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        pRow3.setOpaque(false);
        styleCheck(chkActive); styleCheck(chkBan); styleCheck(chkAdmin); styleCheck(chkLoanTin);
        pRow3.add(chkActive); pRow3.add(chkBan); pRow3.add(chkAdmin); pRow3.add(chkLoanTin);
        
        gp.gridx = 1; gp.gridy = 2;
        pProfile.add(pRow3, gp);

        gbc.gridy = 0; 
        pMain.add(pProfile, gbc);

        // 2. ASSETS SECTION
        JPanel pAsset = new JPanel(new GridLayout(1, 4, 15, 0)); // 4 columns equal width
        pAsset.setOpaque(false);
        pAsset.add(createBorderedGroup("VND", txtVnd));
        pAsset.add(createBorderedGroup("Đã Nạp", txtDanap));
        pAsset.add(createBorderedGroup("Vàng (Gold)", txtGold));
        pAsset.add(createBorderedGroup("Tích điểm", txtPoint));
        
        gbc.gridy = 1;
        pMain.add(pAsset, gbc);

        // 3. EVENTS SECTION
        JPanel pEventContainer = createSectionPanel("VẬT PHẨM & SỰ KIỆN");
        pEventContainer.setLayout(new BorderLayout());
        
        JPanel pEventGrid = new JPanel(new GridLayout(0, 4, 10, 10)); // 4 columns
        pEventGrid.setBackground(Color.WHITE);
        pEventGrid.setBorder(new EmptyBorder(10, 10, 10, 10));
        for(String key : eventCols) {
            pEventGrid.add(createCompactInput(key, eventMap.get(key)));
        }
        
        // Wrap events in scroll pane in case of many items
        JScrollPane scrollEvents = new JScrollPane(pEventGrid);
        scrollEvents.setBorder(null);
        scrollEvents.getVerticalScrollBar().setUnitIncrement(16);
        scrollEvents.setPreferredSize(new Dimension(800, 200)); // Set preferred height
        
        pEventContainer.add(scrollEvents, BorderLayout.CENTER);

        gbc.gridy = 2; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.BOTH;
        pMain.add(pEventContainer, gbc);

        // --- BUTTONS ---
        JPanel pBtn = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pBtn.setBackground(new Color(245, 245, 245));
        pBtn.setBorder(new MatteBorder(1, 0, 0, 0, COL_BORDER));
        
        JButton btnSave = createButton("LƯU THAY ĐỔI", new Color(0, 120, 215));
        btnSave.setPreferredSize(new Dimension(150, 40));
        btnSave.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        JButton btnCancel = new JButton("Đóng");
        btnCancel.setFont(FONT_DATA);
        btnCancel.addActionListener(e->d.dispose());

        btnSave.addActionListener(e -> {
            // Khôi phục giá trị thật trước khi save nếu đang bị mask
            if (!passVisible[0]) txtPass.setText(realPass[0]);
            if (!emailVisible[0]) txtEmail.setText(realEmail[0]);
            saveAccount(d, accountId, txtUser, txtPass, txtEmail, chkActive, chkBan, chkAdmin, chkLoanTin,
                        txtRole, txtVip, txtServer, txtVnd, txtDanap, txtGold, txtPoint, eventMap,
                        oldCash[0], oldDanap[0]);
        });

        pBtn.add(btnCancel);
        pBtn.add(btnSave);

        d.add(pMain, BorderLayout.CENTER);
        d.add(pBtn, BorderLayout.SOUTH);
        d.setVisible(true);
    }

    // ========================================================================
    // 4. DATA LOGIC (LOAD & SAVE)
    // ========================================================================
    private void loadAccountData(int id, JTextField user, JTextField pass, JTextField email, 
                                 JCheckBox act, JCheckBox ban, JCheckBox adm, JCheckBox loanTin,
                                 JTextField role, JTextField vip, JTextField server,
                                 JTextField vnd, JTextField danap, JTextField gold, JTextField point,
                                 Map<String, JTextField> events, int[] headRef) {
        try (Connection conn = DBConnecter.getConnectionServer();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT a.*, (SELECT head FROM player WHERE account_id = a.id LIMIT 1) as p_head FROM account a WHERE a.id = ?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                headRef[0] = rs.getInt("p_head");
                user.setText(rs.getString("username"));
                pass.setText(rs.getString("password"));
                email.setText(rs.getString("email"));
                act.setSelected(rs.getInt("active") == 1);
                ban.setSelected(rs.getInt("ban") == 1);
                adm.setSelected(rs.getInt("is_admin") == 1);
                try { loanTin.setSelected(rs.getInt("loantin") == 1); } catch (Exception ex) { loanTin.setSelected(false); }
                role.setText(rs.getString("role"));
                vip.setText(rs.getString("vip"));
                server.setText(rs.getString("server_login"));
                
                // Mapping: VND = vnd (số dư VNĐ thực tế), Đã Nạp = danap
                vnd.setText(String.valueOf(rs.getLong("vnd")));
                danap.setText(String.valueOf(rs.getLong("danap")));
                gold.setText(String.valueOf(rs.getLong("vang")));
                point.setText(String.valueOf(rs.getLong("tichdiem")));

                for (String key : events.keySet()) {
                    try { events.get(key).setText(String.valueOf(rs.getLong(key))); } catch (Exception ex) {}
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void saveAccount(JDialog d, int id, JTextField user, JTextField pass, JTextField email, 
                             JCheckBox act, JCheckBox ban, JCheckBox adm, JCheckBox loanTin,
                             JTextField role, JTextField vip, JTextField server,
                             JTextField vnd, JTextField danap, JTextField gold, JTextField point,
                             Map<String, JTextField> events, long oldCash, long oldDanap) {
        if (JOptionPane.showConfirmDialog(d, "Lưu dữ liệu?", "Xác nhận", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        
        new Thread(() -> {
            // vnd = số dư VNĐ, cash sync = vnd, danap = tổng nạp
            StringBuilder sql = new StringBuilder("UPDATE account SET username=?, password=?, email=?, active=?, ban=?, is_admin=?, loantin=?, role=?, vip=?, server_login=?, ");
            sql.append("cash=?, vnd=?, danap=?, vang=?, tichdiem=?, "); 
            for (String k : events.keySet()) sql.append(k).append("=?, ");
            sql.append("update_time=NOW() WHERE id=?");

            try (Connection conn = DBConnecter.getConnectionServer();
                 PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                int i = 1;
                ps.setString(i++, user.getText());
                ps.setString(i++, pass.getText());
                ps.setString(i++, email.getText());
                ps.setInt(i++, act.isSelected() ? 1 : 0);
                ps.setInt(i++, ban.isSelected() ? 1 : 0);
                ps.setInt(i++, adm.isSelected() ? 1 : 0);
                ps.setInt(i++, loanTin.isSelected() ? 1 : 0);
                ps.setInt(i++, safeInt(role));
                ps.setInt(i++, safeInt(vip));
                ps.setInt(i++, safeInt(server));
                
                long newCash = safeLong(vnd);
                long newDanap = safeLong(danap);
                ps.setLong(i++, newCash); // cash = vnd
                ps.setLong(i++, newCash); // vnd (sync with cash)
                ps.setLong(i++, newDanap); // danap
                ps.setLong(i++, safeLong(gold));
                ps.setLong(i++, safeLong(point));
                
                for (String k : events.keySet()) ps.setLong(i++, safeLong(events.get(k)));
                ps.setInt(i++, id);

                ps.executeUpdate();

                // Ghi audit log nếu VND thay đổi
                if (newCash != oldCash) {
                    long diff = newCash - oldCash;
                    try (PreparedStatement psLog = conn.prepareStatement(
                            "INSERT INTO cash_audit_log (account_id, player_name, amount, balance_before, balance_after, source, detail) VALUES (?,?,?,?,?,?,?)")) {
                        psLog.setInt(1, id);
                        psLog.setString(2, user.getText());
                        psLog.setLong(3, diff);
                        psLog.setLong(4, oldCash);
                        psLog.setLong(5, newCash);
                        psLog.setString(6, "ADMIN_PANEL");
                        psLog.setString(7, "Admin sửa VND qua Panel: " + oldCash + " → " + newCash);
                        psLog.executeUpdate();
                    }
                }
                // Ghi audit log nếu danap thay đổi
                if (newDanap != oldDanap) {
                    long diff = newDanap - oldDanap;
                    try (PreparedStatement psLog = conn.prepareStatement(
                            "INSERT INTO cash_audit_log (account_id, player_name, amount, balance_before, balance_after, source, detail) VALUES (?,?,?,?,?,?,?)")) {
                        psLog.setInt(1, id);
                        psLog.setString(2, user.getText());
                        psLog.setLong(3, diff);
                        psLog.setLong(4, oldDanap);
                        psLog.setLong(5, newDanap);
                        psLog.setString(6, "ADMIN_PANEL_DANAP");
                        psLog.setString(7, "Admin sửa Tổng Nạp qua Panel: " + oldDanap + " → " + newDanap);
                        psLog.executeUpdate();
                    }
                }

                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(d, "Lưu thành công!");
                    d.dispose();
                    loadData();
                });
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(d, "Lỗi: " + e.getMessage()));
            }
        }).start();
    }

    // ========================================================================
    // 5. HELPER UI BUILDERS (Làm đẹp Dialog)
    // ========================================================================
    
    private JPanel createSectionPanel(String title) {
        JPanel p = new JPanel();
        p.setBackground(COL_SECTION_BG);
        TitledBorder b = BorderFactory.createTitledBorder(new LineBorder(COL_BORDER), title);
        b.setTitleFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setTitleColor(COL_PRIMARY);
        p.setBorder(new CompoundBorder(b, new EmptyBorder(5,5,5,5)));
        return p;
    }

    private JPanel createInputGroup(String label, JTextField txt) {
        JPanel p = new JPanel(new BorderLayout(0, 3));
        p.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(COL_TEXT_GRAY);
        p.add(lbl, BorderLayout.NORTH);
        p.add(txt, BorderLayout.CENTER);
        return p;
    }

    /** Tạo input group có nút mắt toggle ẩn/hiện nội dung */
    private JPanel createInputGroupWithEye(String label, JTextField txt, boolean[] visible, String[] realValue) {
        JPanel p = new JPanel(new BorderLayout(0, 3));
        p.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(COL_TEXT_GRAY);
        p.add(lbl, BorderLayout.NORTH);

        JPanel fieldPanel = new JPanel(new BorderLayout(3, 0));
        fieldPanel.setOpaque(false);
        fieldPanel.add(txt, BorderLayout.CENTER);

        JButton btnEye = new JButton("👁");
        btnEye.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        btnEye.setPreferredSize(new Dimension(36, 28));
        btnEye.setFocusPainted(false);
        btnEye.setMargin(new Insets(0, 2, 0, 2));
        btnEye.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnEye.setToolTipText("Ẩn/Hiện nội dung");
        btnEye.setBackground(new Color(240, 240, 240));
        btnEye.setBorder(BorderFactory.createLineBorder(COL_BORDER));

        btnEye.addActionListener(e -> {
            visible[0] = !visible[0];
            if (visible[0]) {
                // Nếu user đã sửa trong lúc hiện, lấy giá trị từ field
                // Nếu chưa, hiện giá trị gốc
                txt.setText(realValue[0]);
                txt.setEditable(true);
                btnEye.setText("🔒");
            } else {
                // Lưu giá trị thật khi ẩn
                realValue[0] = txt.getText();
                if (label.contains("Email")) {
                    txt.setText(maskEmail(realValue[0]));
                } else {
                    txt.setText("●●●●●●●●");
                }
                txt.setEditable(false);
                btnEye.setText("👁");
            }
        });

        fieldPanel.add(btnEye, BorderLayout.EAST);
        p.add(fieldPanel, BorderLayout.CENTER);
        return p;
    }

    /** Mask email: m***@gmail.com */
    private String maskEmail(String email) {
        if (email == null || email.isEmpty()) return "";
        int at = email.indexOf('@');
        if (at <= 1) return "●●●@●●●";
        return email.charAt(0) + "●●●" + email.substring(at);
    }

    private JPanel createBorderedGroup(String title, JTextField txt) {
        JPanel p = new JPanel(new BorderLayout(0, 5));
        p.setBackground(Color.WHITE);
        p.setBorder(new CompoundBorder(
            new LineBorder(COL_BORDER, 1, true),
            new EmptyBorder(8, 10, 8, 10)
        ));
        
        JLabel lbl = new JLabel(title);
        lbl.setFont(FONT_BOLD);
        lbl.setForeground(COL_TEXT_GRAY);
        
        txt.setBorder(null);
        txt.setBackground(Color.WHITE);
        
        p.add(lbl, BorderLayout.NORTH);
        p.add(txt, BorderLayout.CENTER);
        return p;
    }

    private JPanel createCompactInput(String title, JTextField txt) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(new CompoundBorder(
            new LineBorder(new Color(230, 230, 230)),
            new EmptyBorder(4, 5, 4, 5)
        ));
        
        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl.setForeground(COL_TEXT_GRAY);
        lbl.setPreferredSize(new Dimension(80, 20));
        
        txt.setBorder(null);
        txt.setFont(new Font("Consolas", Font.PLAIN, 13));
        txt.setHorizontalAlignment(JTextField.RIGHT);
        
        p.add(lbl, BorderLayout.WEST);
        p.add(txt, BorderLayout.CENTER);
        return p;
    }

    private JTextField createField(boolean edit) {
        JTextField t = new JTextField();
        t.setEditable(edit);
        t.setFont(FONT_DATA);
        if(edit) {
            t.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(COL_BORDER), new EmptyBorder(5, 8, 5, 8)));
        } else {
            t.setBorder(null); t.setOpaque(false); 
            t.setFont(new Font("Segoe UI", Font.BOLD, 15));
            t.setForeground(new Color(0, 102, 204));
        }
        return t;
    }

    private void styleCheck(JCheckBox c) {
        c.setFont(FONT_BOLD);
        c.setOpaque(false);
        c.setFocusPainted(false);
    }

    private int safeInt(JTextField t) { try { return Integer.parseInt(t.getText().trim()); } catch(Exception e) { return 0; } }
    private long safeLong(JTextField t) { try { return Long.parseLong(t.getText().trim()); } catch(Exception e) { return 0; } }

    private JButton createButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(8, 15, 8, 15));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    private String formatNum(long num) { return java.text.NumberFormat.getInstance().format(num); }

    // ========================================================================
    // 6. DB LOADERS (TABLE)
    // ========================================================================
    private void loadHeadPartCache() {
        new Thread(() -> {
            try (Connection conn = DBConnecter.getConnectionServer(); Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id, data FROM part WHERE type = 0")) {
                while (rs.next()) {
                    try {
                        JsonArray arr = new JsonParser().parse(rs.getString("data")).getAsJsonArray();
                        if (arr.size() > 0) partHeadIconMap.put(rs.getInt("id"), arr.get(0).getAsJsonArray().get(0).getAsInt());
                    } catch (Exception ignored) {}
                }
            } catch (Exception e) {}
            SwingUtilities.invokeLater(this::loadData);
        }).start();
    }

    private ImageIcon getAvatar(int headId, String text, int size) {
        if (headId > 0) {
            if (headCache.containsKey(headId) && size == 28) return headCache.get(headId);
            Integer iconId = partHeadIconMap.get(headId);
            if (iconId != null) {
                try {
                    String[] zooms = {"x4", "x3", "x2", "x1"};
                    for (String z : zooms) {
                        File f = new File(ICON_FOLDER + z + "/" + iconId + ".png");
                        if (f.exists()) {
                            Image img = ImageIO.read(f).getScaledInstance(size, size, Image.SCALE_SMOOTH);
                            ImageIcon icon = new ImageIcon(img);
                            if(size == 28) headCache.put(headId, icon);
                            return icon;
                        }
                    }
                } catch (Exception e) {}
            }
        }
        return AvatarGenerator.generate(text, size);
    }

    private void loadData() {
        // Cập nhật lấy vnd, danap (Mapping: vnd=số dư VNĐ thực, danap=tổng nạp)
        updateTable("SELECT a.id, a.username, a.password, a.active, a.ban, a.vip, a.vnd, a.danap, a.create_time, " +
                    "(SELECT head FROM player WHERE account_id = a.id LIMIT 1) AS head, " +
                    "(SELECT name FROM player WHERE account_id = a.id LIMIT 1) AS p_name FROM account a ORDER BY a.id ASC");
    }

    private void searchData(String txt) {
        if(txt.isEmpty()) { loadData(); return; }
        updateTable("SELECT a.id, a.username, a.password, a.active, a.ban, a.vip, a.vnd, a.danap, a.create_time, " +
                    "(SELECT head FROM player WHERE account_id = a.id LIMIT 1) AS head, " +
                    "(SELECT name FROM player WHERE account_id = a.id LIMIT 1) AS p_name FROM account a " +
                    "WHERE a.username LIKE '%"+txt+"%' OR a.id='"+txt+"' OR (SELECT name FROM player WHERE account_id=a.id LIMIT 1) LIKE '%"+txt+"%'");
    }

    private void updateTable(String sql) {
        model.setRowCount(0);
        new Thread(() -> {
            try (Connection conn = DBConnecter.getConnectionServer(); Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(sql)) {
                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    row.add(Boolean.FALSE); // checkbox
                    String u = rs.getString("username");
                    row.add(getAvatar(rs.getInt("head"), u, 28));
                    row.add(rs.getInt("id"));
                    row.add(u);
                    String pn = rs.getString("p_name");
                    row.add(pn == null ? "-(Chưa tạo)-" : pn);
                    row.add("******");
                    
                    int active = rs.getInt("active");
                    int ban = rs.getInt("ban");
                    row.add(ban == 1 ? "ĐÃ BAN" : (active == 1 ? "Active" : "Chưa KH"));
                    
                    row.add(rs.getInt("vip"));
                    row.add(formatNum(rs.getLong("vnd"))); // VND (số dư VNĐ thực tế)
                    row.add(formatNum(rs.getLong("danap"))); // Đã nạp
                    Timestamp ts = rs.getTimestamp("create_time");
                    row.add(ts != null ? DATE_FMT.format(ts) : "N/A");
                    
                    SwingUtilities.invokeLater(() -> model.addRow(row));
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    static class AvatarGenerator {
        private static final Color[] COLORS = { new Color(26,188,156), new Color(46,204,113), new Color(52,152,219), new Color(155,89,182), new Color(230,126,34), new Color(231,76,60) };
        public static ImageIcon generate(String text, int size) {
            if (text == null || text.isEmpty()) text = "?";
            String l = text.substring(0, 1).toUpperCase();
            Color bg = COLORS[Math.abs(text.hashCode()) % COLORS.length];
            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg); g2.fill(new Ellipse2D.Float(0, 0, size, size));
            g2.setColor(Color.WHITE); g2.setFont(new Font("Segoe UI", Font.BOLD, size / 2));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(l, (size - fm.stringWidth(l)) / 2, (size - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
            return new ImageIcon(img);
        }
    }

    /**
     * Xóa toàn bộ tài khoản chưa tạo nhân vật (không có record trong bảng player)
     */
    private void purgeEmptyAccounts() {
        new Thread(() -> {
            try (Connection conn = DBConnecter.getConnectionServer()) {
                // Đếm số tài khoản chưa tạo NV
                String countSql = "SELECT COUNT(*) FROM account a WHERE NOT EXISTS (SELECT 1 FROM player p WHERE p.account_id = a.id)";
                int count = 0;
                try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(countSql)) {
                    if (rs.next()) count = rs.getInt(1);
                }

                if (count == 0) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                            "Không có tài khoản nào chưa tạo nhân vật!", "Thông báo", JOptionPane.INFORMATION_MESSAGE));
                    return;
                }

                final int totalCount = count;
                SwingUtilities.invokeAndWait(() -> {
                    int confirm = JOptionPane.showConfirmDialog(this,
                            "⚠ Tìm thấy " + totalCount + " tài khoản chưa tạo nhân vật.\n\n"
                            + "Bạn có chắc muốn XÓA VĨNH VIỄN tất cả?\n"
                            + "Hành động này KHÔNG THỂ hoàn tác!",
                            "Xác nhận xóa TK rỗng", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

                    if (confirm != JOptionPane.YES_OPTION) return;

                    // Double confirm
                    int doubleConfirm = JOptionPane.showConfirmDialog(this,
                            "XÁC NHẬN LẦN 2:\nXóa " + totalCount + " tài khoản chưa tạo nhân vật?",
                            "Xác nhận lần cuối", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);

                    if (doubleConfirm != JOptionPane.YES_OPTION) return;

                    // Thực hiện xóa trên thread riêng
                    new Thread(() -> {
                        try (Connection conn2 = DBConnecter.getConnectionServer(); Statement stmt2 = conn2.createStatement()) {
                            String deleteSql = "DELETE FROM account WHERE NOT EXISTS (SELECT 1 FROM player p WHERE p.account_id = account.id)";
                            int deleted = stmt2.executeUpdate(deleteSql);
                            SwingUtilities.invokeLater(() -> {
                                JOptionPane.showMessageDialog(this,
                                        "✅ Đã xóa thành công " + deleted + " tài khoản chưa tạo nhân vật!",
                                        "Hoàn tất", JOptionPane.INFORMATION_MESSAGE);
                                loadData();
                            });
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage()));
                        }
                    }).start();
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage()));
            }
        }).start();
    }

    /**
     * Tự động thêm cột 'loantin' vào bảng account nếu chưa có
     */
    private void ensureLoanTinColumn() {
        new Thread(() -> {
            try (Connection conn = DBConnecter.getConnectionServer(); Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("ALTER TABLE account ADD COLUMN IF NOT EXISTS loantin TINYINT(1) DEFAULT 0");
            } catch (Exception e) {
                try (Connection conn = DBConnecter.getConnectionServer(); Statement stmt = conn.createStatement()) {
                    try { stmt.executeUpdate("ALTER TABLE account ADD COLUMN loantin TINYINT(1) DEFAULT 0"); } catch (Exception ignored) {}
                } catch (Exception ignored) {}
            }
        }).start();
    }
}