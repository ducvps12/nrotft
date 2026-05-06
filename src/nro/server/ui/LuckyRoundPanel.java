package nro.server.ui;

import services.func.LuckyRoundConfig;
import services.func.LuckyRoundConfig.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.List;

public class LuckyRoundPanel extends JPanel {

    private JTabbedPane tabs;
    private JTextField tfPriceGold, tfPriceGem, tfPriceTicket, tfTicketId, tfGoldMin, tfGoldMax;
    private TierTablePanel vipPanel, normalPanel;

    public LuckyRoundPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // Header
        JLabel header = new JLabel("  🎰 Quản Lý Vòng Quay Thượng Đế");
        header.setFont(new Font("Segoe UI", Font.BOLD, 18));
        header.setForeground(new Color(0, 102, 204));
        header.setBorder(new EmptyBorder(15, 10, 10, 0));
        add(header, BorderLayout.NORTH);

        tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));

        // Tab 1: Cấu hình chung
        tabs.addTab("⚙ Cấu Hình Chung", buildConfigTab());
        // Tab 2: VIP Tiers
        vipPanel = new TierTablePanel(true);
        tabs.addTab("🏆 Quay VIP (Thỏi Vàng)", vipPanel);
        // Tab 3: Normal Tiers
        normalPanel = new TierTablePanel(false);
        tabs.addTab("🎲 Quay Thường (Vàng/Ngọc)", normalPanel);

        add(tabs, BorderLayout.CENTER);

        // Bottom buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        btnPanel.setBackground(Color.WHITE);

        JButton btnReload = ServerGuiUtils.createStyledButton("🔄 Tải lại từ file", new Color(52, 152, 219), Color.WHITE);
        btnReload.addActionListener(e -> reloadConfig());

        JButton btnSave = ServerGuiUtils.createStyledButton("💾 Lưu & Áp dụng", new Color(40, 167, 69), Color.WHITE);
        btnSave.addActionListener(e -> saveConfig());

        btnPanel.add(btnReload);
        btnPanel.add(btnSave);
        add(btnPanel, BorderLayout.SOUTH);

        loadFromConfig();
    }

    // ========== Tab Cấu hình chung ==========
    private JScrollPane buildConfigTab() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Giá quay
        JPanel pricePanel = createSection("💰 Giá Quay");
        tfPriceGold = addField(pricePanel, "Giá quay Vàng:", "25000000");
        tfPriceGem = addField(pricePanel, "Giá quay Ngọc:", "4");
        tfPriceTicket = addField(pricePanel, "Số Thỏi Vàng/lượt:", "1");
        tfTicketId = addField(pricePanel, "Item ID Thỏi Vàng:", "457");
        p.add(pricePanel);
        p.add(Box.createRigidArea(new Dimension(0, 15)));

        // Vàng mặc định
        JPanel goldPanel = createSection("🪙 Vàng Mặc Định (khi không trúng tier)");
        tfGoldMin = addField(goldPanel, "Vàng tối thiểu:", "5000");
        tfGoldMax = addField(goldPanel, "Vàng tối đa:", "50000");
        p.add(goldPanel);
        p.add(Box.createRigidArea(new Dimension(0, 15)));

        // Hướng dẫn
        JPanel helpPanel = createSection("📖 Hướng Dẫn");
        JTextArea helpText = new JTextArea(
            "• Tỉ lệ: Tử số / Mẫu số (VD: 1/10000 = 0.01%)\n"
            + "• Các tier check theo thứ tự từ trên xuống (if-else cascade)\n"
            + "• Tier đầu tiên trúng sẽ dừng, không check tiếp\n"
            + "• Weight: Trọng số random giữa các item trong cùng 1 tier\n"
            + "• Options: Chỉ số item, format: optionId:min-max (mỗi dòng 1 option)\n"
            + "• Bấm 'Lưu & Áp dụng' để cập nhật ngay, không cần restart server"
        );
        helpText.setEditable(false);
        helpText.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        helpText.setBackground(new Color(248, 249, 250));
        helpText.setBorder(new EmptyBorder(10, 10, 10, 10));
        helpPanel.add(helpText);
        p.add(helpPanel);

        return new JScrollPane(p);
    }

    // ========== TIER TABLE PANEL ==========
    private class TierTablePanel extends JPanel {
        private final boolean isVip;
        private JPanel tiersContainer;

        TierTablePanel(boolean isVip) {
            this.isVip = isVip;
            setLayout(new BorderLayout());
            setBackground(Color.WHITE);

            // Toolbar
            JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
            toolbar.setBackground(Color.WHITE);
            JButton btnAdd = ServerGuiUtils.createStyledButton("➕ Thêm Tier", new Color(0, 123, 255), Color.WHITE);
            btnAdd.addActionListener(e -> addNewTier());
            toolbar.add(btnAdd);
            add(toolbar, BorderLayout.NORTH);

            tiersContainer = new JPanel();
            tiersContainer.setLayout(new BoxLayout(tiersContainer, BoxLayout.Y_AXIS));
            tiersContainer.setBackground(Color.WHITE);
            add(new JScrollPane(tiersContainer), BorderLayout.CENTER);
        }

        void loadTiers(List<RewardTier> tiers) {
            tiersContainer.removeAll();
            for (int i = 0; i < tiers.size(); i++) {
                tiersContainer.add(new TierEditorCard(tiers.get(i), i, this));
                tiersContainer.add(Box.createRigidArea(new Dimension(0, 5)));
            }
            tiersContainer.revalidate();
            tiersContainer.repaint();
        }

        List<RewardTier> getTiers() {
            return isVip ? LuckyRoundConfig.gI().getVipTiers() : LuckyRoundConfig.gI().getNormalTiers();
        }

        void addNewTier() {
            RewardTier tier = new RewardTier("Tier mới", 1, 1000, true, false, "");
            getTiers().add(tier);
            loadTiers(getTiers());
        }

        void removeTier(int index) {
            if (index >= 0 && index < getTiers().size()) {
                getTiers().remove(index);
                loadTiers(getTiers());
            }
        }

        void moveTier(int from, int to) {
            List<RewardTier> list = getTiers();
            if (from >= 0 && from < list.size() && to >= 0 && to < list.size()) {
                RewardTier t = list.remove(from);
                list.add(to, t);
                loadTiers(list);
            }
        }
    }

    // ========== TIER EDITOR CARD ==========
    private class TierEditorCard extends JPanel {
        private final RewardTier tier;
        private JTextField tfName, tfNum, tfDen, tfPrefix;
        private JCheckBox cbEnabled, cbAnnounce;
        private DefaultTableModel itemModel;

        TierEditorCard(RewardTier tier, int index, TierTablePanel parent) {
            this.tier = tier;
            setLayout(new BorderLayout(5, 5));
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(230, 230, 230)),
                new EmptyBorder(8, 12, 8, 12)
            ));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));

            // === TOP: Tier info ===
            JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));
            top.setBackground(Color.WHITE);

            cbEnabled = new JCheckBox("", tier.enabled);
            top.add(cbEnabled);

            top.add(new JLabel("Tên:"));
            tfName = new JTextField(tier.name, 18);
            tfName.setFont(new Font("Segoe UI", Font.BOLD, 12));
            top.add(tfName);

            top.add(new JLabel("Tỉ lệ:"));
            tfNum = new JTextField(String.valueOf(tier.ratioNumerator), 4);
            top.add(tfNum);
            top.add(new JLabel("/"));
            tfDen = new JTextField(String.valueOf(tier.ratioDenominator), 6);
            top.add(tfDen);

            JLabel pctLabel = new JLabel(String.format("(%.4f%%)", tier.getPercentage()));
            pctLabel.setForeground(new Color(220, 53, 69));
            pctLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
            top.add(pctLabel);

            // Update % khi thay đổi tỉ lệ
            Runnable updatePct = () -> {
                try {
                    int n = Integer.parseInt(tfNum.getText().trim());
                    int d = Integer.parseInt(tfDen.getText().trim());
                    if (d > 0) pctLabel.setText(String.format("(%.4f%%)", (double) n / d * 100));
                } catch (Exception ignored) {}
            };
            tfNum.addActionListener(e -> updatePct.run());
            tfDen.addActionListener(e -> updatePct.run());

            cbAnnounce = new JCheckBox("TB Server", tier.announce);
            top.add(cbAnnounce);
            tfPrefix = new JTextField(tier.announcePrefix != null ? tier.announcePrefix : "", 8);
            top.add(tfPrefix);

            // Buttons
            JButton btnUp = new JButton("▲");
            btnUp.setMargin(new Insets(1, 4, 1, 4));
            btnUp.addActionListener(e -> parent.moveTier(index, index - 1));
            JButton btnDown = new JButton("▼");
            btnDown.setMargin(new Insets(1, 4, 1, 4));
            btnDown.addActionListener(e -> parent.moveTier(index, index + 1));
            JButton btnDel = new JButton("✕");
            btnDel.setForeground(Color.RED);
            btnDel.setMargin(new Insets(1, 4, 1, 4));
            btnDel.addActionListener(e -> {
                if (JOptionPane.showConfirmDialog(this, "Xóa tier '" + tier.name + "'?", "Xác nhận", JOptionPane.YES_NO_OPTION) == 0)
                    parent.removeTier(index);
            });
            top.add(btnUp);
            top.add(btnDown);
            top.add(btnDel);

            add(top, BorderLayout.NORTH);

            // === CENTER: Item table ===
            String[] cols = {"Item ID", "Tên", "SL Min", "SL Max", "Weight", "Options (id:min-max mỗi dòng)"};
            itemModel = new DefaultTableModel(cols, 0) {
                @Override public boolean isCellEditable(int r, int c) { return true; }
            };
            for (RewardItem item : tier.items) {
                StringBuilder opts = new StringBuilder();
                for (ItemOptionDef o : item.options) {
                    if (opts.length() > 0) opts.append("; ");
                    opts.append(o.optionId).append(":").append(o.valueMin).append("-").append(o.valueMax);
                }
                itemModel.addRow(new Object[]{item.itemId, item.itemName, item.quantityMin, item.quantityMax, item.weight, opts.toString()});
            }
            JTable table = new JTable(itemModel);
            table.setRowHeight(24);
            table.setFont(new Font("Consolas", Font.PLAIN, 12));
            table.getColumnModel().getColumn(0).setPreferredWidth(60);
            table.getColumnModel().getColumn(1).setPreferredWidth(140);
            table.getColumnModel().getColumn(5).setPreferredWidth(250);
            JScrollPane sp = new JScrollPane(table);
            sp.setPreferredSize(new Dimension(0, Math.min(120, 24 + tier.items.size() * 24)));
            add(sp, BorderLayout.CENTER);

            // Item buttons
            JPanel itemBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
            itemBtns.setBackground(Color.WHITE);
            JButton btnAddItem = new JButton("+ Thêm Item");
            btnAddItem.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            btnAddItem.addActionListener(e -> itemModel.addRow(new Object[]{0, "Item mới", 1, 1, 10, ""}));
            JButton btnDelItem = new JButton("- Xóa Item");
            btnDelItem.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            btnDelItem.addActionListener(e -> {
                int row = table.getSelectedRow();
                if (row >= 0) itemModel.removeRow(row);
            });
            itemBtns.add(btnAddItem);
            itemBtns.add(btnDelItem);
            add(itemBtns, BorderLayout.SOUTH);
        }

        /** Đọc dữ liệu từ UI vào tier object */
        void applyToTier() {
            tier.name = tfName.getText().trim();
            try { tier.ratioNumerator = Integer.parseInt(tfNum.getText().trim()); } catch (Exception e) {}
            try { tier.ratioDenominator = Integer.parseInt(tfDen.getText().trim()); } catch (Exception e) {}
            tier.enabled = cbEnabled.isSelected();
            tier.announce = cbAnnounce.isSelected();
            tier.announcePrefix = tfPrefix.getText().trim();

            tier.items.clear();
            for (int r = 0; r < itemModel.getRowCount(); r++) {
                try {
                    RewardItem item = new RewardItem();
                    item.itemId = Integer.parseInt(itemModel.getValueAt(r, 0).toString().trim());
                    item.itemName = itemModel.getValueAt(r, 1).toString().trim();
                    item.quantityMin = Integer.parseInt(itemModel.getValueAt(r, 2).toString().trim());
                    item.quantityMax = Integer.parseInt(itemModel.getValueAt(r, 3).toString().trim());
                    item.weight = Integer.parseInt(itemModel.getValueAt(r, 4).toString().trim());
                    // Parse options
                    String optStr = itemModel.getValueAt(r, 5).toString().trim();
                    if (!optStr.isEmpty()) {
                        for (String part : optStr.split("[;,]")) {
                            part = part.trim();
                            if (part.contains(":")) {
                                String[] kv = part.split(":");
                                int optId = Integer.parseInt(kv[0].trim());
                                String[] range = kv[1].split("-");
                                int min = Integer.parseInt(range[0].trim());
                                int max = range.length > 1 ? Integer.parseInt(range[1].trim()) : min;
                                item.options.add(new ItemOptionDef(optId, min, max));
                            }
                        }
                    }
                    tier.items.add(item);
                } catch (Exception ex) {
                    System.err.println("[LuckyRoundPanel] Parse error row " + r + ": " + ex.getMessage());
                }
            }
        }
    }

    // ========== HELPERS ==========
    private JPanel createSection(String title) {
        JPanel p = new JPanel(new GridLayout(0, 2, 10, 8));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), title, TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 14), new Color(0, 102, 204)));
        return p;
    }

    private JTextField addField(JPanel parent, String label, String value) {
        parent.add(new JLabel(label));
        JTextField tf = new JTextField(value);
        tf.setFont(new Font("Consolas", Font.PLAIN, 13));
        parent.add(tf);
        return tf;
    }

    // ========== LOAD / SAVE ==========
    private void loadFromConfig() {
        LuckyRoundConfig cfg = LuckyRoundConfig.gI();
        tfPriceGold.setText(String.valueOf(cfg.getPriceGold()));
        tfPriceGem.setText(String.valueOf(cfg.getPriceGem()));
        tfPriceTicket.setText(String.valueOf(cfg.getPriceTicket()));
        tfTicketId.setText(String.valueOf(cfg.getTicketItemId()));
        tfGoldMin.setText(String.valueOf(cfg.getDefaultGoldMin()));
        tfGoldMax.setText(String.valueOf(cfg.getDefaultGoldMax()));
        vipPanel.loadTiers(cfg.getVipTiers());
        normalPanel.loadTiers(cfg.getNormalTiers());
    }

    private void reloadConfig() {
        LuckyRoundConfig.gI().reload();
        loadFromConfig();
        JOptionPane.showMessageDialog(this, "Đã tải lại cấu hình từ file!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
    }

    private void saveConfig() {
        LuckyRoundConfig cfg = LuckyRoundConfig.gI();
        try {
            cfg.setPriceGold(Integer.parseInt(tfPriceGold.getText().trim()));
            cfg.setPriceGem(Integer.parseInt(tfPriceGem.getText().trim()));
            cfg.setPriceTicket(Integer.parseInt(tfPriceTicket.getText().trim()));
            cfg.setTicketItemId(Integer.parseInt(tfTicketId.getText().trim()));
            cfg.setDefaultGoldMin(Integer.parseInt(tfGoldMin.getText().trim()));
            cfg.setDefaultGoldMax(Integer.parseInt(tfGoldMax.getText().trim()));
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Lỗi: Giá trị phải là số!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Apply all tier cards
        applyAllCards(vipPanel.tiersContainer);
        applyAllCards(normalPanel.tiersContainer);

        cfg.save();
        JOptionPane.showMessageDialog(this, "✅ Đã lưu cấu hình vòng quay!\nÁp dụng ngay, không cần restart.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
    }

    private void applyAllCards(JPanel container) {
        for (Component c : container.getComponents()) {
            if (c instanceof TierEditorCard card) {
                card.applyToTier();
            }
        }
    }
}
