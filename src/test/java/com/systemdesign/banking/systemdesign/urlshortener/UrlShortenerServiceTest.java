package com.systemdesign.banking.systemdesign.urlshortener;

import com.systemdesign.banking.systemdesign.cache.RedisCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlShortenerServiceTest {

    @Mock
    private ShortUrlRepository repository;

    @Mock
    private RedisCacheService cacheService;

    private UrlShortenerService shortenerService;

    @BeforeEach
    void setUp() {
        shortenerService = new UrlShortenerService(repository, cacheService);
    }

    @Test
    @DisplayName("Should successfully shorten permitted local public route")
    void testShortenPermittedRoute() {
        String targetUrl = "/synthetic/page1.html";
        when(repository.findByTargetUrl(targetUrl)).thenReturn(Optional.empty());
        when(repository.findByShortCode(any())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ShortUrlMapping mapping = shortenerService.shortenUrl(targetUrl);

        assertNotNull(mapping);
        assertEquals(targetUrl, mapping.getTargetUrl());
        assertEquals(6, mapping.getShortCode().length());
        verify(cacheService, times(1)).put(eq("cache:shorturl:" + mapping.getShortCode()), eq(targetUrl), anyLong());
    }

    @Test
    @DisplayName("Should reject shortening private or external URLs to prevent open-redirects")
    void testRejectNonSyntheticRoute() {
        assertThrows(IllegalArgumentException.class, () -> shortenerService.shortenUrl("https://malicious.com"));
        assertThrows(IllegalArgumentException.class, () -> shortenerService.shortenUrl("/api/accounts/secret"));
    }
}
