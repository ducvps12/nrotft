/**
 * Author: MinhLuong
 * Trao Đổi: https://zalo.me/g/mjevun948
 */
package zalo.message;

public class Message {

    public static int getClientMessageType(String msgType) {
        if (msgType == null) {
            return 1;
        }
        switch (msgType) {
            case "webchat":
                return 1;
            case "chat.voice":
                return 31;
            case "chat.photo":
                return 32;
            case "chat.sticker":
                return 36;
            case "chat.doodle":
                return 37;
            case "chat.recommended":
                return 38;
            case "chat.link":
                return 38;
            case "chat.video.msg":
                return 44;
            case "share.file":
                return 46;
            case "chat.gif":
                return 49;
            case "chat.location.new":
                return 43;
            default:
                return 1;
        }
    }
}
