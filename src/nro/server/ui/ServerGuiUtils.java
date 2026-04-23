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
     * Tạo icon premium vẽ bằng Graphics2D — hình vẽ thực sự thay vì chữ cái
     */
    private static Icon createPremiumIcon(String path) {
        String p = path.toLowerCase();

        Color color1, color2;
        int iconType;

        if (p.contains("dashboard")) {
            color1 = new Color(0, 120, 215); color2 = new Color(0, 90, 180); iconType = 0;
        } else if (p.contains("account")) {
            color1 = new Color(65, 105, 225); color2 = new Color(45, 85, 195); iconType = 1;
        } else if (p.contains("user") || p.contains("player")) {
            color1 = new Color(0, 153, 51); color2 = new Color(0, 120, 40); iconType = 2;
        } else if (p.contains("shop")) {
            color1 = new Color(255, 87, 34); color2 = new Color(220, 60, 20); iconType = 3;
        } else if (p.contains("gift")) {
            color1 = new Color(220, 53, 69); color2 = new Color(180, 40, 55); iconType = 4;
        } else if (p.contains("topup") || p.contains("reward")) {
            color1 = new Color(102, 51, 204); color2 = new Color(80, 40, 170); iconType = 5;
        } else if (p.contains("calendar") || p.contains("event")) {
            color1 = new Color(142, 68, 173); color2 = new Color(110, 50, 140); iconType = 6;
        } else if (p.contains("badge") || p.contains("star")) {
            color1 = new Color(255, 179, 0); color2 = new Color(230, 150, 0); iconType = 7;
        } else if (p.contains("map")) {
            color1 = new Color(56, 142, 60); color2 = new Color(40, 110, 45); iconType = 8;
        } else if (p.contains("item")) {
            color1 = new Color(255, 152, 0); color2 = new Color(220, 130, 0); iconType = 9;
        } else if (p.contains("radar")) {
            color1 = new Color(0, 172, 105); color2 = new Color(0, 140, 85); iconType = 10;
        } else if (p.contains("monster") || p.contains("boss")) {
            color1 = new Color(80, 80, 80); color2 = new Color(50, 50, 50); iconType = 11;
        } else if (p.contains("shield") || p.contains("security") || p.contains("firewall")) {
            color1 = new Color(220, 53, 69); color2 = new Color(180, 40, 55); iconType = 12;
        } else if (p.contains("database") || p.contains("db")) {
            color1 = new Color(63, 81, 181); color2 = new Color(48, 63, 150); iconType = 13;
        } else if (p.contains("traffic") || p.contains("chart")) {
            color1 = new Color(0, 150, 136); color2 = new Color(0, 120, 110); iconType = 14;
        } else if (p.contains("transaction") || p.contains("payment")) {
            color1 = new Color(255, 111, 0); color2 = new Color(220, 90, 0); iconType = 15;
        } else if (p.contains("setting") || p.contains("gear") || p.contains("config")) {
            color1 = new Color(96, 125, 139); color2 = new Color(69, 90, 100); iconType = 16;
        } else {
            color1 = new Color(120, 120, 120); color2 = new Color(90, 90, 90); iconType = -1;
        }

        final Color fC1 = color1;
        final Color fC2 = color2;
        final int fType = iconType;
        final int SIZE = 28;

        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

                // Shadow
                g2.setColor(new Color(0, 0, 0, 30));
                g2.fillRoundRect(x + 1, y + 2, SIZE, SIZE, 8, 8);

                // Gradient background
                GradientPaint gp = new GradientPaint(x, y, fC1, x + SIZE, y + SIZE, fC2);
                g2.setPaint(gp);
                g2.fillRoundRect(x, y, SIZE, SIZE, 8, 8);

                // Light reflection
                g2.setColor(new Color(255, 255, 255, 50));
                g2.fillRoundRect(x + 1, y + 1, SIZE - 2, SIZE / 2, 7, 7);

                // Draw icon shape
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                int cx = x + SIZE / 2;
                int cy = y + SIZE / 2;
                int pad = 7;
                int s = SIZE - pad * 2;

                switch (fType) {
                    case 0: drawDashboard(g2, x + pad, y + pad, s); break;
                    case 1: drawPerson(g2, cx, cy, s); break;
                    case 2: drawGroup(g2, cx, cy, s); break;
                    case 3: drawBag(g2, cx, cy, s); break;
                    case 4: drawGift(g2, x + pad, y + pad, s); break;
                    case 5: drawCard(g2, x + pad, y + pad, s); break;
                    case 6: drawCalendar(g2, x + pad, y + pad, s); break;
                    case 7: drawStar(g2, cx, cy, s); break;
                    case 8: drawPin(g2, cx, cy, s, fC1); break;
                    case 9: drawChest(g2, x + pad, y + pad, s); break;
                    case 10: drawRadar(g2, cx, cy, s); break;
                    case 11: drawSkull(g2, cx, cy, s, fC2); break;
                    case 12: drawShield(g2, cx, cy, s, fC1); break;
                    case 13: drawDatabase(g2, x + pad, y + pad, s); break;
                    case 14: drawBarChart(g2, x + pad, y + pad, s); break;
                    case 15: drawDollar(g2, cx, cy, s); break;
                    case 16: drawGear(g2, cx, cy, s, fC1); break;
                    default: g2.fillOval(cx - 3, cy - 3, 6, 6); break;
                }

                g2.dispose();
            }

            @Override public int getIconWidth() { return SIZE; }
            @Override public int getIconHeight() { return SIZE; }
        };
    }

    // ===== ICON DRAWING METHODS =====

    private static void drawDashboard(Graphics2D g, int x, int y, int s) {
        int half = s / 2 - 1;
        int gap = 2;
        g.fillRoundRect(x, y, half, half, 2, 2);
        g.fillRoundRect(x + half + gap, y, half, half, 2, 2);
        g.fillRoundRect(x, y + half + gap, half, half, 2, 2);
        g.fillRoundRect(x + half + gap, y + half + gap, half, half, 2, 2);
    }

    private static void drawPerson(Graphics2D g, int cx, int cy, int s) {
        int r = s / 4;
        g.fillOval(cx - r, cy - s / 2, r * 2, r * 2);
        g.fillArc(cx - s / 2, cy + 1, s, s, 0, 180);
    }

    private static void drawGroup(Graphics2D g, int cx, int cy, int s) {
        int r = s / 5;
        g.fillOval(cx - r - 2, cy - s / 2, r * 2, r * 2);
        g.fillArc(cx - s / 2, cy + 1, s - 3, s - 3, 0, 180);
        g.setColor(new Color(255, 255, 255, 150));
        g.fillOval(cx + r - 1, cy - s / 2 - 1, r * 2, r * 2);
        g.fillArc(cx + 1, cy, s - 3, s - 3, 0, 180);
        g.setColor(Color.WHITE);
    }

    private static void drawBag(Graphics2D g, int cx, int cy, int s) {
        int bw = s * 3 / 4;
        int bh = s * 2 / 3;
        g.drawRoundRect(cx - bw / 2, cy - bh / 4, bw, bh, 3, 3);
        g.drawArc(cx - s / 4, cy - s / 2, s / 2, s / 2, 0, 180);
    }

    private static void drawGift(Graphics2D g, int x, int y, int s) {
        int half = s / 2;
        g.drawRect(x, y + s / 3, s, s * 2 / 3);
        g.fillRect(x - 1, y + s / 3 - 2, s + 2, 3);
        g.drawLine(x + half, y + s / 3, x + half, y + s);
        g.drawArc(x + half - 4, y + 1, 4, s / 3, 0, 180);
        g.drawArc(x + half, y + 1, 4, s / 3, 0, 180);
    }

    private static void drawCard(Graphics2D g, int x, int y, int s) {
        int h = s * 3 / 4;
        int yOff = (s - h) / 2;
        g.drawRoundRect(x, y + yOff, s, h, 3, 3);
        g.fillRect(x + 1, y + yOff + h / 4, s - 1, 3);
        g.drawRect(x + 3, y + yOff + h / 2 + 1, 4, 3);
    }

    private static void drawCalendar(Graphics2D g, int x, int y, int s) {
        g.drawRoundRect(x, y + 3, s, s - 3, 2, 2);
        g.fillRect(x + 1, y + 3, s - 1, 4);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(x + s / 4, y, x + s / 4, y + 5);
        g.drawLine(x + s * 3 / 4, y, x + s * 3 / 4, y + 5);
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int d = 2;
        g.fillRect(x + 3, y + 9, d, d);
        g.fillRect(x + 7, y + 9, d, d);
        if (s > 12) g.fillRect(x + 11, y + 9, d, d);
        g.fillRect(x + 3, y + 12, d, d);
        g.fillRect(x + 7, y + 12, d, d);
    }

    private static void drawStar(Graphics2D g, int cx, int cy, int s) {
        int r = s / 2;
        int points = 5;
        int[] xp = new int[points * 2];
        int[] yp = new int[points * 2];
        for (int i = 0; i < points * 2; i++) {
            double angle = Math.PI * i / points - Math.PI / 2;
            double rad = (i % 2 == 0) ? r : r * 0.4;
            xp[i] = cx + (int) (Math.cos(angle) * rad);
            yp[i] = cy + (int) (Math.sin(angle) * rad);
        }
        g.fillPolygon(xp, yp, points * 2);
    }

    private static void drawPin(Graphics2D g, int cx, int cy, int s, Color bg) {
        int r = s / 3;
        g.fillOval(cx - r, cy - s / 2, r * 2, r * 2);
        int[] triX = {cx - r + 1, cx + r - 1, cx};
        int[] triY = {cy - 1, cy - 1, cy + s / 2};
        g.fillPolygon(triX, triY, 3);
        g.setColor(bg);
        g.fillOval(cx - 2, cy - s / 2 + r - 2, 4, 4);
        g.setColor(Color.WHITE);
    }

    private static void drawChest(Graphics2D g, int x, int y, int s) {
        int h = s * 2 / 3;
        int yOff = s - h;
        g.drawRoundRect(x, y + yOff + 3, s, h - 3, 2, 2);
        g.drawRoundRect(x - 1, y + yOff, s + 2, 5, 2, 2);
        g.fillRect(x + s / 2 - 1, y + yOff + 5, 3, 3);
    }

    private static void drawRadar(Graphics2D g, int cx, int cy, int s) {
        int r = s / 2;
        g.drawOval(cx - r, cy - r, s, s);
        g.drawOval(cx - r / 2, cy - r / 2, r, r);
        g.drawLine(cx, cy - r, cx, cy + r);
        g.drawLine(cx - r, cy, cx + r, cy);
        g.setStroke(new BasicStroke(2f));
        int sx = cx + (int) (r * Math.cos(Math.toRadians(-45)));
        int sy = cy + (int) (r * Math.sin(Math.toRadians(-45)));
        g.drawLine(cx, cy, sx, sy);
        g.fillOval(cx + r / 3, cy - r / 3 - 1, 3, 3);
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    }

    private static void drawSkull(Graphics2D g, int cx, int cy, int s, Color dark) {
        int r = s / 2;
        g.fillOval(cx - r, cy - r, s, s - 2);
        g.fillRoundRect(cx - r + 2, cy + 1, s - 4, s / 3, 2, 2);
        g.setColor(dark);
        g.fillOval(cx - r / 2 - 1, cy - r / 3, 4, 4);
        g.fillOval(cx + r / 2 - 3, cy - r / 3, 4, 4);
        g.fillOval(cx - 1, cy + 1, 3, 2);
        g.setColor(Color.WHITE);
    }

    private static void drawShield(Graphics2D g, int cx, int cy, int s, Color bg) {
        int r = s / 2;
        int[] sx = {cx, cx + r, cx + r, cx, cx - r, cx - r};
        int[] sy = {cy - r, cy - r + 3, cy + 2, cy + r, cy + 2, cy - r + 3};
        g.fillPolygon(sx, sy, 6);
        g.setColor(bg);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(cx - 3, cy, cx - 1, cy + 3);
        g.drawLine(cx - 1, cy + 3, cx + 4, cy - 3);
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(Color.WHITE);
    }

    private static void drawDatabase(Graphics2D g, int x, int y, int s) {
        int ellipseH = s / 4;
        g.drawLine(x, y + ellipseH / 2, x, y + s - ellipseH / 2);
        g.drawLine(x + s, y + ellipseH / 2, x + s, y + s - ellipseH / 2);
        g.fillOval(x, y, s, ellipseH);
        g.drawOval(x, y + s / 3, s, ellipseH);
        g.drawArc(x, y + s - ellipseH, s, ellipseH, 180, 180);
    }

    private static void drawBarChart(Graphics2D g, int x, int y, int s) {
        int bw = Math.max(2, s / 4);
        int gap = 1;
        g.fillRect(x, y + s * 2 / 5, bw, s * 3 / 5);
        g.fillRect(x + bw + gap, y + s / 5, bw, s * 4 / 5);
        g.fillRect(x + (bw + gap) * 2, y, bw, s);
        g.fillRect(x + (bw + gap) * 3, y + s * 3 / 10, bw, s * 7 / 10);
    }

    private static void drawDollar(Graphics2D g, int cx, int cy, int s) {
        int r = s / 2;
        g.drawOval(cx - r, cy - r, s, s);
        g.setFont(new Font("Segoe UI", Font.BOLD, Math.max(8, s * 2 / 3)));
        FontMetrics fm = g.getFontMetrics();
        String ds = "$";
        g.drawString(ds, cx - fm.stringWidth(ds) / 2, cy + fm.getAscent() / 2 - 1);
    }

    private static void drawGear(Graphics2D g, int cx, int cy, int s, Color bg) {
        int outerR = s / 2;
        int innerR = s / 4;
        int teeth = 6;

        GeneralPath gear = new GeneralPath();
        for (int i = 0; i < teeth; i++) {
            double a1 = 2 * Math.PI * i / teeth - Math.PI / 14;
            double a2 = 2 * Math.PI * i / teeth + Math.PI / 14;
            double a3 = 2 * Math.PI * (i + 0.5) / teeth - Math.PI / 14;
            double a4 = 2 * Math.PI * (i + 0.5) / teeth + Math.PI / 14;

            double nextA = 2 * Math.PI * (i + 1) / teeth - Math.PI / 14;
            if (i == 0) gear.moveTo(cx + outerR * Math.cos(a1), cy + outerR * Math.sin(a1));
            gear.lineTo(cx + outerR * Math.cos(a2), cy + outerR * Math.sin(a2));
            gear.lineTo(cx + (outerR - 3) * Math.cos(a3), cy + (outerR - 3) * Math.sin(a3));
            gear.lineTo(cx + (outerR - 3) * Math.cos(a4), cy + (outerR - 3) * Math.sin(a4));
            gear.lineTo(cx + outerR * Math.cos(nextA), cy + outerR * Math.sin(nextA));
        }
        gear.closePath();
        g.fill(gear);
        g.setColor(bg);
        g.fillOval(cx - innerR, cy - innerR, innerR * 2, innerR * 2);
        g.setColor(Color.WHITE);
    }
}