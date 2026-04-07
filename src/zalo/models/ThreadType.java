/**
 * Author: MinhLuong
 * Trao Đổi: https://zalo.me/g/mjevun948
 */
package zalo.models;

public enum ThreadType {

    USER(0),
    GROUP(1);

    private final int value;

    ThreadType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static ThreadType fromValue(int value) {
        for (ThreadType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        return USER;
    }
}
