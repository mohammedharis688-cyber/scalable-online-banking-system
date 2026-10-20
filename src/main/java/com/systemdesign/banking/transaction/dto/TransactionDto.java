package com.systemdesign.banking.transaction.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class TransactionDto {

    private String transactionReference;
    private Instant timestamp;
    private String type;
    private BigDecimal amount;
    private String direction; // DEBIT or CREDIT relative to user
    private String sourceAccount;
    private String destinationAccount;
    private String status;
    private String description;

    public TransactionDto() {
    }

    public TransactionDto(String transactionReference, Instant timestamp, String type,
                          BigDecimal amount, String direction, String sourceAccount,
                          String destinationAccount, String status, String description) {
        this.transactionReference = transactionReference;
        this.timestamp = timestamp;
        this.type = type;
        this.amount = amount;
        this.direction = direction;
        this.sourceAccount = sourceAccount;
        this.destinationAccount = destinationAccount;
        this.status = status;
        this.description = description;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getSourceAccount() {
        return sourceAccount;
    }

    public void setSourceAccount(String sourceAccount) {
        this.sourceAccount = sourceAccount;
    }

    public String getDestinationAccount() {
        return destinationAccount;
    }

    public void setDestinationAccount(String destinationAccount) {
        this.destinationAccount = destinationAccount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
