package com.interview.urlshortener.application;

import com.interview.urlshortener.api.dto.AnalyticsResponse;
import com.interview.urlshortener.domain.exception.ShortUrlNotFoundException;
import com.interview.urlshortener.infrastructure.persistence.ClickEventRepository;
import com.interview.urlshortener.infrastructure.persistence.ShortUrlRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsService {

    private final ShortUrlRepository shortUrlRepository;
    private final ClickEventRepository clickEventRepository;

    public AnalyticsService(
            ShortUrlRepository shortUrlRepository,
            ClickEventRepository clickEventRepository) {

        this.shortUrlRepository = shortUrlRepository;
        this.clickEventRepository = clickEventRepository;
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalytics(String shortCode) {
        if (!shortUrlRepository.existsByShortCode(shortCode)) {
            throw new ShortUrlNotFoundException(shortCode);
        }

        return new AnalyticsResponse(
                shortCode,
                clickEventRepository.countByShortUrlShortCode(shortCode),
                clickEventRepository
                        .findLastAccessedAt(shortCode)
                        .orElse(null));
    }
}