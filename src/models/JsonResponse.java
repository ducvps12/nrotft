package models;

import lombok.Getter;

import java.util.List;

@Getter
public class JsonResponse {
    // ACB API uses "messageStatus" instead of "success"
    private String messageStatus;
    private boolean success;
    private String message;
    private List<TransactionHistory> data;

    /**
     * Check if the API response is successful - supports both ACB and MB format
     */
    public boolean isSuccess() {
        if (messageStatus != null) {
            return "success".equalsIgnoreCase(messageStatus);
        }
        return success;
    }
}
