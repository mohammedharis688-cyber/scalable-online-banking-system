package com.systemdesign.banking.systemdesign.urlshortener;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.time.Instant;

@Document(collection = "short_urls")
public class ShortUrlMapping implements Serializable {

    @Id
    private String id;

    @Indexed(unique = true)
    private String shortCode;

    private String targetUrl;
    private Instant createdAt = Instant.now();
    private long accessCount = 0;
    private Instant lastAccessed;

    public ShortUrlMapping() {
    }

    public ShortUrlMapping(String shortCode, String targetUrl) {
        this.shortCode = shortCode;
        this.targetUrl = targetUrl;
        this.createdAt = Instant.now();
        this.accessCount = 0;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public long getAccessCount() {
        return accessCount;
    }

    public void setAccessCount(long accessCount) {
        this.accessCount = accessCount;
    }

    public Instant getLastAccessed() {
        return lastAccessed;
    }

    public void setLastAccessed(Instant lastAccessed) {
        this.lastAccessed = lastAccessed;
    }
}
