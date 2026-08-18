package com.interview.urlshortener.application;

import com.interview.urlshortener.domain.ShortUrl;
import com.interview.urlshortener.infrastructure.persistence.ClickEventRepository;
import com.interview.urlshortener.infrastructure.persistence.ShortUrlRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ShortUrlCacheIntegrationTest {

    @Autowired
    private ShortUrlLookupService shortUrlLookupService;

    @Autowired
    private UrlShorteningService urlShorteningService;

    @Autowired
    private ShortUrlRepository shortUrlRepository;

    @Autowired
    private ClickEventRepository clickEventRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        clearDatabaseAndCache();
    }

    @AfterEach
    void tearDown() {
        clearDatabaseAndCache();
    }

    @Test
    void repeatedLookupShouldUseCachedValue() {
        ShortUrl saved = shortUrlRepository.saveAndFlush(
                new ShortUrl(
                        "cache001",
                        "https://example.com",
                        Instant.now(),
                        null));

        ShortUrl first =
                shortUrlLookupService.findByShortCode(
                        "cache001");

        Cache cache = cacheManager.getCache("shortUrls");

        assertThat(cache).isNotNull();
        assertThat(cache.get("cache001")).isNotNull();

        shortUrlRepository.deleteAll();
        shortUrlRepository.flush();

        assertThat(shortUrlRepository
                .findByShortCode("cache001"))
                .isEmpty();

        ShortUrl second =
                shortUrlLookupService.findByShortCode(
                        "cache001");

        assertThat(first.getShortCode())
                .isEqualTo(saved.getShortCode());
        assertThat(second.getShortCode())
                .isEqualTo("cache001");
        assertThat(second.getOriginalUrl())
                .isEqualTo("https://example.com");
    }

    @Test
    void disablingUrlShouldEvictCacheEntry() {
        shortUrlRepository.saveAndFlush(
                new ShortUrl(
                        "cache002",
                        "https://example.com",
                        Instant.now(),
                        null));

        shortUrlLookupService.findByShortCode(
                "cache002");

        Cache cache = cacheManager.getCache("shortUrls");

        assertThat(cache).isNotNull();
        assertThat(cache.get("cache002")).isNotNull();

        urlShorteningService.disable("cache002");

        assertThat(cache.get("cache002")).isNull();

        ShortUrl updated =
                shortUrlRepository
                        .findByShortCode("cache002")
                        .orElseThrow();

        assertThat(updated.isActive()).isFalse();
    }

    private void clearDatabaseAndCache() {
        clickEventRepository.deleteAll();
        shortUrlRepository.deleteAll();

        Cache cache = cacheManager.getCache("shortUrls");

        if (cache != null) {
            cache.clear();
        }
    }
}