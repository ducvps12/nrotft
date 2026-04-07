/**
 * Author: MinhLuong
 * Trao Đổi: https://zalo.me/g/mjevun948
 */
package zalo.apis;

import zalo.utils.Json;
import zalo.utils.Crypto;
import zalo.utils.Url;
import zalo.services.HttpServices;
import zalo.utils.Apis;
import zalo.utils.Context;
import zalo.utils.ZaloApiError;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class GetUserInfoApi {

    private Context ctx;
    private Apis api;

    public GetUserInfoApi(Context ctx, Apis api) {
        this.ctx = ctx;
        this.api = api;
    }

    public CompletableFuture<Map<String, Object>> getUserInfo(Object userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (userId == null) {
                    throw new ZaloApiError("Missing user id");
                }

                List<String> userIdList = new ArrayList<>();
                if (userId instanceof String) {
                    userIdList.add((String) userId);
                } else if (userId instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> list = (List<Object>) userId;
                    for (Object id : list) {
                        userIdList.add(String.valueOf(id));
                    }
                } else if (userId instanceof Object[]) {
                    for (Object id : (Object[]) userId) {
                        userIdList.add(String.valueOf(id));
                    }
                } else {
                    userIdList.add(String.valueOf(userId));
                }

                List<String> formattedIds = new ArrayList<>();
                for (String id : userIdList) {
                    if (id.contains("_")) {
                        formattedIds.add(id);
                    } else {
                        formattedIds.add(id + "_0");
                    }
                }

                Map<String, Object> serviceMap = api.getZpwServiceMap();
                String baseUrl;
                Object profileObj = serviceMap.get("profile");
                if (profileObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> profileServices = (List<String>) profileObj;
                    baseUrl = profileServices.get(0) + "/api/social/friend/getprofiles/v2";
                } else if (profileObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> profileMap = (Map<String, Object>) profileObj;
                    Object profile0Obj = profileMap.get("0");
                    if (profile0Obj instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<String> profileServices = (List<String>) profile0Obj;
                        baseUrl = profileServices.get(0) + "/api/social/friend/getprofiles/v2";
                    } else {
                        throw new ZaloApiError("Invalid profile services format");
                    }
                } else {
                    throw new ZaloApiError("Invalid profile services format");
                }
                String url = Url.makeURL(ctx, baseUrl, new HashMap<>());

                Object extraVerObj = ctx.getExtraVer();
                Map<String, Object> params = new HashMap<>();
                if (extraVerObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> extraVer = (Map<String, Object>) extraVerObj;
                    params.put("phonebook_version", extraVer.get("phonebook"));
                } else {
                    params.put("phonebook_version", "0");
                }
                params.put("friend_pversion_map", formattedIds);
                params.put("avatar_size", 120);
                params.put("language", ctx.getLanguage());
                params.put("show_online_status", 1);
                params.put("imei", ctx.getImei());

                String encryptedParams = Crypto.encodeAES(ctx.getSecretKey(),
                        Json.stringify(params));

                if (encryptedParams == null) {
                    throw new ZaloApiError("Failed to encrypt params");
                }

                HttpServices.RequestOptions options = new HttpServices.RequestOptions();
                options.setMethod("POST");
                options.setBody("params=" + java.net.URLEncoder.encode(encryptedParams, "UTF-8"));

                HttpServices.HttpResponse response = HttpServices.request(ctx, url, options);

                if (!response.isOk()) {
                    throw new ZaloApiError("Request failed with status code " + response.getStatusCode());
                }

                String responseBody = response.getBody();
                if (responseBody == null || responseBody.isEmpty()) {
                    throw new ZaloApiError("Empty response body");
                }

                Object parsed = Json.parse(responseBody);
                if (!(parsed instanceof Map)) {
                    throw new ZaloApiError("Expected JSON object in response");
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> jsonData = (Map<String, Object>) parsed;

                if (jsonData.containsKey("error_code")) {
                    Object errorCodeObj = jsonData.get("error_code");
                    int errorCode = errorCodeObj instanceof Number ? ((Number) errorCodeObj).intValue() : 0;
                    if (errorCode != 0) {
                        String errorMessage = jsonData.containsKey("error_message")
                                ? String.valueOf(jsonData.get("error_message"))
                                : "Unknown error";
                        throw new ZaloApiError(errorMessage, errorCode);
                    }
                }

                if (!jsonData.containsKey("data") || ctx.getSecretKey() == null) {
                    throw new ZaloApiError("Missing data field or secret key");
                }

                Object dataObj = jsonData.get("data");
                if (!(dataObj instanceof String)) {
                    throw new ZaloApiError("Data field is not a string");
                }

                String encryptedData = (String) dataObj;
                String decrypted = Crypto.decodeAES(ctx.getSecretKey(), encryptedData);

                if (decrypted == null || decrypted.isEmpty()) {
                    throw new ZaloApiError("Failed to decrypt data field");
                }

                Object decryptedParsed = Json.parse(decrypted);

                if (!(decryptedParsed instanceof Map)) {
                    throw new ZaloApiError("Decrypted data is not a JSON object");
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> decodedData = (Map<String, Object>) decryptedParsed;

                if (decodedData.containsKey("error_code")) {
                    Object errorCodeObj = decodedData.get("error_code");
                    int errorCode = errorCodeObj instanceof Number ? ((Number) errorCodeObj).intValue() : 0;
                    if (errorCode != 0) {
                        String errorMessage = decodedData.containsKey("error_message")
                                ? String.valueOf(decodedData.get("error_message"))
                                : "Unknown error";
                        throw new ZaloApiError(errorMessage, errorCode);
                    }
                }

                if (decodedData.containsKey("data")) {
                    Object dataValue = decodedData.get("data");
                    if (dataValue instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> result = (Map<String, Object>) dataValue;
                        return result;
                    } else {
                        Map<String, Object> result = new HashMap<>();
                        result.put("data", dataValue);
                        return result;
                    }
                } else {
                    return decodedData;
                }
            } catch (Exception e) {
                throw new ZaloApiError("Failed to get user info: " + e.getMessage(), e);
            }
        });
    }
}
