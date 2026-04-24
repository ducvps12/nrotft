package models;

import lombok.Getter;

@Getter
public class TransactionHistory {
    // ACB API fields
    private double amount;
    private String description;
    private long postingDate;
    private String type;

    // Legacy MB fields (kept for backward compatibility)
    private String transactionDate;
    private String accountNo;
    private String creditAmount;
    private String debitAmount;
    private String currency;
    private String availableBalance;
    private String beneficiaryAccount;
    private String refNo;
    private String benAccountName;
    private String bankName;
    private String benAccountNo;
    private String dueDate;
    private String docId;
    private String transactionType;

    /**
     * Get the transaction amount - supports both ACB and MB format
     */
    public String getCreditAmount() {
        if (creditAmount != null && !creditAmount.isEmpty()) {
            return creditAmount;
        }
        return String.valueOf(amount);
    }
}
