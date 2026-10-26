package com.systemdesign.banking.beneficiary;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "beneficiaries")
public class Beneficiary {

    @Id
    private String id;

    @Indexed
    private String customerId;

    private String name;

    @Indexed
    private String accountNumber;

    private String bankName;
    private String email;
    private Instant createdAt = Instant.now();

    public Beneficiary() {
    }

    public Beneficiary(String customerId, String name, String accountNumber, String bankName, String email) {
        this.customerId = customerId;
        this.name = name;
        this.accountNumber = accountNumber;
        this.bankName = bankName != null ? bankName : "Apex Reserve Bank";
        this.email = email;
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
