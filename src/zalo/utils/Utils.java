/**
 * Author: MinhLuong
 * Trao Đổi: https://zalo.me/g/mjevun948
 */
package zalo.utils;

import java.security.MessageDigest;
import java.util.Map;
import java.util.UUID;

public class Utils {

    public static String generateZaloUUID(String userAgent) {
        try {
            String uuid = UUID.randomUUID().toString();
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(userAgent.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return uuid + "-" + sb.toString();
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }

    public static boolean hasOwn(Object obj, String key) {
        if (obj instanceof Map) {
            return ((Map<?, ?>) obj).containsKey(key);
        }
        return false;
    }
}
