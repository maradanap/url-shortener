package com.interview.urlshortener.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ShortUrlTest {

    private static final Instant NOW =
            Instant.parse("2026-08-18T15:00:00Z");

    @Test
    void activeUrlWithoutExpirationShouldRedirect() {
        ShortUrl shortUrl = new ShortUrl(
                "abc1234",
                "https://example.com",
                NOW.minusSeconds(60),
                null);

        assertThat(shortUrl.canRedirect(NOW)).isTrue();
        assertThat(shortUrl.isExpired(NOW)).isFalse();
    }

    @Test
    void activeUrlWithFutureExpirationShouldRedirect() {
        ShortUrl shortUrl = new ShortUrl(
                "abc1234",
                "https://example.com",
                NOW.minusSeconds(60),
                NOW.plusSeconds(3600));

        assertThat(shortUrl.canRedirect(NOW)).isTrue();
        assertThat(shortUrl.isExpired(NOW)).isFalse();
    }

    @Test
    void expiredUrlShouldNotRedirect() {
        ShortUrl shortUrl = new ShortUrl(
                "abc1234",
                "https://example.com",
                NOW.minusSeconds(7200),
                NOW.minusSeconds(60));

        assertThat(shortUrl.isExpired(NOW)).isTrue();
        assertThat(shortUrl.canRedirect(NOW)).isFalse();
    }

    @Test
    void expirationEqualToCurrentTimeShouldBeExpired() {
        ShortUrl shortUrl = new ShortUrl(
                "abc1234",
                "https://example.com",
                NOW.minusSeconds(60),
                NOW);

        assertThat(shortUrl.isExpired(NOW)).isTrue();
        assertThat(shortUrl.canRedirect(NOW)).isFalse();
    }

    @Test
    void disabledUrlShouldNotRedirect() {
        ShortUrl shortUrl = new ShortUrl(
                "abc1234",
                "https://example.com",
                NOW.minusSeconds(60),
                null);

        shortUrl.disable();

        assertThat(shortUrl.isActive()).isFalse();
        assertThat(shortUrl.canRedirect(NOW)).isFalse();
    }

    @Test
    void constructorShouldPopulateFields() {
        Instant expiration = NOW.plusSeconds(3600);

        ShortUrl shortUrl = new ShortUrl(
                "abc1234",
                "https://example.com",
                NOW,
                expiration);

        assertThat(shortUrl.getShortCode())
                .isEqualTo("abc1234");
        assertThat(shortUrl.getOriginalUrl())
                .isEqualTo("https://example.com");
        assertThat(shortUrl.getCreatedAt())
                .isEqualTo(NOW);
        assertThat(shortUrl.getExpiresAt())
                .isEqualTo(expiration);
        assertThat(shortUrl.isActive()).isTrue();
    }
}