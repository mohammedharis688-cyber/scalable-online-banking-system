package com.systemdesign.banking.billpayment;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;

@Document(collection = "billers")
public class Biller implements Serializable {

    @Id
    private String id;

    @Indexed(unique = true)
    private String billerCode;

    private String name;
    private String category; // ELECTRICITY, WATER, INTERNET, MOBILE
    private String description;
    private String syntheticAccount;

    public Biller() {
    }

    public Biller(String billerCode, String name, String category, String description, String syntheticAccount) {
        this.billerCode = billerCode;
        this.name = name;
        this.category = category;
        this.description = description;
        this.syntheticAccount = syntheticAccount;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBillerCode() {
        return billerCode;
    }

    public void setBillerCode(String billerCode) {
        this.billerCode = billerCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSyntheticAccount() {
        return syntheticAccount;
    }

    public void setSyntheticAccount(String syntheticAccount) {
        this.syntheticAccount = syntheticAccount;
    }
}
