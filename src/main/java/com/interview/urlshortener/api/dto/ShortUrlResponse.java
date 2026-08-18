package com.interview.urlshortener.api.dto;

import java.time.Instant;

public record ShortUrlResponse(
        String shortCode,
        String shortUrl,
        String originalUrl,
        Instant createdAt,
        Instant expiresAt,
        boolean active
) {
}