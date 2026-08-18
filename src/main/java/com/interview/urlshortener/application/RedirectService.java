package com.interview.urlshortener.application;

import com.interview.urlshortener.domain.ClickEvent;
import com.interview.urlshortener.domain.ShortUrl;
import com.interview.urlshortener.domain.exception.ShortUrlGoneException;
import com.interview.urlshortener.domain.exception.ShortUrlNotFoundException;
import com.interview.urlshortener.infrastructure.persistence.ClickEventRepository;
import com.interview.urlshortener.infrastructure.persistence.ShortUrlRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;

@Service
public class RedirectService {

    private final ShortUrlRepository shortUrlRepository;
    private final ClickEventRepository clickEventRepository;
    private final Clock clock;

    public RedirectService(
            ShortUrlRepository shortUrlRepository,
            ClickEventRepository clickEventRepository,
            Clock clock) {

        this.shortUrlRepository = shortUrlRepository;
        this.clickEventRepository = clickEventRepository;
        this.clock = clock;
    }

    @Transactional
    public String redirect(
            String shortCode,
            String referrer,
            String userAgent,
            String clientIp) {

        ShortUrl shortUrl = findByShortCode(shortCode);
        Instant now = clock.instant();

        if (!shortUrl.canRedirect(now)) {
            throw new ShortUrlGoneException(shortCode);
        }

        ClickEvent clickEvent = new ClickEvent(
                shortUrl,
                now,
                truncate(referrer, 1024),
                truncate(userAgent, 512),
                hashIp(clientIp));

        clickEventRepository.save(clickEvent);

        return shortUrl.getOriginalUrl();
    }

    @Cacheable(cacheNames = "shortUrls", key = "#shortCode")
    @Transactional(readOnly = true)
    public ShortUrl findByShortCode(String shortCode) {
        return shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() ->
                        new ShortUrlNotFoundException(shortCode));
    }

    private String truncate(String value, int maximumLength) {
        if (value == null) {
            return null;
        }

        return value.length() <= maximumLength
                ? value
                : value.substring(0, maximumLength);
    }

    private String hashIp(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return null;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(
                    clientIp.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 algorithm is unavailable", exception);
        }
    }
}