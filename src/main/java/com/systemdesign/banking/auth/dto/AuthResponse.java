package com.systemdesign.banking.auth.dto;

public class AuthResponse {

    private String token;
    private String tokenType = "Bearer";
    private String customerId;
    private String email;
    private String fullName;
    private String role;
    private String primaryAccountNumber;

    public AuthResponse() {
    }

    public AuthResponse(String token, String customerId, String email, String fullName, String role, String primaryAccountNumber) {
        this.token = token;
        this.tokenType = "Bearer";
        this.customerId = customerId;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.primaryAccountNumber = primaryAccountNumber;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPrimaryAccountNumber() {
        return primaryAccountNumber;
    }

    public void setPrimaryAccountNumber(String primaryAccountNumber) {
        this.primaryAccountNumber = primaryAccountNumber;
    }
}
