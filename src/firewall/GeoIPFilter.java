package firewall;

import utils.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Geo-IP Filter - Chặn IP ngoài Việt Nam
 * Sử dụng danh sách CIDR range của VN từ APNIC
 * Hỗ trợ cache để tăng tốc độ check
 */
public class GeoIPFilter {

    private static final GeoIPFilter INSTANCE = new GeoIPFilter();
    private static final String VN_RANGES_FILE = "vn_ip_ranges.txt";
    private static final String APNIC_URL = "https://stat.ripe.net/data/country-resource-list/data.json?resource=VN&v4_format=prefix";

    private final List<CIDRRange> vnRanges = new ArrayList<>();
    private final ConcurrentHashMap<String, Boolean> cache = new ConcurrentHashMap<>();
    private volatile boolean loaded = false;

    public static GeoIPFilter getInstance() {
        return INSTANCE;
    }

    /**
     * Load danh sách IP range Việt Nam
     */
    public void load() {
        try {
            File f = new File(VN_RANGES_FILE);
            if (f.exists()) {
                loadFromFile(f);
            } else {
                loadDefaultRanges();
                saveToFile();
            }
            loaded = true;
            Logger.log("GeoIP Filter loaded: " + vnRanges.size() + " VN IP ranges.");
        } catch (Exception e) {
            Logger.error("GeoIP load error: " + e.getMessage());
            loadDefaultRanges();
            loaded = true;
        }
    }

    /**
     * Kiểm tra IP có thuộc Việt Nam không
     * @return true nếu là IP Việt Nam
     */
    public boolean isVietnamIP(String ip) {
        if (!loaded) load();
        if (ip == null || ip.isEmpty()) return false;

        // Localhost luôn cho phép
        if (ip.equals("127.0.0.1") || ip.equals("::1") || ip.startsWith("192.168.") 
            || ip.startsWith("10.") || ip.startsWith("172.16.")) {
            return true;
        }

        // Check cache
        Boolean cached = cache.get(ip);
        if (cached != null) return cached;

        // Check ranges
        try {
            long ipLong = ipToLong(ip);
            for (CIDRRange range : vnRanges) {
                if (range.contains(ipLong)) {
                    cache.put(ip, true);
                    return true;
                }
            }
        } catch (Exception e) {
            return true; // Nếu lỗi parse, cho phép (an toàn hơn)
        }

        cache.put(ip, false);
        return false;
    }

    /**
     * Thêm custom IP range (CIDR notation, ví dụ: "1.0.0.0/8")
     */
    public void addRange(String cidr) {
        try {
            vnRanges.add(new CIDRRange(cidr));
            cache.clear(); // Invalidate cache
        } catch (Exception e) {
            Logger.error("Invalid CIDR: " + cidr);
        }
    }

    /**
     * Xóa cache (gọi khi thay đổi config)
     */
    public void clearCache() {
        cache.clear();
    }

    public int getRangeCount() {
        return vnRanges.size();
    }

    // ===== LOAD/SAVE =====

