package zalo.login;

/**
 * Author: MinhLuong
 * Trao Đổi: https://zalo.me/g/mjevun948
 */
public class Credentials {

    private String imei;
    private Object cookie;
    private String userAgent;
    private String language;

    public String getImei() {
        return imei;
    }

    public void setImei(String imei) {
        this.imei = imei;
    }

    public Object getCookie() {
        return cookie;
    }

    public void setCookie(Object cookie) {
        this.cookie = cookie;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
