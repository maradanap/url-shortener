package com.interview.urlshortener.application;

import com.interview.urlshortener.api.dto.CreateShortUrlRequest;
import com.interview.urlshortener.api.dto.ShortUrlResponse;
import com.interview.urlshortener.domain.ShortUrl;
import com.interview.urlshortener.domain.exception.ShortCodeGenerationException;
import com.interview.urlshortener.domain.exception.ShortUrlConflictException;
import com.interview.urlshortener.domain.exception.ShortUrlNotFoundException;
import com.interview.urlshortener.infrastructure.persistence.ShortUrlRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class UrlShorteningService {

    private final ShortUrlRepository shortUrlRepository;
    private final ShortCodeGenerator shortCodeGenerator;
    private final UrlValidator urlValidator;
    private final Clock clock;
    private final String baseUrl;
    private final int collisionRetries;

    public UrlShorteningService(
            ShortUrlRepository shortUrlRepository,
            ShortCodeGenerator shortCodeGenerator,
            UrlValidator urlValidator,
            Clock clock,
            @Value("${app.base-url}") String baseUrl,
            @Value("${app.collision-retries:5}") int collisionRetries) {

        this.shortUrlRepository = shortUrlRepository;
        this.shortCodeGenerator = shortCodeGenerator;
        this.urlValidator = urlValidator;
        this.clock = clock;
        this.baseUrl = baseUrl;
        this.collisionRetries = collisionRetries;
    }

    @Transactional
    public ShortUrlResponse create(CreateShortUrlRequest request) {
        urlValidator.validate(request.url());

        Instant now = clock.instant();

        if (request.expiresAt() != null &&
                !request.expiresAt().isAfter(now)) {
            throw new IllegalArgumentException(
                    "Expiration time must be in the future");
        }

        String requestedAlias = normalizeAlias(request.customAlias());

        if (requestedAlias != null) {
            return createWithCustomAlias(request, requestedAlias, now);
        }

        return createWithGeneratedCode(request, now);
    }

    @Transactional(readOnly = true)
    public ShortUrlResponse getMetadata(String shortCode) {
        ShortUrl shortUrl = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() ->
                        new ShortUrlNotFoundException(shortCode));

        return toResponse(shortUrl);
    }

    @Transactional
    @CacheEvict(cacheNames = "shortUrls", key = "#shortCode")
    public void disable(String shortCode) {
        ShortUrl shortUrl = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() ->
                        new ShortUrlNotFoundException(shortCode));

        shortUrl.disable();
    }

    private ShortUrlResponse createWithCustomAlias(
            CreateShortUrlRequest request,
            String alias,
            Instant now) {

        if (shortUrlRepository.existsByShortCode(alias)) {
            throw new ShortUrlConflictException(alias);
        }

        try {
            ShortUrl saved = shortUrlRepository.saveAndFlush(
                    new ShortUrl(
                            alias,
                            request.url(),
                            now,
                            request.expiresAt()));

            return toResponse(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new ShortUrlConflictException(alias);
        }
    }

    private ShortUrlResponse createWithGeneratedCode(
            CreateShortUrlRequest request,
            Instant now) {

        for (int attempt = 0; attempt < collisionRetries; attempt++) {
            String generatedCode = shortCodeGenerator.generate();

            if (shortUrlRepository.existsByShortCode(generatedCode)) {
                continue;
            }

            try {
                ShortUrl saved = shortUrlRepository.saveAndFlush(
                        new ShortUrl(
                                generatedCode,
                                request.url(),
                                now,
                                request.expiresAt()));

                return toResponse(saved);
            } catch (DataIntegrityViolationException exception) {
                // Another request may have claimed the generated code.
            }
        }

        throw new ShortCodeGenerationException();
    }

    private String normalizeAlias(String alias) {
        if (alias == null || alias.isBlank()) {
            return null;
        }

        return alias;
    }

    private ShortUrlResponse toResponse(ShortUrl shortUrl) {
        return new ShortUrlResponse(
                shortUrl.getShortCode(),
                baseUrl + "/" + shortUrl.getShortCode(),
                shortUrl.getOriginalUrl(),
                shortUrl.getCreatedAt(),
                shortUrl.getExpiresAt(),
                shortUrl.isActive());
    }
}