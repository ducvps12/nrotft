package nro.server.ui;

import consts.ConstDataEventSM;
import consts.ConstDataEventTOP;
import nro.server.Manager;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class EventTimeManagerPanel extends JPanel {

    private JTextField txtYear;
    private JTextField txtMonthOpen, txtDateOpen, txtHourOpen, txtMinOpen;
    private JTextField txtMonthEnd, txtDateEnd, txtHourEnd, txtMinEnd;
    private JTextField txtMonthRew, txtDateRew, txtHourRew, txtMinRew;

    public EventTimeManagerPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // --- Group: Cấu hình chung ---
        JPanel commonPanel = createGroupPanel("Cấu hình Năm Hệ Thống");
        txtYear = addInputRow(commonPanel, "Năm diễn ra sự kiện:", String.valueOf(ConstDataEventSM.YEAR_EVENT));
        mainPanel.add(commonPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        // --- Group: Đua Top Đại Thiên Sứ ---
        JPanel topEventPanel = createGroupPanel("Chi tiết thời gian Đua TOP");
        
        // Bắt đầu
        addHeader(topEventPanel, "--- THỜI GIAN BẮT ĐẦU ---");
        txtMonthOpen = addInputRow(topEventPanel, "Tháng mở:", String.valueOf(ConstDataEventTOP.MONTH_OPEN));
        txtDateOpen = addInputRow(topEventPanel, "Ngày mở:", String.valueOf(ConstDataEventTOP.DATE_OPEN));
        txtHourOpen = addInputRow(topEventPanel, "Giờ mở:", String.valueOf(ConstDataEventTOP.HOUR_OPEN));
        txtMinOpen = addInputRow(topEventPanel, "Phút mở:", String.valueOf(ConstDataEventTOP.MIN_OPEN));

        // Kết thúc
        addHeader(topEventPanel, "--- THỜI GIAN KẾT THÚC ---");
        txtMonthEnd = addInputRow(topEventPanel, "Tháng kết thúc:", String.valueOf(ConstDataEventTOP.MONTH_END));
        txtDateEnd = addInputRow(topEventPanel, "Ngày kết thúc:", String.valueOf(ConstDataEventTOP.DATE_END));
        txtHourEnd = addInputRow(topEventPanel, "Giờ kết thúc:", String.valueOf(ConstDataEventTOP.HOUR_END));
        txtMinEnd = addInputRow(topEventPanel, "Phút kết thúc:", String.valueOf(ConstDataEventTOP.MIN_END));

        // Nhận thưởng
        addHeader(topEventPanel, "--- THỜI GIAN NHẬN THƯỞNG ---");
        txtMonthRew = addInputRow(topEventPanel, "Tháng nhận thưởng:", String.valueOf(ConstDataEventTOP.MONTH_REWARD));
        txtDateRew = addInputRow(topEventPanel, "Ngày nhận thưởng:", String.valueOf(ConstDataEventTOP.DATE_REWARD));
        txtHourRew = addInputRow(topEventPanel, "Giờ nhận thưởng:", String.valueOf(ConstDataEventTOP.HOUR_REWARD));
        txtMinRew = addInputRow(topEventPanel, "Phút nhận thưởng:", String.valueOf(ConstDataEventTOP.MIN_REWARD));

        mainPanel.add(topEventPanel);

        // --- Nút Lưu ---
        JButton btnSave = new JButton("Lưu & Đồng Bộ Toàn Máy Chủ");
        btnSave.setBackground(new Color(40, 167, 69)); // Màu xanh lá cho an tâm
        btnSave.setForeground(Color.WHITE);
        btnSave.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnSave.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSave.addActionListener(e -> saveConfig());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setBackground(Color.WHITE);
        btnPanel.add(btnSave);

        add(new JScrollPane(mainPanel), BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);
    }

    private void addHeader(JPanel p, String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 11));
        label.setForeground(new Color(120, 120, 120));
        p.add(label);
        p.add(new JLabel("")); 
    }

    private JPanel createGroupPanel(String title) {
        JPanel p = new JPanel(new GridLayout(0, 2, 10, 8));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), title, TitledBorder.LEFT, TitledBorder.TOP, 
                new Font("Segoe UI", Font.BOLD, 14), new Color(0, 102, 204)));
        return p;
    }

    private JTextField addInputRow(JPanel parent, String label, String value) {
        parent.add(new JLabel(label));
        JTextField tf = new JTextField(value);
        tf.setFont(new Font("Consolas", Font.PLAIN, 13));
        parent.add(tf);
        return tf;
    }

    private void saveConfig() {
        try {
            // 0. Cập nhật NĂM
            ConstDataEventSM.YEAR_EVENT = Short.parseShort(txtYear.getText());

            // 1. Cập nhật RAM - Mở
            ConstDataEventTOP.MONTH_OPEN = Short.parseShort(txtMonthOpen.getText());
            ConstDataEventTOP.DATE_OPEN = Short.parseShort(txtDateOpen.getText());
            ConstDataEventTOP.HOUR_OPEN = Short.parseShort(txtHourOpen.getText());
            ConstDataEventTOP.MIN_OPEN = Short.parseShort(txtMinOpen.getText());

            // 2. Cập nhật RAM - Kết thúc
            ConstDataEventTOP.MONTH_END = Short.parseShort(txtMonthEnd.getText());
            ConstDataEventTOP.DATE_END = Short.parseShort(txtDateEnd.getText());
            ConstDataEventTOP.HOUR_END = Short.parseShort(txtHourEnd.getText());
            ConstDataEventTOP.MIN_END = Short.parseShort(txtMinEnd.getText());

            // 3. Cập nhật RAM - Nhận thưởng
            ConstDataEventTOP.MONTH_REWARD = Short.parseShort(txtMonthRew.getText());
            ConstDataEventTOP.DATE_REWARD = Short.parseShort(txtDateRew.getText());
            ConstDataEventTOP.HOUR_REWARD = Short.parseShort(txtHourRew.getText());
            ConstDataEventTOP.MIN_REWARD = Short.parseShort(txtMinRew.getText());

            // 4. Ghi file vật lý
            Manager.saveProperties();

            JOptionPane.showMessageDialog(this, "Đã lưu thành công năm " + ConstDataEventSM.YEAR_EVENT + " và các mốc sự kiện!");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi: Dữ liệu nhập vào phải là số!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
}