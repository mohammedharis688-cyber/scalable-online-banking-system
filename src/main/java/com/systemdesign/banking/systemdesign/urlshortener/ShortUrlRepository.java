package com.systemdesign.banking.systemdesign.urlshortener;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShortUrlRepository extends MongoRepository<ShortUrlMapping, String> {
    Optional<ShortUrlMapping> findByShortCode(String shortCode);
    Optional<ShortUrlMapping> findByTargetUrl(String targetUrl);
}
