package nro.server.ui;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Stress Test Panel - Kiểm tra khả năng chịu tải và Anti-DDoS.
 * Chỉ test trên localhost (127.0.0.1) - KHÔNG phải công cụ tấn công.
 * 
 * Chức năng:
 * 1. Connection Stress Test - giả lập nhiều kết nối TCP đồng thời
 * 2. Rate Limit Test - kiểm tra firewall có chặn đúng ngưỡng
 * 3. Connection Flood Test - gửi kết nối nhanh liên tục để test Anti-DDoS
 * 4. Sustained Load Test - duy trì kết nối lâu dài để test memory/stability
 */
public class StressTestPanel extends JPanel {

    private JTextArea logArea;
    private JTextField tfHost, tfPort, tfConnections, tfDuration, tfDelay;
    private JTextField tfFloodRate, tfFloodDuration;
    private JProgressBar progressBar;
    private JLabel lblStatus, lblActiveConns, lblSuccessConns, lblFailedConns, lblBlocked;
    private JLabel lblAvgResponseTime, lblMaxResponseTime, lblMinResponseTime;
    private JLabel lblThroughput, lblMemUsed;
    private JButton btnStartStress, btnStartRateLimit, btnStartFlood, btnStartSustained, btnStop;
    private volatile boolean running = false;
    private ExecutorService executor;
    private final List<Socket> activeSockets = Collections.synchronizedList(new ArrayList<>());

    // Stats
    private final AtomicInteger totalAttempted = new AtomicInteger(0);
    private final AtomicInteger totalSuccess = new AtomicInteger(0);
    private final AtomicInteger totalFailed = new AtomicInteger(0);
    private final AtomicInteger totalBlocked = new AtomicInteger(0);
    private final AtomicInteger currentActive = new AtomicInteger(0);
    private final AtomicLong totalResponseTime = new AtomicLong(0);
    private final AtomicLong maxResponseTime = new AtomicLong(0);
    private final AtomicLong minResponseTime = new AtomicLong(Long.MAX_VALUE);

    private ScheduledExecutorService statsUpdater;

    public StressTestPanel() {
        setLayout(new BorderLayout(0, 10));
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        add(createHeader(), BorderLayout.NORTH);
        add(createMainContent(), BorderLayout.CENTER);
        add(createLogPanel(), BorderLayout.SOUTH);
    }

