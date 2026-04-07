/**
 * Author: MinhLuong
 * Trao Đổi: https://zalo.me/g/mjevun948
 */
package zalo.utils;

import zalo.services.HttpServices;
import java.util.Map;

public class Reponse {

    public static ResponseResult handleZaloResponse(Context ctx, HttpServices.HttpResponse response,
            boolean isEncrypted) {
        ResponseResult result = new ResponseResult();

        if (!response.isOk()) {
            result.setError(new ErrorInfo("Request failed with status code " + response.getStatusCode(), null));
            return result;
        }

        try {
            Object parsed = Json.parse(response.getBody());
            if (!(parsed instanceof Map)) {
                throw new RuntimeException("Expected JSON object, got: " + parsed.getClass());
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
                    result.setError(new ErrorInfo(errorMessage, errorCode));
                    return result;
                }
            }

            Map<String, Object> decodedData;
            if (isEncrypted && jsonData.containsKey("data") && ctx.getSecretKey() != null) {
                String encryptedData = String.valueOf(jsonData.get("data"));
                String decrypted = Crypto.decodeAES(ctx.getSecretKey(), encryptedData);
                if (decrypted != null) {
                    Object decryptedParsed = Json.parse(decrypted);
                    if (decryptedParsed instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> parsedMap = (Map<String, Object>) decryptedParsed;
                        decodedData = parsedMap;
                    } else {
                        decodedData = jsonData;
                    }
                } else {
                    decodedData = jsonData;
                }
            } else {
                decodedData = jsonData;
            }

            if (decodedData.containsKey("error_code")) {
                Object errorCodeObj = decodedData.get("error_code");
                int errorCode = errorCodeObj instanceof Number ? ((Number) errorCodeObj).intValue() : 0;
                if (errorCode != 0) {
                    String errorMessage = decodedData.containsKey("error_message")
                            ? String.valueOf(decodedData.get("error_message"))
                            : "Unknown error";
                    result.setError(new ErrorInfo(errorMessage, errorCode));
                    return result;
                }
            }

            Object dataToReturn;
            if (decodedData.containsKey("data")) {
                Object dataValue = decodedData.get("data");
                dataToReturn = dataValue;
            } else {
                dataToReturn = decodedData;
            }
            result.setData(dataToReturn);
        } catch (Exception e) {
            result.setError(new ErrorInfo("Failed to parse response data: " + e.getMessage(), null));
        }

        return result;
    }

    public static Object resolveResponse(Context ctx, HttpServices.HttpResponse response,
            ResponseCallback callback, boolean isEncrypted) {
        ResponseResult result = handleZaloResponse(ctx, response, isEncrypted);
        if (result.getError() != null) {
            throw new ZaloApiError(result.getError().getMessage(), result.getError().getCode());
        }
        if (callback != null) {
            return callback.apply(result);
        }
        return result.getData();
    }

    public static class ResponseResult {
        private Object data;
        private ErrorInfo error;

        public Object getData() {
            return data;
        }

        public void setData(Object data) {
            this.data = data;
        }

        public ErrorInfo getError() {
            return error;
        }

        public void setError(ErrorInfo error) {
            this.error = error;
        }
    }

    public static class ErrorInfo {
        private String message;
        private Integer code;

        public ErrorInfo(String message, Integer code) {
            this.message = message;
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Integer getCode() {
            return code;
        }

        public void setCode(Integer code) {
            this.code = code;
        }
    }

    @FunctionalInterface
    public interface ResponseCallback {
        Object apply(ResponseResult result);
    }
}
