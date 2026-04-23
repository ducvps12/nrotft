import java.io.*;
import java.nio.file.*;
import java.nio.charset.*;
import java.util.*;

/**
 * Fixes double-encoded UTF-8 mojibake in Java source files.
 * Processes LINE-BY-LINE to safely handle files with mixed content
 * (some lines already fixed, some still mojibaked).
 */
public class FixEncoding {

    static int fixedCount = 0;
    static int skippedCount = 0;
    static int errorCount = 0;

    // Win-1252 Unicode -> Byte mapping (all 256 values)
    static final Map<Character, Byte> WIN1252 = new HashMap<>();
    static {
        for (int i = 0; i <= 0x7F; i++) WIN1252.put((char) i, (byte) i);
        for (int i = 0xA0; i <= 0xFF; i++) WIN1252.put((char) i, (byte) i);
        // Win-1252 specific (0x80-0x9F defined positions)
        WIN1252.put('\u20AC', (byte) 0x80); WIN1252.put('\u201A', (byte) 0x82);
        WIN1252.put('\u0192', (byte) 0x83); WIN1252.put('\u201E', (byte) 0x84);
        WIN1252.put('\u2026', (byte) 0x85); WIN1252.put('\u2020', (byte) 0x86);
        WIN1252.put('\u2021', (byte) 0x87); WIN1252.put('\u02C6', (byte) 0x88);
        WIN1252.put('\u2030', (byte) 0x89); WIN1252.put('\u0160', (byte) 0x8A);
        WIN1252.put('\u2039', (byte) 0x8B); WIN1252.put('\u0152', (byte) 0x8C);
        WIN1252.put('\u017D', (byte) 0x8E); WIN1252.put('\u2018', (byte) 0x91);
        WIN1252.put('\u2019', (byte) 0x92); WIN1252.put('\u201C', (byte) 0x93);
        WIN1252.put('\u201D', (byte) 0x94); WIN1252.put('\u2022', (byte) 0x95);
        WIN1252.put('\u2013', (byte) 0x96); WIN1252.put('\u2014', (byte) 0x97);
        WIN1252.put('\u02DC', (byte) 0x98); WIN1252.put('\u2122', (byte) 0x99);
        WIN1252.put('\u0161', (byte) 0x9A); WIN1252.put('\u203A', (byte) 0x9B);
        WIN1252.put('\u0153', (byte) 0x9C); WIN1252.put('\u017E', (byte) 0x9E);
        WIN1252.put('\u0178', (byte) 0x9F);
        // Undefined C1 control positions (critical for Đ, ề, ọ, ỏ, ờ)
        WIN1252.put('\u0081', (byte) 0x81); WIN1252.put('\u008D', (byte) 0x8D);
        WIN1252.put('\u008F', (byte) 0x8F); WIN1252.put('\u0090', (byte) 0x90);
        WIN1252.put('\u009D', (byte) 0x9D);
    }

    /** Check if a line contains mojibake patterns */
    static boolean hasMojibake(String line) {
        for (int i = 0; i < line.length() - 1; i++) {
            char c = line.charAt(i);
            char next = line.charAt(i + 1);
            // Ã + Latin supplement = double-encoded 2-byte UTF-8
            if (c == '\u00C3' && next >= '\u0080' && next <= '\u00BF') return true;
            // Ä + Win-1252 specific = double-encoded đ/Đ range
            if (c == '\u00C4' && (WIN1252.containsKey(next) || (next >= '\u0080' && next <= '\u00BF'))) return true;
            // á» = double-encoded 3-byte Vietnamese (U+1Exx range)
            if (c == '\u00E1' && (next == '\u00BB' || next == '\u00BA')) return true;
            // Æ° = double-encoded ư
            if (c == '\u00C6' && next == '\u00B0') return true;
            // Å© = double-encoded ũ family
            if (c == '\u00C5' && (next == '\u00A9' || next == '\u00A8' || next == '\u00A3')) return true;
        }
        return false;
    }

    /** Encode string to Win-1252 bytes using custom mapper */
    static byte[] toWin1252(String text) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            Byte b = WIN1252.get(c);
            if (b != null) {
                out.write(b & 0xFF);
            } else {
                // Character not in Win-1252 - should not be in mojibake lines
                // Write as UTF-8 bytes (passthrough for already-correct chars)
                byte[] utf8 = String.valueOf(c).getBytes(StandardCharsets.UTF_8);
                out.write(utf8, 0, utf8.length);
            }
        }
        return out.toByteArray();
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java FixEncoding <src_directory> [--dry-run]");
            return;
        }

        Path srcDir = Paths.get(args[0]);
        boolean dryRun = args.length > 1 && args[1].equals("--dry-run");
        Charset utf8 = StandardCharsets.UTF_8;

        System.out.println("=== Java Encoding Fixer v3 (Line-by-Line) ===");
        System.out.println("Scanning: " + srcDir);
        System.out.println("Mode: " + (dryRun ? "DRY RUN" : "LIVE FIX"));
        System.out.println();

        Files.walk(srcDir)
            .filter(p -> p.toString().endsWith(".java"))
            .sorted()
            .forEach(path -> {
                try {
                    byte[] rawBytes = Files.readAllBytes(path);

                    // Remove BOM if present
                    int offset = 0;
                    if (rawBytes.length >= 3 &&
                        (rawBytes[0] & 0xFF) == 0xEF &&
                        (rawBytes[1] & 0xFF) == 0xBB &&
                        (rawBytes[2] & 0xFF) == 0xBF) {
                        offset = 3;
                    }

                    byte[] contentBytes = Arrays.copyOfRange(rawBytes, offset, rawBytes.length);
                    String fullText = new String(contentBytes, utf8);

                    // Check if file has any mojibake at all
                    if (!hasMojibake(fullText)) {
                        skippedCount++;
                        return;
                    }

                    // Process line by line
                    String[] lines = fullText.split("\r?\n", -1);
                    StringBuilder result = new StringBuilder();
                    int fixedLines = 0;

                    for (int i = 0; i < lines.length; i++) {
                        if (i > 0) result.append('\n');

                        String line = lines[i];
                        if (hasMojibake(line)) {
                            // Fix this line: encode as Win-1252, decode as UTF-8
                            byte[] reverted = toWin1252(line);
                            String fixed = new String(reverted, utf8);
                            result.append(fixed);
                            fixedLines++;
                        } else {
                            // Keep this line as-is
                            result.append(line);
                        }
                    }

                    String fixedText = result.toString();

                    if (dryRun) {
                        System.out.println("WOULD FIX (" + fixedLines + " lines): " + srcDir.relativize(path));
                    } else {
                        Files.write(path, fixedText.getBytes(utf8));
                        System.out.println("FIXED (" + fixedLines + " lines): " + srcDir.relativize(path));
                    }
                    fixedCount++;

                } catch (Exception e) {
                    errorCount++;
                    System.err.println("ERROR: " + path + " - " + e.getMessage());
                    e.printStackTrace();
                }
            });

        System.out.println();
        System.out.println("=== Summary ===");
        System.out.println((dryRun ? "Would fix" : "Fixed") + ": " + fixedCount);
        System.out.println("Skipped: " + skippedCount);
        System.out.println("Errors: " + errorCount);
    }
}