    private void loadFromFile(File f) throws Exception {
        List<String> lines = Files.readAllLines(f.toPath());
        vnRanges.clear();
        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty() && !line.startsWith("#")) {
                try {
                    vnRanges.add(new CIDRRange(line));
                } catch (Exception ignored) {}
            }
        }
    }

    private void saveToFile() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("# Vietnam IP Ranges (CIDR)\n");
            sb.append("# Tự động tạo bởi Firewall System\n");
            sb.append("# Thêm/sửa range theo format: x.x.x.x/prefix\n\n");
            for (CIDRRange r : vnRanges) {
                sb.append(r.cidr).append("\n");
            }
            Files.writeString(Paths.get(VN_RANGES_FILE), sb.toString());
        } catch (Exception e) {
            Logger.error("Error saving VN ranges: " + e.getMessage());
        }
    }

    /**
     * Danh sách IP range chính của Việt Nam (APNIC allocated)
     * Bao gồm các nhà mạng lớn: VNPT, Viettel, FPT, CMC, SPT...
     */
    private void loadDefaultRanges() {
        vnRanges.clear();
        String[] defaults = {
            // VNPT
            "14.160.0.0/11", "14.224.0.0/11", "27.64.0.0/12", "27.68.0.0/14",
            "113.160.0.0/11", "113.176.0.0/12", "113.185.0.0/16",
            "115.72.0.0/13", "115.84.0.0/14",
            "123.16.0.0/12", "123.24.0.0/13",
            // Viettel
            "1.52.0.0/14", "1.56.0.0/13",
            "5.180.128.0/18",
            "14.0.0.0/11",
            "27.72.0.0/13",
            "42.112.0.0/13", "42.96.0.0/14",
            "49.236.192.0/18",
            "58.186.0.0/15",
            "171.224.0.0/11",
            "221.132.0.0/14",
            // FPT
            "1.0.128.0/17", "1.1.0.0/16",
            "27.2.0.0/15", "27.0.0.0/17",
            "42.1.64.0/18",
            "113.22.0.0/15", "113.20.0.0/15",
            "118.68.0.0/14",
            "210.245.0.0/16",
            // CMC, SPT, Mobifone, SCTV...
            "103.1.236.0/22", "103.7.36.0/22",
            "103.9.0.0/22", "103.9.76.0/22",
            "103.21.148.0/22", "103.28.36.0/22",
            "103.37.28.0/22", "103.38.132.0/22",
            "103.48.80.0/22", "103.53.168.0/22",
            "103.56.156.0/22", "103.57.220.0/22",
            "103.63.104.0/22", "103.68.80.0/22",
            "103.69.80.0/22", "103.70.28.0/22",
            "103.74.100.0/22", "103.75.184.0/22",
            "103.77.160.0/22", "103.78.36.0/22",
            "103.79.76.0/22", "103.82.24.0/22",
            "103.83.120.0/22", "103.85.84.0/22",
            "103.90.220.0/22", "103.91.68.0/22",
            "103.95.196.0/22", "103.97.124.0/22",
            "103.99.20.0/22", "103.100.134.0/23",
            "103.101.160.0/22", "103.107.180.0/22",
            "103.110.84.0/22", "103.116.100.0/22",
            "103.119.56.0/22", "103.124.92.0/22",
            "103.126.156.0/22", "103.129.188.0/22",
            "103.130.208.0/22", "103.131.172.0/22",
            "103.135.12.0/22", "103.137.4.0/22",
            "103.140.36.0/22", "103.143.204.0/22",
            "103.148.56.0/22", "103.149.28.0/22",
            "103.153.72.0/22", "103.156.56.0/22",
            "103.159.48.0/22", "103.160.0.0/22",
            "103.162.20.0/22", "103.163.52.0/22",
            "103.166.180.0/22", "103.167.88.0/22",
            "103.170.248.0/22", "103.173.228.0/22",
            "103.176.108.0/22", "103.178.228.0/22",
            "103.179.188.0/22", "103.183.116.0/22",
            "103.186.96.0/22", "103.190.212.0/22",
            "103.196.20.0/22", "103.199.76.0/22",
            "103.200.20.0/22", "103.205.96.0/22",
            "103.207.36.0/22", "103.209.32.0/22",
            "103.216.116.0/22", "103.221.220.0/22",
            "103.226.248.0/22", "103.229.40.0/22",
            "103.232.120.0/22", "103.234.36.0/22",
            "103.238.68.0/22", "103.243.104.0/22",
            "103.245.248.0/22", "103.252.0.0/22",
            "103.253.88.0/22", "103.255.236.0/22",
            // Mobifone
            "125.212.192.0/18", "125.214.0.0/15",
            "116.96.0.0/12",
            "203.113.128.0/17", "203.162.0.0/16",
            // Misc VN
            "180.148.0.0/15",
            "183.80.0.0/13",
            "222.252.0.0/14",
            "101.99.0.0/16", "101.96.0.0/14"
        };
        for (String cidr : defaults) {
            try {
                vnRanges.add(new CIDRRange(cidr));
            } catch (Exception e) {
                Logger.error("Invalid default CIDR: " + cidr);
            }
        }
    }

    // ===== CIDR RANGE =====

    private static long ipToLong(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length != 4) throw new IllegalArgumentException("Invalid IPv4: " + ip);
        long result = 0;
        for (int i = 0; i < 4; i++) {
            result = (result << 8) | (Integer.parseInt(parts[i]) & 0xFF);
        }
        return result;
    }

    private static class CIDRRange {
        final String cidr;
        final long networkAddress;
        final long mask;

        CIDRRange(String cidr) {
            this.cidr = cidr.trim();
            String[] parts = this.cidr.split("/");
            this.networkAddress = ipToLong(parts[0]);
            int prefix = Integer.parseInt(parts[1]);
            this.mask = prefix == 0 ? 0 : (0xFFFFFFFFL << (32 - prefix));
        }

        boolean contains(long ip) {
            return (ip & mask) == (networkAddress & mask);
        }
    }
}
