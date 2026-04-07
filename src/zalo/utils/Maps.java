/**
 * Author: MinhLuong
 * Trao Đổi: https://zalo.me/g/mjevun948
 */
package zalo.utils;

import java.util.Iterator;
import java.util.Map;

public class Maps {

    public static <K, V> Map<K, V> removeUndefinedKeys(Map<K, V> map) {
        if (map == null) {
            return map;
        }
        Iterator<Map.Entry<K, V>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<K, V> entry = iterator.next();
            if (entry.getValue() == null) {
                iterator.remove();
            }
        }
        return map;
    }
}
