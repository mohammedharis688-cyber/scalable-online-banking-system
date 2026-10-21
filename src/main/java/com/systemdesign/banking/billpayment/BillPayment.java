package com.systemdesign.banking.billpayment;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;

@Document(collection = "bill_payments")
public class BillPayment {

    @Id
    private String id;

    @Indexed
    private String paymentReference;

    @Indexed
    private String customerId;

    private String sourceAccountNumber;
    private String billerCode;
    private String billerName;
    private String consumerReference;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal amount;

    private String status = "COMMITTED";
    private Instant timestamp = Instant.now();

    public BillPayment() {
    }

    public BillPayment(String paymentReference, String customerId, String sourceAccountNumber,
                       String billerCode, String billerName, String consumerReference,
                       BigDecimal amount, String status) {
        this.paymentReference = paymentReference;
        this.customerId = customerId;
        this.sourceAccountNumber = sourceAccountNumber;
        this.billerCode = billerCode;
        this.billerName = billerName;
        this.consumerReference = consumerReference;
        this.amount = amount;
        this.status = status;
        this.timestamp = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getSourceAccountNumber() {
        return sourceAccountNumber;
    }

    public void setSourceAccountNumber(String sourceAccountNumber) {
        this.sourceAccountNumber = sourceAccountNumber;
    }

    public String getBillerCode() {
        return billerCode;
    }

    public void setBillerCode(String billerCode) {
        this.billerCode = billerCode;
    }

    public String getBillerName() {
        return billerName;
    }

    public void setBillerName(String billerName) {
        this.billerName = billerName;
    }

    public String getConsumerReference() {
        return consumerReference;
    }

    public void setConsumerReference(String consumerReference) {
        this.consumerReference = consumerReference;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
