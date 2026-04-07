/**
 * Author: MinhLuong
 * Trao Đổi: https://zalo.me/g/mjevun948
 */
package zalo.models;

import java.util.Map;

public class UserMessage extends Message {

    public UserMessage(String uid, Map<String, Object> data) {
        this.type = ThreadType.USER;
        this.data = data;

        String uidFrom = (String) data.get("uidFrom");
        String idTo = (String) data.get("idTo");

        if ("0".equals(uidFrom)) {
            this.threadId = idTo;
        } else {
            this.threadId = uidFrom;
        }

        this.isSelf = "0".equals(uidFrom);

        if ("0".equals(idTo)) {
            data.put("idTo", uid);
        }
        if ("0".equals(uidFrom)) {
            data.put("uidFrom", uid);
        }
    }
}
