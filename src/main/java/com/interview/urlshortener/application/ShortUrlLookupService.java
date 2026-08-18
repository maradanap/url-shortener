package com.interview.urlshortener.application;

import com.interview.urlshortener.domain.ShortUrl;
import com.interview.urlshortener.domain.exception.ShortUrlNotFoundException;
import com.interview.urlshortener.infrastructure.persistence.ShortUrlRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShortUrlLookupService {

    private final ShortUrlRepository shortUrlRepository;

    public ShortUrlLookupService(
            ShortUrlRepository shortUrlRepository) {
        this.shortUrlRepository = shortUrlRepository;
    }

    @Cacheable(cacheNames = "shortUrls", key = "#shortCode")
    @Transactional(readOnly = true)
    public ShortUrl findByShortCode(String shortCode) {
        return shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() ->
                        new ShortUrlNotFoundException(shortCode));
    }
}