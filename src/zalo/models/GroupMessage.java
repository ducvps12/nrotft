/**
 * Author: MinhLuong
 * Trao Đổi: https://zalo.me/g/mjevun948
 */
package zalo.models;

import java.util.Map;

public class GroupMessage extends Message {

    public GroupMessage(String uid, Map<String, Object> data) {
        this.type = ThreadType.GROUP;
        this.data = data;
        this.threadId = (String) data.get("idTo");
        this.isSelf = "0".equals(data.get("uidFrom"));

        String uidFrom = (String) data.get("uidFrom");
        if ("0".equals(uidFrom)) {
            data.put("uidFrom", uid);
        }

        Object quoteObj = data.get("quote");
        if (quoteObj instanceof Map) {
            Map<String, Object> quote = (Map<String, Object>) quoteObj;
            Object ownerId = quote.get("ownerId");
            if (ownerId != null) {
                quote.put("ownerId", String.valueOf(ownerId));
            }
        }
    }
}
