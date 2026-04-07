package firewall;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Rate Limiter per IP - Token Bucket Algorithm
 * Chống DDoS Layer 4 bằng cách giới hạn kết nối/giây per IP
 */
public class RateLimiter {

    private static final RateLimiter INSTANCE = new RateLimiter();

    // Theo dõi số kết nối per IP trong 1 giây
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    // Theo dõi số kết nối đồng thời per IP
    private final Map<String, AtomicInteger> concurrentConnections = new ConcurrentHashMap<>();
    // Theo dõi IP bị block tạm thời (auto-ban)
    private final Map<String, Long> tempBanned = new ConcurrentHashMap<>();
    // Thống kê
    private final AtomicLong totalAccepted = new AtomicLong(0);
    private final AtomicLong totalRejected = new AtomicLong(0);
    private final Map<String, AtomicInteger> rejectCountPerIP = new ConcurrentHashMap<>();

    public static RateLimiter getInstance() {
        return INSTANCE;
    }

    /**
     * Kiểm tra xem IP có được phép kết nối không
     * @return true nếu cho phép, false nếu reject
     */
    public boolean allowConnection(String ip) {
        FirewallConfig config = FirewallConfig.getInstance();

        if (!config.rateLimitEnabled) {
            totalAccepted.incrementAndGet();
            return true;
        }

        // Check temp ban
        Long banExpiry = tempBanned.get(ip);
        if (banExpiry != null) {
            if (System.currentTimeMillis() < banExpiry) {
                totalRejected.incrementAndGet();
                return false;
            } else {
                tempBanned.remove(ip);
            }
        }

        // Check concurrent connections
        AtomicInteger concurrent = concurrentConnections.computeIfAbsent(ip, k -> new AtomicInteger(0));
        if (concurrent.get() >= config.maxConcurrentPerIP) {
            rejectAndMaybeBlock(ip, config);
            return false;
        }

        // Token bucket rate limit
        TokenBucket bucket = buckets.computeIfAbsent(ip,
                k -> new TokenBucket(config.maxConnectionsPerSecond, config.burstSize));
        
        if (!bucket.tryConsume()) {
            rejectAndMaybeBlock(ip, config);
            return false;
        }

        totalAccepted.incrementAndGet();
        concurrent.incrementAndGet();
        return true;
    }

    /**
     * Giảm concurrent count khi connection đóng
     */
    public void releaseConnection(String ip) {
        AtomicInteger concurrent = concurrentConnections.get(ip);
        if (concurrent != null) {
            concurrent.decrementAndGet();
        }
    }

    /**
     * Reject + tự động block nếu vi phạm nhiều lần
     */
    private void rejectAndMaybeBlock(String ip, FirewallConfig config) {
        totalRejected.incrementAndGet();
        AtomicInteger rejects = rejectCountPerIP.computeIfAbsent(ip, k -> new AtomicInteger(0));
        int count = rejects.incrementAndGet();

        // Nếu bị reject > 10 lần trong session → auto ban
        if (count > 10 && config.autoBlockEnabled) {
            long banDuration = config.blockDurationMinutes * 60L * 1000L;
            tempBanned.put(ip, System.currentTimeMillis() + banDuration);

            // Alert
            TelegramAlert.getInstance().sendAlert(
                "🚨 *AUTO-BLOCK IP*\n" +
                "IP: `" + ip + "`\n" +
                "Lý do: Vượt rate limit (" + count + " violations)\n" +
                "Thời gian block: " + config.blockDurationMinutes + " phút"
            );
        }
    }

    /**
     * Xóa ban tạm thời
     */
    public void unban(String ip) {
        tempBanned.remove(ip);
        rejectCountPerIP.remove(ip);
    }

    /**
     * Xóa tất cả ban
     */
    public void unbanAll() {
        tempBanned.clear();
        rejectCountPerIP.clear();
    }

    /**
     * Kiểm tra IP có bị ban không
     */
    public boolean isBanned(String ip) {
        Long expiry = tempBanned.get(ip);
        if (expiry == null) return false;
        if (System.currentTimeMillis() >= expiry) {
            tempBanned.remove(ip);
            return false;
        }
        return true;
    }

    // ===== THỐNG KÊ =====
    public long getTotalAccepted() { return totalAccepted.get(); }
    public long getTotalRejected() { return totalRejected.get(); }
    public int getBannedCount() { return tempBanned.size(); }
    public int getActiveIPCount() { return concurrentConnections.size(); }
    public Map<String, Long> getTempBanned() { return tempBanned; }
    public Map<String, AtomicInteger> getConcurrentConnections() { return concurrentConnections; }

    /**
     * Dọn dẹp các entry cũ
     */
    public void cleanup() {
        long now = System.currentTimeMillis();
        tempBanned.entrySet().removeIf(e -> now >= e.getValue());
        concurrentConnections.entrySet().removeIf(e -> e.getValue().get() <= 0);
        buckets.entrySet().removeIf(e -> now - e.getValue().lastRefill > 60000);
    }

    // ===== TOKEN BUCKET =====
    private static class TokenBucket {
        private final int maxTokens;
        private final int refillRate; // tokens per second
        private double tokens;
        private long lastRefill;

        TokenBucket(int refillRate, int maxTokens) {
            this.refillRate = refillRate;
            this.maxTokens = maxTokens;
            this.tokens = maxTokens;
            this.lastRefill = System.currentTimeMillis();
        }

        synchronized boolean tryConsume() {
            refill();
            if (tokens >= 1) {
                tokens--;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            double elapsed = (now - lastRefill) / 1000.0;
            tokens = Math.min(maxTokens, tokens + elapsed * refillRate);
            lastRefill = now;
        }
    }
}