    private JPanel createHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(0, 5, 10, 5));

        JLabel title = new JLabel("🔥 Stress Test & Load Tester");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(new Color(50, 50, 50));

        JLabel subtitle = new JLabel("Kiểm tra khả năng chịu tải server, test Anti-DDoS rate limiting (Chỉ test trên localhost)");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(new Color(120, 120, 120));

        JPanel textPanel = new JPanel(new GridLayout(2, 1));
        textPanel.setOpaque(false);
        textPanel.add(title);
        textPanel.add(subtitle);

        // Warning label
        JLabel warn = new JLabel("⚠ CHỈ SỬ DỤNG ĐỂ TEST SERVER CỦA BẠN");
        warn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        warn.setForeground(new Color(255, 152, 0));

        p.add(textPanel, BorderLayout.WEST);
        p.add(warn, BorderLayout.EAST);
        return p;
    }

    private JScrollPane createMainContent() {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setOpaque(false);

        container.add(createStatsPanel());
        container.add(Box.createVerticalStrut(10));
        container.add(createConnectionTestSection());
        container.add(Box.createVerticalStrut(10));
        container.add(createRateLimitTestSection());
        container.add(Box.createVerticalStrut(10));
        container.add(createFloodTestSection());
        container.add(Box.createVerticalStrut(10));
        container.add(createSustainedTestSection());
        container.add(Box.createVerticalStrut(10));
        container.add(createProgressSection());

        JScrollPane scroll = new JScrollPane(container);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    // ===== REALTIME STATS =====
    private JPanel createStatsPanel() {
        JPanel section = createSection("📊 Thống Kê Real-time");
        JPanel content = new JPanel(new GridLayout(2, 5, 15, 8));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(5, 10, 5, 10));

        lblActiveConns = createStatLabel("0", "Active Conns", new Color(0, 123, 255));
        lblSuccessConns = createStatLabel("0", "Thành Công", new Color(40, 167, 69));
        lblFailedConns = createStatLabel("0", "Thất Bại", new Color(220, 53, 69));
        lblBlocked = createStatLabel("0", "Bị Chặn", new Color(255, 152, 0));
        lblThroughput = createStatLabel("0/s", "Throughput", new Color(102, 51, 204));

        lblAvgResponseTime = createStatLabel("0ms", "Avg Response", new Color(0, 150, 136));
        lblMinResponseTime = createStatLabel("0ms", "Min Response", new Color(40, 167, 69));
        lblMaxResponseTime = createStatLabel("0ms", "Max Response", new Color(220, 53, 69));
        lblMemUsed = createStatLabel("0 MB", "RAM Used", new Color(63, 81, 181));
        lblStatus = createStatLabel("Idle", "Trạng Thái", new Color(108, 117, 125));

        content.add(lblActiveConns);
        content.add(lblSuccessConns);
        content.add(lblFailedConns);
        content.add(lblBlocked);
        content.add(lblThroughput);
        content.add(lblAvgResponseTime);
        content.add(lblMinResponseTime);
        content.add(lblMaxResponseTime);
        content.add(lblMemUsed);
        content.add(lblStatus);

        section.add(content, BorderLayout.CENTER);
        return section;
    }

    private JLabel createStatLabel(String value, String label, Color color) {
        JLabel lbl = new JLabel("<html><center><span style='font-size:16px;color:" +
                String.format("#%06x", color.getRGB() & 0xFFFFFF) + "'><b>" + value +
                "</b></span><br><span style='font-size:10px;color:#888'>" + label + "</span></center></html>");
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        lbl.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(230, 230, 230), 1, true),
                new EmptyBorder(8, 5, 8, 5)));
        lbl.setOpaque(true);
        lbl.setBackground(Color.WHITE);
        lbl.putClientProperty("statColor", color);
        return lbl;
    }

    private void updateStatLabel(JLabel lbl, String value) {
        Color color = (Color) lbl.getClientProperty("statColor");
        String text = lbl.getText();
        // Extract label part
        int brIdx = text.lastIndexOf("<br>");
        if (brIdx > 0) {
            String labelPart = text.substring(brIdx);
            lbl.setText("<html><center><span style='font-size:16px;color:" +
                    String.format("#%06x", color.getRGB() & 0xFFFFFF) + "'><b>" + value +
                    "</b></span>" + labelPart + "</center></html>");
        }
    }

    // ===== TEST 1: CONNECTION STRESS =====
    private JPanel createConnectionTestSection() {
        JPanel section = createSection("🔌 Test 1: Connection Stress (Giả lập nhiều người chơi)");
        JPanel content = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 8));
        content.setOpaque(false);

        content.add(makeLabel("Host:"));
        tfHost = new JTextField("127.0.0.1", 10);
        tfHost.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        content.add(tfHost);

        content.add(makeLabel("Port:"));
        tfPort = new JTextField("14445", 5);
        tfPort.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        content.add(tfPort);

        content.add(makeLabel("Số kết nối:"));
        tfConnections = new JTextField("100", 5);
        tfConnections.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        content.add(tfConnections);

        content.add(makeLabel("Delay (ms):"));
        tfDelay = new JTextField("50", 5);
        tfDelay.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        content.add(tfDelay);

        btnStartStress = ServerGuiUtils.createStyledButton("▶ Bắt Đầu", new Color(40, 167, 69), Color.WHITE);
        btnStartStress.addActionListener(e -> startConnectionStressTest());
        content.add(btnStartStress);

        section.add(content, BorderLayout.CENTER);

        // Quick presets
        JPanel presets = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        presets.setOpaque(false);
        presets.setBorder(new EmptyBorder(0, 10, 5, 0));
        presets.add(makeLabel("Nhanh:"));
        for (int[] preset : new int[][]{{50, 100}, {100, 50}, {500, 20}, {1000, 10}}) {
            JButton btn = new JButton(preset[0] + " conn");
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            final int conns = preset[0], delay = preset[1];
            btn.addActionListener(e -> {
                tfConnections.setText(String.valueOf(conns));
                tfDelay.setText(String.valueOf(delay));
            });
            presets.add(btn);
        }
        section.add(presets, BorderLayout.SOUTH);
        return section;
    }

    // ===== TEST 2: RATE LIMIT =====
    private JPanel createRateLimitTestSection() {
        JPanel section = createSection("⚡ Test 2: Rate Limit Test (Kiểm tra firewall chặn đúng ngưỡng)");
        JPanel content = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 8));
        content.setOpaque(false);

        JLabel info = new JLabel("Gửi kết nối liên tục từ 1 IP đến khi bị chặn (Ngưỡng mặc định: 21 conn/IP)");
        info.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        info.setForeground(new Color(100, 100, 100));
        content.add(info);

        btnStartRateLimit = ServerGuiUtils.createStyledButton("⚡ Test Rate Limit", new Color(255, 152, 0), Color.WHITE);
        btnStartRateLimit.addActionListener(e -> startRateLimitTest());
        content.add(btnStartRateLimit);

        section.add(content, BorderLayout.CENTER);
        return section;
    }

    // ===== TEST 3: CONNECTION FLOOD =====
    private JPanel createFloodTestSection() {
        JPanel section = createSection("🌊 Test 3: Connection Flood (Test Anti-DDoS chịu tải nhanh)");
        JPanel content = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 8));
        content.setOpaque(false);

        content.add(makeLabel("Tốc độ (conn/s):"));
        tfFloodRate = new JTextField("50", 5);
        tfFloodRate.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        content.add(tfFloodRate);

        content.add(makeLabel("Thời gian (giây):"));
        tfFloodDuration = new JTextField("10", 5);
        tfFloodDuration.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        content.add(tfFloodDuration);

        btnStartFlood = ServerGuiUtils.createStyledButton("🌊 Bắt Đầu Flood Test", new Color(220, 53, 69), Color.WHITE);
        btnStartFlood.addActionListener(e -> startConnectionFloodTest());
        content.add(btnStartFlood);

        section.add(content, BorderLayout.CENTER);
        return section;
    }

    // ===== TEST 4: SUSTAINED LOAD =====
    private JPanel createSustainedTestSection() {
        JPanel section = createSection("⏱ Test 4: Sustained Load (Duy trì kết nối lâu dài test stability)");
        JPanel content = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 8));
        content.setOpaque(false);

        content.add(makeLabel("Số kết nối giữ:"));
        tfDuration = new JTextField("20", 5);
        tfDuration.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        content.add(tfDuration);

        JLabel info = new JLabel("Mở N kết nối và giữ mở — theo dõi RAM, stability");
        info.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        info.setForeground(new Color(100, 100, 100));
        content.add(info);

        btnStartSustained = ServerGuiUtils.createStyledButton("⏱ Sustained Test", new Color(102, 51, 204), Color.WHITE);
        btnStartSustained.addActionListener(e -> startSustainedLoadTest());
        content.add(btnStartSustained);

        section.add(content, BorderLayout.CENTER);
        return section;
    }

    // ===== PROGRESS & STOP =====
    private JPanel createProgressSection() {
        JPanel section = createSection("📈 Tiến Trình");
        JPanel content = new JPanel(new BorderLayout(10, 5));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(5, 10, 5, 10));

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        progressBar.setPreferredSize(new Dimension(0, 25));
        content.add(progressBar, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        btnStop = ServerGuiUtils.createStyledButton("⏹ Dừng Tất Cả", new Color(220, 53, 69), Color.WHITE);
        btnStop.setEnabled(false);
        btnStop.addActionListener(e -> stopAllTests());
        btnPanel.add(btnStop);

        JButton btnReset = ServerGuiUtils.createStyledButton("🔄 Reset Stats", new Color(108, 117, 125), Color.WHITE);
        btnReset.addActionListener(e -> resetStats());
        btnPanel.add(btnReset);

        JButton btnExport = ServerGuiUtils.createStyledButton("📋 Export Report", new Color(0, 123, 255), Color.WHITE);
        btnExport.addActionListener(e -> exportReport());
        btnPanel.add(btnExport);

        content.add(btnPanel, BorderLayout.SOUTH);

        section.add(content, BorderLayout.CENTER);
        return section;
    }

    // ===== LOG =====
    private JPanel createLogPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(ServerGuiUtils.createSectionBorder("📝 Test Log"));
        p.setPreferredSize(new Dimension(0, 150));

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        logArea.setBackground(new Color(30, 30, 30));
        logArea.setForeground(new Color(0, 255, 128));
        logArea.setCaretColor(new Color(0, 255, 128));

        JScrollPane scroll = new JScrollPane(logArea);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        btnPanel.setOpaque(false);
        JButton btnClear = new JButton("Clear");
        btnClear.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        btnClear.addActionListener(e -> logArea.setText(""));
        btnPanel.add(btnClear);

        p.add(scroll, BorderLayout.CENTER);
        p.add(btnPanel, BorderLayout.SOUTH);
        return p;
    }

    // ===================================================================
    // TEST IMPLEMENTATIONS
    // ===================================================================

    private void startConnectionStressTest() {
        if (running) {
            log("⚠ Test đang chạy. Hãy dừng trước khi bắt đầu test mới.");
            return;
        }

        String host = tfHost.getText().trim();
        int port, numConnections, delay;
        try {
            port = Integer.parseInt(tfPort.getText().trim());
            numConnections = Integer.parseInt(tfConnections.getText().trim());
            delay = Integer.parseInt(tfDelay.getText().trim());
        } catch (NumberFormatException e) {
            log("❌ Giá trị không hợp lệ!");
            return;
        }

        resetStats();
        running = true;
        setButtonsEnabled(false);
        btnStop.setEnabled(true);
        updateStatLabel(lblStatus, "Running");
        progressBar.setMaximum(numConnections);
        progressBar.setValue(0);

        startStatsUpdater();

        log("🔌 CONNECTION STRESS TEST");
        log("   Host: " + host + ":" + port);
        log("   Connections: " + numConnections + " | Delay: " + delay + "ms");
        log("   Bắt đầu...");

        executor = Executors.newFixedThreadPool(Math.min(numConnections, 200));

        Thread testThread = new Thread(() -> {
            long startTime = System.currentTimeMillis();

            for (int i = 0; i < numConnections && running; i++) {
                final int connId = i + 1;
                executor.submit(() -> {
                    totalAttempted.incrementAndGet();
                    long connStart = System.currentTimeMillis();
                    try {
                        Socket socket = new Socket();
                        socket.connect(new java.net.InetSocketAddress(host, port), 5000);
                        long elapsed = System.currentTimeMillis() - connStart;

                        totalSuccess.incrementAndGet();
                        currentActive.incrementAndGet();
                        activeSockets.add(socket);

                        totalResponseTime.addAndGet(elapsed);
                        maxResponseTime.accumulateAndGet(elapsed, Math::max);
                        minResponseTime.accumulateAndGet(elapsed, Math::min);

                        if (connId % 50 == 0 || connId <= 5) {
                            log("✅ Conn #" + connId + " OK (" + elapsed + "ms) | Active: " + currentActive.get());
                        }
                    } catch (java.net.ConnectException e) {
                        totalBlocked.incrementAndGet();
                        if (connId <= 30 || connId % 100 == 0) {
                            log("🚫 Conn #" + connId + " BLOCKED - " + e.getMessage());
                        }
                    } catch (Exception e) {
                        totalFailed.incrementAndGet();
                        if (connId <= 10) {
                            log("❌ Conn #" + connId + " FAILED - " + e.getMessage());
                        }
                    }

                    SwingUtilities.invokeLater(() -> progressBar.setValue(totalAttempted.get()));
                });

                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    break;
                }
            }

            executor.shutdown();
            try { executor.awaitTermination(30, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}

            long totalTime = System.currentTimeMillis() - startTime;
            running = false;

            SwingUtilities.invokeLater(() -> {
                setButtonsEnabled(true);
                btnStop.setEnabled(false);
                updateStatLabel(lblStatus, "Done");
                progressBar.setValue(progressBar.getMaximum());
            });

            log("\n===== KẾT QUẢ CONNECTION STRESS TEST =====");
            log("   Tổng thời gian: " + totalTime + "ms");
            log("   Kết nối thử: " + totalAttempted.get());
            log("   Thành công: " + totalSuccess.get());
            log("   Thất bại: " + totalFailed.get());
            log("   Bị chặn: " + totalBlocked.get());
            log("   Đang active: " + currentActive.get());
            if (totalSuccess.get() > 0) {
                log("   Avg response: " + (totalResponseTime.get() / totalSuccess.get()) + "ms");
                log("   Min response: " + (minResponseTime.get() == Long.MAX_VALUE ? 0 : minResponseTime.get()) + "ms");
                log("   Max response: " + maxResponseTime.get() + "ms");
            }
            log("   Throughput: " + (totalTime > 0 ? (totalSuccess.get() * 1000 / totalTime) : 0) + " conn/s");
            log("   ➡ Server có thể chịu tối thiểu " + totalSuccess.get() + " kết nối đồng thời");
            log("============================================\n");

            stopStatsUpdater();
        });
        testThread.setDaemon(true);
        testThread.start();
    }

    private void startRateLimitTest() {
        if (running) return;

        String host = tfHost.getText().trim();
        int port;
        try {
            port = Integer.parseInt(tfPort.getText().trim());
        } catch (NumberFormatException e) {
            log("❌ Port không hợp lệ!"); return;
        }

        resetStats();
        running = true;
        setButtonsEnabled(false);
        btnStop.setEnabled(true);
        updateStatLabel(lblStatus, "Rate Test");
        startStatsUpdater();

        log("⚡ RATE LIMIT TEST");
        log("   Gửi kết nối liên tục cho đến khi bị chặn...");
        log("   Host: " + host + ":" + port);

        Thread testThread = new Thread(() -> {
            int blocked = 0;
            int success = 0;
            long startTime = System.currentTimeMillis();

            for (int i = 0; i < 200 && running; i++) {
                totalAttempted.incrementAndGet();
                long connStart = System.currentTimeMillis();
                try {
                    Socket socket = new Socket();
                    socket.connect(new java.net.InetSocketAddress(host, port), 3000);
                    long elapsed = System.currentTimeMillis() - connStart;
                    success++;
                    totalSuccess.incrementAndGet();
                    currentActive.incrementAndGet();
                    activeSockets.add(socket);

                    totalResponseTime.addAndGet(elapsed);
                    maxResponseTime.accumulateAndGet(elapsed, Math::max);
                    minResponseTime.accumulateAndGet(elapsed, Math::min);

                    log("   ✅ Conn #" + (i + 1) + " OK (" + elapsed + "ms)");
                } catch (Exception e) {
                    blocked++;
                    totalBlocked.incrementAndGet();
                    log("   🚫 Conn #" + (i + 1) + " BLOCKED! (Firewall kích hoạt)");
                    if (blocked >= 3) {
                        log("   ⚡ RATE LIMIT DETECTED! Sau " + success + " kết nối thành công.");
                        break;
                    }
                }

                try { Thread.sleep(10); } catch (InterruptedException e) { break; }
            }

            long totalTime = System.currentTimeMillis() - startTime;
            running = false;

            SwingUtilities.invokeLater(() -> {
                setButtonsEnabled(true);
                btnStop.setEnabled(false);
                updateStatLabel(lblStatus, "Done");
            });

            log("\n===== KẾT QUẢ RATE LIMIT TEST =====");
            log("   Kết nối thành công trước khi bị chặn: " + success);
            log("   Kết nối bị chặn: " + blocked);
            log("   Thời gian: " + totalTime + "ms");
            log("   ➡ Firewall ngưỡng hoạt động: ~" + success + " conn/IP");
            log("====================================\n");

            stopStatsUpdater();
        });
        testThread.setDaemon(true);
        testThread.start();
    }

    private void startConnectionFloodTest() {
        if (running) return;

        String host = tfHost.getText().trim();
        int port, rate, duration;
        try {
            port = Integer.parseInt(tfPort.getText().trim());
            rate = Integer.parseInt(tfFloodRate.getText().trim());
            duration = Integer.parseInt(tfFloodDuration.getText().trim());
        } catch (NumberFormatException e) {
            log("❌ Giá trị không hợp lệ!"); return;
        }

        resetStats();
        running = true;
        setButtonsEnabled(false);
        btnStop.setEnabled(true);
        updateStatLabel(lblStatus, "Flood Test");
        progressBar.setMaximum(duration);
        progressBar.setValue(0);
        startStatsUpdater();

        log("🌊 CONNECTION FLOOD TEST");
        log("   Rate: " + rate + " conn/s | Duration: " + duration + "s");
        log("   Host: " + host + ":" + port);

        executor = Executors.newFixedThreadPool(Math.min(rate, 500));

        Thread testThread = new Thread(() -> {
            long startTime = System.currentTimeMillis();
            int delayPerConn = 1000 / rate;

            for (int sec = 0; sec < duration && running; sec++) {
                int currentSec = sec + 1;
                for (int i = 0; i < rate && running; i++) {
                    executor.submit(() -> {
                        totalAttempted.incrementAndGet();
                        try {
                            Socket socket = new Socket();
                            socket.connect(new java.net.InetSocketAddress(host, port), 2000);
                            totalSuccess.incrementAndGet();
                            currentActive.incrementAndGet();
                            // Close quickly for flood test
                            try { socket.close(); } catch (Exception ignored) {}
                            currentActive.decrementAndGet();
                        } catch (java.net.ConnectException e) {
                            totalBlocked.incrementAndGet();
                        } catch (Exception e) {
                            totalFailed.incrementAndGet();
                        }
                    });
                    try { Thread.sleep(delayPerConn); } catch (InterruptedException e) { break; }
                }

                int finalSec = currentSec;
                SwingUtilities.invokeLater(() -> progressBar.setValue(finalSec));
                log("   Giây " + currentSec + "/" + duration +
                        " | Attempted: " + totalAttempted.get() +
                        " | Success: " + totalSuccess.get() +
                        " | Blocked: " + totalBlocked.get());
            }

            executor.shutdown();
            try { executor.awaitTermination(10, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}

            long totalTime = System.currentTimeMillis() - startTime;
            running = false;

            SwingUtilities.invokeLater(() -> {
                setButtonsEnabled(true);
                btnStop.setEnabled(false);
                updateStatLabel(lblStatus, "Done");
            });

            log("\n===== KẾT QUẢ FLOOD TEST =====");
            log("   Tổng thời gian: " + (totalTime / 1000) + "s");
            log("   Tổng kết nối thử: " + totalAttempted.get());
            log("   Thành công: " + totalSuccess.get());
            log("   Bị chặn: " + totalBlocked.get());
            log("   Thất bại: " + totalFailed.get());
            double blockRate = totalAttempted.get() > 0 ?
                    (totalBlocked.get() * 100.0 / totalAttempted.get()) : 0;
            log("   Tỉ lệ chặn: " + String.format("%.1f%%", blockRate));
            log("   Throughput thực: " + (totalTime > 0 ? (totalSuccess.get() * 1000 / totalTime) : 0) + " conn/s");
            log("================================\n");

            stopStatsUpdater();
        });
        testThread.setDaemon(true);
        testThread.start();
    }

    private void startSustainedLoadTest() {
        if (running) return;

        String host = tfHost.getText().trim();
        int port, numConns;
        try {
            port = Integer.parseInt(tfPort.getText().trim());
            numConns = Integer.parseInt(tfDuration.getText().trim());
        } catch (NumberFormatException e) {
            log("❌ Giá trị không hợp lệ!"); return;
        }

        resetStats();
        running = true;
        setButtonsEnabled(false);
        btnStop.setEnabled(true);
        updateStatLabel(lblStatus, "Sustained");
        startStatsUpdater();

        log("⏱ SUSTAINED LOAD TEST");
        log("   Mở " + numConns + " kết nối và giữ mở...");
        log("   Host: " + host + ":" + port);

        Thread testThread = new Thread(() -> {
            Runtime rt = Runtime.getRuntime();
            long memBefore = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
            log("   RAM trước test: " + memBefore + " MB");

            // Open connections
            for (int i = 0; i < numConns && running; i++) {
                totalAttempted.incrementAndGet();
                try {
                    Socket socket = new Socket();
                    socket.connect(new java.net.InetSocketAddress(host, port), 5000);
                    socket.setKeepAlive(true);
                    totalSuccess.incrementAndGet();
                    currentActive.incrementAndGet();
                    activeSockets.add(socket);
                    if ((i + 1) % 5 == 0 || i < 3) {
                        log("   ✅ Conn #" + (i + 1) + " mở thành công | Active: " + currentActive.get());
                    }
                } catch (Exception e) {
                    totalFailed.incrementAndGet();
                    log("   ❌ Conn #" + (i + 1) + " thất bại: " + e.getMessage());
                }
                try { Thread.sleep(100); } catch (InterruptedException e) { break; }
            }

            log("\n   📊 " + currentActive.get() + " kết nối đang mở. Giám sát stability...");
            log("   (Nhấn 'Dừng Tất Cả' để kết thúc test và đóng kết nối)");

            // Monitor phase
            int checkCount = 0;
            while (running) {
                try { Thread.sleep(5000); } catch (InterruptedException e) { break; }
                checkCount++;

                long memNow = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;

                // Check if connections are still alive
                int alive = 0;
                synchronized (activeSockets) {
                    for (Socket s : activeSockets) {
                        if (s.isConnected() && !s.isClosed()) alive++;
                    }
                }

                log("   [" + (checkCount * 5) + "s] Active: " + alive +
                        "/" + activeSockets.size() + " | RAM: " + memNow + " MB");
            }

            long memAfter = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;

            log("\n===== KẾT QUẢ SUSTAINED LOAD TEST =====");
            log("   Kết nối mở thành công: " + totalSuccess.get());
            log("   RAM trước: " + memBefore + " MB → Sau: " + memAfter + " MB");
            log("   RAM tăng: " + (memAfter - memBefore) + " MB");
            log("   ➡ Mỗi kết nối tốn ~" +
                    (totalSuccess.get() > 0 ? ((memAfter - memBefore) * 1024 / totalSuccess.get()) : 0) + " KB RAM");
            log("=========================================\n");

            // Cleanup connections
            closeAllSockets();

            SwingUtilities.invokeLater(() -> {
                setButtonsEnabled(true);
                btnStop.setEnabled(false);
                updateStatLabel(lblStatus, "Done");
            });

            stopStatsUpdater();
        });
        testThread.setDaemon(true);
        testThread.start();
    }

    // ===================================================================
    // CONTROL
    // ===================================================================
    private void stopAllTests() {
        running = false;
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
        log("⏹ Test đã dừng.");
        closeAllSockets();
        setButtonsEnabled(true);
        btnStop.setEnabled(false);
        updateStatLabel(lblStatus, "Stopped");
        stopStatsUpdater();
    }

    private void closeAllSockets() {
        int closed = 0;
        synchronized (activeSockets) {
            for (Socket s : activeSockets) {
                try {
                    if (!s.isClosed()) {
                        s.close();
                        closed++;
                    }
                } catch (Exception ignored) {}
            }
            activeSockets.clear();
        }
        currentActive.set(0);
        if (closed > 0) {
            log("   Đã đóng " + closed + " kết nối.");
        }
    }

    private void resetStats() {
        totalAttempted.set(0);
        totalSuccess.set(0);
        totalFailed.set(0);
        totalBlocked.set(0);
        currentActive.set(0);
        totalResponseTime.set(0);
        maxResponseTime.set(0);
        minResponseTime.set(Long.MAX_VALUE);

        closeAllSockets();

        updateStatLabel(lblActiveConns, "0");
        updateStatLabel(lblSuccessConns, "0");
        updateStatLabel(lblFailedConns, "0");
        updateStatLabel(lblBlocked, "0");
        updateStatLabel(lblThroughput, "0/s");
        updateStatLabel(lblAvgResponseTime, "0ms");
        updateStatLabel(lblMinResponseTime, "0ms");
        updateStatLabel(lblMaxResponseTime, "0ms");
        updateStatLabel(lblMemUsed, "0 MB");
        updateStatLabel(lblStatus, "Idle");
        progressBar.setValue(0);
    }

    private void setButtonsEnabled(boolean enabled) {
        btnStartStress.setEnabled(enabled);
        btnStartRateLimit.setEnabled(enabled);
        btnStartFlood.setEnabled(enabled);
        btnStartSustained.setEnabled(enabled);
    }

    private void startStatsUpdater() {
        stopStatsUpdater();
        statsUpdater = Executors.newSingleThreadScheduledExecutor();
        final long startTime = System.currentTimeMillis();
        statsUpdater.scheduleAtFixedRate(() -> {
            SwingUtilities.invokeLater(() -> {
                updateStatLabel(lblActiveConns, String.valueOf(currentActive.get()));
                updateStatLabel(lblSuccessConns, String.valueOf(totalSuccess.get()));
                updateStatLabel(lblFailedConns, String.valueOf(totalFailed.get()));
                updateStatLabel(lblBlocked, String.valueOf(totalBlocked.get()));

                long elapsed = System.currentTimeMillis() - startTime;
                if (elapsed > 0) {
                    updateStatLabel(lblThroughput, (totalSuccess.get() * 1000 / Math.max(elapsed, 1)) + "/s");
                }

                int succ = totalSuccess.get();
                if (succ > 0) {
                    updateStatLabel(lblAvgResponseTime, (totalResponseTime.get() / succ) + "ms");
                    updateStatLabel(lblMinResponseTime,
                            (minResponseTime.get() == Long.MAX_VALUE ? 0 : minResponseTime.get()) + "ms");
                    updateStatLabel(lblMaxResponseTime, maxResponseTime.get() + "ms");
                }

                Runtime rt = Runtime.getRuntime();
                long usedMem = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
                updateStatLabel(lblMemUsed, usedMem + " MB");
            });
        }, 0, 500, TimeUnit.MILLISECONDS);
    }

    private void stopStatsUpdater() {
        if (statsUpdater != null && !statsUpdater.isShutdown()) {
            statsUpdater.shutdown();
        }
    }

    private void exportReport() {
        String report = "===== STRESS TEST REPORT =====\n" +
                "Thời gian: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) + "\n" +
                "Host: " + tfHost.getText() + ":" + tfPort.getText() + "\n\n" +
                "Tổng kết nối thử: " + totalAttempted.get() + "\n" +
                "Thành công: " + totalSuccess.get() + "\n" +
                "Thất bại: " + totalFailed.get() + "\n" +
                "Bị chặn: " + totalBlocked.get() + "\n" +
                "Active: " + currentActive.get() + "\n\n" +
                (totalSuccess.get() > 0 ?
                        "Avg Response: " + (totalResponseTime.get() / totalSuccess.get()) + "ms\n" +
                        "Min Response: " + (minResponseTime.get() == Long.MAX_VALUE ? 0 : minResponseTime.get()) + "ms\n" +
                        "Max Response: " + maxResponseTime.get() + "ms\n" : "") +
                "\n==============================\n";

        try {
            String fileName = "stress_test_report_" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt";
            try (PrintWriter pw = new PrintWriter(new FileWriter(fileName))) {
                pw.print(report);
            }
            log("📋 Report exported to: " + fileName);
        } catch (Exception e) {
            log("❌ Export error: " + e.getMessage());
        }
    }

    // ===================================================================
    // HELPERS
    // ===================================================================
    private JPanel createSection(String title) {
        JPanel p = new JPanel(new BorderLayout(0, 5));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createTitledBorder(
                new LineBorder(new Color(220, 220, 220)), title,
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 13), new Color(50, 50, 50)));
        return p;
    }

    private JLabel makeLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(new Color(80, 80, 80));
        return lbl;
    }

    private void log(String msg) {
        if (logArea != null) {
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            SwingUtilities.invokeLater(() -> {
                logArea.append("[" + time + "] " + msg + "\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
            });
        }
    }
}
