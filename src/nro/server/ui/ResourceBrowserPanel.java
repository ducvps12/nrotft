package nro.server.ui;

import jdbc.DBConnecter;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.sql.*;
import java.util.*;
import javax.imageio.ImageIO;
import consts.ConstPlayer;

public class ResourceBrowserPanel extends JPanel {

    private static final String ICON_FOLDER = "data/icon/";
    private final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    private final Font FONT_PLAIN = new Font("Segoe UI", Font.PLAIN, 13);
    private final Map<Integer, ImageIcon> iconCache = new HashMap<>();
    private final Map<Integer, Integer> partIconMap = new HashMap<>();

    private JTabbedPane tabbedPane;

    // Costume tab
    private JTable tableCostume;
    private DefaultTableModel modelCostume;
    private JTextField txtSearchCostume;
    private TableRowSorter<DefaultTableModel> sorterCostume;

    // NPC tab
    private JTable tableNpc;
    private DefaultTableModel modelNpc;
    private JTextField txtSearchNpc;
    private TableRowSorter<DefaultTableModel> sorterNpc;

    // Item tab
    private JTable tableItem;
    private DefaultTableModel modelItem;
    private JTextField txtSearchItem;
    private JComboBox<String> cboItemType;
    private TableRowSorter<DefaultTableModel> sorterItem;

    // Boss Reward tab
    private JList<String> listBossFiles;
    private JTextArea txtBossSource;
    private JTextField txtSearchBoss;

