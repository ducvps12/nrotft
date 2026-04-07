package nro.server.ui;

import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.geom.*;
import java.net.URL;

public class ServerGuiUtils {

    public static void setupTheme() {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
            UIManager.put("Component.arc", 10);
            UIManager.put("Button.arc", 10);
            UIManager.put("TextComponent.arc", 8);
            UIManager.put("ScrollBar.width", 10);
            UIManager.put("TabbedPane.selectedBackground", new Color(230, 242, 255));
            UIManager.put("TabbedPane.focusColor", new Color(0, 120, 215));
        } catch (Exception e) {
            System.err.println("Failed to initialize LaF: " + e.getMessage());
        }
    }

    public static TitledBorder createSectionBorder(String title) {
        return BorderFactory.createTitledBorder(
                new LineBorder(new Color(220, 220, 220)), title,
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12), Color.DARK_GRAY);
    }

    public static JButton createStyledButton(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFocusPainted(false);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(
            b.getFontMetrics(b.getFont()).stringWidth(text) + 30,
            32
        ));
        return b;
    }

    public static JLabel createStyledLabel(String text, int size, boolean bold) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", bold ? Font.BOLD : Font.PLAIN, size));
        return l;
    }

    public static Icon loadIcon(String path) {
        try {
            URL url = ServerGuiUtils.class.getResource(path);
            if (url != null) {
                ImageIcon icon = new ImageIcon(url);
                Image img = icon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
                return new ImageIcon(img);
            }
        } catch (Exception ignored) {
        }
        return createPremiumIcon(path);
    }

    /**
     * Tạo icon premium với gradient, bo tròn, shadow
     * Thay thế emoji fallback cũ → vẽ icon bằng Graphics2D chuyên nghiệp
     */
    private static Icon createPremiumIcon(String path) {
        String p = path.toLowerCase();

        // Xác định màu gradient và ký tự icon
        Color color1, color2;
        String symbol;

        if (p.contains("dashboard")) {
            color1 = new Color(0, 120, 215);
            color2 = new Color(0, 90, 180);
            symbol = "D";
        } else if (p.contains("account")) {
            color1 = new Color(65, 105, 225);
            color2 = new Color(45, 85, 195);
            symbol = "A";
        } else if (p.contains("user") || p.contains("player")) {
            color1 = new Color(0, 153, 51);
            color2 = new Color(0, 120, 40);
            symbol = "P";
        } else if (p.contains("shop")) {
            color1 = new Color(255, 87, 34);
            color2 = new Color(220, 60, 20);
            symbol = "S";
        } else if (p.contains("gift")) {
            color1 = new Color(220, 53, 69);
            color2 = new Color(180, 40, 55);
            symbol = "G";
        } else if (p.contains("topup") || p.contains("reward")) {
            color1 = new Color(102, 51, 204);
            color2 = new Color(80, 40, 170);
            symbol = "T";
        } else if (p.contains("calendar") || p.contains("event")) {
            color1 = new Color(142, 68, 173);
            color2 = new Color(110, 50, 140);
            symbol = "E";
        } else if (p.contains("badge") || p.contains("star")) {
            color1 = new Color(255, 179, 0);
            color2 = new Color(230, 150, 0);
            symbol = "B";
        } else if (p.contains("map")) {
            color1 = new Color(56, 142, 60);
            color2 = new Color(40, 110, 45);
            symbol = "M";
        } else if (p.contains("drop")) {
            color1 = new Color(156, 39, 176);
            color2 = new Color(126, 30, 146);
            symbol = "Dr";
        } else if (p.contains("item")) {
            color1 = new Color(255, 152, 0);
            color2 = new Color(220, 130, 0);
            symbol = "I";
        } else if (p.contains("radar")) {
            color1 = new Color(0, 172, 105);
            color2 = new Color(0, 140, 85);
            symbol = "R";
        } else if (p.contains("monster") || p.contains("boss")) {
            color1 = new Color(80, 80, 80);
            color2 = new Color(50, 50, 50);
            symbol = "Bo";
        } else if (p.contains("shield") || p.contains("security")) {
            color1 = new Color(220, 53, 69);
            color2 = new Color(180, 40, 55);
            symbol = "FW";
        } else if (p.contains("database") || p.contains("db")) {
            color1 = new Color(63, 81, 181);
            color2 = new Color(48, 63, 150);
            symbol = "DB";
        } else if (p.contains("traffic") || p.contains("chart")) {
            color1 = new Color(0, 150, 136);
            color2 = new Color(0, 120, 110);
            symbol = "Tr";
        } else if (p.contains("transaction") || p.contains("payment")) {
            color1 = new Color(255, 111, 0);
            color2 = new Color(220, 90, 0);
            symbol = "$";
        } else {
            color1 = new Color(120, 120, 120);
            color2 = new Color(90, 90, 90);
            symbol = "•";
        }

        final String finalSymbol = symbol;
        final Color c1 = color1;
        final Color c2 = color2;
        final int SIZE = 28;

        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

                // Shadow
                g2.setColor(new Color(0, 0, 0, 30));
                g2.fillRoundRect(x + 1, y + 2, SIZE, SIZE, 8, 8);

                // Gradient background
                GradientPaint gp = new GradientPaint(x, y, c1, x + SIZE, y + SIZE, c2);
                g2.setPaint(gp);
                g2.fillRoundRect(x, y, SIZE, SIZE, 8, 8);

                // Light reflection (top-left)
                g2.setColor(new Color(255, 255, 255, 50));
                g2.fillRoundRect(x + 1, y + 1, SIZE - 2, SIZE / 2, 7, 7);

                // Text
                int fontSize = finalSymbol.length() > 2 ? 10 : (finalSymbol.length() > 1 ? 11 : 13);
                g2.setFont(new Font("Segoe UI", Font.BOLD, fontSize));
                FontMetrics fm = g2.getFontMetrics();
                int tw = fm.stringWidth(finalSymbol);
                int tx = x + (SIZE - tw) / 2;
                int ty = y + ((SIZE - fm.getHeight()) / 2) + fm.getAscent();

                // Text shadow
                g2.setColor(new Color(0, 0, 0, 60));
                g2.drawString(finalSymbol, tx + 1, ty + 1);

                // Text
                g2.setColor(Color.WHITE);
                g2.drawString(finalSymbol, tx, ty);

                g2.dispose();
            }

            @Override
            public int getIconWidth() { return SIZE; }

            @Override
            public int getIconHeight() { return SIZE; }
        };
    }
}