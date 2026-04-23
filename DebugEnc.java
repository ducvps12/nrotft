import java.io.*;
import java.nio.file.*;
import java.nio.charset.*;
import java.util.*;

public class DebugEnc {
    static final Map<Character, Byte> M = new HashMap<>();
    static {
        for (int i = 0; i <= 0x7F; i++) M.put((char)i, (byte)i);
        for (int i = 0xA0; i <= 0xFF; i++) M.put((char)i, (byte)i);
        M.put('\u20AC',(byte)0x80); M.put('\u201A',(byte)0x82); M.put('\u0192',(byte)0x83);
        M.put('\u201E',(byte)0x84); M.put('\u2026',(byte)0x85); M.put('\u2020',(byte)0x86);
        M.put('\u2021',(byte)0x87); M.put('\u02C6',(byte)0x88); M.put('\u2030',(byte)0x89);
        M.put('\u0160',(byte)0x8A); M.put('\u2039',(byte)0x8B); M.put('\u0152',(byte)0x8C);
        M.put('\u017D',(byte)0x8E); M.put('\u2018',(byte)0x91); M.put('\u2019',(byte)0x92);
        M.put('\u201C',(byte)0x93); M.put('\u201D',(byte)0x94); M.put('\u2022',(byte)0x95);
        M.put('\u2013',(byte)0x96); M.put('\u2014',(byte)0x97); M.put('\u02DC',(byte)0x98);
        M.put('\u2122',(byte)0x99); M.put('\u0161',(byte)0x9A); M.put('\u203A',(byte)0x9B);
        M.put('\u0153',(byte)0x9C); M.put('\u017E',(byte)0x9E); M.put('\u0178',(byte)0x9F);
        M.put('\u0081',(byte)0x81); M.put('\u008D',(byte)0x8D); M.put('\u008F',(byte)0x8F);
        M.put('\u0090',(byte)0x90); M.put('\u009D',(byte)0x9D);
    }
    public static void main(String[] a) throws Exception {
        byte[] raw = Files.readAllBytes(Paths.get(a[0]));
        int off = (raw.length>=3 && (raw[0]&0xFF)==0xEF && (raw[1]&0xFF)==0xBB && (raw[2]&0xFF)==0xBF) ? 3 : 0;
        String text = new String(Arrays.copyOfRange(raw, off, raw.length), StandardCharsets.UTF_8);
        // Find chars NOT in map
        Set<Character> unmapped = new TreeSet<>();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (!M.containsKey(c)) unmapped.add(c);
        }
        System.out.println("Unmapped chars (" + unmapped.size() + "):");
        for (char c : unmapped) {
            System.out.printf("  U+%04X '%c'%n", (int)c, c);
        }
    }
}