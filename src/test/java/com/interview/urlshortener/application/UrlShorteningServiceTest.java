package com.interview.urlshortener.application;

import com.interview.urlshortener.api.dto.CreateShortUrlRequest;
import com.interview.urlshortener.api.dto.ShortUrlResponse;
import com.interview.urlshortener.domain.ShortUrl;
import com.interview.urlshortener.domain.exception.ShortCodeGenerationException;
import com.interview.urlshortener.domain.exception.ShortUrlConflictException;
import com.interview.urlshortener.domain.exception.ShortUrlNotFoundException;
import com.interview.urlshortener.infrastructure.persistence.ShortUrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UrlShorteningServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-08-18T15:00:00Z");

    @Mock
    private ShortUrlRepository shortUrlRepository;

    @Mock
    private ShortCodeGenerator shortCodeGenerator;

    @Mock
    private UrlValidator urlValidator;

    private UrlShorteningService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

        service = new UrlShorteningService(
                shortUrlRepository,
                shortCodeGenerator,
                urlValidator,
                clock,
                "http://localhost:8080",
                3);
    }

    @Test
    void shouldCreateUrlWithCustomAlias() {
        CreateShortUrlRequest request =
                new CreateShortUrlRequest(
                        "https://example.com",
                        "custom-01",
                        NOW.plusSeconds(3600));

        when(shortUrlRepository
                .existsByShortCode("custom-01"))
                .thenReturn(false);

        when(shortUrlRepository
                .saveAndFlush(any(ShortUrl.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        ShortUrlResponse response =
                service.create(request);

        assertThat(response.shortCode())
                .isEqualTo("custom-01");
        assertThat(response.shortUrl())
                .isEqualTo(
                        "http://localhost:8080/custom-01");
        assertThat(response.originalUrl())
                .isEqualTo("https://example.com");
        assertThat(response.createdAt())
                .isEqualTo(NOW);
        assertThat(response.expiresAt())
                .isEqualTo(NOW.plusSeconds(3600));
        assertThat(response.active()).isTrue();

        verify(urlValidator)
                .validate("https://example.com");
        verify(shortCodeGenerator, never()).generate();
    }

    @Test
    void shouldCreateUrlWithGeneratedCode() {
        CreateShortUrlRequest request =
                new CreateShortUrlRequest(
                        "https://example.com",
                        null,
                        null);

        when(shortCodeGenerator.generate())
                .thenReturn("Ab12Cd3");
        when(shortUrlRepository
                .existsByShortCode("Ab12Cd3"))
                .thenReturn(false);
        when(shortUrlRepository
                .saveAndFlush(any(ShortUrl.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        ShortUrlResponse response =
                service.create(request);

        assertThat(response.shortCode())
                .isEqualTo("Ab12Cd3");
        assertThat(response.shortUrl())
                .isEqualTo(
                        "http://localhost:8080/Ab12Cd3");
        assertThat(response.originalUrl())
                .isEqualTo("https://example.com");
    }

    @Test
    void shouldRejectExistingCustomAlias() {
        CreateShortUrlRequest request =
                new CreateShortUrlRequest(
                        "https://example.com",
                        "existing-01",
                        null);

        when(shortUrlRepository
                .existsByShortCode("existing-01"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(
                        ShortUrlConflictException.class)
                .hasMessageContaining("existing-01");

        verify(shortUrlRepository, never())
                .saveAndFlush(any(ShortUrl.class));
    }

    @Test
    void shouldConvertDatabaseConstraintViolationToConflict() {
        CreateShortUrlRequest request =
                new CreateShortUrlRequest(
                        "https://example.com",
                        "race-alias",
                        null);

        when(shortUrlRepository
                .existsByShortCode("race-alias"))
                .thenReturn(false);

        when(shortUrlRepository
                .saveAndFlush(any(ShortUrl.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "duplicate short code"));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(
                        ShortUrlConflictException.class)
                .hasMessageContaining("race-alias");
    }

    @Test
    void shouldRejectPastExpiration() {
        CreateShortUrlRequest request =
                new CreateShortUrlRequest(
                        "https://example.com",
                        "expired-01",
                        NOW.minusSeconds(1));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "Expiration time must be in the future");

        verify(shortUrlRepository, never())
                .saveAndFlush(any(ShortUrl.class));
    }

    @Test
    void shouldRetryWhenGeneratedCodeAlreadyExists() {
        CreateShortUrlRequest request =
                new CreateShortUrlRequest(
                        "https://example.com",
                        null,
                        null);

        when(shortCodeGenerator.generate())
                .thenReturn("AAAAAAA", "BBBBBBB");

        when(shortUrlRepository
                .existsByShortCode("AAAAAAA"))
                .thenReturn(true);

        when(shortUrlRepository
                .existsByShortCode("BBBBBBB"))
                .thenReturn(false);

        when(shortUrlRepository
                .saveAndFlush(any(ShortUrl.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        ShortUrlResponse response =
                service.create(request);

        assertThat(response.shortCode())
                .isEqualTo("BBBBBBB");

        verify(shortCodeGenerator, times(2)).generate();
    }

    @Test
    void shouldFailAfterCollisionRetriesAreExhausted() {
        CreateShortUrlRequest request =
                new CreateShortUrlRequest(
                        "https://example.com",
                        null,
                        null);

        when(shortCodeGenerator.generate())
                .thenReturn(
                        "AAAAAAA",
                        "BBBBBBB",
                        "CCCCCCC");

        when(shortUrlRepository
                .existsByShortCode(any(String.class)))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(
                        ShortCodeGenerationException.class);

        verify(shortCodeGenerator, times(3)).generate();
        verify(shortUrlRepository, never())
                .saveAndFlush(any(ShortUrl.class));
    }

    @Test
    void shouldReturnMetadata() {
        ShortUrl shortUrl = new ShortUrl(
                "meta-001",
                "https://example.com",
                NOW,
                null);

        when(shortUrlRepository
                .findByShortCode("meta-001"))
                .thenReturn(Optional.of(shortUrl));

        ShortUrlResponse response =
                service.getMetadata("meta-001");

        assertThat(response.shortCode())
                .isEqualTo("meta-001");
        assertThat(response.originalUrl())
                .isEqualTo("https://example.com");
        assertThat(response.active()).isTrue();
    }

    @Test
    void shouldThrowWhenMetadataDoesNotExist() {
        when(shortUrlRepository
                .findByShortCode("missing"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.getMetadata("missing"))
                .isInstanceOf(
                        ShortUrlNotFoundException.class);
    }

    @Test
    void shouldDisableExistingUrl() {
        ShortUrl shortUrl = new ShortUrl(
                "disable1",
                "https://example.com",
                NOW,
                null);

        when(shortUrlRepository
                .findByShortCode("disable1"))
                .thenReturn(Optional.of(shortUrl));

        service.disable("disable1");

        assertThat(shortUrl.isActive()).isFalse();
    }

    @Test
    void shouldThrowWhenDisablingUnknownUrl() {
        when(shortUrlRepository
                .findByShortCode("missing"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.disable("missing"))
                .isInstanceOf(
                        ShortUrlNotFoundException.class);
    }
}