    public ResourceBrowserPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(245, 245, 245));
        loadPartIcons();
        initUI();
        // Auto-load data
        SwingUtilities.invokeLater(() -> {
            loadCostumeData();
            loadNpcData();
            loadItemData();
            loadBossFiles();
        });
    }

    private void initUI() {
        // Header
        JLabel header = new JLabel("  🔍 TRÌNH DUYỆT TÀI NGUYÊN (Resource Browser)");
        header.setFont(new Font("Segoe UI", Font.BOLD, 20));
        header.setForeground(new Color(0, 102, 204));
        header.setBorder(new EmptyBorder(15, 10, 10, 0));
        add(header, BorderLayout.NORTH);

        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(FONT_BOLD);
        tabbedPane.addTab("🎭 Cải Trang (Costume)", createCostumeTab());
        tabbedPane.addTab("👤 NPC Template", createNpcTab());
        tabbedPane.addTab("📦 Vật Phẩm (Items)", createItemTab());
        tabbedPane.addTab("💀 Boss Reward Source", createBossRewardTab());
        tabbedPane.addTab("⚡ Biến Hình Đệ", createBienHinhTab());
        tabbedPane.addTab("💍 Hợp Thể Bông Tai", createFusionTab());
        add(tabbedPane, BorderLayout.CENTER);
    }

    // ==================== COSTUME TAB ====================
    private JPanel createCostumeTab() {
        JPanel p = new JPanel(new BorderLayout(5, 5));
        p.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Search bar
        JPanel topBar = new JPanel(new BorderLayout(5, 0));
        txtSearchCostume = new JTextField();
        txtSearchCostume.setFont(FONT_PLAIN);
        txtSearchCostume.putClientProperty("JTextField.placeholderText", "Tìm theo tên hoặc ID...");
        JButton btnReload = createBtn("Tải Lại", new Color(0, 123, 255));
        btnReload.addActionListener(e -> loadCostumeData());

        JLabel lblInfo = new JLabel("  Lọc Type 5 (Cải trang) từ item_template  ");
        lblInfo.setFont(FONT_PLAIN);
        lblInfo.setForeground(Color.GRAY);

        topBar.add(lblInfo, BorderLayout.WEST);
        topBar.add(txtSearchCostume, BorderLayout.CENTER);
        topBar.add(btnReload, BorderLayout.EAST);
        p.add(topBar, BorderLayout.NORTH);

        // Table
        modelCostume = new DefaultTableModel(
                new String[]{"Icon", "ID", "Tên Cải Trang", "Gender", "Part Head", "Part Body", "Part Leg", "Mô Tả"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
            public Class<?> getColumnClass(int c) { return c == 0 ? ImageIcon.class : Object.class; }
        };
        tableCostume = new JTable(modelCostume);
        tableCostume.setRowHeight(40);
        tableCostume.setFont(FONT_PLAIN);
        tableCostume.getTableHeader().setFont(FONT_BOLD);
        tableCostume.getColumnModel().getColumn(0).setMaxWidth(50);
        tableCostume.getColumnModel().getColumn(1).setMaxWidth(60);

        sorterCostume = new TableRowSorter<>(modelCostume);
        tableCostume.setRowSorter(sorterCostume);
        addSearchFilter(txtSearchCostume, sorterCostume, new int[]{1, 2, 3});

        p.add(new JScrollPane(tableCostume), BorderLayout.CENTER);

        // Bottom bar with buttons
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        JLabel lblCount = new JLabel("Double-click → Editor cải trang (sửa Part, preview ghép Head+Body+Leg)");
        lblCount.setFont(FONT_PLAIN);
        lblCount.setForeground(new Color(0, 150, 0));
        bottomBar.add(lblCount);

        JButton btnOpenRes = createBtn("📁 Mở Thư Mục Icon", new Color(108, 117, 125));
        btnOpenRes.addActionListener(e -> {
            try {
                File dir = new File(ICON_FOLDER + "x4");
                if (dir.exists()) java.awt.Desktop.getDesktop().open(dir);
                else JOptionPane.showMessageDialog(this, "Không tìm thấy: " + dir.getAbsolutePath());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi mở thư mục: " + ex.getMessage());
            }
        });
        bottomBar.add(btnOpenRes);
        p.add(bottomBar, BorderLayout.SOUTH);

        // Double click → open full editor
        tableCostume.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = tableCostume.getSelectedRow();
                    if (row >= 0) {
                        row = tableCostume.convertRowIndexToModel(row);
                        openCostumeEditor(row);
                    }
                }
            }
        });

        return p;
    }

    /** Editor cải trang — Preview ghép Head+Body+Leg + sửa nhanh Part ID + lưu DB */
    private void openCostumeEditor(int modelRow) {
        try {
        int costumeId = toInt(modelCostume.getValueAt(modelRow, 1));
        String name = String.valueOf(modelCostume.getValueAt(modelRow, 2));
        String gender = String.valueOf(modelCostume.getValueAt(modelRow, 3));
        int headPart = toInt(modelCostume.getValueAt(modelRow, 4));
        int bodyPart = toInt(modelCostume.getValueAt(modelRow, 5));
        int legPart = toInt(modelCostume.getValueAt(modelRow, 6));
        String desc = String.valueOf(modelCostume.getValueAt(modelRow, 7));

        JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "✏️ Editor Cải Trang — ID " + costumeId + " — " + name, true);
        d.setSize(850, 600);
        d.setLocationRelativeTo(this);
        d.setLayout(new BorderLayout(10, 10));

        JPanel mainPanel = new JPanel(new BorderLayout(15, 10));
        mainPanel.setBorder(new EmptyBorder(15, 15, 10, 15));
        mainPanel.setBackground(Color.WHITE);

        // === LEFT: Preview Panel ===
        JPanel previewPanel = new JPanel();
        previewPanel.setLayout(new BoxLayout(previewPanel, BoxLayout.Y_AXIS));
        previewPanel.setBackground(new Color(240, 245, 255));
        previewPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(0, 102, 204)),
                " 🎭 PREVIEW GHÉP ", TitledBorder.CENTER, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 13), new Color(0, 102, 204)),
            new EmptyBorder(10, 15, 10, 15)));
        previewPanel.setPreferredSize(new Dimension(280, 0));

        // Composite preview (3 icons stacked)
        JLabel lblHeadIcon = new JLabel("", SwingConstants.CENTER);
        JLabel lblBodyIcon = new JLabel("", SwingConstants.CENTER);
        JLabel lblLegIcon = new JLabel("", SwingConstants.CENTER);
        lblHeadIcon.setPreferredSize(new Dimension(100, 100));
        lblBodyIcon.setPreferredSize(new Dimension(100, 100));
        lblLegIcon.setPreferredSize(new Dimension(100, 100));

        // Load initial icons
        updatePartPreview(lblHeadIcon, headPart, "🗣️ HEAD");
        updatePartPreview(lblBodyIcon, bodyPart, "👕 BODY");
        updatePartPreview(lblLegIcon, legPart, "👖 LEG");

        JPanel iconsRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        iconsRow.setOpaque(false);
        iconsRow.add(wrapIconWithLabel(lblHeadIcon, "Head"));
        iconsRow.add(wrapIconWithLabel(lblBodyIcon, "Body"));
        iconsRow.add(wrapIconWithLabel(lblLegIcon, "Leg"));
        previewPanel.add(iconsRow);

        previewPanel.add(Box.createVerticalStrut(10));

        // Info labels
        JLabel lblInfo = new JLabel("<html><center>" +
                "<b>" + name + "</b><br>" +
                "ID: " + costumeId + " | " + gender + "<br>" +
                desc + "</center></html>");
        lblInfo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblInfo.setAlignmentX(Component.CENTER_ALIGNMENT);
        previewPanel.add(lblInfo);

        previewPanel.add(Box.createVerticalStrut(10));

        // Open folder button
        JButton btnFolder = new JButton("📁 Mở thư mục Icon");
        btnFolder.setFont(FONT_PLAIN);
        btnFolder.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnFolder.addActionListener(ev -> {
            try {
                File dir = new File(ICON_FOLDER + "x4");
                if (dir.exists()) java.awt.Desktop.getDesktop().open(dir);
            } catch (Exception ex) { ex.printStackTrace(); }
        });
        previewPanel.add(btnFolder);

        mainPanel.add(previewPanel, BorderLayout.WEST);

        // === RIGHT: Editor Form ===
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(40, 167, 69)),
                " ✏️ CHỈNH SỬA NHANH ", TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 13), new Color(40, 167, 69)),
            new EmptyBorder(10, 10, 10, 10)));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 8, 6, 8);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1.0;

        Font inputFont = new Font("Segoe UI", Font.PLAIN, 14);
        int row = 0;

        // ID (readonly)
        JTextField txtId = new JTextField(String.valueOf(costumeId));
        txtId.setEditable(false);
        txtId.setFont(inputFont);
        addFormRow(formPanel, g, row++, "ID:", txtId);

        // Name
        JTextField txtName = new JTextField(name);
        txtName.setFont(inputFont);
        addFormRow(formPanel, g, row++, "Tên Cải Trang:", txtName);

        // Head Part + preview button
        JTextField txtHead = new JTextField(String.valueOf(headPart));
        txtHead.setFont(inputFont);
        JPanel headRow = createPartRow(txtHead, lblHeadIcon, "HEAD");
        addFormRow(formPanel, g, row++, "Head Part:", headRow);

        // Body Part + preview button
        JTextField txtBody = new JTextField(String.valueOf(bodyPart));
        txtBody.setFont(inputFont);
        JPanel bodyRow = createPartRow(txtBody, lblBodyIcon, "BODY");
        addFormRow(formPanel, g, row++, "Body Part:", bodyRow);

        // Leg Part + preview button
        JTextField txtLeg = new JTextField(String.valueOf(legPart));
        txtLeg.setFont(inputFont);
        JPanel legRow = createPartRow(txtLeg, lblLegIcon, "LEG");
        addFormRow(formPanel, g, row++, "Leg Part:", legRow);

        // Description
        JTextArea txtDesc = new JTextArea(desc, 3, 20);
        txtDesc.setFont(inputFont);
        txtDesc.setLineWrap(true);
        g.gridx = 0; g.gridy = row; g.gridwidth = 1;
        formPanel.add(new JLabel("Mô tả:", SwingConstants.RIGHT), g);
        g.gridx = 1; g.gridwidth = 2;
        formPanel.add(new JScrollPane(txtDesc), g);
        g.gridwidth = 1;
        row++;

        // Part ID lookup helper
        JPanel lookupPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        lookupPanel.setOpaque(false);
        JTextField txtLookup = new JTextField(8);
        txtLookup.setFont(inputFont);
        JButton btnLookup = new JButton("🔍 Xem Icon");
        btnLookup.setFont(FONT_PLAIN);
        btnLookup.addActionListener(ev -> {
            try {
                int lookId = Integer.parseInt(txtLookup.getText().trim());
                ImageIcon ic = getIcon(lookId, 64);
                if (ic != null) {
                    JOptionPane.showMessageDialog(d, new JLabel(ic), "Icon #" + lookId, JOptionPane.PLAIN_MESSAGE);
                } else {
                    // Try direct file
                    File f = new File(ICON_FOLDER + "x4/" + lookId + ".png");
                    if (f.exists()) {
                        BufferedImage img = ImageIO.read(f);
                        JOptionPane.showMessageDialog(d, new JLabel(new ImageIcon(img)), "Icon #" + lookId, JOptionPane.PLAIN_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(d, "Không tìm thấy icon ID: " + lookId);
                    }
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(d, "ID không hợp lệ!");
            }
        });
        lookupPanel.add(new JLabel("Tra cứu Icon ID:"));
        lookupPanel.add(txtLookup);
        lookupPanel.add(btnLookup);
        g.gridx = 0; g.gridy = row; g.gridwidth = 3;
        formPanel.add(lookupPanel, g);
        g.gridwidth = 1;

        mainPanel.add(formPanel, BorderLayout.CENTER);

        // === BOTTOM: Save/Close buttons ===
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        bottomPanel.setBackground(Color.WHITE);

        JButton btnSave = new JButton("💾 LƯU VÀO DATABASE");
        btnSave.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnSave.setBackground(new Color(0, 123, 255));
        btnSave.setForeground(Color.WHITE);
        btnSave.setFocusPainted(false);
        btnSave.setPreferredSize(new Dimension(250, 45));
        btnSave.addActionListener(ev -> {
            try (Connection conn = DBConnecter.getConnectionServer();
                 PreparedStatement ps = conn.prepareStatement(
                     "UPDATE item_template SET name=?, head=?, body=?, leg=?, description=? WHERE id=?")) {
                ps.setString(1, txtName.getText().trim());
                ps.setInt(2, Integer.parseInt(txtHead.getText().trim()));
                ps.setInt(3, Integer.parseInt(txtBody.getText().trim()));
                ps.setInt(4, Integer.parseInt(txtLeg.getText().trim()));
                ps.setString(5, txtDesc.getText().trim());
                ps.setInt(6, costumeId);
                int updated = ps.executeUpdate();
                if (updated > 0) {
                    JOptionPane.showMessageDialog(d, "✅ Đã lưu thành công! (Cần restart server để áp dụng in-game)");
                    // Update table
                    modelCostume.setValueAt(txtName.getText().trim(), modelRow, 2);
                    modelCostume.setValueAt(Integer.parseInt(txtHead.getText().trim()), modelRow, 4);
                    modelCostume.setValueAt(Integer.parseInt(txtBody.getText().trim()), modelRow, 5);
                    modelCostume.setValueAt(Integer.parseInt(txtLeg.getText().trim()), modelRow, 6);
                    modelCostume.setValueAt(txtDesc.getText().trim(), modelRow, 7);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(d, "❌ Lỗi lưu: " + ex.getMessage());
            }
        });

        JButton btnClose = new JButton("Đóng");
        btnClose.setFont(FONT_BOLD);
        btnClose.setPreferredSize(new Dimension(100, 45));
        btnClose.addActionListener(ev -> d.dispose());

        bottomPanel.add(btnSave);
        bottomPanel.add(btnClose);

        d.add(mainPanel, BorderLayout.CENTER);
        d.add(bottomPanel, BorderLayout.SOUTH);
        d.setVisible(true);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi mở Editor: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel createPartRow(JTextField txt, JLabel previewLabel, String partName) {
        JPanel row = new JPanel(new BorderLayout(5, 0));
        row.setOpaque(false);
        row.add(txt, BorderLayout.CENTER);
        JButton btn = new JButton("👁 Xem");
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btn.addActionListener(e -> {
            try {
                int partId = Integer.parseInt(txt.getText().trim());
                updatePartPreview(previewLabel, partId, partName);
            } catch (Exception ex) { /* ignore */ }
        });
        row.add(btn, BorderLayout.EAST);
        return row;
    }

    private void updatePartPreview(JLabel lbl, int partId, String label) {
        if (partId <= 0) {
            lbl.setIcon(null);
            lbl.setText(label + "\n(không có)");
            return;
        }
        // Try to get icon from partIconMap first, then direct
        int iconId = partIconMap.getOrDefault(partId, partId);
        ImageIcon ic = getIcon(iconId, 80);
        if (ic != null) {
            lbl.setIcon(ic);
            lbl.setText(null);
        } else {
            lbl.setIcon(null);
            lbl.setText(label + " #" + partId);
        }
    }

    private JPanel wrapIconWithLabel(JLabel iconLabel, String text) {
        JPanel p = new JPanel(new BorderLayout(0, 3));
        p.setOpaque(false);
        p.add(iconLabel, BorderLayout.CENTER);
        JLabel lbl = new JLabel(text, SwingConstants.CENTER);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl.setForeground(new Color(0, 102, 204));
        p.add(lbl, BorderLayout.SOUTH);
        return p;
    }

    private void addFormRow(JPanel panel, GridBagConstraints g, int row, String label, JComponent field) {
        g.gridx = 0; g.gridy = row; g.gridwidth = 1; g.weightx = 0;
        panel.add(new JLabel(label, SwingConstants.RIGHT), g);
        g.gridx = 1; g.gridwidth = 2; g.weightx = 1.0;
        panel.add(field, g);
        g.gridwidth = 1;
    }

    private int toInt(Object o) {
        if (o == null) return -1;
        try { return Integer.parseInt(o.toString().trim()); }
        catch (Exception e) { return -1; }
    }

    // ==================== NPC TAB ====================
    private JPanel createNpcTab() {
        JPanel p = new JPanel(new BorderLayout(5, 5));
        p.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel topBar = new JPanel(new BorderLayout(5, 0));
        txtSearchNpc = new JTextField();
        txtSearchNpc.setFont(FONT_PLAIN);
        txtSearchNpc.putClientProperty("JTextField.placeholderText", "Tìm NPC theo tên hoặc ID...");
        JButton btnReload = createBtn("Tải Lại", new Color(0, 123, 255));
        btnReload.addActionListener(e -> loadNpcData());
        topBar.add(new JLabel("  NPC Template  "), BorderLayout.WEST);
        topBar.add(txtSearchNpc, BorderLayout.CENTER);
        topBar.add(btnReload, BorderLayout.EAST);
        p.add(topBar, BorderLayout.NORTH);

        modelNpc = new DefaultTableModel(
                new String[]{"Icon", "ID", "Tên NPC", "Head", "Body", "Leg", "Avatar ID", "Menu/Shop"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
            public Class<?> getColumnClass(int c) { return c == 0 ? ImageIcon.class : Object.class; }
        };
        tableNpc = new JTable(modelNpc);
        tableNpc.setRowHeight(40);
        tableNpc.setFont(FONT_PLAIN);
        tableNpc.getTableHeader().setFont(FONT_BOLD);
        tableNpc.getColumnModel().getColumn(0).setMaxWidth(50);
        tableNpc.getColumnModel().getColumn(1).setMaxWidth(60);

        sorterNpc = new TableRowSorter<>(modelNpc);
        tableNpc.setRowSorter(sorterNpc);
        addSearchFilter(txtSearchNpc, sorterNpc, new int[]{1, 2});

        p.add(new JScrollPane(tableNpc), BorderLayout.CENTER);
        return p;
    }

    // ==================== ITEM TAB ====================
    private JPanel createItemTab() {
        JPanel p = new JPanel(new BorderLayout(5, 5));
        p.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel topBar = new JPanel(new BorderLayout(5, 0));
        txtSearchItem = new JTextField();
        txtSearchItem.setFont(FONT_PLAIN);
        txtSearchItem.putClientProperty("JTextField.placeholderText", "Tìm vật phẩm theo tên hoặc ID...");

        String[] types = {"Tất cả", "0-Trang bị", "1-Trang bị", "2-Trang bị", "3-Trang bị", "4-Trang bị",
                "5-Cải trang", "6-Bùa", "7-Skill", "8-Rada", "9-Cải trang",
                "12-Ngọc Rồng", "14-Capsule", "21-Sự kiện", "27-Event"};
        cboItemType = new JComboBox<>(types);
        cboItemType.setFont(FONT_PLAIN);
        cboItemType.addActionListener(e -> filterItems());

        JButton btnReload = createBtn("Tải Lại", new Color(0, 123, 255));
        btnReload.addActionListener(e -> loadItemData());

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        filterPanel.add(new JLabel("Type:"));
        filterPanel.add(cboItemType);

        topBar.add(filterPanel, BorderLayout.WEST);
        topBar.add(txtSearchItem, BorderLayout.CENTER);
        topBar.add(btnReload, BorderLayout.EAST);
        p.add(topBar, BorderLayout.NORTH);

        modelItem = new DefaultTableModel(
                new String[]{"Icon", "ID", "Tên Item", "Type", "Gender", "Level", "Part", "Mô Tả"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
            public Class<?> getColumnClass(int c) { return c == 0 ? ImageIcon.class : Object.class; }
        };
        tableItem = new JTable(modelItem);
        tableItem.setRowHeight(35);
        tableItem.setFont(FONT_PLAIN);
        tableItem.getTableHeader().setFont(FONT_BOLD);
        tableItem.getColumnModel().getColumn(0).setMaxWidth(50);
        tableItem.getColumnModel().getColumn(1).setMaxWidth(60);
        tableItem.getColumnModel().getColumn(3).setMaxWidth(50);

        sorterItem = new TableRowSorter<>(modelItem);
        tableItem.setRowSorter(sorterItem);

        txtSearchItem.getDocument().addDocumentListener(new SimpleDocumentListener() {
            public void update() { filterItems(); }
        });

        p.add(new JScrollPane(tableItem), BorderLayout.CENTER);
        return p;
    }

    // ==================== BOSS REWARD TAB ====================
    private JPanel createBossRewardTab() {
        JPanel p = new JPanel(new BorderLayout(5, 5));
        p.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Left: boss file list
        JPanel leftPanel = new JPanel(new BorderLayout(0, 5));
        leftPanel.setPreferredSize(new Dimension(250, 0));

        txtSearchBoss = new JTextField();
        txtSearchBoss.setFont(FONT_PLAIN);
        txtSearchBoss.putClientProperty("JTextField.placeholderText", "Tìm boss...");
        leftPanel.add(txtSearchBoss, BorderLayout.NORTH);

        listBossFiles = new JList<>();
        listBossFiles.setFont(FONT_PLAIN);
        listBossFiles.setFixedCellHeight(30);
        leftPanel.add(new JScrollPane(listBossFiles), BorderLayout.CENTER);

        JButton btnReload = createBtn("Tải Lại", new Color(0, 123, 255));
        btnReload.addActionListener(e -> loadBossFiles());
        leftPanel.add(btnReload, BorderLayout.SOUTH);

        // Right: source viewer
        txtBossSource = new JTextArea();
        txtBossSource.setFont(new Font("Consolas", Font.PLAIN, 13));
        txtBossSource.setEditable(false);
        txtBossSource.setLineWrap(false);
        JScrollPane scrollSource = new JScrollPane(txtBossSource);
        scrollSource.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                "Source Code - reward() & drop logic",
                TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, FONT_BOLD, new Color(0, 102, 204)));

        // Selection listener
        listBossFiles.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = listBossFiles.getSelectedValue();
                if (selected != null) loadBossRewardSource(selected);
            }
        });

        // Search filter
        txtSearchBoss.getDocument().addDocumentListener(new SimpleDocumentListener() {
            public void update() { filterBossFiles(); }
        });

        p.add(leftPanel, BorderLayout.WEST);
        p.add(scrollSource, BorderLayout.CENTER);
        return p;
    }

    // ==================== BIẾN HÌNH ĐỆ TAB ====================
    private JPanel createBienHinhTab() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBorder(new EmptyBorder(10, 10, 10, 10));
        p.setBackground(Color.WHITE);

        // Header info
        JLabel lblInfo = new JLabel("<html><b>⚡ Biến Hình Đệ Tử</b> — Dữ liệu từ <code>ConstPlayer.java</code>"
            + " | Head/Body/Leg Part cho mỗi chủng tộc × 5 cấp biến hình</html>");
        lblInfo.setFont(FONT_PLAIN);
        lblInfo.setBorder(new EmptyBorder(5, 5, 10, 5));
        p.add(lblInfo, BorderLayout.NORTH);

        String[] races = {"Trái Đất", "Namec", "Xayda", "Majin"};
        String[] cols = {"Chủng Tộc", "Cấp BH", "Head Part", "Body Part", "Leg Part", "Aura", "Preview Head", "Preview Body", "Preview Leg"};

        java.util.List<Object[]> rows = new ArrayList<>();
        for (int g = 0; g < 4; g++) {
            for (int lv = 0; lv < 5; lv++) {
                int headPart = ConstPlayer.HEADBIENHINH[g][lv];
                int bodyPart = ConstPlayer.BODYBIENHINH[g];
                int legPart = ConstPlayer.LEGBIENHINH[g];
                int aura = ConstPlayer.AURABIENHINH[g][lv];
                ImageIcon headIc = getIcon(headPart, 32);
                ImageIcon bodyIc = getIcon(bodyPart, 32);
                ImageIcon legIc = getIcon(legPart, 32);
                rows.add(new Object[]{races[g], "Cấp " + (lv + 1), headPart, bodyPart, legPart, aura, headIc, bodyIc, legIc});
            }
        }

        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
            public Class<?> getColumnClass(int c) { return (c >= 6) ? ImageIcon.class : Object.class; }
        };
        for (Object[] row : rows) model.addRow(row);

        JTable table = new JTable(model);
        table.setRowHeight(40);
        table.setFont(FONT_PLAIN);
        table.getTableHeader().setFont(FONT_BOLD);
        table.getTableHeader().setBackground(new Color(255, 152, 0));
        table.getTableHeader().setForeground(Color.WHITE);
        // Color alternate rows by race
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(t, v, s, f, r, c);
                if (!s) {
                    int modelRow = t.convertRowIndexToModel(r);
                    int raceIdx = modelRow / 5;
                    Color[] bg = {new Color(255, 248, 230), new Color(230, 255, 230), new Color(230, 240, 255), new Color(255, 230, 245)};
                    setBackground(bg[raceIdx % 4]);
                }
                return this;
            }
        });

        p.add(new JScrollPane(table), BorderLayout.CENTER);

        // Bottom: preview panel
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        bottomPanel.setBackground(new Color(245, 245, 255));
        bottomPanel.setBorder(BorderFactory.createTitledBorder("Double-click hàng để xem Preview ghép | Sửa ConstPlayer.java nếu muốn thay đổi Part ID"));

        JLabel lblPreviewH = new JLabel(); lblPreviewH.setPreferredSize(new Dimension(80, 80));
        JLabel lblPreviewB = new JLabel(); lblPreviewB.setPreferredSize(new Dimension(80, 80));
        JLabel lblPreviewL = new JLabel(); lblPreviewL.setPreferredSize(new Dimension(80, 80));
        JLabel lblPreviewInfo = new JLabel("← Click hàng để xem");
        lblPreviewInfo.setFont(new Font("Segoe UI", Font.ITALIC, 12));

        bottomPanel.add(wrapIconWithLabel(lblPreviewH, "Head"));
        bottomPanel.add(wrapIconWithLabel(lblPreviewB, "Body"));
        bottomPanel.add(wrapIconWithLabel(lblPreviewL, "Leg"));
        bottomPanel.add(lblPreviewInfo);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int sel = table.getSelectedRow();
                if (sel >= 0) {
                    int mr = table.convertRowIndexToModel(sel);
                    int headP = (int) model.getValueAt(mr, 2);
                    int bodyP = (int) model.getValueAt(mr, 3);
                    int legP = (int) model.getValueAt(mr, 4);
                    updatePartPreview(lblPreviewH, headP, "HEAD");
                    updatePartPreview(lblPreviewB, bodyP, "BODY");
                    updatePartPreview(lblPreviewL, legP, "LEG");
                    lblPreviewInfo.setText(model.getValueAt(mr, 0) + " " + model.getValueAt(mr, 1)
                        + " | H:" + headP + " B:" + bodyP + " L:" + legP);
                }
            }
        });

        p.add(bottomPanel, BorderLayout.SOUTH);
        return p;
    }

    // ==================== HỢP THỂ BÔNG TAI TAB ====================
    private JPanel createFusionTab() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBorder(new EmptyBorder(10, 10, 10, 10));
        p.setBackground(Color.WHITE);

        JLabel lblInfo = new JLabel("<html><b>💍 Hợp Thể Bông Tai (Potara Fusion)</b> — Dữ liệu từ <code>Player.idOutfitFusion</code>"
            + " | Outfit Head/Body/Leg khi hợp thể với đệ</html>");
        lblInfo.setFont(FONT_PLAIN);
        lblInfo.setBorder(new EmptyBorder(5, 5, 10, 5));
        p.add(lblInfo, BorderLayout.NORTH);

        // Fusion outfit data from Player.java
        short[][] outfits = {
            {380, 381, 382}, {383, 384, 385}, {391, 392, 393},      // LLNT: TD, Porata TD, NM
            {870, 871, 872}, {873, 874, 875}, {867, 868, 869},      // old c2
            {1834, 1835, 1836}, {1839, 1840, 1841}, {1829, 1830, 1831}, // Porata2/BTC2
            {-1, -1, -1}, {-1, -1, -1}, {-1, -1, -1},               // Porata3/BTC3 (dynamic)
            {1980, 1981, 1982}, {1946, 1947, 1948}, {1985, 1986, 1987}  // Porata4/BTC4
        };
        String[] fusionNames = {
            "LLNT - Trái Đất", "Porata C1 - Trái Đất", "LLNT - Namec",
            "LLNT C2 - Trái Đất", "LLNT C2 - Namec", "LLNT C2 - Xayda",
            "Porata C2 (BTC2) - Trái Đất", "Porata C2 (BTC2) - Namec", "Porata C2 (BTC2) - Xayda",
            "Porata C3 (BTC3) - Trái Đất [Động]", "Porata C3 (BTC3) - Namec [Động]", "Porata C3 (BTC3) - Xayda [Động]",
            "Porata C4 (BTC4) - Trái Đất", "Porata C4 (BTC4) - Namec", "Porata C4 (BTC4) - Xayda"
        };
        String[] fusionTypes = {
            "LLNT (4)", "Porata (6)", "LLNT (4)",
            "LLNT (4)", "LLNT (4)", "LLNT (4)",
            "Porata2 (8)", "Porata2 (8)", "Porata2 (8)",
            "Porata3 (10)", "Porata3 (10)", "Porata3 (10)",
            "Porata4 (11)", "Porata4 (11)", "Porata4 (11)"
        };

        String[] cols = {"#", "Loại Hợp Thể", "Type", "Head Part", "Body Part", "Leg Part", "Head", "Body", "Leg"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
            public Class<?> getColumnClass(int c) { return (c >= 6) ? ImageIcon.class : Object.class; }
        };

        for (int i = 0; i < outfits.length; i++) {
            ImageIcon h = outfits[i][0] > 0 ? getIcon(outfits[i][0], 32) : null;
            ImageIcon b = outfits[i][1] > 0 ? getIcon(outfits[i][1], 32) : null;
            ImageIcon l = outfits[i][2] > 0 ? getIcon(outfits[i][2], 32) : null;
            model.addRow(new Object[]{i, fusionNames[i], fusionTypes[i],
                outfits[i][0], outfits[i][1], outfits[i][2], h, b, l});
        }

        JTable table = new JTable(model);
        table.setRowHeight(40);
        table.setFont(FONT_PLAIN);
        table.getTableHeader().setFont(FONT_BOLD);
        table.getTableHeader().setBackground(new Color(156, 39, 176));
        table.getTableHeader().setForeground(Color.WHITE);
        table.getColumnModel().getColumn(0).setMaxWidth(30);

        // Color rows by fusion type
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(t, v, s, f, r, c);
                if (!s) {
                    int mr = t.convertRowIndexToModel(r);
                    Color[] bg = {
                        new Color(255, 248, 230), new Color(255, 248, 230), new Color(255, 248, 230),
                        new Color(230, 255, 230), new Color(230, 255, 230), new Color(230, 255, 230),
                        new Color(230, 240, 255), new Color(230, 240, 255), new Color(230, 240, 255),
                        new Color(255, 240, 245), new Color(255, 240, 245), new Color(255, 240, 245),
                        new Color(255, 255, 220), new Color(255, 255, 220), new Color(255, 255, 220)
                    };
                    setBackground(mr < bg.length ? bg[mr] : Color.WHITE);
                }
                return this;
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        p.add(scroll, BorderLayout.CENTER);

        // Bottom: Preview + DB lookup + edit
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 5));
        bottomPanel.setBackground(new Color(248, 240, 255));
        bottomPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(156, 39, 176)),
                " Preview & Tra cứu DB Item ", TitledBorder.LEFT, TitledBorder.TOP,
                FONT_BOLD, new Color(156, 39, 176)),
            new EmptyBorder(8, 8, 8, 8)));

        // Preview icons
        JPanel previewRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        previewRow.setOpaque(false);
        JLabel fHead = new JLabel(); fHead.setPreferredSize(new Dimension(80, 80));
        JLabel fBody = new JLabel(); fBody.setPreferredSize(new Dimension(80, 80));
        JLabel fLeg = new JLabel(); fLeg.setPreferredSize(new Dimension(80, 80));
        JLabel fInfo = new JLabel("← Click hàng để xem preview");
        fInfo.setFont(new Font("Segoe UI", Font.ITALIC, 12));

        previewRow.add(wrapIconWithLabel(fHead, "Head"));
        previewRow.add(wrapIconWithLabel(fBody, "Body"));
        previewRow.add(wrapIconWithLabel(fLeg, "Leg"));
        previewRow.add(fInfo);

        // DB item lookup
        JPanel lookupRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        lookupRow.setOpaque(false);
        JTextField txtLookupFusion = new JTextField(6);
        txtLookupFusion.setFont(FONT_PLAIN);
        JButton btnLookupFusion = new JButton("🔍 Tra Item BTC");
        btnLookupFusion.setFont(FONT_PLAIN);
        JLabel lblLookupResult = new JLabel("");
        lblLookupResult.setFont(FONT_PLAIN);

        btnLookupFusion.addActionListener(e -> {
            try {
                int itemId = Integer.parseInt(txtLookupFusion.getText().trim());
                try (Connection conn = DBConnecter.getConnectionServer();
                     PreparedStatement ps = conn.prepareStatement(
                         "SELECT id, name, head, body, leg, description FROM item_template WHERE id = ?")) {
                    ps.setInt(1, itemId);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        lblLookupResult.setText("<html><b>" + rs.getString("name") + "</b>"
                            + " | Head: " + rs.getInt("head") + " Body: " + rs.getInt("body") + " Leg: " + rs.getInt("leg")
                            + " | " + rs.getString("description") + "</html>");
                        updatePartPreview(fHead, rs.getInt("head"), "HEAD");
                        updatePartPreview(fBody, rs.getInt("body"), "BODY");
                        updatePartPreview(fLeg, rs.getInt("leg"), "LEG");
                    } else {
                        lblLookupResult.setText("Không tìm thấy item ID: " + itemId);
                    }
                }
            } catch (Exception ex) {
                lblLookupResult.setText("Lỗi: " + ex.getMessage());
            }
        });

        lookupRow.add(new JLabel("Item ID (VD: 1966=BTC, 2022=BTC3 VegitoGod):"));
        lookupRow.add(txtLookupFusion);
        lookupRow.add(btnLookupFusion);
        lookupRow.add(lblLookupResult);

        bottomPanel.add(previewRow, BorderLayout.NORTH);
        bottomPanel.add(lookupRow, BorderLayout.CENTER);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int sel = table.getSelectedRow();
                if (sel >= 0) {
                    int mr = table.convertRowIndexToModel(sel);
                    int h = toInt(model.getValueAt(mr, 3));
                    int b = toInt(model.getValueAt(mr, 4));
                    int l = toInt(model.getValueAt(mr, 5));
                    updatePartPreview(fHead, h, "HEAD");
                    updatePartPreview(fBody, b, "BODY");
                    updatePartPreview(fLeg, l, "LEG");
                    fInfo.setText(model.getValueAt(mr, 1).toString() + " | H:" + h + " B:" + b + " L:" + l);
                }
            }
        });

        p.add(bottomPanel, BorderLayout.SOUTH);
        return p;
    }

    // ==================== DATA LOADING ====================
    private void loadPartIcons() {
        new Thread(() -> {
            try (Connection conn = DBConnecter.getConnectionServer();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id, DATA FROM part WHERE TYPE = 0")) {
                while (rs.next()) {
                    int partId = rs.getInt("id");
                    String json = rs.getString("DATA");
                    try {
                        com.google.gson.JsonArray arr = new com.google.gson.JsonParser().parse(json).getAsJsonArray();
                        if (arr.size() > 0) {
                            com.google.gson.JsonArray first = arr.get(0).getAsJsonArray();
                            partIconMap.put(partId, first.get(0).getAsInt());
                        }
                    } catch (Exception ignored) {}
                }
            } catch (Exception e) {
                System.err.println("[ResourceBrowser] Part load error: " + e.getMessage());
            }
        }).start();
    }

    private void loadCostumeData() {
        new Thread(() -> {
            try (Connection conn = DBConnecter.getConnectionServer();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT id, NAME as name, TYPE as type, gender, head, body, leg, description FROM item_template WHERE TYPE = 5 ORDER BY id")) {
                java.util.List<Object[]> rows = new ArrayList<>();
                while (rs.next()) {
                    int id = rs.getInt("id");
                    int head = rs.getInt("head");
                    ImageIcon icon = getIcon(head, 32);
                    String genderStr = switch (rs.getInt("gender")) {
                        case 0 -> "Trái Đất";
                        case 1 -> "Namec";
                        case 2 -> "Xayda";
                        default -> "All";
                    };
                    rows.add(new Object[]{icon, id, rs.getString("name"), genderStr,
                            head, rs.getInt("body"), rs.getInt("leg"),
                            rs.getString("description")});
                }
                SwingUtilities.invokeLater(() -> {
                    modelCostume.setRowCount(0);
                    for (Object[] row : rows) modelCostume.addRow(row);
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this, "Lỗi load costume: " + e.getMessage()));
            }
        }).start();
    }

    private void loadNpcData() {
        new Thread(() -> {
            try (Connection conn = DBConnecter.getConnectionServer();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT id, NAME as name, head, body, leg, avatar FROM npc_template ORDER BY id")) {
                java.util.List<Object[]> rows = new ArrayList<>();
                while (rs.next()) {
                    int id = rs.getInt("id");
                    int avatarId = rs.getInt("avatar");
                    ImageIcon icon = getIcon(avatarId, 32);
                    rows.add(new Object[]{icon, id, rs.getString("name"),
                            rs.getInt("head"), rs.getInt("body"), rs.getInt("leg"),
                            avatarId, "-"});
                }
                SwingUtilities.invokeLater(() -> {
                    modelNpc.setRowCount(0);
                    for (Object[] row : rows) modelNpc.addRow(row);
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this, "Lỗi load NPC: " + e.getMessage()));
            }
        }).start();
    }

    private void loadItemData() {
        new Thread(() -> {
            try (Connection conn = DBConnecter.getConnectionServer();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT id, NAME as name, TYPE as type, gender, level, part, description FROM item_template ORDER BY id")) {
                java.util.List<Object[]> rows = new ArrayList<>();
                while (rs.next()) {
                    int id = rs.getInt("id");
                    int part = rs.getInt("part");
                    ImageIcon icon = getIcon(part >= 0 ? partIconMap.getOrDefault(part, part) : id, 28);
                    String genderStr = switch (rs.getInt("gender")) {
                        case 0 -> "Trái Đất";
                        case 1 -> "Namec";
                        case 2 -> "Xayda";
                        default -> "All";
                    };
                    rows.add(new Object[]{icon, id, rs.getString("name"), rs.getInt("type"),
                            genderStr, rs.getInt("level"), part, rs.getString("description")});
                }
                SwingUtilities.invokeLater(() -> {
                    modelItem.setRowCount(0);
                    for (Object[] row : rows) modelItem.addRow(row);
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this, "Lỗi load items: " + e.getMessage()));
            }
        }).start();
    }

    private Vector<String> allBossFiles = new Vector<>();

    private void loadBossFiles() {
        allBossFiles.clear();
        File dir = new File("src/boss/boss_manifest");
        if (dir.exists()) scanJavaFiles(dir, "");
        listBossFiles.setListData(allBossFiles);
    }

    private void scanJavaFiles(File dir, String prefix) {
        File[] files = dir.listFiles();
        if (files == null) return;
        Arrays.sort(files);
        for (File f : files) {
            if (f.isDirectory()) {
                scanJavaFiles(f, prefix + f.getName() + "/");
            } else if (f.getName().endsWith(".java")) {
                allBossFiles.add(prefix + f.getName());
            }
        }
    }

    private void loadBossRewardSource(String relativePath) {
        File f = new File("src/boss/boss_manifest/" + relativePath);
        if (!f.exists()) {
            txtBossSource.setText("File không tồn tại: " + f.getAbsolutePath());
            return;
        }
        try {
            String content = new String(java.nio.file.Files.readAllBytes(f.toPath()), java.nio.charset.StandardCharsets.UTF_8);

            // Extract reward method and related drop logic
            StringBuilder sb = new StringBuilder();
            sb.append("// ============ FILE: ").append(relativePath).append(" ============\n");
            sb.append("// Kích thước: ").append(f.length()).append(" bytes\n\n");

            // Find reward() method
            int rewardIdx = content.indexOf("void reward(");
            if (rewardIdx >= 0) {
                sb.append("// ====== PHẦN THƯỞNG (reward method) ======\n");
                String rewardBlock = extractMethodBlock(content, rewardIdx);
                sb.append(rewardBlock).append("\n\n");
            } else {
                sb.append("// ⚠ KHÔNG TÌM THẤY method reward() → Boss này có thể dùng reward mặc định từ Boss.java\n\n");
            }

            // Find injured() method
            int injuredIdx = content.indexOf("injured(");
            if (injuredIdx >= 0) {
                sb.append("// ====== XỬ LÝ SÁT THƯƠNG (injured method) ======\n");
                String injuredBlock = extractMethodBlock(content, injuredIdx);
                sb.append(injuredBlock).append("\n\n");
            }

            // Find dropItem or ItemMap references
            if (content.contains("ItemMap")) {
                sb.append("// ====== CÓ SỬ DỤNG ItemMap (drop vật phẩm) ======\n");
            } else {
                sb.append("// ⚠ KHÔNG SỬ DỤNG ItemMap → Boss này KHÔNG drop vật phẩm trực tiếp!\n");
            }

            // Show full source
            sb.append("\n\n// ==================== FULL SOURCE CODE ====================\n\n");
            sb.append(content);

            txtBossSource.setText(sb.toString());
            txtBossSource.setCaretPosition(0);
        } catch (Exception e) {
            txtBossSource.setText("Lỗi đọc file: " + e.getMessage());
        }
    }

    private String extractMethodBlock(String content, int startIdx) {
        // Find the start of the method line
        int lineStart = content.lastIndexOf('\n', startIdx);
        if (lineStart < 0) lineStart = 0;

        // Find matching braces
        int braceStart = content.indexOf('{', startIdx);
        if (braceStart < 0) return content.substring(lineStart, Math.min(lineStart + 200, content.length()));

        int depth = 0;
        int i = braceStart;
        while (i < content.length()) {
            if (content.charAt(i) == '{') depth++;
            else if (content.charAt(i) == '}') {
                depth--;
                if (depth == 0) {
                    return content.substring(lineStart, i + 1).trim();
                }
            }
            i++;
        }
        return content.substring(lineStart, Math.min(braceStart + 500, content.length()));
    }

    // ==================== FILTERS ====================
    private void filterItems() {
        java.util.List<RowFilter<DefaultTableModel, Object>> filters = new ArrayList<>();
        String searchText = txtSearchItem.getText().trim();
        if (!searchText.isEmpty()) {
            filters.add(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(searchText), 1, 2));
        }
        String typeStr = (String) cboItemType.getSelectedItem();
        if (typeStr != null && !typeStr.equals("Tất cả")) {
            String typeNum = typeStr.split("-")[0];
            filters.add(RowFilter.regexFilter("^" + typeNum + "$", 3));
        }
        if (filters.isEmpty()) sorterItem.setRowFilter(null);
        else sorterItem.setRowFilter(RowFilter.andFilter(filters));
    }

    private void filterBossFiles() {
        String q = txtSearchBoss.getText().trim().toLowerCase();
        if (q.isEmpty()) {
            listBossFiles.setListData(allBossFiles);
        } else {
            Vector<String> filtered = new Vector<>();
            for (String s : allBossFiles) {
                if (s.toLowerCase().contains(q)) filtered.add(s);
            }
            listBossFiles.setListData(filtered);
        }
    }

    private void addSearchFilter(JTextField txt, TableRowSorter<DefaultTableModel> sorter, int[] cols) {
        txt.getDocument().addDocumentListener(new SimpleDocumentListener() {
            public void update() {
                String q = txt.getText().trim();
                if (q.isEmpty()) sorter.setRowFilter(null);
                else sorter.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(q), cols));
            }
        });
    }

    // ==================== UTILS ====================
    private ImageIcon getIcon(int iconId, int size) {
        if (iconId <= -1) return null;
        int cacheKey = iconId * 1000 + size;
        if (iconCache.containsKey(cacheKey)) return iconCache.get(cacheKey);
        try {
            String[] zooms = {"x4", "x3", "x2", "x1"};
            for (String z : zooms) {
                File f = new File(ICON_FOLDER + z + "/" + iconId + ".png");
                if (f.exists()) {
                    BufferedImage img = ImageIO.read(f);
                    ImageIcon icon = new ImageIcon(img.getScaledInstance(size, size, Image.SCALE_SMOOTH));
                    iconCache.put(cacheKey, icon);
                    return icon;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private JButton createBtn(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BOLD);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }
}
