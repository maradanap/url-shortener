package com.interview.urlshortener.api;

import com.interview.urlshortener.api.dto.AnalyticsResponse;
import com.interview.urlshortener.api.dto.CreateShortUrlRequest;
import com.interview.urlshortener.api.dto.ShortUrlResponse;
import com.interview.urlshortener.application.AnalyticsService;
import com.interview.urlshortener.application.UrlShorteningService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/urls")
@Validated
public class UrlController {

    private static final String CODE_PATTERN =
            "^[A-Za-z0-9_-]{4,32}$";

    private final UrlShorteningService urlShorteningService;
    private final AnalyticsService analyticsService;

    public UrlController(
            UrlShorteningService urlShorteningService,
            AnalyticsService analyticsService) {

        this.urlShorteningService = urlShorteningService;
        this.analyticsService = analyticsService;
    }

    @PostMapping
    public ResponseEntity<ShortUrlResponse> create(
            @Valid @RequestBody CreateShortUrlRequest request) {

        ShortUrlResponse response =
                urlShorteningService.create(request);

        return ResponseEntity
                .created(URI.create(response.shortUrl()))
                .body(response);
    }

    @GetMapping("/{shortCode}")
    public ShortUrlResponse getMetadata(
            @PathVariable
            @Pattern(regexp = CODE_PATTERN)
            String shortCode) {

        return urlShorteningService.getMetadata(shortCode);
    }

    @GetMapping("/{shortCode}/analytics")
    public AnalyticsResponse getAnalytics(
            @PathVariable
            @Pattern(regexp = CODE_PATTERN)
            String shortCode) {

        return analyticsService.getAnalytics(shortCode);
    }

    @DeleteMapping("/{shortCode}")
    public ResponseEntity<Void> disable(
            @PathVariable
            @Pattern(regexp = CODE_PATTERN)
            String shortCode) {

        urlShorteningService.disable(shortCode);

        return ResponseEntity.noContent()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .build();
    }
}