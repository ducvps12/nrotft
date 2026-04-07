/**
 * Author: MinhLuong
 * Trao Đổi: https://zalo.me/g/mjevun948
 */
package zalo.utils;

public class ZaloApiError extends RuntimeException {

    private Integer errorCode;

    public ZaloApiError(String message) {
        super(message);
    }

    public ZaloApiError(String message, Throwable cause) {
        super(message, cause);
    }

    public ZaloApiError(String message, Integer errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public ZaloApiError(String message, Integer errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public Integer getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(Integer errorCode) {
        this.errorCode = errorCode;
    }
}
