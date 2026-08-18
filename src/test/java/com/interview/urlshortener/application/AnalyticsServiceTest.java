package com.interview.urlshortener.application;

import com.interview.urlshortener.api.dto.AnalyticsResponse;
import com.interview.urlshortener.domain.exception.ShortUrlNotFoundException;
import com.interview.urlshortener.infrastructure.persistence.ClickEventRepository;
import com.interview.urlshortener.infrastructure.persistence.ShortUrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private ShortUrlRepository shortUrlRepository;

    @Mock
    private ClickEventRepository clickEventRepository;

    private AnalyticsService service;

    @BeforeEach
    void setUp() {
        service = new AnalyticsService(
                shortUrlRepository,
                clickEventRepository);
    }

    @Test
    void shouldReturnAnalytics() {
        Instant lastAccess =
                Instant.parse("2026-08-18T15:30:00Z");

        when(shortUrlRepository
                .existsByShortCode("analytics1"))
                .thenReturn(true);

        when(clickEventRepository
                .countByShortUrlShortCode("analytics1"))
                .thenReturn(5L);

        when(clickEventRepository
                .findLastAccessedAt("analytics1"))
                .thenReturn(Optional.of(lastAccess));

        AnalyticsResponse response =
                service.getAnalytics("analytics1");

        assertThat(response.shortCode())
                .isEqualTo("analytics1");
        assertThat(response.totalClicks())
                .isEqualTo(5);
        assertThat(response.lastAccessedAt())
                .isEqualTo(lastAccess);
    }

    @Test
    void shouldReturnZeroWhenNoClicksExist() {
        when(shortUrlRepository
                .existsByShortCode("no-clicks"))
                .thenReturn(true);

        when(clickEventRepository
                .countByShortUrlShortCode("no-clicks"))
                .thenReturn(0L);

        when(clickEventRepository
                .findLastAccessedAt("no-clicks"))
                .thenReturn(Optional.empty());

        AnalyticsResponse response =
                service.getAnalytics("no-clicks");

        assertThat(response.totalClicks()).isZero();
        assertThat(response.lastAccessedAt()).isNull();
    }

    @Test
    void shouldThrowWhenShortCodeDoesNotExist() {
        when(shortUrlRepository
                .existsByShortCode("missing"))
                .thenReturn(false);

        assertThatThrownBy(() ->
                service.getAnalytics("missing"))
                .isInstanceOf(
                        ShortUrlNotFoundException.class);

        verify(clickEventRepository, never())
                .countByShortUrlShortCode("missing");
        verify(clickEventRepository, never())
                .findLastAccessedAt("missing");
    }
}