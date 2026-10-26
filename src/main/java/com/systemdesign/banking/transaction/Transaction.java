package com.systemdesign.banking.transaction;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;

@Document(collection = "transactions")
public class Transaction {

    @Id
    private String id;

    @Indexed
    private String transactionId;

    @Indexed
    private String transactionReference;

    @Indexed
    private String sourceAccountNumber;

    @Indexed
    private String destinationAccountNumber;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal amount;

    private String currency = "INR";
    private String type; // TRANSFER, BILL_PAYMENT, DEPOSIT
    private String status; // COMMITTED, FAILED, REJECTED

    @Indexed
    private String idempotencyKey;

    private Instant timestamp = Instant.now();
    private String description;
    private String failureReason;

    public Transaction() {
    }

    public Transaction(String transactionId, String transactionReference,
                       String sourceAccountNumber, String destinationAccountNumber,
                       BigDecimal amount, String currency, String type,
                       String status, String idempotencyKey, String description) {
        this.transactionId = transactionId;
        this.transactionReference = transactionReference;
        this.sourceAccountNumber = sourceAccountNumber;
        this.destinationAccountNumber = destinationAccountNumber;
        this.amount = amount;
        this.currency = currency != null ? currency : "INR";
        this.type = type;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
        this.description = description;
        this.timestamp = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public String getSourceAccountNumber() {
        return sourceAccountNumber;
    }

    public void setSourceAccountNumber(String sourceAccountNumber) {
        this.sourceAccountNumber = sourceAccountNumber;
    }

    public String getDestinationAccountNumber() {
        return destinationAccountNumber;
    }

    public void setDestinationAccountNumber(String destinationAccountNumber) {
        this.destinationAccountNumber = destinationAccountNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
}
