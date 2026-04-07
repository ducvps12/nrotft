/**
 * Author: MinhLuong
 * Trao Đổi: https://zalo.me/g/mjevun948
 */
package zalo.utils;

import zalo.services.ListenServices;
import zalo.apis.SendMessageApi;
import zalo.apis.GetUserInfoApi;
import zalo.apis.AddReactionApi;
import zalo.apis.DeleteMessageApi;
import zalo.apis.GetGroupInfoApi;
import zalo.apis.UpdateGroupSettingsApi;
import java.util.Map;

public class Apis {

    private Context ctx;
    private Map<String, Object> zpwServiceMap;
    private Object wsUrls;
    private ListenServices listener;

    public final SendMessageApi sendMessage;
    public final GetUserInfoApi getUserInfo;
    public final AddReactionApi addReaction;
    public final DeleteMessageApi deleteMessage;
    public final GetGroupInfoApi getGroupInfo;
    public final UpdateGroupSettingsApi updateGroupSettings;

    public Apis(Context ctx, Map<String, Object> zpwServiceMap, Object wsUrls) {
        this.ctx = ctx;
        this.zpwServiceMap = zpwServiceMap;
        this.wsUrls = wsUrls;
        this.listener = null;
        this.sendMessage = new SendMessageApi(ctx, this);
        this.getUserInfo = new GetUserInfoApi(ctx, this);
        this.addReaction = new AddReactionApi(ctx, this);
        this.deleteMessage = new DeleteMessageApi(ctx, this);
        this.getGroupInfo = new GetGroupInfoApi(ctx, this);
        this.updateGroupSettings = new UpdateGroupSettingsApi(ctx, this);
        this.listener = new ListenServices(ctx, this);
    }

    public Context getContext() {
        return ctx;
    }

    public Map<String, Object> getZpwServiceMap() {
        return zpwServiceMap;
    }

    public Object getWsUrls() {
        return wsUrls;
    }

    public ListenServices getListener() {
        return listener;
    }
}
