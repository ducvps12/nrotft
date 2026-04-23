package nro.server.ui;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Panel Xem Log Server Real-time
 * - Capture System.out/err output
 * - Filter theo level (ALL/INFO/WARN/ERROR)
 * - Search log entries
 * - Export log file
 * - Auto-scroll toggle
 */
public class LogViewerPanel extends JPanel {

    // --- Constants ---
    private static final Font FONT_LOG = new Font("Consolas", Font.PLAIN, 12);
    private static final Font FONT_UI = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 13);

    private static final Color COLOR_BG_DARK = new Color(30, 30, 30);
    private static final Color COLOR_FG_DEFAULT = new Color(204, 204, 204);
    private static final Color COLOR_INFO = new Color(86, 182, 194);
    private static final Color COLOR_WARN = new Color(229, 192, 123);
    private static final Color COLOR_ERROR = new Color(224, 108, 117);
    private static final Color COLOR_DEBUG = new Color(152, 195, 121);
    private static final Color COLOR_TIMESTAMP = new Color(127, 132, 142);

    // --- Components ---
    private JTextPane logPane;
    private StyledDocument logDoc;
    private JTextField txtSearch;
    private JComboBox<String> cbFilter;
    private JLabel lblLineCount, lblSearchCount;
    private JToggleButton btnAutoScroll;
    private JToggleButton btnCapture;

    // --- State ---
    private boolean autoScroll = true;
    private boolean captureEnabled = true;
    private final List<LogEntry> logEntries = Collections.synchronizedList(new ArrayList<>());
    private static final int MAX_LOG_ENTRIES = 10000;

    // --- Capture Streams ---
    private PrintStream originalOut;
    private PrintStream originalErr;
    private ScheduledExecutorService scheduler;

    private static class LogEntry {
        String timestamp;
        String level;
        String message;

        LogEntry(String timestamp, String level, String message) {
            this.timestamp = timestamp;
            this.level = level;
            this.message = message;
        }
    }

    public LogViewerPanel() {
        setLayout(new BorderLayout(0, 5));
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        initUI();
        startCapture();
        startAutoRefresh();

        addLogEntry("INFO", "Log Viewer initialized. Capturing server output...");
    }

    private void initUI() {
        // ===== TOP: Header + Controls =====
        JPanel topPanel = new JPanel(new BorderLayout(0, 8));
        topPanel.setOpaque(false);

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel title = new JLabel("📋 Server Log Viewer");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(new Color(50, 50, 50));

        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        statsPanel.setOpaque(false);

        lblLineCount = new JLabel("Lines: 0");
        lblLineCount.setFont(FONT_BOLD);
        lblLineCount.setForeground(new Color(0, 120, 215));

        lblSearchCount = new JLabel("");
        lblSearchCount.setFont(FONT_BOLD);
        lblSearchCount.setForeground(new Color(255, 152, 0));

        statsPanel.add(lblLineCount);
        statsPanel.add(lblSearchCount);

        headerPanel.add(title, BorderLayout.WEST);
        headerPanel.add(statsPanel, BorderLayout.EAST);

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        toolbar.setOpaque(false);

        // Filter
        cbFilter = new JComboBox<>(new String[]{"ALL", "INFO", "WARN", "ERROR", "DEBUG"});
        cbFilter.setFont(FONT_UI);
        cbFilter.setPreferredSize(new Dimension(100, 30));
        cbFilter.addActionListener(e -> refreshDisplay());

        // Search
        txtSearch = new JTextField(20);
        txtSearch.setFont(FONT_UI);
        txtSearch.setPreferredSize(new Dimension(200, 30));
        txtSearch.putClientProperty("JTextField.placeholderText", "Tìm kiếm trong log...");
        txtSearch.getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override
            public void update() {
                refreshDisplay();
            }
        });

        // Auto-scroll toggle
        btnAutoScroll = new JToggleButton("📜 Auto-scroll: ON");
        btnAutoScroll.setSelected(true);
        btnAutoScroll.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnAutoScroll.setFocusPainted(false);
        btnAutoScroll.setBackground(new Color(40, 167, 69));
        btnAutoScroll.setForeground(Color.WHITE);
        btnAutoScroll.addActionListener(e -> {
            autoScroll = btnAutoScroll.isSelected();
            btnAutoScroll.setText("📜 Auto-scroll: " + (autoScroll ? "ON" : "OFF"));
            btnAutoScroll.setBackground(autoScroll ? new Color(40, 167, 69) : new Color(108, 117, 125));
        });

        // Capture toggle
        btnCapture = new JToggleButton("🔴 Capture: ON");
        btnCapture.setSelected(true);
        btnCapture.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnCapture.setFocusPainted(false);
        btnCapture.setBackground(new Color(220, 53, 69));
        btnCapture.setForeground(Color.WHITE);
        btnCapture.addActionListener(e -> {
            captureEnabled = btnCapture.isSelected();
            btnCapture.setText("🔴 Capture: " + (captureEnabled ? "ON" : "OFF"));
            btnCapture.setBackground(captureEnabled ? new Color(220, 53, 69) : new Color(108, 117, 125));
        });

        // Action buttons
        JButton btnClear = ServerGuiUtils.createStyledButton("🗑 Xóa Log", new Color(108, 117, 125), Color.WHITE);
        btnClear.addActionListener(e -> clearLog());

        JButton btnExport = ServerGuiUtils.createStyledButton("📤 Export", new Color(0, 123, 255), Color.WHITE);
        btnExport.addActionListener(e -> exportLog());

        JButton btnLoadFile = ServerGuiUtils.createStyledButton("📂 Đọc File Log", new Color(40, 167, 69), Color.WHITE);
        btnLoadFile.addActionListener(e -> loadLogFromFile());

        toolbar.add(new JLabel("Level:"));
        toolbar.add(cbFilter);
        toolbar.add(Box.createHorizontalStrut(10));
        toolbar.add(new JLabel("🔍"));
        toolbar.add(txtSearch);
        toolbar.add(Box.createHorizontalStrut(10));
        toolbar.add(btnAutoScroll);
        toolbar.add(btnCapture);
        toolbar.add(btnClear);
        toolbar.add(btnExport);
        toolbar.add(btnLoadFile);

        topPanel.add(headerPanel, BorderLayout.NORTH);
        topPanel.add(toolbar, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);

        // ===== CENTER: Log View =====
        logPane = new JTextPane();
        logPane.setEditable(false);
        logPane.setBackground(COLOR_BG_DARK);
        logPane.setCaretColor(Color.WHITE);
        logPane.setFont(FONT_LOG);
        logPane.setForeground(COLOR_FG_DEFAULT);

        logDoc = logPane.getStyledDocument();

        // Define styles
        Style defaultStyle = logDoc.addStyle("default", null);
        StyleConstants.setForeground(defaultStyle, COLOR_FG_DEFAULT);
        StyleConstants.setFontFamily(defaultStyle, "Consolas");
        StyleConstants.setFontSize(defaultStyle, 12);

        Style infoStyle = logDoc.addStyle("INFO", defaultStyle);
        StyleConstants.setForeground(infoStyle, COLOR_INFO);

        Style warnStyle = logDoc.addStyle("WARN", defaultStyle);
        StyleConstants.setForeground(warnStyle, COLOR_WARN);
        StyleConstants.setBold(warnStyle, true);

        Style errorStyle = logDoc.addStyle("ERROR", defaultStyle);
        StyleConstants.setForeground(errorStyle, COLOR_ERROR);
        StyleConstants.setBold(errorStyle, true);

        Style debugStyle = logDoc.addStyle("DEBUG", defaultStyle);
        StyleConstants.setForeground(debugStyle, COLOR_DEBUG);

        Style timeStyle = logDoc.addStyle("timestamp", defaultStyle);
        StyleConstants.setForeground(timeStyle, COLOR_TIMESTAMP);

        JScrollPane scrollPane = new JScrollPane(logPane);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 60)));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);
    }

    // ===================================================================
    // CAPTURE SYSTEM OUTPUT
    // ===================================================================
    private void startCapture() {
        originalOut = System.out;
        originalErr = System.err;

        PrintStream capturedOut = new PrintStream(new OutputStream() {
            private final StringBuilder buffer = new StringBuilder();

            @Override
            public void write(int b) {
                if (b == '\n') {
                    String line = buffer.toString();
                    buffer.setLength(0);
                    originalOut.println(line);
                    if (captureEnabled) {
                        addLogEntry(detectLevel(line), line);
                    }
                } else if (b != '\r') {
                    buffer.append((char) b);
                }
            }
        }, true);

        PrintStream capturedErr = new PrintStream(new OutputStream() {
            private final StringBuilder buffer = new StringBuilder();

            @Override
            public void write(int b) {
                if (b == '\n') {
                    String line = buffer.toString();
                    buffer.setLength(0);
                    originalErr.println(line);
                    if (captureEnabled) {
                        addLogEntry("ERROR", line);
                    }
                } else if (b != '\r') {
                    buffer.append((char) b);
                }
            }
        }, true);

        System.setOut(capturedOut);
        System.setErr(capturedErr);
    }

    private String detectLevel(String line) {
        String upper = line.toUpperCase();
        if (upper.contains("ERROR") || upper.contains("EXCEPTION") || upper.contains("FAIL")) return "ERROR";
        if (upper.contains("WARN") || upper.contains("WARNING") || upper.contains("⚠")) return "WARN";
        if (upper.contains("DEBUG") || upper.contains("TRACE")) return "DEBUG";
        return "INFO";
    }

    // ===================================================================
    // LOG MANAGEMENT
    // ===================================================================
    private void addLogEntry(String level, String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        LogEntry entry = new LogEntry(timestamp, level, message);
        logEntries.add(entry);

        // Limit entries
        while (logEntries.size() > MAX_LOG_ENTRIES) {
            logEntries.remove(0);
        }

        // Append immediately to log pane (incremental, not full refresh)
        SwingUtilities.invokeLater(() -> {
            appendEntryToPane(entry);
            lblLineCount.setText("Lines: " + logEntries.size());
        });
    }

    private void appendEntryToPane(LogEntry entry) {
        // Check filter
        String filterLevel = (String) cbFilter.getSelectedItem();
        if (!"ALL".equals(filterLevel) && !filterLevel.equals(entry.level)) return;

        // Check search
        String searchText = txtSearch.getText().trim().toLowerCase();
        if (!searchText.isEmpty() && !entry.message.toLowerCase().contains(searchText)) return;

        try {
            // Timestamp
            logDoc.insertString(logDoc.getLength(), "[" + entry.timestamp + "] ",
                logDoc.getStyle("timestamp"));

            // Level badge
            String levelStr = String.format("%-5s ", entry.level);
            logDoc.insertString(logDoc.getLength(), levelStr,
                logDoc.getStyle(entry.level));

            // Message
            logDoc.insertString(logDoc.getLength(), entry.message + "\n",
                logDoc.getStyle("default"));

            // Auto-scroll
            if (autoScroll) {
                logPane.setCaretPosition(logDoc.getLength());
            }

            // Trim document if too large (keep last 5000 lines worth)
            if (logDoc.getLength() > 500000) {
                logDoc.remove(0, logDoc.getLength() / 4);
            }
        } catch (BadLocationException e) { /* ignore */ }
    }

    private void startAutoRefresh() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "LogViewer-Refresh");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(() -> {
            SwingUtilities.invokeLater(() -> {
                lblLineCount.setText("Lines: " + logEntries.size());
            });
        }, 1, 3, TimeUnit.SECONDS);
    }

    private void refreshDisplay() {
        SwingUtilities.invokeLater(() -> {
            String filterLevel = (String) cbFilter.getSelectedItem();
            String searchText = txtSearch.getText().trim().toLowerCase();

            try {
                logDoc.remove(0, logDoc.getLength());
            } catch (BadLocationException e) { /* ignore */ }

            int matchCount = 0;
            List<LogEntry> snapshot;
            synchronized (logEntries) {
                // Only show last 2000 for performance
                int start = Math.max(0, logEntries.size() - 2000);
                snapshot = new ArrayList<>(logEntries.subList(start, logEntries.size()));
            }

            for (LogEntry entry : snapshot) {
                // Level filter
                if (!"ALL".equals(filterLevel) && !filterLevel.equals(entry.level)) continue;

                // Search filter
                if (!searchText.isEmpty() && !entry.message.toLowerCase().contains(searchText)) continue;

                matchCount++;

                try {
                    // Timestamp
                    logDoc.insertString(logDoc.getLength(), "[" + entry.timestamp + "] ",
                        logDoc.getStyle("timestamp"));

                    // Level badge
                    String levelStr = String.format("%-5s ", entry.level);
                    logDoc.insertString(logDoc.getLength(), levelStr,
                        logDoc.getStyle(entry.level));

                    // Message
                    logDoc.insertString(logDoc.getLength(), entry.message + "\n",
                        logDoc.getStyle("default"));
                } catch (BadLocationException e) { /* ignore */ }
            }

            if (!searchText.isEmpty()) {
                lblSearchCount.setText("Found: " + matchCount);
            } else {
                lblSearchCount.setText("");
            }

            // Auto-scroll
            if (autoScroll) {
                logPane.setCaretPosition(logDoc.getLength());
            }
        });
    }

    private void clearLog() {
        logEntries.clear();
        try {
            logDoc.remove(0, logDoc.getLength());
        } catch (BadLocationException e) { /* ignore */ }
        lblLineCount.setText("Lines: 0");
        addLogEntry("INFO", "Log cleared.");
        refreshDisplay();
    }

    private void exportLog() {
        JFileChooser fc = new JFileChooser(".");
        fc.setSelectedFile(new File("server_log_" +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (PrintWriter pw = new PrintWriter(new FileWriter(fc.getSelectedFile()))) {
                synchronized (logEntries) {
                    for (LogEntry entry : logEntries) {
                        pw.printf("[%s] %-5s %s%n", entry.timestamp, entry.level, entry.message);
                    }
                }
                JOptionPane.showMessageDialog(this, "Đã export " + logEntries.size() + " dòng log.",
                    "Thành công", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void loadLogFromFile() {
        JFileChooser fc = new JFileChooser(".");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Log & Text files", "log", "txt"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            new Thread(() -> {
                try {
                    List<String> lines = Files.readAllLines(fc.getSelectedFile().toPath());
                    for (String line : lines) {
                        addLogEntry(detectLevel(line), line);
                    }
                    SwingUtilities.invokeLater(() -> {
                        refreshDisplay();
                        JOptionPane.showMessageDialog(this, "Đã load " + lines.size() + " dòng từ file.",
                            "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    });
                } catch (IOException e) {
                    SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this, "Lỗi: " + e.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE));
                }
            }).start();
        }
    }

    // ===================================================================
    // CLEANUP
    // ===================================================================
    public void stop() {
        if (scheduler != null) scheduler.shutdownNow();
        // Restore original streams
        if (originalOut != null) System.setOut(originalOut);
        if (originalErr != null) System.setErr(originalErr);
    }
}
