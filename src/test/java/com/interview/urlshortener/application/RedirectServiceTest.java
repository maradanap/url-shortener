package com.interview.urlshortener.application;

import com.interview.urlshortener.domain.ClickEvent;
import com.interview.urlshortener.domain.ShortUrl;
import com.interview.urlshortener.domain.exception.ShortUrlGoneException;
import com.interview.urlshortener.domain.exception.ShortUrlNotFoundException;
import com.interview.urlshortener.infrastructure.persistence.ClickEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedirectServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-08-18T15:00:00Z");

    @Mock
    private ShortUrlLookupService shortUrlLookupService;

    @Mock
    private ClickEventRepository clickEventRepository;

    private RedirectService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

        service = new RedirectService(
                clickEventRepository,
                clock,
                shortUrlLookupService);
    }

    @Test
    void shouldReturnDestinationAndRecordClick() {
        ShortUrl shortUrl = new ShortUrl(
                "redirect1",
                "https://example.com/products/123",
                NOW.minusSeconds(60),
                NOW.plusSeconds(3600));

        when(shortUrlLookupService
                .findByShortCode("redirect1"))
                .thenReturn(shortUrl);

        String destination = service.redirect(
                "redirect1",
                "https://referrer.example",
                "PostmanRuntime/7.0",
                "192.0.2.10");

        assertThat(destination)
                .isEqualTo(
                        "https://example.com/products/123");

        ArgumentCaptor<ClickEvent> captor =
                ArgumentCaptor.forClass(ClickEvent.class);

        verify(clickEventRepository)
                .save(captor.capture());

        ClickEvent event = captor.getValue();

        assertThat(event.getShortUrl())
                .isSameAs(shortUrl);
        assertThat(event.getClickedAt())
                .isEqualTo(NOW);
        assertThat(event.getReferrer())
                .isEqualTo("https://referrer.example");
        assertThat(event.getUserAgent())
                .isEqualTo("PostmanRuntime/7.0");
        assertThat(event.getIpHash())
                .hasSize(64)
                .doesNotContain("192.0.2.10");
    }

    @Test
    void shouldNotRecordClickForExpiredUrl() {
        ShortUrl shortUrl = new ShortUrl(
                "expired1",
                "https://example.com",
                NOW.minusSeconds(7200),
                NOW.minusSeconds(1));

        when(shortUrlLookupService
                .findByShortCode("expired1"))
                .thenReturn(shortUrl);

        assertThatThrownBy(() ->
                service.redirect(
                        "expired1",
                        null,
                        null,
                        "192.0.2.10"))
                .isInstanceOf(
                        ShortUrlGoneException.class);

        verify(clickEventRepository, never())
                .save(any(ClickEvent.class));
    }

    @Test
    void shouldTreatExactExpirationAsGone() {
        ShortUrl shortUrl = new ShortUrl(
                "expired2",
                "https://example.com",
                NOW.minusSeconds(60),
                NOW);

        when(shortUrlLookupService
                .findByShortCode("expired2"))
                .thenReturn(shortUrl);

        assertThatThrownBy(() ->
                service.redirect(
                        "expired2",
                        null,
                        null,
                        "192.0.2.10"))
                .isInstanceOf(
                        ShortUrlGoneException.class);

        verify(clickEventRepository, never())
                .save(any(ClickEvent.class));
    }

    @Test
    void shouldNotRecordClickForDisabledUrl() {
        ShortUrl shortUrl = new ShortUrl(
                "disabled",
                "https://example.com",
                NOW.minusSeconds(60),
                null);

        shortUrl.disable();

        when(shortUrlLookupService
                .findByShortCode("disabled"))
                .thenReturn(shortUrl);

        assertThatThrownBy(() ->
                service.redirect(
                        "disabled",
                        null,
                        null,
                        "192.0.2.10"))
                .isInstanceOf(
                        ShortUrlGoneException.class);

        verify(clickEventRepository, never())
                .save(any(ClickEvent.class));
    }

    @Test
    void shouldPropagateNotFoundException() {
        when(shortUrlLookupService
                .findByShortCode("missing"))
                .thenThrow(
                        new ShortUrlNotFoundException(
                                "missing"));

        assertThatThrownBy(() ->
                service.redirect(
                        "missing",
                        null,
                        null,
                        "192.0.2.10"))
                .isInstanceOf(
                        ShortUrlNotFoundException.class);

        verify(clickEventRepository, never())
                .save(any(ClickEvent.class));
    }

    @Test
    void shouldSupportMissingOptionalRequestInformation() {
        ShortUrl shortUrl = new ShortUrl(
                "optional",
                "https://example.com",
                NOW.minusSeconds(60),
                null);

        when(shortUrlLookupService
                .findByShortCode("optional"))
                .thenReturn(shortUrl);

        service.redirect(
                "optional",
                null,
                null,
                null);

        ArgumentCaptor<ClickEvent> captor =
                ArgumentCaptor.forClass(ClickEvent.class);

        verify(clickEventRepository)
                .save(captor.capture());

        assertThat(captor.getValue().getReferrer()).isNull();
        assertThat(captor.getValue().getUserAgent()).isNull();
        assertThat(captor.getValue().getIpHash()).isNull();
    }
}