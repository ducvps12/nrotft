/**
 * Author: MinhLuong
 * Trao Đổi: https://zalo.me/g/mjevun948
 */
package zalo.utils;

import zalo.utils.Context;

public class ContextSession {

    public static boolean isContextSession(Context ctx) {
        return ctx != null && ctx.getSecretKey() != null && !ctx.getSecretKey().isEmpty();
    }
}
