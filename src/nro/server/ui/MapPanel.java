package nro.server.ui;

import jdbc.DBConnecter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import java.util.Vector;

public class MapPanel extends JPanel {

    private JTable table;
    private DefaultTableModel model;
    private TableRowSorter<DefaultTableModel> sorter;
    private JTextField txtSearch;

    public MapPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(245, 245, 245));
        setBorder(new EmptyBorder(15, 15, 15, 15));
        initUI();
        loadDataFromDB();
    }

    private Connection getConnection() throws SQLException {
        return DBConnecter.getConnectionServer();
    }

    private void initUI() {

        JLabel title = new JLabel("QUẢN LÝ BẢN ĐỒ");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(new Color(0, 102, 204));

        JPanel topLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        topLeft.setOpaque(false);
        topLeft.add(title);

        JPanel topRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        topRight.setOpaque(false);

        txtSearch = new JTextField(25);
        txtSearch.setPreferredSize(new Dimension(260, 32));
        txtSearch.addActionListener(e -> search());

        JButton btnSearch = createButton("Tìm Kiếm", new Color(0, 123, 255));
        JButton btnReload = createButton("Làm Mới Cache", new Color(108, 117, 125));

        btnSearch.addActionListener(e -> search());
        btnReload.addActionListener(e -> loadDataFromDB());

        topRight.add(txtSearch);
        topRight.add(btnSearch);
        topRight.add(btnReload);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(topLeft, BorderLayout.WEST);
        top.add(topRight, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);

        String[] cols = {
                "ID",
                "Tên Bản Đồ",
                "Hành Tinh",
                "Khu Vực",
                "Max Player",
                "Mobs",
                "NPCs",
                "MOBS_DATA",
                "NPCS_DATA"
        };

        model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        table = new JTable(model);
        table.setRowHeight(34);
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

        DefaultTableCellRenderer left = new DefaultTableCellRenderer();
        left.setHorizontalAlignment(JLabel.LEFT);

        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(0).setMaxWidth(70);
        table.getColumnModel().getColumn(0).setCellRenderer(center);

        table.getColumnModel().getColumn(1).setPreferredWidth(260);
        table.getColumnModel().getColumn(1).setCellRenderer(left);

        table.getColumnModel().getColumn(2).setPreferredWidth(120);
        table.getColumnModel().getColumn(2).setCellRenderer(center);

        table.getColumnModel().getColumn(3).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setCellRenderer(center);

        table.getColumnModel().getColumn(4).setPreferredWidth(110);
        table.getColumnModel().getColumn(4).setCellRenderer(center);

        table.getColumnModel().getColumn(5).setPreferredWidth(90);
        table.getColumnModel().getColumn(5).setCellRenderer(center);

        table.getColumnModel().getColumn(6).setPreferredWidth(90);
        table.getColumnModel().getColumn(6).setCellRenderer(center);

        table.getColumnModel().getColumn(7).setMinWidth(0);
        table.getColumnModel().getColumn(7).setMaxWidth(0);
        table.getColumnModel().getColumn(7).setWidth(0);

        table.getColumnModel().getColumn(8).setMinWidth(0);
        table.getColumnModel().getColumn(8).setMaxWidth(0);
        table.getColumnModel().getColumn(8).setWidth(0);

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
                String id = entry.getStringValue(0).toLowerCase();
                String name = entry.getStringValue(1).toLowerCase();
                return id.contains(key) || name.contains(key);
            }
        });
    }

    private void loadDataFromDB() {
        model.setRowCount(0);

        new Thread(() -> {
            try (Connection conn = getConnection();
                    Statement st = conn.createStatement();
                    ResultSet rs = st.executeQuery("SELECT * FROM map_template ORDER BY id ASC")) {

                while (rs.next()) {

                    String mobs = rs.getString("mobs");
                    String npcs = rs.getString("npcs");

                    Vector<Object> row = new Vector<>();
                    row.add(rs.getInt("id"));
                    row.add(rs.getString("name"));
                    row.add(getPlanetName(rs.getInt("planet_id")));
                    row.add(rs.getInt("zones"));
                    row.add(rs.getInt("max_player"));
                    row.add(countArray(mobs));
                    row.add(countArray(npcs));
                    row.add(mobs);
                    row.add(npcs);

                    SwingUtilities.invokeLater(() -> model.addRow(row));
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void openEditor(int rowIndex) {

        int id = (int) model.getValueAt(rowIndex, 0);

        JTextField txtName = new JTextField(model.getValueAt(rowIndex, 1).toString());
        JTextField txtZone = new JTextField(model.getValueAt(rowIndex, 3).toString());
        JTextField txtMax = new JTextField(model.getValueAt(rowIndex, 4).toString());

        JTextArea txtMobs = new JTextArea(model.getValueAt(rowIndex, 7).toString(), 6, 40);
        JTextArea txtNpcs = new JTextArea(model.getValueAt(rowIndex, 8).toString(), 6, 40);

        txtMobs.setLineWrap(true);
        txtMobs.setWrapStyleWord(true);
        txtNpcs.setLineWrap(true);
        txtNpcs.setWrapStyleWord(true);

        JScrollPane spMobs = new JScrollPane(txtMobs);
        JScrollPane spNpcs = new JScrollPane(txtNpcs);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(8, 8, 8, 8);
        g.fill = GridBagConstraints.HORIZONTAL;

        g.gridx = 0;
        g.gridy = 0;
        panel.add(new JLabel("Tên bản đồ"), g);
        g.gridx = 1;
        g.weightx = 1;
        panel.add(txtName, g);

        g.gridx = 0;
        g.gridy++;
        g.weightx = 0;
        panel.add(new JLabel("Khu vực"), g);
        g.gridx = 1;
        g.weightx = 1;
        panel.add(txtZone, g);

        g.gridx = 0;
        g.gridy++;
        g.weightx = 0;
        panel.add(new JLabel("Max player"), g);
        g.gridx = 1;
        g.weightx = 1;
        panel.add(txtMax, g);

        g.gridx = 0;
        g.gridy++;
        g.weightx = 0;
        g.anchor = GridBagConstraints.NORTH;
        panel.add(new JLabel("Mobs"), g);
        g.gridx = 1;
        g.weightx = 1;
        g.fill = GridBagConstraints.BOTH;
        panel.add(spMobs, g);

        g.gridx = 0;
        g.gridy++;
        g.weightx = 0;
        panel.add(new JLabel("NPCs"), g);
        g.gridx = 1;
        g.weightx = 1;
        panel.add(spNpcs, g);

        panel.setPreferredSize(new Dimension(600, 420));

        int result = JOptionPane.showConfirmDialog(
                this,
                panel,
                "Chỉnh sửa Map ID " + id,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION)
            return;

        new Thread(() -> {
            try (Connection conn = getConnection();
                    PreparedStatement ps = conn.prepareStatement(
                            "UPDATE map_template SET name=?, zones=?, max_player=?, mobs=?, npcs=? WHERE id=?")) {

                ps.setString(1, txtName.getText());
                ps.setInt(2, Integer.parseInt(txtZone.getText()));
                ps.setInt(3, Integer.parseInt(txtMax.getText()));
                ps.setString(4, txtMobs.getText());
                ps.setString(5, txtNpcs.getText());
                ps.setInt(6, id);

                ps.executeUpdate();

                SwingUtilities.invokeLater(this::loadDataFromDB);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private String getPlanetName(int id) {
        switch (id) {
            case 0:
                return "Trái Đất";
            case 1:
                return "Namec";
            case 2:
                return "Xayda";
            default:
                return "Unknown";
        }
    }

    private int countArray(String json) {
        if (json == null || json.length() < 2)
            return 0;
        int count = 0;
        for (char c : json.toCharArray())
            if (c == '[')
                count++;
        return count;
    }
}