package com.interview.urlshortener.application;

import com.interview.urlshortener.domain.ShortUrl;
import com.interview.urlshortener.domain.exception.ShortUrlNotFoundException;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShortUrlLookupServiceTest {

    @Mock
    private ShortUrlRepository shortUrlRepository;

    private ShortUrlLookupService service;

    @BeforeEach
    void setUp() {
        service = new ShortUrlLookupService(
                shortUrlRepository);
    }

    @Test
    void shouldReturnExistingShortUrl() {
        ShortUrl shortUrl = new ShortUrl(
                "lookup01",
                "https://example.com",
                Instant.parse("2026-08-18T15:00:00Z"),
                null);

        when(shortUrlRepository
                .findByShortCode("lookup01"))
                .thenReturn(Optional.of(shortUrl));

        ShortUrl result =
                service.findByShortCode("lookup01");

        assertThat(result).isSameAs(shortUrl);
    }

    @Test
    void shouldThrowWhenShortUrlDoesNotExist() {
        when(shortUrlRepository
                .findByShortCode("missing"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.findByShortCode("missing"))
                .isInstanceOf(
                        ShortUrlNotFoundException.class)
                .hasMessageContaining("missing");
    }
}