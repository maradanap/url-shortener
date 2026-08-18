package com.interview.urlshortener.api;

import com.interview.urlshortener.application.RedirectService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@Validated
public class RedirectController {

    private static final String CODE_PATTERN =
            "^[A-Za-z0-9_-]{4,32}$";

    private final RedirectService redirectService;

    public RedirectController(RedirectService redirectService) {
        this.redirectService = redirectService;
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(
            @PathVariable
            @Pattern(regexp = CODE_PATTERN)
            String shortCode,
            HttpServletRequest request) {

        String originalUrl = redirectService.redirect(
                shortCode,
                request.getHeader("Referer"),
                request.getHeader("User-Agent"),
                request.getRemoteAddr());

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI.create(originalUrl))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .build();
    }
}