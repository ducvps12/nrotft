package zalo.login;

/**
 * Author: MinhLuong
 * Trao Đổi: https://zalo.me/g/mjevun948
 */
public class ZaloOptions {

    private Boolean selfListen;
    private Boolean checkUpdate;
    private Boolean logging;
    private Integer apiType;
    private Integer apiVersion;

    public boolean isSelfListen() {
        return selfListen != null ? selfListen : true;
    }

    public void setSelfListen(Boolean selfListen) {
        this.selfListen = selfListen;
    }

    public boolean isCheckUpdate() {
        return checkUpdate != null ? checkUpdate : false;
    }

    public void setCheckUpdate(Boolean checkUpdate) {
        this.checkUpdate = checkUpdate;
    }

    public boolean isLogging() {
        return logging != null ? logging : false;
    }

    public void setLogging(Boolean logging) {
        this.logging = logging;
    }

    public Integer getApiType() {
        return apiType;
    }

    public void setApiType(Integer apiType) {
        this.apiType = apiType;
    }

    public Integer getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(Integer apiVersion) {
        this.apiVersion = apiVersion;
    }
}
